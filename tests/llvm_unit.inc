// Test-only entry point for the production applicator translation unit.
#include "llvm/IR/IRBuilder.h"

static void rejects(Error E, StringRef Expected) {
  if (!E) {
    errs() << "Expected rejection: " << Expected << '\n';
    std::abort();
  }
  auto Message = toString(std::move(E));
  if (!StringRef(Message).contains(Expected)) {
    errs() << "Wrong rejection: " << Message << '\n';
    std::abort();
  }
}

int main() {
  rejects(run(), "input and output must differ");
  LLVMContext Ctx;
  auto Set = [&](Attribute A) { return AttributeSet::get(Ctx, {A}); };
  rejects(validateAttributes(Set(Attribute::get(Ctx, Attribute::NoUnwind)), false),
          "function contract on parameter");
  rejects(validateAttributes(Set(Attribute::get(Ctx, Attribute::NonNull)), true),
          "parameter contract on function");
  auto Conflict = merge(Ctx, Set(Attribute::get(Ctx, Attribute::StackAlignment, 8)),
                        Set(Attribute::get(Ctx, Attribute::StackAlignment, 16)));
  if (Conflict)
    std::abort();
  rejects(Conflict.takeError(), "parameterized contract conflicts");

  Module Invalid("invalid-contract", Ctx);
  Invalid.setTargetTriple(Triple("x86_64-unknown-linux-gnu"));
  auto *I64 = Type::getInt64Ty(Ctx);
  auto *F = Function::Create(FunctionType::get(I64, {I64}, false), GlobalValue::ExternalLinkage,
                             "f", Invalid);
  F->addParamAttr(0, Attribute::NonNull);
  rejects(validateContracts(Invalid), "pointer contracts on non-pointer");

  Module Contract("contract", Ctx);
  Contract.setTargetTriple(Invalid.getTargetTriple());
  auto *Ptr = PointerType::getUnqual(Ctx);
  auto *Signature = FunctionType::get(Ptr, false);
  Function::Create(Signature, GlobalValue::ExternalLinkage, "f", Contract);
  Module Input("input", Ctx);
  Input.setTargetTriple(Contract.getTargetTriple());
  F = Function::Create(Signature, GlobalValue::ExternalLinkage, "f", Input);
  F->addRetAttr(Attribute::getWithByRefType(Ctx, I64));
  rejects(apply(Input, Contract, std::string(64, 'a')), "unsupported ABI attribute");
  F->removeRetAttr(Attribute::ByRef);
  auto *Caller = Function::Create(Signature, GlobalValue::ExternalLinkage, "caller", Input);
  IRBuilder<> Builder(BasicBlock::Create(Ctx, "entry", Caller));
  auto *Call = Builder.CreateCall(F);
  Call->addRetAttr(Attribute::getWithByRefType(Ctx, I64));
  Builder.CreateRet(Call);
  rejects(apply(Input, Contract, std::string(64, 'a')), "unsupported ABI attribute");
  Call->removeRetAttr(Attribute::ByRef);
  // A malformed return must never survive final module verification.
  Caller->getEntryBlock().getTerminator()->eraseFromParent();
  Builder.SetInsertPoint(&Caller->getEntryBlock());
  Builder.CreateRetVoid();
  rejects(apply(Input, Contract, std::string(64, 'a')),
          "rewritten module failed LLVM verification");
  return 0;
}
