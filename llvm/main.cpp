// Apply generated seam contracts to actual LLVM 23 bitcode, before ThinLTO indexing.
#include "llvm/ADT/SmallVector.h"
#include "llvm/ADT/StringExtras.h"
#include "llvm/Analysis/ModuleSummaryAnalysis.h"
#include "llvm/Analysis/ProfileSummaryInfo.h"
#include "llvm/Bitcode/BitcodeReader.h"
#include "llvm/Bitcode/BitcodeWriter.h"
#include "llvm/BinaryFormat/Magic.h"
#include "llvm/IR/Attributes.h"
#include "llvm/IR/Constants.h"
#include "llvm/IR/Instructions.h"
#include "llvm/IR/LLVMContext.h"
#include "llvm/IR/Metadata.h"
#include "llvm/IR/Module.h"
#include "llvm/IR/ModuleSummaryIndex.h"
#include "llvm/IR/Verifier.h"
#include "llvm/IRReader/IRReader.h"
#include "llvm/Object/Archive.h"
#include "llvm/Object/ArchiveWriter.h"
#include "llvm/Support/CommandLine.h"
#include "llvm/Support/Error.h"
#include "llvm/Support/FileSystem.h"
#include "llvm/Support/MemoryBuffer.h"
#include "llvm/Support/SourceMgr.h"
#include "llvm/Support/SHA256.h"
#include "llvm/Support/ToolOutputFile.h"
#include "llvm/TargetParser/Triple.h"
#include "llvm/Transforms/Utils/AssignGUID.h"
#include <memory>
#include <deque>
#include <string>
#include <vector>

using namespace llvm;
static cl::opt<std::string> Contracts("contracts", cl::Required, cl::desc("Generated seam.ll"));
static cl::opt<std::string> Input("input", cl::Required, cl::desc("Bitcode object or regular archive"));
static cl::opt<std::string> Output("output", cl::Required, cl::desc("Rewritten object/archive (distinct path)"));
static unsigned Matched = 0, Calls = 0, Modules = 0, PreservedMembers = 0;

static Error fail(const Twine &Message) {
  return createStringError(inconvertibleErrorCode(), Message);
}
static Expected<std::string> fingerprint(const Module &M, StringRef Key) {
  auto *MD = M.getNamedMetadata(Key);
  if (!MD || MD->getNumOperands() != 1 || MD->getOperand(0)->getNumOperands() != 1)
    return fail("missing or malformed contract fingerprint: " + Key);
  auto *S = dyn_cast<MDString>(MD->getOperand(0)->getOperand(0));
  if (!S || S->getString().size() != 64 || !llvm::all_of(S->getString(), [](char C) {
        return (C >= '0' && C <= '9') || (C >= 'a' && C <= 'f');
      })) return fail("invalid SHA-256 contract fingerprint");
  return S->getString().str();
}
static Error validateAttributes(AttributeSet Attrs, bool FunctionLevel) {
  for (Attribute A : Attrs) {
    if (A.isStringAttribute()) return fail("string attributes are not accepted as seam contracts");
    switch (A.getKindAsEnum()) {
    case Attribute::NoUnwind: case Attribute::NoReturn:
    case Attribute::WillReturn: case Attribute::NoFree:
      if (!FunctionLevel) return fail("function contract on parameter");
      break;
    case Attribute::NonNull: case Attribute::Captures:
    case Attribute::ReadOnly: case Attribute::WriteOnly: case Attribute::NoAlias:
    case Attribute::Alignment: case Attribute::Dereferenceable:
    case Attribute::SExt: case Attribute::ZExt:
      if (FunctionLevel) return fail("parameter contract on function");
      break;
    default: return fail("unsupported seam attribute: " + A.getAsString());
    }
  }
  return Error::success();
}
static Error ordinaryABI(AttributeSet Attrs) {
  for (auto Kind : {Attribute::ByVal, Attribute::ByRef, Attribute::StructRet,
                    Attribute::InAlloca, Attribute::Preallocated, Attribute::InReg,
                    Attribute::Nest, Attribute::SwiftSelf, Attribute::SwiftError,
                    Attribute::SwiftAsync})
    if (Attrs.hasAttribute(Kind)) return fail("unsupported ABI attribute: " + Attribute::getNameFromAttrKind(Kind));
  return Error::success();
}
static bool supportedType(Type *T, bool Return) {
  if (T->isVoidTy()) return Return;
  if (auto *P = dyn_cast<PointerType>(T)) return P->getAddressSpace() == 0;
  return T->isIntegerTy(1) || T->isIntegerTy(8) || T->isIntegerTy(16) || T->isIntegerTy(32) ||
         T->isIntegerTy(64) || T->isFloatTy() || T->isDoubleTy();
}
// Both the compiler's facts and the contract's hold, so equal kinds merge to the stronger fact.
static Expected<SmallVector<Attribute, 8>> merge(LLVMContext &Ctx, AttributeSet Existing, AttributeSet Incoming) {
  for (auto Pair : {std::make_pair(Attribute::ReadOnly, Attribute::WriteOnly),
                    std::make_pair(Attribute::NoReturn, Attribute::WillReturn)}) {
    if ((Existing.hasAttribute(Pair.first) && Incoming.hasAttribute(Pair.second)) ||
        (Existing.hasAttribute(Pair.second) && Incoming.hasAttribute(Pair.first)))
      return fail("existing attributes contradict seam contracts");
  }
  SmallVector<Attribute, 8> Out;
  for (Attribute A : Incoming) {
    auto Kind = A.getKindAsEnum();
    if (Kind == Attribute::SExt || Kind == Attribute::ZExt) continue;
    if ((Kind == Attribute::ReadOnly || Kind == Attribute::WriteOnly) && Existing.hasAttribute(Attribute::ReadNone))
      continue;
    Attribute Old = Existing.getAttribute(Kind);
    if (!A.isIntAttribute() || !Old.isValid() || Old == A) {
      Out.push_back(A);
      continue;
    }
    switch (Kind) {
    case Attribute::Captures:
      Out.push_back(Attribute::getWithCaptureInfo(Ctx, Old.getCaptureInfo() & A.getCaptureInfo()));
      break;
    case Attribute::Alignment:
      Out.push_back(Old.getAlignment().valueOrOne() >= A.getAlignment().valueOrOne() ? Old : A);
      break;
    case Attribute::Dereferenceable:
      Out.push_back(Old.getDereferenceableBytes() >= A.getDereferenceableBytes() ? Old : A);
      break;
    default:
      return fail("parameterized contract conflicts with existing attribute: " + A.getAsString() + " vs " +
                  Old.getAsString());
    }
  }
  return Out;
}
static Error annotate(Function &F, CallBase *Call, const Function &Contract) {
  auto Attrs = Call ? Call->getAttributes() : F.getAttributes();
  auto In = Contract.getAttributes();
  auto Fn = merge(F.getContext(), Attrs.getFnAttrs(), In.getFnAttrs());
  if (!Fn) return joinErrors(fail(F.getName() + ":"), Fn.takeError());
  std::vector<SmallVector<Attribute, 8>> Params;
  for (unsigned I = 0; I < F.arg_size(); ++I) {
    // Extension is ABI, not an optimizer fact: clang and rustc agree on parameters, so a difference
    // means the producer and the descriptor disagree on signedness or target.
    auto Ext = [](AttributeSet A) {
      return A.hasAttribute(Attribute::SExt) ? 1 : A.hasAttribute(Attribute::ZExt) ? 2 : 0;
    };
    if (Ext(Attrs.getParamAttrs(I)) != Ext(In.getParamAttrs(I)))
      return fail(F.getName() + " argument " + Twine(I) + ": ABI extension mismatch (signedness or target)");
    auto P = merge(F.getContext(), Attrs.getParamAttrs(I), In.getParamAttrs(I));
    if (!P) return joinErrors(fail(F.getName() + " argument " + Twine(I) + ":"), P.takeError());
    Params.push_back(std::move(*P));
  }
  for (Attribute A : *Fn) Call ? Call->addFnAttr(A) : F.addFnAttr(A);
  for (unsigned I = 0; I < F.arg_size(); ++I)
    for (Attribute A : Params[I]) Call ? Call->addParamAttr(I, A) : F.addParamAttr(I, A);
  return Error::success();
}
static Error validateContracts(const Module &C) {
  if (C.getTargetTriple().empty()) return fail("contract module needs a target triple");
  if (!C.global_empty() || !C.alias_empty() || !C.ifunc_empty())
    return fail("contract module may only contain function declarations");
  for (const Function &F : C) {
    if (!F.isDeclaration() || F.isIntrinsic() || F.isVarArg() || F.getCallingConv() != CallingConv::C)
      return fail("unsupported contract function: " + F.getName());
    if (!supportedType(F.getReturnType(), true)) return fail("unsupported return ABI type");
    if (F.getAttributes().getRetAttrs().hasAttributes()) return fail("return contracts are not supported yet");
    if (auto E = validateAttributes(F.getAttributes().getFnAttrs(), true)) return E;
    for (unsigned I = 0; I < F.arg_size(); ++I) {
      if (!supportedType(F.getFunctionType()->getParamType(I), false)) return fail("unsupported parameter ABI type");
      auto Attrs = F.getAttributes().getParamAttrs(I);
      Type *T = F.getFunctionType()->getParamType(I);
      for (Attribute A : Attrs) {
        bool Ext = A.hasKindAsEnum() && (A.getKindAsEnum() == Attribute::SExt || A.getKindAsEnum() == Attribute::ZExt);
        if (Ext && !(T->isIntegerTy() && T->getIntegerBitWidth() < 32))
          return fail("extension on a non-narrow argument");
        if (!Ext && !T->isPointerTy()) return fail("pointer contracts on non-pointer argument");
      }
      if (auto E = validateAttributes(Attrs, false)) return E;
    }
  }
  return Error::success();
}
static Error apply(Module &M, const Module &C, StringRef Hash) {
  Triple Actual(M.getTargetTriple()), Expected(C.getTargetTriple());
  if (Actual.getArch() != Expected.getArch() || (Actual.getOS() != Expected.getOS() && !(Actual.isMacOSX() && Expected.isMacOSX())) ||
      Actual.getEnvironment() != Expected.getEnvironment() || Actual.getObjectFormat() != Expected.getObjectFormat())
    return fail("target mismatch: " + M.getTargetTriple().str() + " versus " + C.getTargetTriple().str());
  if (!M.getDataLayoutStr().empty() && M.getDataLayout().getPointerSizeInBits() != 64)
    return fail("expected 64-bit pointers");
  if (M.getNamedMetadata("svmgen.applied")) {
    auto Old = fingerprint(M, "svmgen.applied");
    if (!Old) return Old.takeError();
    if (*Old != Hash) return fail("object already has different seam contracts; rebuild from original compiler output");
  }
  bool Changed = false;
  for (const Function &Contract : C) {
    auto *Value = M.getNamedValue(Contract.getName());
    if (!Value) continue;
    auto *F = dyn_cast<Function>(Value);
    if (!F) return fail("contract symbol names an alias or non-function: " + Contract.getName());
    if (F->hasLocalLinkage() || F->getAddressSpace() != Contract.getAddressSpace() ||
        F->getFunctionType() != Contract.getFunctionType() || F->getCallingConv() != Contract.getCallingConv())
      return fail("ABI signature mismatch for " + F->getName());
    if (auto E = ordinaryABI(F->getAttributes().getRetAttrs())) return E;
    for (unsigned I = 0; I < F->arg_size(); ++I)
      if (auto E = ordinaryABI(F->getAttributes().getParamAttrs(I))) return E;
    if (auto E = annotate(*F, nullptr, Contract)) return E;
    ++Matched; Changed = true;
    for (Function &Caller : M) for (BasicBlock &BB : Caller) for (Instruction &Inst : BB) {
      auto *Call = dyn_cast<CallBase>(&Inst);
      if (!Call || Call->getCalledOperand()->stripPointerCasts() != F) continue;
      if (Call->getFunctionType() != F->getFunctionType() || Call->getCallingConv() != F->getCallingConv())
        return fail("direct call ABI mismatch for " + F->getName());
      if (auto E = ordinaryABI(Call->getAttributes().getRetAttrs())) return E;
      for (unsigned I = 0; I < F->arg_size(); ++I)
        if (auto E = ordinaryABI(Call->getAttributes().getParamAttrs(I))) return E;
      if (auto E = annotate(*F, Call, Contract)) return E;
      ++Calls;
    }
  }
  if (Changed) {
    if (!M.getNamedMetadata("svmgen.applied"))
      M.getOrInsertNamedMetadata("svmgen.applied")->addOperand(MDNode::get(M.getContext(), MDString::get(M.getContext(), Hash)));
    if (verifyModule(M, &errs())) return fail("rewritten module failed LLVM verification");
    ++Modules;
  }
  return Error::success();
}
static Expected<std::unique_ptr<MemoryBuffer>> rewrite(MemoryBufferRef Buffer, LLVMContext &Context,
                                                      const Module &Contract, StringRef Hash) {
  auto Parsed = parseBitcodeFile(Buffer, Context);
  if (!Parsed) return Parsed.takeError();
  Module &M = **Parsed;
  unsigned Before = Matched;
  if (auto E = apply(M, Contract, Hash)) return E;
  if (Before == Matched) return MemoryBuffer::getMemBufferCopy(Buffer.getBuffer(), Buffer.getBufferIdentifier());
  // Recompute both summary and module hash. Reusing old summaries/cache keys is incorrect.
  // Producers without a GUID pass (llvm-as, older tools) leave definitions unassigned.
  AssignGUIDPass::runOnModule(M);
  ProfileSummaryInfo PSI(M);
  auto Index = buildModuleSummaryIndex(M, nullptr, &PSI);
  SmallVector<char, 0> Bytes;
  raw_svector_ostream Stream(Bytes);
  WriteBitcodeToFile(M, Stream, false, &Index, true);
  return MemoryBuffer::getMemBufferCopy(StringRef(Bytes.data(), Bytes.size()), Buffer.getBufferIdentifier());
}
static Error run() {
  if (Input == Output) return fail("input and output must differ; preserve the original object for contract changes");
  if (sys::fs::exists(Output) && sys::fs::equivalent(Input, Output))
    return fail("input and output refer to the same file");
  LLVMContext Context;
  SMDiagnostic Diagnostic;
  auto ContractBuffer = MemoryBuffer::getFile(Contracts);
  if (!ContractBuffer) return errorCodeToError(ContractBuffer.getError());
  auto Contract = parseIR((*ContractBuffer)->getMemBufferRef(), Diagnostic, Context);
  std::string ContentHash = toHex(SHA256::hash(arrayRefFromStringRef((*ContractBuffer)->getBuffer())), true);
  if (!Contract) { Diagnostic.print("svmgen-llvm", errs()); return fail("cannot parse contracts"); }
  if (verifyModule(*Contract, &errs())) return fail("invalid contract IR");
  auto Hash = fingerprint(*Contract, "svmgen.contract");
  if (!Hash) return Hash.takeError();
  if (auto E = validateContracts(*Contract)) return E;
  auto Buffer = MemoryBuffer::getFile(Input);
  if (!Buffer) return errorCodeToError(Buffer.getError());
  std::unique_ptr<MemoryBuffer> Result;
  auto Magic = identify_magic((*Buffer)->getBuffer());
  if (Magic == file_magic::bitcode) {
    auto Rewritten = rewrite((*Buffer)->getMemBufferRef(), Context, *Contract, ContentHash);
    if (!Rewritten) return Rewritten.takeError();
    Result = std::move(*Rewritten);
  } else if (Magic == file_magic::archive) {
    auto Archive = object::Archive::create((*Buffer)->getMemBufferRef());
    if (!Archive) return Archive.takeError();
    if ((*Archive)->isThin()) return fail("thin archives are unsupported; provide a regular archive");
    std::vector<NewArchiveMember> Members;
    std::deque<std::string> MemberNames;
    Error Iteration = Error::success();
    for (const auto &Child : (*Archive)->children(Iteration)) {
      auto Member = NewArchiveMember::getOldMember(Child, true);
      if (!Member) return Member.takeError();
      MemberNames.push_back(Member->MemberName.str());
      Member->MemberName = MemberNames.back();
      if (identify_magic(Member->Buf->getBuffer()) == file_magic::bitcode) {
        auto Rewritten = rewrite(Member->Buf->getMemBufferRef(), Context, *Contract, ContentHash);
        if (!Rewritten) return Rewritten.takeError();
        Member->Buf = std::move(*Rewritten);
      } else ++PreservedMembers;
      Members.push_back(std::move(*Member));
    }
    if (Iteration) return Iteration;
    auto Rebuilt = writeArchiveToBuffer(Members, SymtabWritingMode::NormalSymtab,
                                        (*Archive)->kind(), true, false);
    if (!Rebuilt) return Rebuilt.takeError();
    Result = std::move(*Rebuilt);
  } else return fail("input has no standalone LLVM bitcode; compile with -flto=thin or Rust -Clinker-plugin-lto (native machine-code objects cannot carry usable optimizer annotations)");
  if (Matched == 0) return fail("no contract symbols matched input; refusing an ineffective rewrite");
  // Commit after every member, signature and contract has been validated.
  SmallString<256> Temporary;
  int FD;
  if (auto EC = sys::fs::createUniqueFile(Output + ".tmp-%%%%%%", FD, Temporary)) return errorCodeToError(EC);
  ToolOutputFile File(Temporary, FD);
  File.os() << Result->getBuffer();
  File.os().flush();
  if (File.os().has_error()) return fail("failed writing output");
  if (auto EC = sys::fs::rename(Temporary, Output)) return errorCodeToError(EC);
  outs() << "applied " << *Hash << ": " << Matched << " functions, " << Calls << " direct calls, "
         << Modules << " modules; preserved " << PreservedMembers << " non-bitcode archive members\n";
  return Error::success();
}
int main(int argc, char **argv) {
  cl::ParseCommandLineOptions(argc, argv, "svmgen LLVM 23 seam contract applicator\n");
  if (auto E = run()) { logAllUnhandledErrors(std::move(E), errs(), "svmgen-llvm: "); return 2; }
  return 0;
}
