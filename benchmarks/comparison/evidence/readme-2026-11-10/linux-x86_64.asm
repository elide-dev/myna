# native: direct
00000000000021a0 <direct>:
    21a0: 48 89 f0                     	movq	%rsi, %rax
    21a3: 85 ff                        	testl	%edi, %edi
    21a5: 7e 16                        	jle	0x21bd <direct+0x1d>
    21a7: 53                           	pushq	%rbx
    21a8: 89 fb                        	movl	%edi, %ebx
    21aa: 66 0f 1f 44 00 00            	nopw	(%rax,%rax)
    21b0: 48 89 c7                     	movq	%rax, %rdi
    21b3: e8 18 02 00 00               	callq	0x23d0 <myna_bench_leaf>
    21b8: ff cb                        	decl	%ebx
    21ba: 75 f4                        	jne	0x21b0 <direct+0x10>
    21bc: 5b                           	popq	%rbx
    21bd: c3                           	retq
    21be: 66 90                        	nop

# native: indirect
00000000000021c0 <indirect>:
    21c0: 41 56                        	pushq	%r14
    21c2: 53                           	pushq	%rbx
    21c3: 50                           	pushq	%rax
    21c4: 48 89 f0                     	movq	%rsi, %rax
    21c7: 4c 8b 35 6a 28 00 00         	movq	0x286a(%rip), %r14      # 0x4a38 <target>
    21ce: 85 ff                        	testl	%edi, %edi
    21d0: 7e 18                        	jle	0x21ea <indirect+0x2a>
    21d2: 89 fb                        	movl	%edi, %ebx
    21d4: 66 66 66 2e 0f 1f 84 00 00 00 00 00  	nopw	%cs:(%rax,%rax)
    21e0: 48 89 c7                     	movq	%rax, %rdi
    21e3: 41 ff d6                     	callq	*%r14
    21e6: ff cb                        	decl	%ebx
    21e8: 75 f6                        	jne	0x21e0 <indirect+0x20>
    21ea: 48 83 c4 08                  	addq	$0x8, %rsp
    21ee: 5b                           	popq	%rbx
    21ef: 41 5e                        	popq	%r14
    21f1: c3                           	retq
    21f2: 66 66 66 66 66 2e 0f 1f 84 00 00 00 00 00    	nopw	%cs:(%rax,%rax)

# native: identity_direct
00000000000022c0 <identity_direct>:
    22c0: 48 89 f0                     	movq	%rsi, %rax
    22c3: 85 ff                        	testl	%edi, %edi
    22c5: 7e 16                        	jle	0x22dd <identity_direct+0x1d>
    22c7: 53                           	pushq	%rbx
    22c8: 89 fb                        	movl	%edi, %ebx
    22ca: 66 0f 1f 44 00 00            	nopw	(%rax,%rax)
    22d0: 48 89 c7                     	movq	%rax, %rdi
    22d3: e8 d0 03 00 00               	callq	0x26a8 <myna_bench_identity>
    22d8: ff cb                        	decl	%ebx
    22da: 75 f4                        	jne	0x22d0 <identity_direct+0x10>
    22dc: 5b                           	popq	%rbx
    22dd: c3                           	retq
    22de: 66 90                        	nop

# native: identity_indirect
00000000000022e0 <identity_indirect>:
    22e0: 41 56                        	pushq	%r14
    22e2: 53                           	pushq	%rbx
    22e3: 50                           	pushq	%rax
    22e4: 48 89 f0                     	movq	%rsi, %rax
    22e7: 4c 8b 35 52 27 00 00         	movq	0x2752(%rip), %r14      # 0x4a40 <identity_target>
    22ee: 85 ff                        	testl	%edi, %edi
    22f0: 7e 18                        	jle	0x230a <identity_indirect+0x2a>
    22f2: 89 fb                        	movl	%edi, %ebx
    22f4: 66 66 66 2e 0f 1f 84 00 00 00 00 00  	nopw	%cs:(%rax,%rax)
    2300: 48 89 c7                     	movq	%rax, %rdi
    2303: 41 ff d6                     	callq	*%r14
    2306: ff cb                        	decl	%ebx
    2308: 75 f6                        	jne	0x2300 <identity_indirect+0x20>
    230a: 48 83 c4 08                  	addq	$0x8, %rsp
    230e: 5b                           	popq	%rbx
    230f: 41 5e                        	popq	%r14
    2311: c3                           	retq
    2312: 66 66 66 66 66 2e 0f 1f 84 00 00 00 00 00    	nopw	%cs:(%rax,%rax)

# native: identity_direct_unrolled4
0000000000002320 <identity_direct_unrolled4>:
    2320: 48 89 f0                     	movq	%rsi, %rax
    2323: 85 ff                        	testl	%edi, %edi
    2325: 7e 36                        	jle	0x235d <identity_direct_unrolled4+0x3d>
    2327: 55                           	pushq	%rbp
    2328: 53                           	pushq	%rbx
    2329: 50                           	pushq	%rax
    232a: 89 fb                        	movl	%edi, %ebx
    232c: 31 ed                        	xorl	%ebp, %ebp
    232e: 66 90                        	nop
    2330: 48 89 c7                     	movq	%rax, %rdi
    2333: e8 70 03 00 00               	callq	0x26a8 <myna_bench_identity>
    2338: 48 89 c7                     	movq	%rax, %rdi
    233b: e8 68 03 00 00               	callq	0x26a8 <myna_bench_identity>
    2340: 48 89 c7                     	movq	%rax, %rdi
    2343: e8 60 03 00 00               	callq	0x26a8 <myna_bench_identity>
    2348: 48 89 c7                     	movq	%rax, %rdi
    234b: e8 58 03 00 00               	callq	0x26a8 <myna_bench_identity>
    2350: 83 c5 04                     	addl	$0x4, %ebp
    2353: 39 dd                        	cmpl	%ebx, %ebp
    2355: 7c d9                        	jl	0x2330 <identity_direct_unrolled4+0x10>
    2357: 48 83 c4 08                  	addq	$0x8, %rsp
    235b: 5b                           	popq	%rbx
    235c: 5d                           	popq	%rbp
    235d: c3                           	retq
    235e: 66 90                        	nop

# native: identity_indirect_unrolled4
0000000000002360 <identity_indirect_unrolled4>:
    2360: 55                           	pushq	%rbp
    2361: 41 56                        	pushq	%r14
    2363: 53                           	pushq	%rbx
    2364: 48 89 f0                     	movq	%rsi, %rax
    2367: 4c 8b 35 d2 26 00 00         	movq	0x26d2(%rip), %r14      # 0x4a40 <identity_target>
    236e: 85 ff                        	testl	%edi, %edi
    2370: 7e 2d                        	jle	0x239f <identity_indirect_unrolled4+0x3f>
    2372: 89 fb                        	movl	%edi, %ebx
    2374: 31 ed                        	xorl	%ebp, %ebp
    2376: 66 2e 0f 1f 84 00 00 00 00 00	nopw	%cs:(%rax,%rax)
    2380: 48 89 c7                     	movq	%rax, %rdi
    2383: 41 ff d6                     	callq	*%r14
    2386: 48 89 c7                     	movq	%rax, %rdi
    2389: 41 ff d6                     	callq	*%r14
    238c: 48 89 c7                     	movq	%rax, %rdi
    238f: 41 ff d6                     	callq	*%r14
    2392: 48 89 c7                     	movq	%rax, %rdi
    2395: 41 ff d6                     	callq	*%r14
    2398: 83 c5 04                     	addl	$0x4, %ebp
    239b: 39 dd                        	cmpl	%ebx, %ebp
    239d: 7c e1                        	jl	0x2380 <identity_indirect_unrolled4+0x20>
    239f: 5b                           	popq	%rbx
    23a0: 41 5e                        	popq	%r14
    23a2: 5d                           	popq	%rbp
    23a3: c3                           	retq
    23a4: cc                           	int3
    23a5: cc                           	int3
    23a6: cc                           	int3
    23a7: cc                           	int3
    23a8: cc                           	int3
    23a9: cc                           	int3
    23aa: cc                           	int3
    23ab: cc                           	int3
    23ac: cc                           	int3
    23ad: cc                           	int3
    23ae: cc                           	int3
    23af: cc                           	int3

# native: myna_bench_identity
00000000000026a8 <myna_bench_identity>:
    26a8: 48 89 f8                     	movq	%rdi, %rax
    26ab: c3                           	retq
    26ac: 0f 1f 40 00                  	nopl	(%rax)

# native: myna_bench_identity_address
00000000000026b0 <myna_bench_identity_address>:
    26b0: 48 8d 05 f1 ff ff ff         	leaq	-0xf(%rip), %rax        # 0x26a8 <myna_bench_identity>
    26b7: c3                           	retq
    26b8: cc                           	int3
    26b9: cc                           	int3
    26ba: cc                           	int3
    26bb: cc                           	int3
    26bc: cc                           	int3
    26bd: cc                           	int3
    26be: cc                           	int3
    26bf: cc                           	int3

# svm: Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG
00000000000403a0 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG>:
   403a0: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   403a5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   403a9: 0f 86 b1 98 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   403af: 48 8b e3                     	movq	%rbx, %rsp
   403b2: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   403b6: 83 fe 01                     	cmpl	$0x1, %esi
   403b9: 7c 29                        	jl	0x403e4 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x44>
   403bb: 49 8b be 68 f1 3f 00         	movq	0x3ff168(%r14), %rdi
   403c2: 48 8b f2                     	movq	%rdx, %rsi
   403c5: e8 76 1e 00 00               	callq	0x42240 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1>
   403ca: 81 7c 24 14 01 01 00 00      	cmpl	$0x101, 0x14(%rsp)      # imm = 0x101
   403d2: 7d 23                        	jge	0x403f7 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x57>
   403d4: 48 83 c4 18                  	addq	$0x18, %rsp
   403d8: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   403dd: 0f 8e 9d b5 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   403e3: c3                           	retq
   403e4: 48 8b c2                     	movq	%rdx, %rax
   403e7: 48 83 c4 18                  	addq	$0x18, %rsp
   403eb: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   403f0: 0f 8e 8a b5 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   403f6: c3                           	retq
   403f7: 48 8b f0                     	movq	%rax, %rsi
   403fa: c7 44 24 10 00 01 00 00      	movl	$0x100, 0x10(%rsp)      # imm = 0x100
   40402: 66 66 0f 1f 84 00 00 00 00 00	nopw	(%rax,%rax)
   4040c: 66 66 66 90                  	nop
   40410: 49 8b be 68 f1 3f 00         	movq	0x3ff168(%r14), %rdi
   40417: e8 24 1e 00 00               	callq	0x42240 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1>
   4041c: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   40420: 8d 91 00 01 00 00            	leal	0x100(%rcx), %edx
   40426: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   4042a: 3b f2                        	cmpl	%edx, %esi
   4042c: 7e a6                        	jle	0x403d4 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x34>
   4042e: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40433: 7e 09                        	jle	0x4043e <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x9e>
   40435: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   40439: 48 8b f0                     	movq	%rax, %rsi
   4043c: eb d2                        	jmp	0x40410 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x70>
   4043e: 66 90                        	nop
   40440: e8 3b b5 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40445: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   40449: 48 8b f0                     	movq	%rax, %rsi
   4044c: eb c2                        	jmp	0x40410 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x70>
   4044e: cc                           	int3
   4044f: cc                           	int3
   40450: cc                           	int3
   40451: cc                           	int3
   40452: cc                           	int3
   40453: cc                           	int3
   40454: cc                           	int3
   40455: cc                           	int3
   40456: cc                           	int3
   40457: cc                           	int3
   40458: cc                           	int3
   40459: cc                           	int3
   4045a: cc                           	int3
   4045b: cc                           	int3
   4045c: cc                           	int3
   4045d: cc                           	int3
   4045e: cc                           	int3
   4045f: cc                           	int3

# svm: Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6
0000000000040460 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6>:
   40460: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   40465: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   40469: 0f 86 f1 97 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   4046f: 48 8b e3                     	movq	%rbx, %rsp
   40472: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   40476: 83 fe 01                     	cmpl	$0x1, %esi
   40479: 7c 31                        	jl	0x404ac <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x4c>
   4047b: 48 8b 0d 46 05 89 00         	movq	0x890546(%rip), %rcx    # 0x8d09c8 <__svm_vm_is_static_binary+0x590>
   40482: 48 89 4c 24 08               	movq	%rcx, 0x8(%rsp)
   40487: 48 8b fa                     	movq	%rdx, %rdi
   4048a: b8 00 00 00 00               	movl	$0x0, %eax
   4048f: 4c 8b d1                     	movq	%rcx, %r10
   40492: 41 ff d2                     	callq	*%r10
   40495: 83 7c 24 14 11               	cmpl	$0x11, 0x14(%rsp)
   4049a: 7d 23                        	jge	0x404bf <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x5f>
   4049c: 48 83 c4 18                  	addq	$0x18, %rsp
   404a0: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   404a5: 0f 8e d5 b4 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   404ab: c3                           	retq
   404ac: 48 8b c2                     	movq	%rdx, %rax
   404af: 48 83 c4 18                  	addq	$0x18, %rsp
   404b3: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   404b8: 0f 8e c2 b4 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   404be: c3                           	retq
   404bf: c7 44 24 10 10 00 00 00      	movl	$0x10, 0x10(%rsp)
   404c7: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   404cc: 0f 1f 40 00                  	nopl	(%rax)
   404d0: 48 8b f8                     	movq	%rax, %rdi
   404d3: b8 00 00 00 00               	movl	$0x0, %eax
   404d8: 4c 8b d1                     	movq	%rcx, %r10
   404db: 41 ff d2                     	callq	*%r10
   404de: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   404e2: 8d 51 10                     	leal	0x10(%rcx), %edx
   404e5: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   404e9: 3b f2                        	cmpl	%edx, %esi
   404eb: 7e af                        	jle	0x4049c <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x3c>
   404ed: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   404f2: 7e 0b                        	jle	0x404ff <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x9f>
   404f4: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   404f8: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   404fd: eb d1                        	jmp	0x404d0 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x70>
   404ff: 48 8b 5c 24 08               	movq	0x8(%rsp), %rbx
   40504: e8 77 b4 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40509: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   4050d: 48 8b cb                     	movq	%rbx, %rcx
   40510: eb be                        	jmp	0x404d0 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x70>
   40512: cc                           	int3
   40513: cc                           	int3
   40514: cc                           	int3
   40515: cc                           	int3
   40516: cc                           	int3
   40517: cc                           	int3
   40518: cc                           	int3
   40519: cc                           	int3
   4051a: cc                           	int3
   4051b: cc                           	int3
   4051c: cc                           	int3
   4051d: cc                           	int3
   4051e: cc                           	int3
   4051f: cc                           	int3

# svm: Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF
0000000000040520 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF>:
   40520: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   40525: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   40529: 0f 86 31 97 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   4052f: 48 8b e3                     	movq	%rbx, %rsp
   40532: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   40536: 83 fe 01                     	cmpl	$0x1, %esi
   40539: 7c 26                        	jl	0x40561 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x41>
   4053b: 49 8b be 68 f1 3f 00         	movq	0x3ff168(%r14), %rdi
   40542: 48 8b f2                     	movq	%rdx, %rsi
   40545: e8 96 1e 00 00               	callq	0x423e0 <Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI>
   4054a: 83 7c 24 14 05               	cmpl	$0x5, 0x14(%rsp)
   4054f: 7d 23                        	jge	0x40574 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x54>
   40551: 48 83 c4 18                  	addq	$0x18, %rsp
   40555: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4055a: 0f 8e 20 b4 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40560: c3                           	retq
   40561: 48 8b c2                     	movq	%rdx, %rax
   40564: 48 83 c4 18                  	addq	$0x18, %rsp
   40568: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4056d: 0f 8e 0d b4 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40573: c3                           	retq
   40574: 48 8b f0                     	movq	%rax, %rsi
   40577: c7 44 24 10 04 00 00 00      	movl	$0x4, 0x10(%rsp)
   4057f: 90                           	nop
   40580: 49 8b be 68 f1 3f 00         	movq	0x3ff168(%r14), %rdi
   40587: e8 54 1e 00 00               	callq	0x423e0 <Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI>
   4058c: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   40590: 8d 51 04                     	leal	0x4(%rcx), %edx
   40593: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   40597: 3b f2                        	cmpl	%edx, %esi
   40599: 7e b6                        	jle	0x40551 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x31>
   4059b: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   405a0: 7e 09                        	jle	0x405ab <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x8b>
   405a2: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   405a6: 48 8b f0                     	movq	%rax, %rsi
   405a9: eb d5                        	jmp	0x40580 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x60>
   405ab: e8 d0 b3 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   405b0: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   405b4: 48 8b f0                     	movq	%rax, %rsi
   405b7: eb c7                        	jmp	0x40580 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x60>
   405b9: cc                           	int3
   405ba: cc                           	int3
   405bb: cc                           	int3
   405bc: cc                           	int3
   405bd: cc                           	int3
   405be: cc                           	int3
   405bf: cc                           	int3

# svm: Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE
00000000000405c0 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE>:
   405c0: 48 8d 5c 24 c8               	leaq	-0x38(%rsp), %rbx
   405c5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   405c9: 0f 86 91 96 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   405cf: 48 8b e3                     	movq	%rbx, %rsp
   405d2: 89 74 24 2c                  	movl	%esi, 0x2c(%rsp)
   405d6: 83 fe 01                     	cmpl	$0x1, %esi
   405d9: bf 01 00 00 00               	movl	$0x1, %edi
   405de: 0f 4e fe                     	cmovlel	%esi, %edi
   405e1: 83 ff 01                     	cmpl	$0x1, %edi
   405e4: 0f 8c 74 01 00 00            	jl	0x4075e <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x19e>
   405ea: 48 8b fa                     	movq	%rdx, %rdi
   405ed: e8 ce 26 00 00               	callq	0x42cc0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   405f2: bf 01 00 00 00               	movl	$0x1, %edi
   405f7: 8b 74 24 2c                  	movl	0x2c(%rsp), %esi
   405fb: 8d 4e fc                     	leal	-0x4(%rsi), %ecx
   405fe: 81 fe 04 00 00 80            	cmpl	$0x80000004, %esi       # imm = 0x80000004
   40604: ba 00 00 00 80               	movl	$0x80000000, %edx       # imm = 0x80000000
   40609: 0f 4c ca                     	cmovll	%edx, %ecx
   4060c: 89 4c 24 20                  	movl	%ecx, 0x20(%rsp)
   40610: 8d 51 03                     	leal	0x3(%rcx), %edx
   40613: 89 54 24 1c                  	movl	%edx, 0x1c(%rsp)
   40617: 8b df                        	movl	%edi, %ebx
   40619: 0f 1f 80 00 00 00 00         	nopl	(%rax)
   40620: 3b cb                        	cmpl	%ebx, %ecx
   40622: 0f 8e 0a 01 00 00            	jle	0x40732 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x172>
   40628: 89 5c 24 24                  	movl	%ebx, 0x24(%rsp)
   4062c: 8b fa                        	movl	%edx, %edi
   4062e: 2b fb                        	subl	%ebx, %edi
   40630: 8b ef                        	movl	%edi, %ebp
   40632: c1 ed 02                     	shrl	$0x2, %ebp
   40635: 83 e7 fc                     	andl	$-0x4, %edi
   40638: 81 fd 00 10 00 00            	cmpl	$0x1000, %ebp           # imm = 0x1000
   4063e: bd 00 40 00 00               	movl	$0x4000, %ebp           # imm = 0x4000
   40643: 0f 42 ef                     	cmovbl	%edi, %ebp
   40646: 83 fd 01                     	cmpl	$0x1, %ebp
   40649: 0f 8c b6 00 00 00            	jl	0x40705 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x145>
   4064f: 89 6c 24 34                  	movl	%ebp, 0x34(%rsp)
   40653: 48 8b f8                     	movq	%rax, %rdi
   40656: b8 00 00 00 00               	movl	$0x0, %eax
   4065b: eb 13                        	jmp	0x40670 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0xb0>
   4065d: 66 66 90                     	nop
   40660: 8b 5c 24 24                  	movl	0x24(%rsp), %ebx
   40664: 48 89 44 24 10               	movq	%rax, 0x10(%rsp)
   40669: 8b c7                        	movl	%edi, %eax
   4066b: 48 8b 7c 24 10               	movq	0x10(%rsp), %rdi
   40670: 44 8b c0                     	movl	%eax, %r8d
   40673: 44 03 c3                     	addl	%ebx, %r8d
   40676: 44 89 44 24 18               	movl	%r8d, 0x18(%rsp)
   4067b: 89 44 24 28                  	movl	%eax, 0x28(%rsp)
   4067f: 90                           	nop
   40680: e8 3b 26 00 00               	callq	0x42cc0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   40685: 44 8b 44 24 18               	movl	0x18(%rsp), %r8d
   4068a: 41 ff c0                     	incl	%r8d
   4068d: 48 8b f8                     	movq	%rax, %rdi
   40690: 44 89 44 24 30               	movl	%r8d, 0x30(%rsp)
   40695: e8 26 26 00 00               	callq	0x42cc0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   4069a: 44 8b 44 24 18               	movl	0x18(%rsp), %r8d
   4069f: 41 8d 48 02                  	leal	0x2(%r8), %ecx
   406a3: 48 8b f8                     	movq	%rax, %rdi
   406a6: 89 4c 24 30                  	movl	%ecx, 0x30(%rsp)
   406aa: e8 11 26 00 00               	callq	0x42cc0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   406af: 44 8b 44 24 18               	movl	0x18(%rsp), %r8d
   406b4: 41 8d 48 03                  	leal	0x3(%r8), %ecx
   406b8: 48 8b f8                     	movq	%rax, %rdi
   406bb: 89 4c 24 18                  	movl	%ecx, 0x18(%rsp)
   406bf: 90                           	nop
   406c0: e8 fb 25 00 00               	callq	0x42cc0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   406c5: 8b 7c 24 28                  	movl	0x28(%rsp), %edi
   406c9: 8d 7f 04                     	leal	0x4(%rdi), %edi
   406cc: 8b 6c 24 34                  	movl	0x34(%rsp), %ebp
   406d0: 3b ef                        	cmpl	%edi, %ebp
   406d2: 7f 8c                        	jg	0x40660 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0xa0>
   406d4: 03 7c 24 24                  	addl	0x24(%rsp), %edi
   406d8: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   406dd: 7e 0f                        	jle	0x406ee <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x12e>
   406df: 8b df                        	movl	%edi, %ebx
   406e1: 8b 54 24 1c                  	movl	0x1c(%rsp), %edx
   406e5: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   406e9: e9 32 ff ff ff               	jmp	0x40620 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x60>
   406ee: e8 8d b2 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   406f3: 8b df                        	movl	%edi, %ebx
   406f5: 8b 54 24 1c                  	movl	0x1c(%rsp), %edx
   406f9: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   406fd: 66 66 90                     	nop
   40700: e9 1b ff ff ff               	jmp	0x40620 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x60>
   40705: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4070a: 7e 30                        	jle	0x4073c <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x17c>
   4070c: 8b 5c 24 24                  	movl	0x24(%rsp), %ebx
   40710: e9 0b ff ff ff               	jmp	0x40620 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x60>
   40715: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   40720: 89 5c 24 28                  	movl	%ebx, 0x28(%rsp)
   40724: 48 8b f8                     	movq	%rax, %rdi
   40727: e8 94 25 00 00               	callq	0x42cc0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   4072c: 8b 5c 24 28                  	movl	0x28(%rsp), %ebx
   40730: ff c3                        	incl	%ebx
   40732: 8b 74 24 2c                  	movl	0x2c(%rsp), %esi
   40736: 3b f3                        	cmpl	%ebx, %esi
   40738: 7f e6                        	jg	0x40720 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x160>
   4073a: eb 12                        	jmp	0x4074e <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x18e>
   4073c: 0f 1f 40 00                  	nopl	(%rax)
   40740: e8 3b b2 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40745: 8b 5c 24 24                  	movl	0x24(%rsp), %ebx
   40749: e9 d2 fe ff ff               	jmp	0x40620 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x60>
   4074e: 48 83 c4 38                  	addq	$0x38, %rsp
   40752: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40757: 0f 8e 23 b2 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   4075d: c3                           	retq
   4075e: bf 00 00 00 00               	movl	$0x0, %edi
   40763: 48 8b c2                     	movq	%rdx, %rax
   40766: e9 90 fe ff ff               	jmp	0x405fb <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x3b>
   4076b: cc                           	int3
   4076c: cc                           	int3
   4076d: cc                           	int3
   4076e: cc                           	int3
   4076f: cc                           	int3
   40770: cc                           	int3
   40771: cc                           	int3
   40772: cc                           	int3
   40773: cc                           	int3
   40774: cc                           	int3
   40775: cc                           	int3
   40776: cc                           	int3
   40777: cc                           	int3
   40778: cc                           	int3
   40779: cc                           	int3
   4077a: cc                           	int3
   4077b: cc                           	int3
   4077c: cc                           	int3
   4077d: cc                           	int3
   4077e: cc                           	int3
   4077f: cc                           	int3

# svm: Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669
0000000000040780 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669>:
   40780: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   40785: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   40789: 0f 86 d1 94 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   4078f: 48 8b e3                     	movq	%rbx, %rsp
   40792: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   40796: 83 fe 01                     	cmpl	$0x1, %esi
   40799: 0f 8c 62 00 00 00            	jl	0x40801 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x81>
   4079f: 49 8b 8e 70 f1 3f 00         	movq	0x3ff170(%r14), %rcx
   407a6: 48 89 4c 24 08               	movq	%rcx, 0x8(%rsp)
   407ab: 48 8b fa                     	movq	%rdx, %rdi
   407ae: b8 00 00 00 00               	movl	$0x0, %eax
   407b3: 4c 8b d1                     	movq	%rcx, %r10
   407b6: 41 ff d2                     	callq	*%r10
   407b9: 48 8b f8                     	movq	%rax, %rdi
   407bc: b8 00 00 00 00               	movl	$0x0, %eax
   407c1: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   407c6: 41 ff d2                     	callq	*%r10
   407c9: 48 8b f8                     	movq	%rax, %rdi
   407cc: b8 00 00 00 00               	movl	$0x0, %eax
   407d1: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   407d6: 41 ff d2                     	callq	*%r10
   407d9: 48 8b f8                     	movq	%rax, %rdi
   407dc: b8 00 00 00 00               	movl	$0x0, %eax
   407e1: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   407e6: 41 ff d2                     	callq	*%r10
   407e9: 83 7c 24 14 05               	cmpl	$0x5, 0x14(%rsp)
   407ee: 7d 24                        	jge	0x40814 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x94>
   407f0: 48 83 c4 18                  	addq	$0x18, %rsp
   407f4: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   407f9: 0f 8e 81 b1 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   407ff: 90                           	nop
   40800: c3                           	retq
   40801: 48 8b c2                     	movq	%rdx, %rax
   40804: 48 83 c4 18                  	addq	$0x18, %rsp
   40808: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4080d: 0f 8e 6d b1 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40813: c3                           	retq
   40814: c7 44 24 10 04 00 00 00      	movl	$0x4, 0x10(%rsp)
   4081c: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   40821: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   4082c: 66 66 66 90                  	nop
   40830: 48 8b f8                     	movq	%rax, %rdi
   40833: b8 00 00 00 00               	movl	$0x0, %eax
   40838: 4c 8b d1                     	movq	%rcx, %r10
   4083b: 41 ff d2                     	callq	*%r10
   4083e: 48 8b f8                     	movq	%rax, %rdi
   40841: b8 00 00 00 00               	movl	$0x0, %eax
   40846: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4084b: 41 ff d2                     	callq	*%r10
   4084e: 48 8b f8                     	movq	%rax, %rdi
   40851: b8 00 00 00 00               	movl	$0x0, %eax
   40856: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4085b: 41 ff d2                     	callq	*%r10
   4085e: 48 8b f8                     	movq	%rax, %rdi
   40861: b8 00 00 00 00               	movl	$0x0, %eax
   40866: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4086b: 41 ff d2                     	callq	*%r10
   4086e: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   40872: 8d 51 04                     	leal	0x4(%rcx), %edx
   40875: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   40879: 0f 1f 80 00 00 00 00         	nopl	(%rax)
   40880: 3b f2                        	cmpl	%edx, %esi
   40882: 0f 8e 68 ff ff ff            	jle	0x407f0 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x70>
   40888: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4088d: 7e 0b                        	jle	0x4089a <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x11a>
   4088f: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   40893: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   40898: eb 96                        	jmp	0x40830 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0xb0>
   4089a: 48 8b 5c 24 08               	movq	0x8(%rsp), %rbx
   4089f: 90                           	nop
   408a0: e8 db b0 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   408a5: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   408a9: 48 8b cb                     	movq	%rbx, %rcx
   408ac: eb 82                        	jmp	0x40830 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0xb0>
   408ae: cc                           	int3
   408af: cc                           	int3
   408b0: cc                           	int3
   408b1: cc                           	int3
   408b2: cc                           	int3
   408b3: cc                           	int3
   408b4: cc                           	int3
   408b5: cc                           	int3
   408b6: cc                           	int3
   408b7: cc                           	int3
   408b8: cc                           	int3
   408b9: cc                           	int3
   408ba: cc                           	int3
   408bb: cc                           	int3
   408bc: cc                           	int3
   408bd: cc                           	int3
   408be: cc                           	int3
   408bf: cc                           	int3

# svm: Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4
00000000000408c0 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4>:
   408c0: 48 8d 5c 24 c8               	leaq	-0x38(%rsp), %rbx
   408c5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   408c9: 0f 86 91 93 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   408cf: 48 8b e3                     	movq	%rbx, %rsp
   408d2: 89 74 24 28                  	movl	%esi, 0x28(%rsp)
   408d6: 49 8b 8e 70 f1 3f 00         	movq	0x3ff170(%r14), %rcx
   408dd: 48 89 4c 24 10               	movq	%rcx, 0x10(%rsp)
   408e2: 83 fe 01                     	cmpl	$0x1, %esi
   408e5: bf 01 00 00 00               	movl	$0x1, %edi
   408ea: 0f 4e fe                     	cmovlel	%esi, %edi
   408ed: 83 ff 01                     	cmpl	$0x1, %edi
   408f0: 0f 8c bf 01 00 00            	jl	0x40ab5 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1f5>
   408f6: 48 8b fa                     	movq	%rdx, %rdi
   408f9: b8 00 00 00 00               	movl	$0x0, %eax
   408fe: 4c 8b d1                     	movq	%rcx, %r10
   40901: 41 ff d2                     	callq	*%r10
   40904: bf 01 00 00 00               	movl	$0x1, %edi
   40909: 8b 74 24 28                  	movl	0x28(%rsp), %esi
   4090d: 44 8d 56 fc                  	leal	-0x4(%rsi), %r10d
   40911: 81 fe 04 00 00 80            	cmpl	$0x80000004, %esi       # imm = 0x80000004
   40917: b9 00 00 00 80               	movl	$0x80000000, %ecx       # imm = 0x80000000
   4091c: 44 0f 4c d1                  	cmovll	%ecx, %r10d
   40920: 44 89 54 24 18               	movl	%r10d, 0x18(%rsp)
   40925: 41 8d 4a 03                  	leal	0x3(%r10), %ecx
   40929: 89 4c 24 20                  	movl	%ecx, 0x20(%rsp)
   4092d: 8b d7                        	movl	%edi, %edx
   4092f: 90                           	nop
   40930: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   40935: 44 3b d2                     	cmpl	%edx, %r10d
   40938: 0f 8e 27 01 00 00            	jle	0x40a65 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1a5>
   4093e: 89 54 24 24                  	movl	%edx, 0x24(%rsp)
   40942: 8b f9                        	movl	%ecx, %edi
   40944: 2b fa                        	subl	%edx, %edi
   40946: 8b ef                        	movl	%edi, %ebp
   40948: c1 ed 02                     	shrl	$0x2, %ebp
   4094b: 83 e7 fc                     	andl	$-0x4, %edi
   4094e: 81 fd 00 10 00 00            	cmpl	$0x1000, %ebp           # imm = 0x1000
   40954: bd 00 40 00 00               	movl	$0x4000, %ebp           # imm = 0x4000
   40959: 0f 42 ef                     	cmovbl	%edi, %ebp
   4095c: 0f 1f 40 00                  	nopl	(%rax)
   40960: 83 fd 01                     	cmpl	$0x1, %ebp
   40963: 0f 8c de 00 00 00            	jl	0x40a47 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x187>
   40969: 89 6c 24 30                  	movl	%ebp, 0x30(%rsp)
   4096d: 48 8b f8                     	movq	%rax, %rdi
   40970: b9 00 00 00 00               	movl	$0x0, %ecx
   40975: eb 15                        	jmp	0x4098c <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0xcc>
   40977: 66 0f 1f 84 00 00 00 00 00   	nopw	(%rax,%rax)
   40980: 48 8b f8                     	movq	%rax, %rdi
   40983: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   40987: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   4098c: 44 8b c9                     	movl	%ecx, %r9d
   4098f: 44 03 ca                     	addl	%edx, %r9d
   40992: 44 89 4c 24 1c               	movl	%r9d, 0x1c(%rsp)
   40997: b8 00 00 00 00               	movl	$0x0, %eax
   4099c: 4c 8b d3                     	movq	%rbx, %r10
   4099f: 89 4c 24 34                  	movl	%ecx, 0x34(%rsp)
   409a3: 41 ff d2                     	callq	*%r10
   409a6: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   409ab: 41 ff c1                     	incl	%r9d
   409ae: 48 8b f8                     	movq	%rax, %rdi
   409b1: b8 00 00 00 00               	movl	$0x0, %eax
   409b6: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   409bb: 44 89 4c 24 2c               	movl	%r9d, 0x2c(%rsp)
   409c0: 41 ff d2                     	callq	*%r10
   409c3: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   409c8: 41 8d 49 02                  	leal	0x2(%r9), %ecx
   409cc: 48 8b f8                     	movq	%rax, %rdi
   409cf: b8 00 00 00 00               	movl	$0x0, %eax
   409d4: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   409d9: 89 4c 24 2c                  	movl	%ecx, 0x2c(%rsp)
   409dd: 66 66 90                     	nop
   409e0: 41 ff d2                     	callq	*%r10
   409e3: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   409e8: 41 8d 49 03                  	leal	0x3(%r9), %ecx
   409ec: 48 8b f8                     	movq	%rax, %rdi
   409ef: b8 00 00 00 00               	movl	$0x0, %eax
   409f4: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   409f9: 89 4c 24 1c                  	movl	%ecx, 0x1c(%rsp)
   409fd: 66 66 90                     	nop
   40a00: 41 ff d2                     	callq	*%r10
   40a03: 8b 4c 24 34                  	movl	0x34(%rsp), %ecx
   40a07: 8d 49 04                     	leal	0x4(%rcx), %ecx
   40a0a: 8b 6c 24 30                  	movl	0x30(%rsp), %ebp
   40a0e: 3b e9                        	cmpl	%ecx, %ebp
   40a10: 0f 8f 6a ff ff ff            	jg	0x40980 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0xc0>
   40a16: 03 4c 24 24                  	addl	0x24(%rsp), %ecx
   40a1a: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40a1f: 90                           	nop
   40a20: 7e 10                        	jle	0x40a32 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x172>
   40a22: 8b d1                        	movl	%ecx, %edx
   40a24: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   40a28: 44 8b 54 24 18               	movl	0x18(%rsp), %r10d
   40a2d: e9 fe fe ff ff               	jmp	0x40930 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x70>
   40a32: e8 49 af 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40a37: 8b d1                        	movl	%ecx, %edx
   40a39: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   40a3d: 44 8b 54 24 18               	movl	0x18(%rsp), %r10d
   40a42: e9 e9 fe ff ff               	jmp	0x40930 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x70>
   40a47: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40a4c: 7e 09                        	jle	0x40a57 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x197>
   40a4e: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   40a52: e9 d9 fe ff ff               	jmp	0x40930 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x70>
   40a57: e8 24 af 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40a5c: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   40a60: e9 cb fe ff ff               	jmp	0x40930 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x70>
   40a65: 48 8b cb                     	movq	%rbx, %rcx
   40a68: eb 33                        	jmp	0x40a9d <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1dd>
   40a6a: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   40a75: 66 66 66 90                  	nop
   40a79: 0f 1f 80 00 00 00 00         	nopl	(%rax)
   40a80: 89 54 24 18                  	movl	%edx, 0x18(%rsp)
   40a84: 48 8b f8                     	movq	%rax, %rdi
   40a87: b8 00 00 00 00               	movl	$0x0, %eax
   40a8c: 4c 8b d1                     	movq	%rcx, %r10
   40a8f: 41 ff d2                     	callq	*%r10
   40a92: 8b 54 24 18                  	movl	0x18(%rsp), %edx
   40a96: ff c2                        	incl	%edx
   40a98: 48 8b 4c 24 10               	movq	0x10(%rsp), %rcx
   40a9d: 8b 74 24 28                  	movl	0x28(%rsp), %esi
   40aa1: 3b f2                        	cmpl	%edx, %esi
   40aa3: 7f db                        	jg	0x40a80 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1c0>
   40aa5: 48 83 c4 38                  	addq	$0x38, %rsp
   40aa9: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40aae: 0f 8e cc ae 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40ab4: c3                           	retq
   40ab5: bf 00 00 00 00               	movl	$0x0, %edi
   40aba: 48 8b c2                     	movq	%rdx, %rax
   40abd: 66 66 90                     	nop
   40ac0: e9 48 fe ff ff               	jmp	0x4090d <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x4d>
   40ac5: cc                           	int3
   40ac6: cc                           	int3
   40ac7: cc                           	int3
   40ac8: cc                           	int3
   40ac9: cc                           	int3
   40aca: cc                           	int3
   40acb: cc                           	int3
   40acc: cc                           	int3
   40acd: cc                           	int3
   40ace: cc                           	int3
   40acf: cc                           	int3
   40ad0: cc                           	int3
   40ad1: cc                           	int3
   40ad2: cc                           	int3
   40ad3: cc                           	int3
   40ad4: cc                           	int3
   40ad5: cc                           	int3
   40ad6: cc                           	int3
   40ad7: cc                           	int3
   40ad8: cc                           	int3
   40ad9: cc                           	int3
   40ada: cc                           	int3
   40adb: cc                           	int3
   40adc: cc                           	int3
   40add: cc                           	int3
   40ade: cc                           	int3
   40adf: cc                           	int3

# svm: Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5
0000000000040ae0 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5>:
   40ae0: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   40ae5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   40ae9: 0f 86 71 91 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   40aef: 48 8b e3                     	movq	%rbx, %rsp
   40af2: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   40af6: 83 fe 01                     	cmpl	$0x1, %esi
   40af9: 0f 8c 62 00 00 00            	jl	0x40b61 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x81>
   40aff: 49 8b 8e 70 f1 3f 00         	movq	0x3ff170(%r14), %rcx
   40b06: 48 89 4c 24 08               	movq	%rcx, 0x8(%rsp)
   40b0b: 48 8b fa                     	movq	%rdx, %rdi
   40b0e: b8 00 00 00 00               	movl	$0x0, %eax
   40b13: 4c 8b d1                     	movq	%rcx, %r10
   40b16: 41 ff d2                     	callq	*%r10
   40b19: 48 8b f8                     	movq	%rax, %rdi
   40b1c: b8 00 00 00 00               	movl	$0x0, %eax
   40b21: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   40b26: 41 ff d2                     	callq	*%r10
   40b29: 48 8b f8                     	movq	%rax, %rdi
   40b2c: b8 00 00 00 00               	movl	$0x0, %eax
   40b31: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   40b36: 41 ff d2                     	callq	*%r10
   40b39: 48 8b f8                     	movq	%rax, %rdi
   40b3c: b8 00 00 00 00               	movl	$0x0, %eax
   40b41: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   40b46: 41 ff d2                     	callq	*%r10
   40b49: 83 7c 24 14 05               	cmpl	$0x5, 0x14(%rsp)
   40b4e: 7d 24                        	jge	0x40b74 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x94>
   40b50: 48 83 c4 18                  	addq	$0x18, %rsp
   40b54: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40b59: 0f 8e 21 ae 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40b5f: 90                           	nop
   40b60: c3                           	retq
   40b61: 48 8b c2                     	movq	%rdx, %rax
   40b64: 48 83 c4 18                  	addq	$0x18, %rsp
   40b68: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40b6d: 0f 8e 0d ae 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40b73: c3                           	retq
   40b74: c7 44 24 10 04 00 00 00      	movl	$0x4, 0x10(%rsp)
   40b7c: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   40b81: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   40b8c: 66 66 66 90                  	nop
   40b90: 48 8b f8                     	movq	%rax, %rdi
   40b93: b8 00 00 00 00               	movl	$0x0, %eax
   40b98: 4c 8b d1                     	movq	%rcx, %r10
   40b9b: 41 ff d2                     	callq	*%r10
   40b9e: 48 8b f8                     	movq	%rax, %rdi
   40ba1: b8 00 00 00 00               	movl	$0x0, %eax
   40ba6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   40bab: 41 ff d2                     	callq	*%r10
   40bae: 48 8b f8                     	movq	%rax, %rdi
   40bb1: b8 00 00 00 00               	movl	$0x0, %eax
   40bb6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   40bbb: 41 ff d2                     	callq	*%r10
   40bbe: 48 8b f8                     	movq	%rax, %rdi
   40bc1: b8 00 00 00 00               	movl	$0x0, %eax
   40bc6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   40bcb: 41 ff d2                     	callq	*%r10
   40bce: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   40bd2: 8d 51 04                     	leal	0x4(%rcx), %edx
   40bd5: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   40bd9: 0f 1f 80 00 00 00 00         	nopl	(%rax)
   40be0: 3b f2                        	cmpl	%edx, %esi
   40be2: 0f 8e 68 ff ff ff            	jle	0x40b50 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x70>
   40be8: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40bed: 7e 0b                        	jle	0x40bfa <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x11a>
   40bef: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   40bf3: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   40bf8: eb 96                        	jmp	0x40b90 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0xb0>
   40bfa: 48 8b 5c 24 08               	movq	0x8(%rsp), %rbx
   40bff: 90                           	nop
   40c00: e8 7b ad 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40c05: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   40c09: 48 8b cb                     	movq	%rbx, %rcx
   40c0c: eb 82                        	jmp	0x40b90 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0xb0>
   40c0e: cc                           	int3
   40c0f: cc                           	int3
   40c10: cc                           	int3
   40c11: cc                           	int3
   40c12: cc                           	int3
   40c13: cc                           	int3
   40c14: cc                           	int3
   40c15: cc                           	int3
   40c16: cc                           	int3
   40c17: cc                           	int3
   40c18: cc                           	int3
   40c19: cc                           	int3
   40c1a: cc                           	int3
   40c1b: cc                           	int3
   40c1c: cc                           	int3
   40c1d: cc                           	int3
   40c1e: cc                           	int3
   40c1f: cc                           	int3

# svm: Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB
0000000000040c20 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB>:
   40c20: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   40c25: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   40c29: 0f 86 31 90 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   40c2f: 48 8b e3                     	movq	%rbx, %rsp
   40c32: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   40c36: 83 fe 01                     	cmpl	$0x1, %esi
   40c39: 7c 34                        	jl	0x40c6f <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x4f>
   40c3b: 48 8b 0d fe fb 88 00         	movq	0x88fbfe(%rip), %rcx    # 0x8d0840 <__svm_vm_is_static_binary+0x408>
   40c42: 48 89 4c 24 08               	movq	%rcx, 0x8(%rsp)
   40c47: 48 8b fa                     	movq	%rdx, %rdi
   40c4a: b8 00 00 00 00               	movl	$0x0, %eax
   40c4f: 4c 8b d1                     	movq	%rcx, %r10
   40c52: 41 ff d2                     	callq	*%r10
   40c55: 81 7c 24 14 01 01 00 00      	cmpl	$0x101, 0x14(%rsp)      # imm = 0x101
   40c5d: 7d 23                        	jge	0x40c82 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x62>
   40c5f: 48 83 c4 18                  	addq	$0x18, %rsp
   40c63: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40c68: 0f 8e 12 ad 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40c6e: c3                           	retq
   40c6f: 48 8b c2                     	movq	%rdx, %rax
   40c72: 48 83 c4 18                  	addq	$0x18, %rsp
   40c76: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40c7b: 0f 8e ff ac 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40c81: c3                           	retq
   40c82: c7 44 24 10 00 01 00 00      	movl	$0x100, 0x10(%rsp)      # imm = 0x100
   40c8a: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   40c8f: 90                           	nop
   40c90: 48 8b f8                     	movq	%rax, %rdi
   40c93: b8 00 00 00 00               	movl	$0x0, %eax
   40c98: 4c 8b d1                     	movq	%rcx, %r10
   40c9b: 41 ff d2                     	callq	*%r10
   40c9e: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   40ca2: 8d 91 00 01 00 00            	leal	0x100(%rcx), %edx
   40ca8: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   40cac: 3b f2                        	cmpl	%edx, %esi
   40cae: 7e af                        	jle	0x40c5f <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x3f>
   40cb0: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40cb5: 7e 0b                        	jle	0x40cc2 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0xa2>
   40cb7: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   40cbb: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   40cc0: eb ce                        	jmp	0x40c90 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x70>
   40cc2: 48 8b 5c 24 08               	movq	0x8(%rsp), %rbx
   40cc7: e8 b4 ac 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40ccc: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   40cd0: 48 8b cb                     	movq	%rbx, %rcx
   40cd3: eb bb                        	jmp	0x40c90 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x70>
   40cd5: cc                           	int3
   40cd6: cc                           	int3
   40cd7: cc                           	int3
   40cd8: cc                           	int3
   40cd9: cc                           	int3
   40cda: cc                           	int3
   40cdb: cc                           	int3
   40cdc: cc                           	int3
   40cdd: cc                           	int3
   40cde: cc                           	int3
   40cdf: cc                           	int3

# svm: Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G
0000000000040ce0 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G>:
   40ce0: 48 8d 5c 24 c8               	leaq	-0x38(%rsp), %rbx
   40ce5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   40ce9: 0f 86 71 8f 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   40cef: 48 8b e3                     	movq	%rbx, %rsp
   40cf2: 89 74 24 2c                  	movl	%esi, 0x2c(%rsp)
   40cf6: 83 fe 01                     	cmpl	$0x1, %esi
   40cf9: bf 01 00 00 00               	movl	$0x1, %edi
   40cfe: 0f 4e fe                     	cmovlel	%esi, %edi
   40d01: 83 ff 01                     	cmpl	$0x1, %edi
   40d04: 0f 8c 74 01 00 00            	jl	0x40e7e <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x19e>
   40d0a: 48 8b fa                     	movq	%rdx, %rdi
   40d0d: e8 ee 13 00 00               	callq	0x42100 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   40d12: bf 01 00 00 00               	movl	$0x1, %edi
   40d17: 8b 74 24 2c                  	movl	0x2c(%rsp), %esi
   40d1b: 8d 4e fc                     	leal	-0x4(%rsi), %ecx
   40d1e: 81 fe 04 00 00 80            	cmpl	$0x80000004, %esi       # imm = 0x80000004
   40d24: ba 00 00 00 80               	movl	$0x80000000, %edx       # imm = 0x80000000
   40d29: 0f 4c ca                     	cmovll	%edx, %ecx
   40d2c: 89 4c 24 20                  	movl	%ecx, 0x20(%rsp)
   40d30: 8d 51 03                     	leal	0x3(%rcx), %edx
   40d33: 89 54 24 1c                  	movl	%edx, 0x1c(%rsp)
   40d37: 8b df                        	movl	%edi, %ebx
   40d39: 0f 1f 80 00 00 00 00         	nopl	(%rax)
   40d40: 3b cb                        	cmpl	%ebx, %ecx
   40d42: 0f 8e 0a 01 00 00            	jle	0x40e52 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x172>
   40d48: 89 5c 24 24                  	movl	%ebx, 0x24(%rsp)
   40d4c: 8b fa                        	movl	%edx, %edi
   40d4e: 2b fb                        	subl	%ebx, %edi
   40d50: 8b ef                        	movl	%edi, %ebp
   40d52: c1 ed 02                     	shrl	$0x2, %ebp
   40d55: 83 e7 fc                     	andl	$-0x4, %edi
   40d58: 81 fd 00 10 00 00            	cmpl	$0x1000, %ebp           # imm = 0x1000
   40d5e: bd 00 40 00 00               	movl	$0x4000, %ebp           # imm = 0x4000
   40d63: 0f 42 ef                     	cmovbl	%edi, %ebp
   40d66: 83 fd 01                     	cmpl	$0x1, %ebp
   40d69: 0f 8c b6 00 00 00            	jl	0x40e25 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x145>
   40d6f: 89 6c 24 34                  	movl	%ebp, 0x34(%rsp)
   40d73: 48 8b f8                     	movq	%rax, %rdi
   40d76: b8 00 00 00 00               	movl	$0x0, %eax
   40d7b: eb 13                        	jmp	0x40d90 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0xb0>
   40d7d: 66 66 90                     	nop
   40d80: 8b 5c 24 24                  	movl	0x24(%rsp), %ebx
   40d84: 48 89 44 24 10               	movq	%rax, 0x10(%rsp)
   40d89: 8b c7                        	movl	%edi, %eax
   40d8b: 48 8b 7c 24 10               	movq	0x10(%rsp), %rdi
   40d90: 44 8b c0                     	movl	%eax, %r8d
   40d93: 44 03 c3                     	addl	%ebx, %r8d
   40d96: 44 89 44 24 18               	movl	%r8d, 0x18(%rsp)
   40d9b: 89 44 24 28                  	movl	%eax, 0x28(%rsp)
   40d9f: 90                           	nop
   40da0: e8 5b 13 00 00               	callq	0x42100 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   40da5: 44 8b 44 24 18               	movl	0x18(%rsp), %r8d
   40daa: 41 ff c0                     	incl	%r8d
   40dad: 48 8b f8                     	movq	%rax, %rdi
   40db0: 44 89 44 24 30               	movl	%r8d, 0x30(%rsp)
   40db5: e8 46 13 00 00               	callq	0x42100 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   40dba: 44 8b 44 24 18               	movl	0x18(%rsp), %r8d
   40dbf: 41 8d 48 02                  	leal	0x2(%r8), %ecx
   40dc3: 48 8b f8                     	movq	%rax, %rdi
   40dc6: 89 4c 24 30                  	movl	%ecx, 0x30(%rsp)
   40dca: e8 31 13 00 00               	callq	0x42100 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   40dcf: 44 8b 44 24 18               	movl	0x18(%rsp), %r8d
   40dd4: 41 8d 48 03                  	leal	0x3(%r8), %ecx
   40dd8: 48 8b f8                     	movq	%rax, %rdi
   40ddb: 89 4c 24 18                  	movl	%ecx, 0x18(%rsp)
   40ddf: 90                           	nop
   40de0: e8 1b 13 00 00               	callq	0x42100 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   40de5: 8b 7c 24 28                  	movl	0x28(%rsp), %edi
   40de9: 8d 7f 04                     	leal	0x4(%rdi), %edi
   40dec: 8b 6c 24 34                  	movl	0x34(%rsp), %ebp
   40df0: 3b ef                        	cmpl	%edi, %ebp
   40df2: 7f 8c                        	jg	0x40d80 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0xa0>
   40df4: 03 7c 24 24                  	addl	0x24(%rsp), %edi
   40df8: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40dfd: 7e 0f                        	jle	0x40e0e <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x12e>
   40dff: 8b df                        	movl	%edi, %ebx
   40e01: 8b 54 24 1c                  	movl	0x1c(%rsp), %edx
   40e05: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   40e09: e9 32 ff ff ff               	jmp	0x40d40 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x60>
   40e0e: e8 6d ab 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40e13: 8b df                        	movl	%edi, %ebx
   40e15: 8b 54 24 1c                  	movl	0x1c(%rsp), %edx
   40e19: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   40e1d: 66 66 90                     	nop
   40e20: e9 1b ff ff ff               	jmp	0x40d40 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x60>
   40e25: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40e2a: 7e 30                        	jle	0x40e5c <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x17c>
   40e2c: 8b 5c 24 24                  	movl	0x24(%rsp), %ebx
   40e30: e9 0b ff ff ff               	jmp	0x40d40 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x60>
   40e35: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   40e40: 89 5c 24 28                  	movl	%ebx, 0x28(%rsp)
   40e44: 48 8b f8                     	movq	%rax, %rdi
   40e47: e8 b4 12 00 00               	callq	0x42100 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   40e4c: 8b 5c 24 28                  	movl	0x28(%rsp), %ebx
   40e50: ff c3                        	incl	%ebx
   40e52: 8b 74 24 2c                  	movl	0x2c(%rsp), %esi
   40e56: 3b f3                        	cmpl	%ebx, %esi
   40e58: 7f e6                        	jg	0x40e40 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x160>
   40e5a: eb 12                        	jmp	0x40e6e <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x18e>
   40e5c: 0f 1f 40 00                  	nopl	(%rax)
   40e60: e8 1b ab 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40e65: 8b 5c 24 24                  	movl	0x24(%rsp), %ebx
   40e69: e9 d2 fe ff ff               	jmp	0x40d40 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x60>
   40e6e: 48 83 c4 38                  	addq	$0x38, %rsp
   40e72: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40e77: 0f 8e 03 ab 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40e7d: c3                           	retq
   40e7e: bf 00 00 00 00               	movl	$0x0, %edi
   40e83: 48 8b c2                     	movq	%rdx, %rax
   40e86: e9 90 fe ff ff               	jmp	0x40d1b <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x3b>
   40e8b: cc                           	int3
   40e8c: cc                           	int3
   40e8d: cc                           	int3
   40e8e: cc                           	int3
   40e8f: cc                           	int3
   40e90: cc                           	int3
   40e91: cc                           	int3
   40e92: cc                           	int3
   40e93: cc                           	int3
   40e94: cc                           	int3
   40e95: cc                           	int3
   40e96: cc                           	int3
   40e97: cc                           	int3
   40e98: cc                           	int3
   40e99: cc                           	int3
   40e9a: cc                           	int3
   40e9b: cc                           	int3
   40e9c: cc                           	int3
   40e9d: cc                           	int3
   40e9e: cc                           	int3
   40e9f: cc                           	int3

# svm: Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7
0000000000040ea0 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7>:
   40ea0: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   40ea5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   40ea9: 0f 86 b1 8d 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   40eaf: 48 8b e3                     	movq	%rbx, %rsp
   40eb2: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   40eb6: 83 fe 01                     	cmpl	$0x1, %esi
   40eb9: 7c 26                        	jl	0x40ee1 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x41>
   40ebb: 49 8b be 68 f1 3f 00         	movq	0x3ff168(%r14), %rdi
   40ec2: 48 8b f2                     	movq	%rdx, %rsi
   40ec5: e8 76 15 00 00               	callq	0x42440 <Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK>
   40eca: 83 7c 24 14 41               	cmpl	$0x41, 0x14(%rsp)
   40ecf: 7d 23                        	jge	0x40ef4 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x54>
   40ed1: 48 83 c4 18                  	addq	$0x18, %rsp
   40ed5: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40eda: 0f 8e a0 aa 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40ee0: c3                           	retq
   40ee1: 48 8b c2                     	movq	%rdx, %rax
   40ee4: 48 83 c4 18                  	addq	$0x18, %rsp
   40ee8: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40eed: 0f 8e 8d aa 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40ef3: c3                           	retq
   40ef4: 48 8b f0                     	movq	%rax, %rsi
   40ef7: c7 44 24 10 40 00 00 00      	movl	$0x40, 0x10(%rsp)
   40eff: 90                           	nop
   40f00: 49 8b be 68 f1 3f 00         	movq	0x3ff168(%r14), %rdi
   40f07: e8 34 15 00 00               	callq	0x42440 <Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK>
   40f0c: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   40f10: 8d 51 40                     	leal	0x40(%rcx), %edx
   40f13: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   40f17: 3b f2                        	cmpl	%edx, %esi
   40f19: 7e b6                        	jle	0x40ed1 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x31>
   40f1b: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40f20: 7e 09                        	jle	0x40f2b <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x8b>
   40f22: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   40f26: 48 8b f0                     	movq	%rax, %rsi
   40f29: eb d5                        	jmp	0x40f00 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x60>
   40f2b: e8 50 aa 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40f30: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   40f34: 48 8b f0                     	movq	%rax, %rsi
   40f37: eb c7                        	jmp	0x40f00 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x60>
   40f39: cc                           	int3
   40f3a: cc                           	int3
   40f3b: cc                           	int3
   40f3c: cc                           	int3
   40f3d: cc                           	int3
   40f3e: cc                           	int3
   40f3f: cc                           	int3

# svm: Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI
0000000000040f40 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI>:
   40f40: 48 8d 5c 24 b8               	leaq	-0x48(%rsp), %rbx
   40f45: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   40f49: 0f 86 11 8d 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   40f4f: 48 8b e3                     	movq	%rbx, %rsp
   40f52: 8b 4f 08                     	movl	0x8(%rdi), %ecx
   40f55: 89 4c 24 3c                  	movl	%ecx, 0x3c(%rsp)
   40f59: 8b 47 04                     	movl	0x4(%rdi), %eax
   40f5c: 49 8d 14 ce                  	leaq	(%r14,%rcx,8), %rdx
   40f60: 48 89 54 24 30               	movq	%rdx, 0x30(%rsp)
   40f65: 85 c0                        	testl	%eax, %eax
   40f67: 0f 84 e7 03 00 00            	je	0x41354 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x414>
   40f6d: 41 8b 7c c6 04               	movl	0x4(%r14,%rax,8), %edi
   40f72: 85 ff                        	testl	%edi, %edi
   40f74: 0f 84 e4 03 00 00            	je	0x4135e <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x41e>
   40f7a: 66 0f 1f 44 00 00            	nopw	(%rax,%rax)
   40f80: 41 8b 5c fe 10               	movl	0x10(%r14,%rdi,8), %ebx
   40f85: 85 db                        	testl	%ebx, %ebx
   40f87: 74 25                        	je	0x40fae <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x6e>
   40f89: 8b eb                        	movl	%ebx, %ebp
   40f8b: ff cd                        	decl	%ebp
   40f8d: 8b c3                        	movl	%ebx, %eax
   40f8f: f0                           	lock
   40f90: 41 0f b1 6c fe 10            	cmpxchgl	%ebp, 0x10(%r14,%rdi,8)
   40f96: 0f 84 da 02 00 00            	je	0x41276 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x336>
   40f9c: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   40fa1: 7f dd                        	jg	0x40f80 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x40>
   40fa3: 49 8d 04 fe                  	leaq	(%r14,%rdi,8), %rax
   40fa7: e8 d4 a9 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   40fac: eb d2                        	jmp	0x40f80 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x40>
   40fae: 49 8d 9e e8 f3 3f 00         	leaq	0x3ff3e8(%r14), %rbx
   40fb5: 48 89 5c 24 10               	movq	%rbx, 0x10(%rsp)
   40fba: 49 8d ae 30 f4 3f 00         	leaq	0x3ff430(%r14), %rbp
   40fc1: 48 89 6c 24 18               	movq	%rbp, 0x18(%rsp)
   40fc6: 4d 8d 86 08 f7 0e 00         	leaq	0xef708(%r14), %r8
   40fcd: 4c 89 44 24 28               	movq	%r8, 0x28(%rsp)
   40fd2: 4d 8d 8e 40 f0 3f 00         	leaq	0x3ff040(%r14), %r9
   40fd9: 4c 89 4c 24 20               	movq	%r9, 0x20(%rsp)
   40fde: 41 ba 00 00 00 00            	movl	$0x0, %r10d
   40fe4: 0f 1f 84 00 00 00 00 00      	nopl	(%rax,%rax)
   40fec: 66 66 66 90                  	nop
   40ff0: 41 81 fa 00 02 00 00         	cmpl	$0x200, %r10d           # imm = 0x200
   40ff7: 0f 83 28 03 00 00            	jae	0x41325 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3e5>
   40ffd: 49 8b 77 40                  	movq	0x40(%r15), %rsi
   41001: 48 8d 8e 08 00 01 00         	leaq	0x10008(%rsi), %rcx
   41008: 49 3b 4f 38                  	cmpq	0x38(%r15), %rcx
   4100c: 0f 87 e7 01 00 00            	ja	0x411f9 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2b9>
   41012: 49 89 4f 40                  	movq	%rcx, 0x40(%r15)
   41016: c7 06 00 23 20 00            	movl	$0x202300, (%rsi)       # imm = 0x202300
   4101c: c7 46 04 00 00 01 00         	movl	$0x10000, 0x4(%rsi)     # imm = 0x10000
   41023: b9 00 00 01 00               	movl	$0x10000, %ecx          # imm = 0x10000
   41028: 48 8d 7e 08                  	leaq	0x8(%rsi), %rdi
   4102c: 48 33 c0                     	xorq	%rax, %rax
   4102f: f3 48 aa                     	rep		stosb	%al, %es:(%rdi)
   41032: 49 2b f6                     	subq	%r14, %rsi
   41035: 48 c1 ee 03                  	shrq	$0x3, %rsi
   41039: 41 89 b6 d8 f0 3f 00         	movl	%esi, 0x3ff0d8(%r14)
   41040: f0                           	lock
   41041: 83 04 24 00                  	addl	$0x0, (%rsp)
   41045: 41 8b 39                     	movl	(%r9), %edi
   41048: 40 f6 c7 02                  	testb	$0x2, %dil
   4104c: 0f 85 1e 01 00 00            	jne	0x41170 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x230>
   41052: 41 8b c2                     	movl	%r10d, %eax
   41055: ff c0                        	incl	%eax
   41057: 66 0f 1f 84 00 00 00 00 00   	nopw	(%rax,%rax)
   41060: 41 f6 c2 0f                  	testb	$0xf, %r10b
   41064: 74 05                        	je	0x4106b <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x12b>
   41066: 44 8b d0                     	movl	%eax, %r10d
   41069: eb 85                        	jmp	0x40ff0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0xb0>
   4106b: 41 8b be 60 f4 3f 00         	movl	0x3ff460(%r14), %edi
   41072: 81 ff 7b fe 07 00            	cmpl	$0x7fe7b, %edi          # imm = 0x7FE7B
   41078: 74 33                        	je	0x410ad <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x16d>
   4107a: 66 0f 1f 44 00 00            	nopw	(%rax,%rax)
   41080: 85 ff                        	testl	%edi, %edi
   41082: 0f 84 a6 01 00 00            	je	0x4122e <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2ee>
   41088: 41 8b 34 fe                  	movl	(%r14,%rdi,8), %esi
   4108c: c1 ee 05                     	shrl	$0x5, %esi
   4108f: 81 fe 28 01 01 00            	cmpl	$0x10128, %esi          # imm = 0x10128
   41095: 0f 85 aa 02 00 00            	jne	0x41345 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x405>
   4109b: 49 8d 3c fe                  	leaq	(%r14,%rdi,8), %rdi
   4109f: 80 7f 04 00                  	cmpb	$0x0, 0x4(%rdi)
   410a3: 74 0d                        	je	0x410b2 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x172>
   410a5: 44 8b d0                     	movl	%eax, %r10d
   410a8: e9 43 ff ff ff               	jmp	0x40ff0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0xb0>
   410ad: 49 8b f8                     	movq	%r8, %rdi
   410b0: eb ed                        	jmp	0x4109f <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x15f>
   410b2: 41 8b be 14 f4 3f 00         	movl	0x3ff414(%r14), %edi
   410b9: 0f 1f 80 00 00 00 00         	nopl	(%rax)
   410c0: 85 ff                        	testl	%edi, %edi
   410c2: 74 13                        	je	0x410d7 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x197>
   410c4: 41 8b 3c fe                  	movl	(%r14,%rdi,8), %edi
   410c8: c1 ef 05                     	shrl	$0x5, %edi
   410cb: 81 ff 39 01 01 00            	cmpl	$0x10139, %edi          # imm = 0x10139
   410d1: 0f 84 cf 00 00 00            	je	0x411a6 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x266>
   410d7: 89 44 24 44                  	movl	%eax, 0x44(%rsp)
   410db: 44 89 54 24 40               	movl	%r10d, 0x40(%rsp)
   410e0: 48 8b fb                     	movq	%rbx, %rdi
   410e3: 48 8b f5                     	movq	%rbp, %rsi
   410e6: ba 01 00 00 00               	movl	$0x1, %edx
   410eb: e8 30 2e 08 00               	callq	0xc3f20 <GCImpl_collectWithoutAllocating_3JkbhdqsFF0bYizLQcyXX9>
   410f0: 85 c0                        	testl	%eax, %eax
   410f2: 0f 84 b6 00 00 00            	je	0x411ae <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x26e>
   410f8: 41 8b be 14 f4 3f 00         	movl	0x3ff414(%r14), %edi
   410ff: 90                           	nop
   41100: 85 ff                        	testl	%edi, %edi
   41102: 0f 84 67 02 00 00            	je	0x4136f <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x42f>
   41108: 41 8b 04 fe                  	movl	(%r14,%rdi,8), %eax
   4110c: 8b c8                        	movl	%eax, %ecx
   4110e: c1 e9 05                     	shrl	$0x5, %ecx
   41111: ba ff ff 00 00               	movl	$0xffff, %edx           # imm = 0xFFFF
   41116: 41 23 54 ce 08               	andl	0x8(%r14,%rcx,8), %edx
   4111b: 8d 8a e0 fe ff ff            	leal	-0x120(%rdx), %ecx
   41121: 83 f9 0c                     	cmpl	$0xc, %ecx
   41124: 0f 83 4f 02 00 00            	jae	0x41379 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x439>
   4112a: c1 e8 05                     	shrl	$0x5, %eax
   4112d: 49 8b 84 c6 a0 00 00 00      	movq	0xa0(%r14,%rax,8), %rax
   41135: 48 8d 0d 24 9f 07 00         	leaq	0x79f24(%rip), %rcx     # 0xbb060 <CollectionPolicy_onMaximumHeapSizeExceeded_rghEcVm2KwLAMqoiYhXY02>
   4113c: 49 2b cd                     	subq	%r13, %rcx
   4113f: 90                           	nop
   41140: 48 3b c8                     	cmpq	%rax, %rcx
   41143: 0f 84 45 01 00 00            	je	0x4128e <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x34e>
   41149: 49 03 c5                     	addq	%r13, %rax
   4114c: 49 8d 3c fe                  	leaq	(%r14,%rdi,8), %rdi
   41150: ff d0                        	callq	*%rax
   41152: 44 8b 54 24 44               	movl	0x44(%rsp), %r10d
   41157: 4c 8b 4c 24 20               	movq	0x20(%rsp), %r9
   4115c: 4c 8b 44 24 28               	movq	0x28(%rsp), %r8
   41161: 48 8b 6c 24 18               	movq	0x18(%rsp), %rbp
   41166: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   4116b: e9 80 fe ff ff               	jmp	0x40ff0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0xb0>
   41170: 49 8b f1                     	movq	%r9, %rsi
   41173: 40 f6 c7 01                  	testb	$0x1, %dil
   41177: 75 53                        	jne	0x411cc <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x28c>
   41179: 48 8b fe                     	movq	%rsi, %rdi
   4117c: 48 81 e7 00 00 f8 ff         	andq	$-0x80000, %rdi         # imm = 0xFFF80000
   41183: 48 8d 97 40 08 00 00         	leaq	0x840(%rdi), %rdx
   4118a: 48 2b f2                     	subq	%rdx, %rsi
   4118d: 48 c1 ee 09                  	shrq	$0x9, %rsi
   41191: 80 7c 37 38 00               	cmpb	$0x0, 0x38(%rdi,%rsi)
   41196: 0f 84 b6 fe ff ff            	je	0x41052 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x112>
   4119c: c6 44 37 38 00               	movb	$0x0, 0x38(%rdi,%rsi)
   411a1: e9 ac fe ff ff               	jmp	0x41052 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x112>
   411a6: 44 8b d0                     	movl	%eax, %r10d
   411a9: e9 42 fe ff ff               	jmp	0x40ff0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0xb0>
   411ae: 44 8b 54 24 44               	movl	0x44(%rsp), %r10d
   411b3: 4c 8b 4c 24 20               	movq	0x20(%rsp), %r9
   411b8: 4c 8b 44 24 28               	movq	0x28(%rsp), %r8
   411bd: 48 8b 6c 24 18               	movq	0x18(%rsp), %rbp
   411c2: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   411c7: e9 24 fe ff ff               	jmp	0x40ff0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0xb0>
   411cc: 48 8b fe                     	movq	%rsi, %rdi
   411cf: 48 2b 7e f8                  	subq	-0x8(%rsi), %rdi
   411d3: 49 8b f1                     	movq	%r9, %rsi
   411d6: 49 8d 96 d8 f0 3f 00         	leaq	0x3ff0d8(%r14), %rdx
   411dd: 48 2b d6                     	subq	%rsi, %rdx
   411e0: 48 c1 ea 09                  	shrq	$0x9, %rdx
   411e4: 80 7c 17 30 00               	cmpb	$0x0, 0x30(%rdi,%rdx)
   411e9: 0f 84 63 fe ff ff            	je	0x41052 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x112>
   411ef: c6 44 17 30 00               	movb	$0x0, 0x30(%rdi,%rdx)
   411f4: e9 59 fe ff ff               	jmp	0x41052 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x112>
   411f9: 44 89 54 24 40               	movl	%r10d, 0x40(%rsp)
   411fe: bf 00 23 20 00               	movl	$0x202300, %edi         # imm = 0x202300
   41203: be 00 00 01 00               	movl	$0x10000, %esi          # imm = 0x10000
   41208: e8 73 91 0a 00               	callq	0xea380 <GenScavengeAllocationSupport_slowNewArray_RH1677CN3I59XJMcBPW18F>
   4120d: 48 8b f0                     	movq	%rax, %rsi
   41210: 44 8b 54 24 40               	movl	0x40(%rsp), %r10d
   41215: 4c 8b 4c 24 20               	movq	0x20(%rsp), %r9
   4121a: 4c 8b 44 24 28               	movq	0x28(%rsp), %r8
   4121f: 48 8b 6c 24 18               	movq	0x18(%rsp), %rbp
   41224: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   41229: e9 04 fe ff ff               	jmp	0x41032 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0xf2>
   4122e: 44 89 54 24 40               	movl	%r10d, 0x40(%rsp)
   41233: e8 08 e6 19 00               	callq	0x1df840 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   41238: 8b 4c 24 3c                  	movl	0x3c(%rsp), %ecx
   4123c: 0f 1f 40 00                  	nopl	(%rax)
   41240: 85 c9                        	testl	%ecx, %ecx
   41242: 0f 84 22 01 00 00            	je	0x4136a <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x42a>
   41248: 48 8b 54 24 30               	movq	0x30(%rsp), %rdx
   4124d: 49 2b c6                     	subq	%r14, %rax
   41250: 48 c1 e8 03                  	shrq	$0x3, %rax
   41254: 41 89 44 ce 04               	movl	%eax, 0x4(%r14,%rcx,8)
   41259: f0                           	lock
   4125a: 83 04 24 00                  	addl	$0x0, (%rsp)
   4125e: f7 02 02 00 00 00            	testl	$0x2, (%rdx)
   41264: 75 2d                        	jne	0x41293 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x353>
   41266: 48 83 c4 48                  	addq	$0x48, %rsp
   4126a: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4126f: 0f 8e 0b a7 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41275: c3                           	retq
   41276: 83 fb 01                     	cmpl	$0x1, %ebx
   41279: 0f 85 2f fd ff ff            	jne	0x40fae <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x6e>
   4127f: 41 8b 44 fe 08               	movl	0x8(%r14,%rdi,8), %eax
   41284: 85 c0                        	testl	%eax, %eax
   41286: 0f 84 22 fd ff ff            	je	0x40fae <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x6e>
   4128c: eb 30                        	jmp	0x412be <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x37e>
   4128e: e8 6d ee 0c 00               	callq	0x110100 <OutOfMemoryUtil_heapSizeExceeded_tgNQlvtYRwAN6ucK4Y7ahE>
   41293: 48 8b 54 24 30               	movq	0x30(%rsp), %rdx
   41298: 48 8b c2                     	movq	%rdx, %rax
   4129b: 48 81 e0 00 00 f8 ff         	andq	$-0x80000, %rax         # imm = 0xFFF80000
   412a2: 48 8d 88 40 08 00 00         	leaq	0x840(%rax), %rcx
   412a9: 48 2b d1                     	subq	%rcx, %rdx
   412ac: 48 c1 ea 09                  	shrq	$0x9, %rdx
   412b0: 80 7c 10 38 00               	cmpb	$0x0, 0x38(%rax,%rdx)
   412b5: 74 af                        	je	0x41266 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x326>
   412b7: c6 44 10 38 00               	movb	$0x0, 0x38(%rax,%rdx)
   412bc: eb a8                        	jmp	0x41266 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x326>
   412be: 41 8b 7c c6 08               	movl	0x8(%r14,%rax,8), %edi
   412c3: 85 ff                        	testl	%edi, %edi
   412c5: 0f 84 e3 fc ff ff            	je	0x40fae <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x6e>
   412cb: 41 83 7c fe 10 00            	cmpl	$0x0, 0x10(%r14,%rdi,8)
   412d1: 0f 84 d7 fc ff ff            	je	0x40fae <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x6e>
   412d7: 66 0f 1f 84 00 00 00 00 00   	nopw	(%rax,%rax)
   412e0: 41 8b 5c fe 10               	movl	0x10(%r14,%rdi,8), %ebx
   412e5: 8b eb                        	movl	%ebx, %ebp
   412e7: 83 e5 fe                     	andl	$-0x2, %ebp
   412ea: 8b c3                        	movl	%ebx, %eax
   412ec: f0                           	lock
   412ed: 41 0f b1 6c fe 10            	cmpxchgl	%ebp, 0x10(%r14,%rdi,8)
   412f3: 74 12                        	je	0x41307 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3c7>
   412f5: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   412fa: 7f e4                        	jg	0x412e0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3a0>
   412fc: 49 8d 04 fe                  	leaq	(%r14,%rdi,8), %rax
   41300: e8 7b a6 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41305: eb d9                        	jmp	0x412e0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3a0>
   41307: 41 8b 7c fe 0c               	movl	0xc(%r14,%rdi,8), %edi
   4130c: 85 ff                        	testl	%edi, %edi
   4130e: 0f 84 9a fc ff ff            	je	0x40fae <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x6e>
   41314: 49 8d 3c fe                  	leaq	(%r14,%rdi,8), %rdi
   41318: e8 e3 89 1b 00               	callq	0x1f9d00 <PlatformThreads_unpark_FJj4Yhv2Ja3jndtYAdmYl0>
   4131d: 66 66 90                     	nop
   41320: e9 89 fc ff ff               	jmp	0x40fae <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x6e>
   41325: 48 83 c4 48                  	addq	$0x48, %rsp
   41329: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4132e: 0f 8e 4c a6 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41334: c3                           	retq
   41335: e9 fe fe ff ff               	jmp	0x41238 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2f8>
   4133a: e9 f9 fe ff ff               	jmp	0x41238 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2f8>
   4133f: 90                           	nop
   41340: e9 f3 fe ff ff               	jmp	0x41238 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2f8>
   41345: 44 89 54 24 40               	movl	%r10d, 0x40(%rsp)
   4134a: e8 71 db 19 00               	callq	0x1deec0 <ImplicitExceptions_createOptClassCastException_Ewnh8dGuUvIm5VBYxiDX2K>
   4134f: e9 e4 fe ff ff               	jmp	0x41238 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2f8>
   41354: e8 e7 e4 19 00               	callq	0x1df840 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   41359: e9 da fe ff ff               	jmp	0x41238 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2f8>
   4135e: 66 90                        	nop
   41360: e8 db e4 19 00               	callq	0x1df840 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   41365: e9 ce fe ff ff               	jmp	0x41238 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2f8>
   4136a: e8 f1 69 1a 00               	callq	0x1e7d60 <ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
   4136f: e8 cc e4 19 00               	callq	0x1df840 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   41374: e9 bf fe ff ff               	jmp	0x41238 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2f8>
   41379: 44 8b 54 24 40               	movl	0x40(%rsp), %r10d
   4137e: 66 90                        	nop
   41380: e8 fb df 19 00               	callq	0x1df380 <ImplicitExceptions_createOptIncompatibleClassChangeError_ExDFiMUY3I5T00lGxxhlnL>
   41385: e9 ae fe ff ff               	jmp	0x41238 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2f8>
   4138a: e9 a9 fe ff ff               	jmp	0x41238 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2f8>
   4138f: cc                           	int3
   41390: cc                           	int3
   41391: cc                           	int3
   41392: cc                           	int3
   41393: cc                           	int3
   41394: cc                           	int3
   41395: cc                           	int3
   41396: cc                           	int3
   41397: cc                           	int3
   41398: cc                           	int3
   41399: cc                           	int3
   4139a: cc                           	int3
   4139b: cc                           	int3
   4139c: cc                           	int3
   4139d: cc                           	int3
   4139e: cc                           	int3
   4139f: cc                           	int3

# svm: Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI
00000000000413a0 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI>:
   413a0: 48 8d 5c 24 c8               	leaq	-0x38(%rsp), %rbx
   413a5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   413a9: 0f 86 b1 88 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   413af: 48 8b e3                     	movq	%rbx, %rsp
   413b2: 89 74 24 28                  	movl	%esi, 0x28(%rsp)
   413b6: 49 8b 8e 70 f1 3f 00         	movq	0x3ff170(%r14), %rcx
   413bd: 48 89 4c 24 10               	movq	%rcx, 0x10(%rsp)
   413c2: 83 fe 01                     	cmpl	$0x1, %esi
   413c5: bf 01 00 00 00               	movl	$0x1, %edi
   413ca: 0f 4e fe                     	cmovlel	%esi, %edi
   413cd: 83 ff 01                     	cmpl	$0x1, %edi
   413d0: 0f 8c bf 01 00 00            	jl	0x41595 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1f5>
   413d6: 48 8b fa                     	movq	%rdx, %rdi
   413d9: b8 00 00 00 00               	movl	$0x0, %eax
   413de: 4c 8b d1                     	movq	%rcx, %r10
   413e1: 41 ff d2                     	callq	*%r10
   413e4: bf 01 00 00 00               	movl	$0x1, %edi
   413e9: 8b 74 24 28                  	movl	0x28(%rsp), %esi
   413ed: 44 8d 56 fc                  	leal	-0x4(%rsi), %r10d
   413f1: 81 fe 04 00 00 80            	cmpl	$0x80000004, %esi       # imm = 0x80000004
   413f7: b9 00 00 00 80               	movl	$0x80000000, %ecx       # imm = 0x80000000
   413fc: 44 0f 4c d1                  	cmovll	%ecx, %r10d
   41400: 44 89 54 24 18               	movl	%r10d, 0x18(%rsp)
   41405: 41 8d 4a 03                  	leal	0x3(%r10), %ecx
   41409: 89 4c 24 20                  	movl	%ecx, 0x20(%rsp)
   4140d: 8b d7                        	movl	%edi, %edx
   4140f: 90                           	nop
   41410: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   41415: 44 3b d2                     	cmpl	%edx, %r10d
   41418: 0f 8e 27 01 00 00            	jle	0x41545 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1a5>
   4141e: 89 54 24 24                  	movl	%edx, 0x24(%rsp)
   41422: 8b f9                        	movl	%ecx, %edi
   41424: 2b fa                        	subl	%edx, %edi
   41426: 8b ef                        	movl	%edi, %ebp
   41428: c1 ed 02                     	shrl	$0x2, %ebp
   4142b: 83 e7 fc                     	andl	$-0x4, %edi
   4142e: 81 fd 00 10 00 00            	cmpl	$0x1000, %ebp           # imm = 0x1000
   41434: bd 00 40 00 00               	movl	$0x4000, %ebp           # imm = 0x4000
   41439: 0f 42 ef                     	cmovbl	%edi, %ebp
   4143c: 0f 1f 40 00                  	nopl	(%rax)
   41440: 83 fd 01                     	cmpl	$0x1, %ebp
   41443: 0f 8c de 00 00 00            	jl	0x41527 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x187>
   41449: 89 6c 24 30                  	movl	%ebp, 0x30(%rsp)
   4144d: 48 8b f8                     	movq	%rax, %rdi
   41450: b9 00 00 00 00               	movl	$0x0, %ecx
   41455: eb 15                        	jmp	0x4146c <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0xcc>
   41457: 66 0f 1f 84 00 00 00 00 00   	nopw	(%rax,%rax)
   41460: 48 8b f8                     	movq	%rax, %rdi
   41463: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   41467: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   4146c: 44 8b c9                     	movl	%ecx, %r9d
   4146f: 44 03 ca                     	addl	%edx, %r9d
   41472: 44 89 4c 24 1c               	movl	%r9d, 0x1c(%rsp)
   41477: b8 00 00 00 00               	movl	$0x0, %eax
   4147c: 4c 8b d3                     	movq	%rbx, %r10
   4147f: 89 4c 24 34                  	movl	%ecx, 0x34(%rsp)
   41483: 41 ff d2                     	callq	*%r10
   41486: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   4148b: 41 ff c1                     	incl	%r9d
   4148e: 48 8b f8                     	movq	%rax, %rdi
   41491: b8 00 00 00 00               	movl	$0x0, %eax
   41496: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   4149b: 44 89 4c 24 2c               	movl	%r9d, 0x2c(%rsp)
   414a0: 41 ff d2                     	callq	*%r10
   414a3: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   414a8: 41 8d 49 02                  	leal	0x2(%r9), %ecx
   414ac: 48 8b f8                     	movq	%rax, %rdi
   414af: b8 00 00 00 00               	movl	$0x0, %eax
   414b4: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   414b9: 89 4c 24 2c                  	movl	%ecx, 0x2c(%rsp)
   414bd: 66 66 90                     	nop
   414c0: 41 ff d2                     	callq	*%r10
   414c3: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   414c8: 41 8d 49 03                  	leal	0x3(%r9), %ecx
   414cc: 48 8b f8                     	movq	%rax, %rdi
   414cf: b8 00 00 00 00               	movl	$0x0, %eax
   414d4: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   414d9: 89 4c 24 1c                  	movl	%ecx, 0x1c(%rsp)
   414dd: 66 66 90                     	nop
   414e0: 41 ff d2                     	callq	*%r10
   414e3: 8b 4c 24 34                  	movl	0x34(%rsp), %ecx
   414e7: 8d 49 04                     	leal	0x4(%rcx), %ecx
   414ea: 8b 6c 24 30                  	movl	0x30(%rsp), %ebp
   414ee: 3b e9                        	cmpl	%ecx, %ebp
   414f0: 0f 8f 6a ff ff ff            	jg	0x41460 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0xc0>
   414f6: 03 4c 24 24                  	addl	0x24(%rsp), %ecx
   414fa: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   414ff: 90                           	nop
   41500: 7e 10                        	jle	0x41512 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x172>
   41502: 8b d1                        	movl	%ecx, %edx
   41504: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   41508: 44 8b 54 24 18               	movl	0x18(%rsp), %r10d
   4150d: e9 fe fe ff ff               	jmp	0x41410 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x70>
   41512: e8 69 a4 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41517: 8b d1                        	movl	%ecx, %edx
   41519: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   4151d: 44 8b 54 24 18               	movl	0x18(%rsp), %r10d
   41522: e9 e9 fe ff ff               	jmp	0x41410 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x70>
   41527: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4152c: 7e 09                        	jle	0x41537 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x197>
   4152e: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   41532: e9 d9 fe ff ff               	jmp	0x41410 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x70>
   41537: e8 44 a4 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   4153c: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   41540: e9 cb fe ff ff               	jmp	0x41410 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x70>
   41545: 48 8b cb                     	movq	%rbx, %rcx
   41548: eb 33                        	jmp	0x4157d <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1dd>
   4154a: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   41555: 66 66 66 90                  	nop
   41559: 0f 1f 80 00 00 00 00         	nopl	(%rax)
   41560: 89 54 24 18                  	movl	%edx, 0x18(%rsp)
   41564: 48 8b f8                     	movq	%rax, %rdi
   41567: b8 00 00 00 00               	movl	$0x0, %eax
   4156c: 4c 8b d1                     	movq	%rcx, %r10
   4156f: 41 ff d2                     	callq	*%r10
   41572: 8b 54 24 18                  	movl	0x18(%rsp), %edx
   41576: ff c2                        	incl	%edx
   41578: 48 8b 4c 24 10               	movq	0x10(%rsp), %rcx
   4157d: 8b 74 24 28                  	movl	0x28(%rsp), %esi
   41581: 3b f2                        	cmpl	%edx, %esi
   41583: 7f db                        	jg	0x41560 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1c0>
   41585: 48 83 c4 38                  	addq	$0x38, %rsp
   41589: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4158e: 0f 8e ec a3 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41594: c3                           	retq
   41595: bf 00 00 00 00               	movl	$0x0, %edi
   4159a: 48 8b c2                     	movq	%rdx, %rax
   4159d: 66 66 90                     	nop
   415a0: e9 48 fe ff ff               	jmp	0x413ed <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x4d>
   415a5: cc                           	int3
   415a6: cc                           	int3
   415a7: cc                           	int3
   415a8: cc                           	int3
   415a9: cc                           	int3
   415aa: cc                           	int3
   415ab: cc                           	int3
   415ac: cc                           	int3
   415ad: cc                           	int3
   415ae: cc                           	int3
   415af: cc                           	int3
   415b0: cc                           	int3
   415b1: cc                           	int3
   415b2: cc                           	int3
   415b3: cc                           	int3
   415b4: cc                           	int3
   415b5: cc                           	int3
   415b6: cc                           	int3
   415b7: cc                           	int3
   415b8: cc                           	int3
   415b9: cc                           	int3
   415ba: cc                           	int3
   415bb: cc                           	int3
   415bc: cc                           	int3
   415bd: cc                           	int3
   415be: cc                           	int3
   415bf: cc                           	int3

# svm: Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15
00000000000415c0 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15>:
   415c0: 48 8d 5c 24 b8               	leaq	-0x48(%rsp), %rbx
   415c5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   415c9: 0f 86 91 86 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   415cf: 48 8b e3                     	movq	%rbx, %rsp
   415d2: 89 74 24 18                  	movl	%esi, 0x18(%rsp)
   415d6: 48 8b 0d 0b f2 88 00         	movq	0x88f20b(%rip), %rcx    # 0x8d07e8 <__svm_vm_is_static_binary+0x3b0>
   415dd: 48 89 4c 24 10               	movq	%rcx, 0x10(%rsp)
   415e2: 48 8b c2                     	movq	%rdx, %rax
   415e5: ba 00 00 00 00               	movl	$0x0, %edx
   415ea: e9 8e 00 00 00               	jmp	0x4167d <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xbd>
   415ef: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   415fa: 66 66 66 90                  	nop
   415fe: 66 90                        	nop
   41600: 89 54 24 1c                  	movl	%edx, 0x1c(%rsp)
   41604: 48 8d 5c 24 20               	leaq	0x20(%rsp), %rbx
   41609: 48 c7 43 08 00 00 00 00      	movq	$0x0, 0x8(%rbx)
   41611: 48 c7 43 10 00 00 00 00      	movq	$0x0, 0x10(%rbx)
   41619: 49 8b 7f 18                  	movq	0x18(%r15), %rdi
   4161d: 48 89 7b 18                  	movq	%rdi, 0x18(%rbx)
   41621: 49 89 5f 18                  	movq	%rbx, 0x18(%r15)
   41625: 48 8b f8                     	movq	%rax, %rdi
   41628: b8 00 00 00 00               	movl	$0x0, %eax
   4162d: 4c 8b d1                     	movq	%rcx, %r10
   41630: 4c 8d 25 00 00 00 00         	leaq	(%rip), %r12            # 0x41637 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x77>
   41637: 4c 89 63 08                  	movq	%r12, 0x8(%rbx)
   4163b: 48 89 63 10                  	movq	%rsp, 0x10(%rbx)
   4163f: 41 c7 47 14 03 00 00 00      	movl	$0x3, 0x14(%r15)
   41647: 41 ff d2                     	callq	*%r10
   4164a: bf 01 00 00 00               	movl	$0x1, %edi
   4164f: 48 8b c8                     	movq	%rax, %rcx
   41652: b8 03 00 00 00               	movl	$0x3, %eax
   41657: f0                           	lock
   41658: 41 0f b1 7f 14               	cmpxchgl	%edi, 0x14(%r15)
   4165d: 75 2b                        	jne	0x4168a <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xca>
   4165f: 49 8b 7f 18                  	movq	0x18(%r15), %rdi
   41663: 48 8b 7f 18                  	movq	0x18(%rdi), %rdi
   41667: 49 89 7f 18                  	movq	%rdi, 0x18(%r15)
   4166b: 8b 54 24 1c                  	movl	0x1c(%rsp), %edx
   4166f: ff c2                        	incl	%edx
   41671: 48 8b c1                     	movq	%rcx, %rax
   41674: 48 8b 4c 24 10               	movq	0x10(%rsp), %rcx
   41679: 8b 74 24 18                  	movl	0x18(%rsp), %esi
   4167d: 66 66 90                     	nop
   41680: 3b f2                        	cmpl	%edx, %esi
   41682: 0f 8f 78 ff ff ff            	jg	0x41600 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x40>
   41688: eb 1b                        	jmp	0x416a5 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xe5>
   4168a: 48 89 4c 24 08               	movq	%rcx, 0x8(%rsp)
   4168f: bf 01 00 00 00               	movl	$0x1, %edi
   41694: be 01 00 00 00               	movl	$0x1, %esi
   41699: e8 22 af 1b 00               	callq	0x1fc5c0 <SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
   4169e: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   416a3: eb c6                        	jmp	0x4166b <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xab>
   416a5: 48 83 c4 48                  	addq	$0x48, %rsp
   416a9: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   416ae: 0f 8e cc a2 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   416b4: c3                           	retq
   416b5: cc                           	int3
   416b6: cc                           	int3
   416b7: cc                           	int3
   416b8: cc                           	int3
   416b9: cc                           	int3
   416ba: cc                           	int3
   416bb: cc                           	int3
   416bc: cc                           	int3
   416bd: cc                           	int3
   416be: cc                           	int3
   416bf: cc                           	int3

# svm: Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH
00000000000416c0 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH>:
   416c0: 48 8d 5c 24 c8               	leaq	-0x38(%rsp), %rbx
   416c5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   416c9: 0f 86 91 85 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   416cf: 48 8b e3                     	movq	%rbx, %rsp
   416d2: 89 74 24 28                  	movl	%esi, 0x28(%rsp)
   416d6: 48 8b 0d 03 f0 88 00         	movq	0x88f003(%rip), %rcx    # 0x8d06e0 <__svm_vm_is_static_binary+0x2a8>
   416dd: 48 89 4c 24 10               	movq	%rcx, 0x10(%rsp)
   416e2: 83 fe 01                     	cmpl	$0x1, %esi
   416e5: bf 01 00 00 00               	movl	$0x1, %edi
   416ea: 0f 4e fe                     	cmovlel	%esi, %edi
   416ed: 83 ff 01                     	cmpl	$0x1, %edi
   416f0: 0f 8c bf 01 00 00            	jl	0x418b5 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x1f5>
   416f6: 48 8b fa                     	movq	%rdx, %rdi
   416f9: b8 00 00 00 00               	movl	$0x0, %eax
   416fe: 4c 8b d1                     	movq	%rcx, %r10
   41701: 41 ff d2                     	callq	*%r10
   41704: bf 01 00 00 00               	movl	$0x1, %edi
   41709: 8b 74 24 28                  	movl	0x28(%rsp), %esi
   4170d: 44 8d 56 fc                  	leal	-0x4(%rsi), %r10d
   41711: 81 fe 04 00 00 80            	cmpl	$0x80000004, %esi       # imm = 0x80000004
   41717: b9 00 00 00 80               	movl	$0x80000000, %ecx       # imm = 0x80000000
   4171c: 44 0f 4c d1                  	cmovll	%ecx, %r10d
   41720: 44 89 54 24 18               	movl	%r10d, 0x18(%rsp)
   41725: 41 8d 4a 03                  	leal	0x3(%r10), %ecx
   41729: 89 4c 24 20                  	movl	%ecx, 0x20(%rsp)
   4172d: 8b d7                        	movl	%edi, %edx
   4172f: 90                           	nop
   41730: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   41735: 44 3b d2                     	cmpl	%edx, %r10d
   41738: 0f 8e 27 01 00 00            	jle	0x41865 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x1a5>
   4173e: 89 54 24 24                  	movl	%edx, 0x24(%rsp)
   41742: 8b f9                        	movl	%ecx, %edi
   41744: 2b fa                        	subl	%edx, %edi
   41746: 8b ef                        	movl	%edi, %ebp
   41748: c1 ed 02                     	shrl	$0x2, %ebp
   4174b: 83 e7 fc                     	andl	$-0x4, %edi
   4174e: 81 fd 00 10 00 00            	cmpl	$0x1000, %ebp           # imm = 0x1000
   41754: bd 00 40 00 00               	movl	$0x4000, %ebp           # imm = 0x4000
   41759: 0f 42 ef                     	cmovbl	%edi, %ebp
   4175c: 0f 1f 40 00                  	nopl	(%rax)
   41760: 83 fd 01                     	cmpl	$0x1, %ebp
   41763: 0f 8c de 00 00 00            	jl	0x41847 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x187>
   41769: 89 6c 24 30                  	movl	%ebp, 0x30(%rsp)
   4176d: 48 8b f8                     	movq	%rax, %rdi
   41770: b9 00 00 00 00               	movl	$0x0, %ecx
   41775: eb 15                        	jmp	0x4178c <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0xcc>
   41777: 66 0f 1f 84 00 00 00 00 00   	nopw	(%rax,%rax)
   41780: 48 8b f8                     	movq	%rax, %rdi
   41783: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   41787: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   4178c: 44 8b c9                     	movl	%ecx, %r9d
   4178f: 44 03 ca                     	addl	%edx, %r9d
   41792: 44 89 4c 24 1c               	movl	%r9d, 0x1c(%rsp)
   41797: b8 00 00 00 00               	movl	$0x0, %eax
   4179c: 4c 8b d3                     	movq	%rbx, %r10
   4179f: 89 4c 24 34                  	movl	%ecx, 0x34(%rsp)
   417a3: 41 ff d2                     	callq	*%r10
   417a6: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   417ab: 41 ff c1                     	incl	%r9d
   417ae: 48 8b f8                     	movq	%rax, %rdi
   417b1: b8 00 00 00 00               	movl	$0x0, %eax
   417b6: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   417bb: 44 89 4c 24 2c               	movl	%r9d, 0x2c(%rsp)
   417c0: 41 ff d2                     	callq	*%r10
   417c3: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   417c8: 41 8d 49 02                  	leal	0x2(%r9), %ecx
   417cc: 48 8b f8                     	movq	%rax, %rdi
   417cf: b8 00 00 00 00               	movl	$0x0, %eax
   417d4: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   417d9: 89 4c 24 2c                  	movl	%ecx, 0x2c(%rsp)
   417dd: 66 66 90                     	nop
   417e0: 41 ff d2                     	callq	*%r10
   417e3: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   417e8: 41 8d 49 03                  	leal	0x3(%r9), %ecx
   417ec: 48 8b f8                     	movq	%rax, %rdi
   417ef: b8 00 00 00 00               	movl	$0x0, %eax
   417f4: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   417f9: 89 4c 24 1c                  	movl	%ecx, 0x1c(%rsp)
   417fd: 66 66 90                     	nop
   41800: 41 ff d2                     	callq	*%r10
   41803: 8b 4c 24 34                  	movl	0x34(%rsp), %ecx
   41807: 8d 49 04                     	leal	0x4(%rcx), %ecx
   4180a: 8b 6c 24 30                  	movl	0x30(%rsp), %ebp
   4180e: 3b e9                        	cmpl	%ecx, %ebp
   41810: 0f 8f 6a ff ff ff            	jg	0x41780 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0xc0>
   41816: 03 4c 24 24                  	addl	0x24(%rsp), %ecx
   4181a: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4181f: 90                           	nop
   41820: 7e 10                        	jle	0x41832 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x172>
   41822: 8b d1                        	movl	%ecx, %edx
   41824: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   41828: 44 8b 54 24 18               	movl	0x18(%rsp), %r10d
   4182d: e9 fe fe ff ff               	jmp	0x41730 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x70>
   41832: e8 49 a1 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41837: 8b d1                        	movl	%ecx, %edx
   41839: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   4183d: 44 8b 54 24 18               	movl	0x18(%rsp), %r10d
   41842: e9 e9 fe ff ff               	jmp	0x41730 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x70>
   41847: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   4184c: 7e 09                        	jle	0x41857 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x197>
   4184e: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   41852: e9 d9 fe ff ff               	jmp	0x41730 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x70>
   41857: e8 24 a1 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   4185c: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   41860: e9 cb fe ff ff               	jmp	0x41730 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x70>
   41865: 48 8b cb                     	movq	%rbx, %rcx
   41868: eb 33                        	jmp	0x4189d <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x1dd>
   4186a: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   41875: 66 66 66 90                  	nop
   41879: 0f 1f 80 00 00 00 00         	nopl	(%rax)
   41880: 89 54 24 18                  	movl	%edx, 0x18(%rsp)
   41884: 48 8b f8                     	movq	%rax, %rdi
   41887: b8 00 00 00 00               	movl	$0x0, %eax
   4188c: 4c 8b d1                     	movq	%rcx, %r10
   4188f: 41 ff d2                     	callq	*%r10
   41892: 8b 54 24 18                  	movl	0x18(%rsp), %edx
   41896: ff c2                        	incl	%edx
   41898: 48 8b 4c 24 10               	movq	0x10(%rsp), %rcx
   4189d: 8b 74 24 28                  	movl	0x28(%rsp), %esi
   418a1: 3b f2                        	cmpl	%edx, %esi
   418a3: 7f db                        	jg	0x41880 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x1c0>
   418a5: 48 83 c4 38                  	addq	$0x38, %rsp
   418a9: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   418ae: 0f 8e cc a0 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   418b4: c3                           	retq
   418b5: bf 00 00 00 00               	movl	$0x0, %edi
   418ba: 48 8b c2                     	movq	%rdx, %rax
   418bd: 66 66 90                     	nop
   418c0: e9 48 fe ff ff               	jmp	0x4170d <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x4d>
   418c5: cc                           	int3
   418c6: cc                           	int3
   418c7: cc                           	int3
   418c8: cc                           	int3
   418c9: cc                           	int3
   418ca: cc                           	int3
   418cb: cc                           	int3
   418cc: cc                           	int3
   418cd: cc                           	int3
   418ce: cc                           	int3
   418cf: cc                           	int3
   418d0: cc                           	int3
   418d1: cc                           	int3
   418d2: cc                           	int3
   418d3: cc                           	int3
   418d4: cc                           	int3
   418d5: cc                           	int3
   418d6: cc                           	int3
   418d7: cc                           	int3
   418d8: cc                           	int3
   418d9: cc                           	int3
   418da: cc                           	int3
   418db: cc                           	int3
   418dc: cc                           	int3
   418dd: cc                           	int3
   418de: cc                           	int3
   418df: cc                           	int3

# svm: Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB
00000000000418e0 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB>:
   418e0: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   418e5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   418e9: 0f 86 71 83 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   418ef: 48 8b e3                     	movq	%rbx, %rsp
   418f2: 83 fe 01                     	cmpl	$0x1, %esi
   418f5: 0f 8c 70 01 00 00            	jl	0x41a6b <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x18b>
   418fb: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   418ff: 49 8b 8e 70 f1 3f 00         	movq	0x3ff170(%r14), %rcx
   41906: 48 89 4c 24 08               	movq	%rcx, 0x8(%rsp)
   4190b: c7 44 24 10 00 00 00 00      	movl	$0x0, 0x10(%rsp)
   41913: 66 0f 1f 84 00 00 00 00 00   	nopw	(%rax,%rax)
   4191c: 66 66 66 90                  	nop
   41920: 48 8b fa                     	movq	%rdx, %rdi
   41923: b8 00 00 00 00               	movl	$0x0, %eax
   41928: 4c 8b d1                     	movq	%rcx, %r10
   4192b: 41 ff d2                     	callq	*%r10
   4192e: 48 8b f8                     	movq	%rax, %rdi
   41931: b8 00 00 00 00               	movl	$0x0, %eax
   41936: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4193b: 41 ff d2                     	callq	*%r10
   4193e: 48 8b f8                     	movq	%rax, %rdi
   41941: b8 00 00 00 00               	movl	$0x0, %eax
   41946: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4194b: 41 ff d2                     	callq	*%r10
   4194e: 48 8b f8                     	movq	%rax, %rdi
   41951: b8 00 00 00 00               	movl	$0x0, %eax
   41956: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4195b: 41 ff d2                     	callq	*%r10
   4195e: 48 8b f8                     	movq	%rax, %rdi
   41961: b8 00 00 00 00               	movl	$0x0, %eax
   41966: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4196b: 41 ff d2                     	callq	*%r10
   4196e: 48 8b f8                     	movq	%rax, %rdi
   41971: b8 00 00 00 00               	movl	$0x0, %eax
   41976: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4197b: 41 ff d2                     	callq	*%r10
   4197e: 48 8b f8                     	movq	%rax, %rdi
   41981: b8 00 00 00 00               	movl	$0x0, %eax
   41986: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4198b: 41 ff d2                     	callq	*%r10
   4198e: 48 8b f8                     	movq	%rax, %rdi
   41991: b8 00 00 00 00               	movl	$0x0, %eax
   41996: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4199b: 41 ff d2                     	callq	*%r10
   4199e: 48 8b f8                     	movq	%rax, %rdi
   419a1: b8 00 00 00 00               	movl	$0x0, %eax
   419a6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   419ab: 41 ff d2                     	callq	*%r10
   419ae: 48 8b f8                     	movq	%rax, %rdi
   419b1: b8 00 00 00 00               	movl	$0x0, %eax
   419b6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   419bb: 41 ff d2                     	callq	*%r10
   419be: 48 8b f8                     	movq	%rax, %rdi
   419c1: b8 00 00 00 00               	movl	$0x0, %eax
   419c6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   419cb: 41 ff d2                     	callq	*%r10
   419ce: 48 8b f8                     	movq	%rax, %rdi
   419d1: b8 00 00 00 00               	movl	$0x0, %eax
   419d6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   419db: 41 ff d2                     	callq	*%r10
   419de: 48 8b f8                     	movq	%rax, %rdi
   419e1: b8 00 00 00 00               	movl	$0x0, %eax
   419e6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   419eb: 41 ff d2                     	callq	*%r10
   419ee: 48 8b f8                     	movq	%rax, %rdi
   419f1: b8 00 00 00 00               	movl	$0x0, %eax
   419f6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   419fb: 41 ff d2                     	callq	*%r10
   419fe: 48 8b f8                     	movq	%rax, %rdi
   41a01: b8 00 00 00 00               	movl	$0x0, %eax
   41a06: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41a0b: 41 ff d2                     	callq	*%r10
   41a0e: 48 8b f8                     	movq	%rax, %rdi
   41a11: b8 00 00 00 00               	movl	$0x0, %eax
   41a16: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41a1b: 41 ff d2                     	callq	*%r10
   41a1e: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   41a22: 8d 51 10                     	leal	0x10(%rcx), %edx
   41a25: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   41a29: 3b f2                        	cmpl	%edx, %esi
   41a2b: 7e 2e                        	jle	0x41a5b <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x17b>
   41a2d: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41a32: 7e 11                        	jle	0x41a45 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x165>
   41a34: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   41a38: 48 8b d0                     	movq	%rax, %rdx
   41a3b: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   41a40: e9 db fe ff ff               	jmp	0x41920 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x40>
   41a45: e8 36 9f 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41a4a: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   41a4e: 48 8b d0                     	movq	%rax, %rdx
   41a51: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   41a56: e9 c5 fe ff ff               	jmp	0x41920 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x40>
   41a5b: 48 83 c4 18                  	addq	$0x18, %rsp
   41a5f: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41a64: 0f 8e 16 9f 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41a6a: c3                           	retq
   41a6b: 48 8b c2                     	movq	%rdx, %rax
   41a6e: eb eb                        	jmp	0x41a5b <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x17b>
   41a70: cc                           	int3
   41a71: cc                           	int3
   41a72: cc                           	int3
   41a73: cc                           	int3
   41a74: cc                           	int3
   41a75: cc                           	int3
   41a76: cc                           	int3
   41a77: cc                           	int3
   41a78: cc                           	int3
   41a79: cc                           	int3
   41a7a: cc                           	int3
   41a7b: cc                           	int3
   41a7c: cc                           	int3
   41a7d: cc                           	int3
   41a7e: cc                           	int3
   41a7f: cc                           	int3

# svm: Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK
0000000000041a80 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK>:
   41a80: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   41a85: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   41a89: 0f 86 d1 81 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   41a8f: 48 8b e3                     	movq	%rbx, %rsp
   41a92: 83 fe 01                     	cmpl	$0x1, %esi
   41a95: 0f 8c 70 01 00 00            	jl	0x41c0b <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x18b>
   41a9b: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   41a9f: 49 8b 8e 70 f1 3f 00         	movq	0x3ff170(%r14), %rcx
   41aa6: 48 89 4c 24 08               	movq	%rcx, 0x8(%rsp)
   41aab: c7 44 24 10 00 00 00 00      	movl	$0x0, 0x10(%rsp)
   41ab3: 66 0f 1f 84 00 00 00 00 00   	nopw	(%rax,%rax)
   41abc: 66 66 66 90                  	nop
   41ac0: 48 8b fa                     	movq	%rdx, %rdi
   41ac3: b8 00 00 00 00               	movl	$0x0, %eax
   41ac8: 4c 8b d1                     	movq	%rcx, %r10
   41acb: 41 ff d2                     	callq	*%r10
   41ace: 48 8b f8                     	movq	%rax, %rdi
   41ad1: b8 00 00 00 00               	movl	$0x0, %eax
   41ad6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41adb: 41 ff d2                     	callq	*%r10
   41ade: 48 8b f8                     	movq	%rax, %rdi
   41ae1: b8 00 00 00 00               	movl	$0x0, %eax
   41ae6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41aeb: 41 ff d2                     	callq	*%r10
   41aee: 48 8b f8                     	movq	%rax, %rdi
   41af1: b8 00 00 00 00               	movl	$0x0, %eax
   41af6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41afb: 41 ff d2                     	callq	*%r10
   41afe: 48 8b f8                     	movq	%rax, %rdi
   41b01: b8 00 00 00 00               	movl	$0x0, %eax
   41b06: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41b0b: 41 ff d2                     	callq	*%r10
   41b0e: 48 8b f8                     	movq	%rax, %rdi
   41b11: b8 00 00 00 00               	movl	$0x0, %eax
   41b16: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41b1b: 41 ff d2                     	callq	*%r10
   41b1e: 48 8b f8                     	movq	%rax, %rdi
   41b21: b8 00 00 00 00               	movl	$0x0, %eax
   41b26: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41b2b: 41 ff d2                     	callq	*%r10
   41b2e: 48 8b f8                     	movq	%rax, %rdi
   41b31: b8 00 00 00 00               	movl	$0x0, %eax
   41b36: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41b3b: 41 ff d2                     	callq	*%r10
   41b3e: 48 8b f8                     	movq	%rax, %rdi
   41b41: b8 00 00 00 00               	movl	$0x0, %eax
   41b46: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41b4b: 41 ff d2                     	callq	*%r10
   41b4e: 48 8b f8                     	movq	%rax, %rdi
   41b51: b8 00 00 00 00               	movl	$0x0, %eax
   41b56: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41b5b: 41 ff d2                     	callq	*%r10
   41b5e: 48 8b f8                     	movq	%rax, %rdi
   41b61: b8 00 00 00 00               	movl	$0x0, %eax
   41b66: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41b6b: 41 ff d2                     	callq	*%r10
   41b6e: 48 8b f8                     	movq	%rax, %rdi
   41b71: b8 00 00 00 00               	movl	$0x0, %eax
   41b76: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41b7b: 41 ff d2                     	callq	*%r10
   41b7e: 48 8b f8                     	movq	%rax, %rdi
   41b81: b8 00 00 00 00               	movl	$0x0, %eax
   41b86: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41b8b: 41 ff d2                     	callq	*%r10
   41b8e: 48 8b f8                     	movq	%rax, %rdi
   41b91: b8 00 00 00 00               	movl	$0x0, %eax
   41b96: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41b9b: 41 ff d2                     	callq	*%r10
   41b9e: 48 8b f8                     	movq	%rax, %rdi
   41ba1: b8 00 00 00 00               	movl	$0x0, %eax
   41ba6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41bab: 41 ff d2                     	callq	*%r10
   41bae: 48 8b f8                     	movq	%rax, %rdi
   41bb1: b8 00 00 00 00               	movl	$0x0, %eax
   41bb6: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   41bbb: 41 ff d2                     	callq	*%r10
   41bbe: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   41bc2: 8d 51 10                     	leal	0x10(%rcx), %edx
   41bc5: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   41bc9: 3b f2                        	cmpl	%edx, %esi
   41bcb: 7e 2e                        	jle	0x41bfb <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x17b>
   41bcd: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41bd2: 7e 11                        	jle	0x41be5 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x165>
   41bd4: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   41bd8: 48 8b d0                     	movq	%rax, %rdx
   41bdb: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   41be0: e9 db fe ff ff               	jmp	0x41ac0 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x40>
   41be5: e8 96 9d 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41bea: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   41bee: 48 8b d0                     	movq	%rax, %rdx
   41bf1: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   41bf6: e9 c5 fe ff ff               	jmp	0x41ac0 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x40>
   41bfb: 48 83 c4 18                  	addq	$0x18, %rsp
   41bff: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41c04: 0f 8e 76 9d 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41c0a: c3                           	retq
   41c0b: 48 8b c2                     	movq	%rdx, %rax
   41c0e: eb eb                        	jmp	0x41bfb <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x17b>
   41c10: cc                           	int3
   41c11: cc                           	int3
   41c12: cc                           	int3
   41c13: cc                           	int3
   41c14: cc                           	int3
   41c15: cc                           	int3
   41c16: cc                           	int3
   41c17: cc                           	int3
   41c18: cc                           	int3
   41c19: cc                           	int3
   41c1a: cc                           	int3
   41c1b: cc                           	int3
   41c1c: cc                           	int3
   41c1d: cc                           	int3
   41c1e: cc                           	int3
   41c1f: cc                           	int3

# svm: Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6
0000000000041c20 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6>:
   41c20: 48 8d 5c 24 c8               	leaq	-0x38(%rsp), %rbx
   41c25: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   41c29: 0f 86 31 80 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   41c2f: 48 8b e3                     	movq	%rbx, %rsp
   41c32: 89 74 24 28                  	movl	%esi, 0x28(%rsp)
   41c36: 48 8b 0d 23 e7 88 00         	movq	0x88e723(%rip), %rcx    # 0x8d0360 <.data+0x360>
   41c3d: 48 89 4c 24 10               	movq	%rcx, 0x10(%rsp)
   41c42: 83 fe 01                     	cmpl	$0x1, %esi
   41c45: bf 01 00 00 00               	movl	$0x1, %edi
   41c4a: 0f 4e fe                     	cmovlel	%esi, %edi
   41c4d: 83 ff 01                     	cmpl	$0x1, %edi
   41c50: 0f 8c bf 01 00 00            	jl	0x41e15 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x1f5>
   41c56: 48 8b fa                     	movq	%rdx, %rdi
   41c59: b8 00 00 00 00               	movl	$0x0, %eax
   41c5e: 4c 8b d1                     	movq	%rcx, %r10
   41c61: 41 ff d2                     	callq	*%r10
   41c64: bf 01 00 00 00               	movl	$0x1, %edi
   41c69: 8b 74 24 28                  	movl	0x28(%rsp), %esi
   41c6d: 44 8d 56 fc                  	leal	-0x4(%rsi), %r10d
   41c71: 81 fe 04 00 00 80            	cmpl	$0x80000004, %esi       # imm = 0x80000004
   41c77: b9 00 00 00 80               	movl	$0x80000000, %ecx       # imm = 0x80000000
   41c7c: 44 0f 4c d1                  	cmovll	%ecx, %r10d
   41c80: 44 89 54 24 18               	movl	%r10d, 0x18(%rsp)
   41c85: 41 8d 4a 03                  	leal	0x3(%r10), %ecx
   41c89: 89 4c 24 20                  	movl	%ecx, 0x20(%rsp)
   41c8d: 8b d7                        	movl	%edi, %edx
   41c8f: 90                           	nop
   41c90: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   41c95: 44 3b d2                     	cmpl	%edx, %r10d
   41c98: 0f 8e 27 01 00 00            	jle	0x41dc5 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x1a5>
   41c9e: 89 54 24 24                  	movl	%edx, 0x24(%rsp)
   41ca2: 8b f9                        	movl	%ecx, %edi
   41ca4: 2b fa                        	subl	%edx, %edi
   41ca6: 8b ef                        	movl	%edi, %ebp
   41ca8: c1 ed 02                     	shrl	$0x2, %ebp
   41cab: 83 e7 fc                     	andl	$-0x4, %edi
   41cae: 81 fd 00 10 00 00            	cmpl	$0x1000, %ebp           # imm = 0x1000
   41cb4: bd 00 40 00 00               	movl	$0x4000, %ebp           # imm = 0x4000
   41cb9: 0f 42 ef                     	cmovbl	%edi, %ebp
   41cbc: 0f 1f 40 00                  	nopl	(%rax)
   41cc0: 83 fd 01                     	cmpl	$0x1, %ebp
   41cc3: 0f 8c de 00 00 00            	jl	0x41da7 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x187>
   41cc9: 89 6c 24 30                  	movl	%ebp, 0x30(%rsp)
   41ccd: 48 8b f8                     	movq	%rax, %rdi
   41cd0: b9 00 00 00 00               	movl	$0x0, %ecx
   41cd5: eb 15                        	jmp	0x41cec <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0xcc>
   41cd7: 66 0f 1f 84 00 00 00 00 00   	nopw	(%rax,%rax)
   41ce0: 48 8b f8                     	movq	%rax, %rdi
   41ce3: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   41ce7: 48 8b 5c 24 10               	movq	0x10(%rsp), %rbx
   41cec: 44 8b c9                     	movl	%ecx, %r9d
   41cef: 44 03 ca                     	addl	%edx, %r9d
   41cf2: 44 89 4c 24 1c               	movl	%r9d, 0x1c(%rsp)
   41cf7: b8 00 00 00 00               	movl	$0x0, %eax
   41cfc: 4c 8b d3                     	movq	%rbx, %r10
   41cff: 89 4c 24 34                  	movl	%ecx, 0x34(%rsp)
   41d03: 41 ff d2                     	callq	*%r10
   41d06: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   41d0b: 41 ff c1                     	incl	%r9d
   41d0e: 48 8b f8                     	movq	%rax, %rdi
   41d11: b8 00 00 00 00               	movl	$0x0, %eax
   41d16: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   41d1b: 44 89 4c 24 2c               	movl	%r9d, 0x2c(%rsp)
   41d20: 41 ff d2                     	callq	*%r10
   41d23: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   41d28: 41 8d 49 02                  	leal	0x2(%r9), %ecx
   41d2c: 48 8b f8                     	movq	%rax, %rdi
   41d2f: b8 00 00 00 00               	movl	$0x0, %eax
   41d34: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   41d39: 89 4c 24 2c                  	movl	%ecx, 0x2c(%rsp)
   41d3d: 66 66 90                     	nop
   41d40: 41 ff d2                     	callq	*%r10
   41d43: 44 8b 4c 24 1c               	movl	0x1c(%rsp), %r9d
   41d48: 41 8d 49 03                  	leal	0x3(%r9), %ecx
   41d4c: 48 8b f8                     	movq	%rax, %rdi
   41d4f: b8 00 00 00 00               	movl	$0x0, %eax
   41d54: 4c 8b 54 24 10               	movq	0x10(%rsp), %r10
   41d59: 89 4c 24 1c                  	movl	%ecx, 0x1c(%rsp)
   41d5d: 66 66 90                     	nop
   41d60: 41 ff d2                     	callq	*%r10
   41d63: 8b 4c 24 34                  	movl	0x34(%rsp), %ecx
   41d67: 8d 49 04                     	leal	0x4(%rcx), %ecx
   41d6a: 8b 6c 24 30                  	movl	0x30(%rsp), %ebp
   41d6e: 3b e9                        	cmpl	%ecx, %ebp
   41d70: 0f 8f 6a ff ff ff            	jg	0x41ce0 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0xc0>
   41d76: 03 4c 24 24                  	addl	0x24(%rsp), %ecx
   41d7a: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41d7f: 90                           	nop
   41d80: 7e 10                        	jle	0x41d92 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x172>
   41d82: 8b d1                        	movl	%ecx, %edx
   41d84: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   41d88: 44 8b 54 24 18               	movl	0x18(%rsp), %r10d
   41d8d: e9 fe fe ff ff               	jmp	0x41c90 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x70>
   41d92: e8 e9 9b 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41d97: 8b d1                        	movl	%ecx, %edx
   41d99: 8b 4c 24 20                  	movl	0x20(%rsp), %ecx
   41d9d: 44 8b 54 24 18               	movl	0x18(%rsp), %r10d
   41da2: e9 e9 fe ff ff               	jmp	0x41c90 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x70>
   41da7: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41dac: 7e 09                        	jle	0x41db7 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x197>
   41dae: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   41db2: e9 d9 fe ff ff               	jmp	0x41c90 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x70>
   41db7: e8 c4 9b 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41dbc: 8b 54 24 24                  	movl	0x24(%rsp), %edx
   41dc0: e9 cb fe ff ff               	jmp	0x41c90 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x70>
   41dc5: 48 8b cb                     	movq	%rbx, %rcx
   41dc8: eb 33                        	jmp	0x41dfd <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x1dd>
   41dca: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   41dd5: 66 66 66 90                  	nop
   41dd9: 0f 1f 80 00 00 00 00         	nopl	(%rax)
   41de0: 89 54 24 18                  	movl	%edx, 0x18(%rsp)
   41de4: 48 8b f8                     	movq	%rax, %rdi
   41de7: b8 00 00 00 00               	movl	$0x0, %eax
   41dec: 4c 8b d1                     	movq	%rcx, %r10
   41def: 41 ff d2                     	callq	*%r10
   41df2: 8b 54 24 18                  	movl	0x18(%rsp), %edx
   41df6: ff c2                        	incl	%edx
   41df8: 48 8b 4c 24 10               	movq	0x10(%rsp), %rcx
   41dfd: 8b 74 24 28                  	movl	0x28(%rsp), %esi
   41e01: 3b f2                        	cmpl	%edx, %esi
   41e03: 7f db                        	jg	0x41de0 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x1c0>
   41e05: 48 83 c4 38                  	addq	$0x38, %rsp
   41e09: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41e0e: 0f 8e 6c 9b 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41e14: c3                           	retq
   41e15: bf 00 00 00 00               	movl	$0x0, %edi
   41e1a: 48 8b c2                     	movq	%rdx, %rax
   41e1d: 66 66 90                     	nop
   41e20: e9 48 fe ff ff               	jmp	0x41c6d <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x4d>
   41e25: cc                           	int3
   41e26: cc                           	int3
   41e27: cc                           	int3
   41e28: cc                           	int3
   41e29: cc                           	int3
   41e2a: cc                           	int3
   41e2b: cc                           	int3
   41e2c: cc                           	int3
   41e2d: cc                           	int3
   41e2e: cc                           	int3
   41e2f: cc                           	int3
   41e30: cc                           	int3
   41e31: cc                           	int3
   41e32: cc                           	int3
   41e33: cc                           	int3
   41e34: cc                           	int3
   41e35: cc                           	int3
   41e36: cc                           	int3
   41e37: cc                           	int3
   41e38: cc                           	int3
   41e39: cc                           	int3
   41e3a: cc                           	int3
   41e3b: cc                           	int3
   41e3c: cc                           	int3
   41e3d: cc                           	int3
   41e3e: cc                           	int3
   41e3f: cc                           	int3

# svm: Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2
0000000000041e40 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2>:
   41e40: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   41e45: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   41e49: 0f 86 11 7e 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   41e4f: 48 8b e3                     	movq	%rbx, %rsp
   41e52: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   41e56: 83 fe 01                     	cmpl	$0x1, %esi
   41e59: 7c 26                        	jl	0x41e81 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x41>
   41e5b: 49 8b be 68 f1 3f 00         	movq	0x3ff168(%r14), %rdi
   41e62: 48 8b f2                     	movq	%rdx, %rsi
   41e65: e8 b6 02 00 00               	callq	0x42120 <Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA>
   41e6a: 83 7c 24 14 11               	cmpl	$0x11, 0x14(%rsp)
   41e6f: 7d 23                        	jge	0x41e94 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x54>
   41e71: 48 83 c4 18                  	addq	$0x18, %rsp
   41e75: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41e7a: 0f 8e 00 9b 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41e80: c3                           	retq
   41e81: 48 8b c2                     	movq	%rdx, %rax
   41e84: 48 83 c4 18                  	addq	$0x18, %rsp
   41e88: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41e8d: 0f 8e ed 9a 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41e93: c3                           	retq
   41e94: 48 8b f0                     	movq	%rax, %rsi
   41e97: c7 44 24 10 10 00 00 00      	movl	$0x10, 0x10(%rsp)
   41e9f: 90                           	nop
   41ea0: 49 8b be 68 f1 3f 00         	movq	0x3ff168(%r14), %rdi
   41ea7: e8 74 02 00 00               	callq	0x42120 <Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA>
   41eac: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   41eb0: 8d 51 10                     	leal	0x10(%rcx), %edx
   41eb3: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   41eb7: 3b f2                        	cmpl	%edx, %esi
   41eb9: 7e b6                        	jle	0x41e71 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x31>
   41ebb: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41ec0: 7e 09                        	jle	0x41ecb <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x8b>
   41ec2: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   41ec6: 48 8b f0                     	movq	%rax, %rsi
   41ec9: eb d5                        	jmp	0x41ea0 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x60>
   41ecb: e8 b0 9a 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41ed0: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   41ed4: 48 8b f0                     	movq	%rax, %rsi
   41ed7: eb c7                        	jmp	0x41ea0 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x60>
   41ed9: cc                           	int3
   41eda: cc                           	int3
   41edb: cc                           	int3
   41edc: cc                           	int3
   41edd: cc                           	int3
   41ede: cc                           	int3
   41edf: cc                           	int3

# svm: Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10
0000000000041ee0 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10>:
   41ee0: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   41ee5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   41ee9: 0f 86 71 7d 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   41eef: 48 8b e3                     	movq	%rbx, %rsp
   41ef2: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   41ef6: 83 fe 01                     	cmpl	$0x1, %esi
   41ef9: 7c 31                        	jl	0x41f2c <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x4c>
   41efb: 48 8b 0d 86 e1 88 00         	movq	0x88e186(%rip), %rcx    # 0x8d0088 <.data+0x88>
   41f02: 48 89 4c 24 08               	movq	%rcx, 0x8(%rsp)
   41f07: 48 8b fa                     	movq	%rdx, %rdi
   41f0a: b8 00 00 00 00               	movl	$0x0, %eax
   41f0f: 4c 8b d1                     	movq	%rcx, %r10
   41f12: 41 ff d2                     	callq	*%r10
   41f15: 83 7c 24 14 41               	cmpl	$0x41, 0x14(%rsp)
   41f1a: 7d 23                        	jge	0x41f3f <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x5f>
   41f1c: 48 83 c4 18                  	addq	$0x18, %rsp
   41f20: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41f25: 0f 8e 55 9a 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41f2b: c3                           	retq
   41f2c: 48 8b c2                     	movq	%rdx, %rax
   41f2f: 48 83 c4 18                  	addq	$0x18, %rsp
   41f33: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41f38: 0f 8e 42 9a 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41f3e: c3                           	retq
   41f3f: c7 44 24 10 40 00 00 00      	movl	$0x40, 0x10(%rsp)
   41f47: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   41f4c: 0f 1f 40 00                  	nopl	(%rax)
   41f50: 48 8b f8                     	movq	%rax, %rdi
   41f53: b8 00 00 00 00               	movl	$0x0, %eax
   41f58: 4c 8b d1                     	movq	%rcx, %r10
   41f5b: 41 ff d2                     	callq	*%r10
   41f5e: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   41f62: 8d 51 40                     	leal	0x40(%rcx), %edx
   41f65: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   41f69: 3b f2                        	cmpl	%edx, %esi
   41f6b: 7e af                        	jle	0x41f1c <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x3c>
   41f6d: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41f72: 7e 0b                        	jle	0x41f7f <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x9f>
   41f74: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   41f78: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   41f7d: eb d1                        	jmp	0x41f50 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x70>
   41f7f: 48 8b 5c 24 08               	movq	0x8(%rsp), %rbx
   41f84: e8 f7 99 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41f89: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   41f8d: 48 8b cb                     	movq	%rbx, %rcx
   41f90: eb be                        	jmp	0x41f50 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x70>
   41f92: cc                           	int3
   41f93: cc                           	int3
   41f94: cc                           	int3
   41f95: cc                           	int3
   41f96: cc                           	int3
   41f97: cc                           	int3
   41f98: cc                           	int3
   41f99: cc                           	int3
   41f9a: cc                           	int3
   41f9b: cc                           	int3
   41f9c: cc                           	int3
   41f9d: cc                           	int3
   41f9e: cc                           	int3
   41f9f: cc                           	int3

# svm: Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL
0000000000041fa0 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL>:
   41fa0: 48 8d 5c 24 e8               	leaq	-0x18(%rsp), %rbx
   41fa5: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   41fa9: 0f 86 b1 7c 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   41faf: 48 8b e3                     	movq	%rbx, %rsp
   41fb2: 89 74 24 14                  	movl	%esi, 0x14(%rsp)
   41fb6: 83 fe 01                     	cmpl	$0x1, %esi
   41fb9: 7c 31                        	jl	0x41fec <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x4c>
   41fbb: 48 8b 0d de e2 88 00         	movq	0x88e2de(%rip), %rcx    # 0x8d02a0 <.data+0x2a0>
   41fc2: 48 89 4c 24 08               	movq	%rcx, 0x8(%rsp)
   41fc7: 48 8b fa                     	movq	%rdx, %rdi
   41fca: b8 00 00 00 00               	movl	$0x0, %eax
   41fcf: 4c 8b d1                     	movq	%rcx, %r10
   41fd2: 41 ff d2                     	callq	*%r10
   41fd5: 83 7c 24 14 05               	cmpl	$0x5, 0x14(%rsp)
   41fda: 7d 23                        	jge	0x41fff <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x5f>
   41fdc: 48 83 c4 18                  	addq	$0x18, %rsp
   41fe0: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41fe5: 0f 8e 95 99 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41feb: c3                           	retq
   41fec: 48 8b c2                     	movq	%rdx, %rax
   41fef: 48 83 c4 18                  	addq	$0x18, %rsp
   41ff3: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   41ff8: 0f 8e 82 99 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   41ffe: c3                           	retq
   41fff: c7 44 24 10 04 00 00 00      	movl	$0x4, 0x10(%rsp)
   42007: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   4200c: 0f 1f 40 00                  	nopl	(%rax)
   42010: 48 8b f8                     	movq	%rax, %rdi
   42013: b8 00 00 00 00               	movl	$0x0, %eax
   42018: 4c 8b d1                     	movq	%rcx, %r10
   4201b: 41 ff d2                     	callq	*%r10
   4201e: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   42022: 8d 51 04                     	leal	0x4(%rcx), %edx
   42025: 8b 74 24 14                  	movl	0x14(%rsp), %esi
   42029: 3b f2                        	cmpl	%edx, %esi
   4202b: 7e af                        	jle	0x41fdc <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x3c>
   4202d: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   42032: 7e 0b                        	jle	0x4203f <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x9f>
   42034: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   42038: 48 8b 4c 24 08               	movq	0x8(%rsp), %rcx
   4203d: eb d1                        	jmp	0x42010 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x70>
   4203f: 48 8b 5c 24 08               	movq	0x8(%rsp), %rbx
   42044: e8 37 99 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   42049: 89 54 24 10                  	movl	%edx, 0x10(%rsp)
   4204d: 48 8b cb                     	movq	%rbx, %rcx
   42050: eb be                        	jmp	0x42010 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x70>
   42052: cc                           	int3
   42053: cc                           	int3
   42054: cc                           	int3
   42055: cc                           	int3
   42056: cc                           	int3
   42057: cc                           	int3
   42058: cc                           	int3
   42059: cc                           	int3
   4205a: cc                           	int3
   4205b: cc                           	int3
   4205c: cc                           	int3
   4205d: cc                           	int3
   4205e: cc                           	int3
   4205f: cc                           	int3

# svm: Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D
0000000000042060 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D>:
   42060: 48 8d 5c 24 f8               	leaq	-0x8(%rsp), %rbx
   42065: 49 3b 5f 08                  	cmpq	0x8(%r15), %rbx
   42069: 0f 86 f1 7b 0b 00            	jbe	0xf9c60 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   4206f: 48 8b e3                     	movq	%rbx, %rsp
   42072: b9 00 00 00 00               	movl	$0x0, %ecx
   42077: 48 8b c2                     	movq	%rdx, %rax
   4207a: 66 0f 1f 44 00 00            	nopw	(%rax,%rax)
   42080: 3b f1                        	cmpl	%ecx, %esi
   42082: 7e 52                        	jle	0x420d6 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x76>
   42084: 8b d6                        	movl	%esi, %edx
   42086: 2b d1                        	subl	%ecx, %edx
   42088: 81 fa 00 10 00 00            	cmpl	$0x1000, %edx           # imm = 0x1000
   4208e: bb 00 10 00 00               	movl	$0x1000, %ebx           # imm = 0x1000
   42093: 0f 46 da                     	cmovbel	%edx, %ebx
   42096: 85 db                        	testl	%ebx, %ebx
   42098: ba 00 00 00 00               	movl	$0x0, %edx
   4209d: 0f 4d d3                     	cmovgel	%ebx, %edx
   420a0: 83 fb 01                     	cmpl	$0x1, %ebx
   420a3: 7c 21                        	jl	0x420c6 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x66>
   420a5: 8b da                        	movl	%edx, %ebx
   420a7: 48 ff cb                     	decq	%rbx
   420aa: bd 00 00 00 00               	movl	$0x0, %ebp
   420af: 90                           	nop
   420b0: 48 69 c0 0d 66 19 00         	imulq	$0x19660d, %rax, %rax   # imm = 0x19660D
   420b7: 48 8d 80 5f f3 6e 3c         	leaq	0x3c6ef35f(%rax), %rax
   420be: 48 ff c5                     	incq	%rbp
   420c1: 48 3b eb                     	cmpq	%rbx, %rbp
   420c4: 7e ea                        	jle	0x420b0 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x50>
   420c6: 03 ca                        	addl	%edx, %ecx
   420c8: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   420cd: 7f b1                        	jg	0x42080 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x20>
   420cf: e8 ac 98 1b 00               	callq	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   420d4: eb aa                        	jmp	0x42080 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x20>
   420d6: 48 83 c4 08                  	addq	$0x8, %rsp
   420da: 41 83 7f 10 00               	cmpl	$0x0, 0x10(%r15)
   420df: 0f 8e 9b 98 1b 00            	jle	0x1fb980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   420e5: c3                           	retq
   420e6: cc                           	int3
   420e7: cc                           	int3
   420e8: cc                           	int3
   420e9: cc                           	int3
   420ea: cc                           	int3
   420eb: cc                           	int3
   420ec: cc                           	int3
   420ed: cc                           	int3
   420ee: cc                           	int3
   420ef: cc                           	int3
   420f0: cc                           	int3
   420f1: cc                           	int3
   420f2: cc                           	int3
   420f3: cc                           	int3
   420f4: cc                           	int3
   420f5: cc                           	int3
   420f6: cc                           	int3
   420f7: cc                           	int3
   420f8: cc                           	int3
   420f9: cc                           	int3
   420fa: cc                           	int3
   420fb: cc                           	int3
   420fc: cc                           	int3
   420fd: cc                           	int3
   420fe: cc                           	int3
   420ff: cc                           	int3

# svm: Floor_identity_RGfigO1fJmId7IgDqMOKbC
0000000000042100 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>:
   42100: 48 83 ec 08                  	subq	$0x8, %rsp
   42104: 4c 8b 15 7d e0 88 00         	movq	0x88e07d(%rip), %r10    # 0x8d0188 <.data+0x188>
   4210b: b8 00 00 00 00               	movl	$0x0, %eax
   42110: 41 ff d2                     	callq	*%r10
   42113: 48 83 c4 08                  	addq	$0x8, %rsp
   42117: c3                           	retq
   42118: cc                           	int3
   42119: cc                           	int3
   4211a: cc                           	int3
   4211b: cc                           	int3
   4211c: cc                           	int3
   4211d: cc                           	int3
   4211e: cc                           	int3
   4211f: cc                           	int3

# svm: Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA
0000000000042120 <Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA>:
   42120: 48 83 ec 08                  	subq	$0x8, %rsp
   42124: 48 8b cf                     	movq	%rdi, %rcx
   42127: 48 8b fe                     	movq	%rsi, %rdi
   4212a: b8 00 00 00 00               	movl	$0x0, %eax
   4212f: 4c 8b d1                     	movq	%rcx, %r10
   42132: 48 89 0c 24                  	movq	%rcx, (%rsp)
   42136: 41 ff d2                     	callq	*%r10
   42139: 48 8b f8                     	movq	%rax, %rdi
   4213c: b8 00 00 00 00               	movl	$0x0, %eax
   42141: 4c 8b 14 24                  	movq	(%rsp), %r10
   42145: 41 ff d2                     	callq	*%r10
   42148: 48 8b f8                     	movq	%rax, %rdi
   4214b: b8 00 00 00 00               	movl	$0x0, %eax
   42150: 4c 8b 14 24                  	movq	(%rsp), %r10
   42154: 41 ff d2                     	callq	*%r10
   42157: 48 8b f8                     	movq	%rax, %rdi
   4215a: b8 00 00 00 00               	movl	$0x0, %eax
   4215f: 4c 8b 14 24                  	movq	(%rsp), %r10
   42163: 41 ff d2                     	callq	*%r10
   42166: 48 8b f8                     	movq	%rax, %rdi
   42169: b8 00 00 00 00               	movl	$0x0, %eax
   4216e: 4c 8b 14 24                  	movq	(%rsp), %r10
   42172: 41 ff d2                     	callq	*%r10
   42175: 48 8b f8                     	movq	%rax, %rdi
   42178: b8 00 00 00 00               	movl	$0x0, %eax
   4217d: 4c 8b 14 24                  	movq	(%rsp), %r10
   42181: 41 ff d2                     	callq	*%r10
   42184: 48 8b f8                     	movq	%rax, %rdi
   42187: b8 00 00 00 00               	movl	$0x0, %eax
   4218c: 4c 8b 14 24                  	movq	(%rsp), %r10
   42190: 41 ff d2                     	callq	*%r10
   42193: 48 8b f8                     	movq	%rax, %rdi
   42196: b8 00 00 00 00               	movl	$0x0, %eax
   4219b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4219f: 90                           	nop
   421a0: 41 ff d2                     	callq	*%r10
   421a3: 48 8b f8                     	movq	%rax, %rdi
   421a6: b8 00 00 00 00               	movl	$0x0, %eax
   421ab: 4c 8b 14 24                  	movq	(%rsp), %r10
   421af: 41 ff d2                     	callq	*%r10
   421b2: 48 8b f8                     	movq	%rax, %rdi
   421b5: b8 00 00 00 00               	movl	$0x0, %eax
   421ba: 4c 8b 14 24                  	movq	(%rsp), %r10
   421be: 66 90                        	nop
   421c0: 41 ff d2                     	callq	*%r10
   421c3: 48 8b f8                     	movq	%rax, %rdi
   421c6: b8 00 00 00 00               	movl	$0x0, %eax
   421cb: 4c 8b 14 24                  	movq	(%rsp), %r10
   421cf: 41 ff d2                     	callq	*%r10
   421d2: 48 8b f8                     	movq	%rax, %rdi
   421d5: b8 00 00 00 00               	movl	$0x0, %eax
   421da: 4c 8b 14 24                  	movq	(%rsp), %r10
   421de: 66 90                        	nop
   421e0: 41 ff d2                     	callq	*%r10
   421e3: 48 8b f8                     	movq	%rax, %rdi
   421e6: b8 00 00 00 00               	movl	$0x0, %eax
   421eb: 4c 8b 14 24                  	movq	(%rsp), %r10
   421ef: 41 ff d2                     	callq	*%r10
   421f2: 48 8b f8                     	movq	%rax, %rdi
   421f5: b8 00 00 00 00               	movl	$0x0, %eax
   421fa: 4c 8b 14 24                  	movq	(%rsp), %r10
   421fe: 66 90                        	nop
   42200: 41 ff d2                     	callq	*%r10
   42203: 48 8b f8                     	movq	%rax, %rdi
   42206: b8 00 00 00 00               	movl	$0x0, %eax
   4220b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4220f: 41 ff d2                     	callq	*%r10
   42212: 48 8b f8                     	movq	%rax, %rdi
   42215: b8 00 00 00 00               	movl	$0x0, %eax
   4221a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4221e: 66 90                        	nop
   42220: 41 ff d2                     	callq	*%r10
   42223: 48 83 c4 08                  	addq	$0x8, %rsp
   42227: c3                           	retq
   42228: cc                           	int3
   42229: cc                           	int3
   4222a: cc                           	int3
   4222b: cc                           	int3
   4222c: cc                           	int3
   4222d: cc                           	int3
   4222e: cc                           	int3
   4222f: cc                           	int3
   42230: cc                           	int3
   42231: cc                           	int3
   42232: cc                           	int3
   42233: cc                           	int3
   42234: cc                           	int3
   42235: cc                           	int3
   42236: cc                           	int3
   42237: cc                           	int3
   42238: cc                           	int3
   42239: cc                           	int3
   4223a: cc                           	int3
   4223b: cc                           	int3
   4223c: cc                           	int3
   4223d: cc                           	int3
   4223e: cc                           	int3
   4223f: cc                           	int3

# svm: Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1
0000000000042240 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1>:
   42240: 48 83 ec 18                  	subq	$0x18, %rsp
   42244: 48 89 7c 24 08               	movq	%rdi, 0x8(%rsp)
   42249: b8 00 00 00 00               	movl	$0x0, %eax
   4224e: eb 2e                        	jmp	0x4227e <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x3e>
   42250: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   4225b: 66 66 66 90                  	nop
   4225f: 90                           	nop
   42260: 48 8b cf                     	movq	%rdi, %rcx
   42263: 48 8b fe                     	movq	%rsi, %rdi
   42266: b8 00 00 00 00               	movl	$0x0, %eax
   4226b: 4c 8b d1                     	movq	%rcx, %r10
   4226e: 41 ff d2                     	callq	*%r10
   42271: 48 8b f0                     	movq	%rax, %rsi
   42274: 48 8b 7c 24 08               	movq	0x8(%rsp), %rdi
   42279: b8 01 00 00 00               	movl	$0x1, %eax
   4227e: 66 90                        	nop
   42280: 85 c0                        	testl	%eax, %eax
   42282: 74 dc                        	je	0x42260 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x20>
   42284: 8b c8                        	movl	%eax, %ecx
   42286: 48 8b c6                     	movq	%rsi, %rax
   42289: e9 ed 00 00 00               	jmp	0x4237b <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x13b>
   4228e: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   42299: 66 66 66 90                  	nop
   4229d: 66 66 90                     	nop
   422a0: 48 8b cf                     	movq	%rdi, %rcx
   422a3: 48 8b f8                     	movq	%rax, %rdi
   422a6: b8 00 00 00 00               	movl	$0x0, %eax
   422ab: 4c 8b d1                     	movq	%rcx, %r10
   422ae: 41 ff d2                     	callq	*%r10
   422b1: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   422b5: ff c1                        	incl	%ecx
   422b7: 48 8b f8                     	movq	%rax, %rdi
   422ba: b8 00 00 00 00               	movl	$0x0, %eax
   422bf: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   422c4: 89 4c 24 14                  	movl	%ecx, 0x14(%rsp)
   422c8: 41 ff d2                     	callq	*%r10
   422cb: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   422cf: 8d 51 02                     	leal	0x2(%rcx), %edx
   422d2: 48 8b f8                     	movq	%rax, %rdi
   422d5: b8 00 00 00 00               	movl	$0x0, %eax
   422da: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   422df: 89 54 24 14                  	movl	%edx, 0x14(%rsp)
   422e3: 41 ff d2                     	callq	*%r10
   422e6: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   422ea: 8d 51 03                     	leal	0x3(%rcx), %edx
   422ed: 48 8b f8                     	movq	%rax, %rdi
   422f0: b8 00 00 00 00               	movl	$0x0, %eax
   422f5: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   422fa: 89 54 24 14                  	movl	%edx, 0x14(%rsp)
   422fe: 66 90                        	nop
   42300: 41 ff d2                     	callq	*%r10
   42303: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   42307: 8d 51 04                     	leal	0x4(%rcx), %edx
   4230a: 48 8b f8                     	movq	%rax, %rdi
   4230d: b8 00 00 00 00               	movl	$0x0, %eax
   42312: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   42317: 89 54 24 14                  	movl	%edx, 0x14(%rsp)
   4231b: 41 ff d2                     	callq	*%r10
   4231e: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   42322: 8d 51 05                     	leal	0x5(%rcx), %edx
   42325: 48 8b f8                     	movq	%rax, %rdi
   42328: b8 00 00 00 00               	movl	$0x0, %eax
   4232d: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   42332: 89 54 24 14                  	movl	%edx, 0x14(%rsp)
   42336: 41 ff d2                     	callq	*%r10
   42339: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   4233d: 8d 51 06                     	leal	0x6(%rcx), %edx
   42340: 48 8b f8                     	movq	%rax, %rdi
   42343: b8 00 00 00 00               	movl	$0x0, %eax
   42348: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   4234d: 89 54 24 14                  	movl	%edx, 0x14(%rsp)
   42351: 41 ff d2                     	callq	*%r10
   42354: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   42358: 8d 51 07                     	leal	0x7(%rcx), %edx
   4235b: 48 8b f8                     	movq	%rax, %rdi
   4235e: b8 00 00 00 00               	movl	$0x0, %eax
   42363: 4c 8b 54 24 08               	movq	0x8(%rsp), %r10
   42368: 89 54 24 14                  	movl	%edx, 0x14(%rsp)
   4236c: 41 ff d2                     	callq	*%r10
   4236f: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   42373: 8d 49 08                     	leal	0x8(%rcx), %ecx
   42376: 48 8b 7c 24 08               	movq	0x8(%rsp), %rdi
   4237b: 89 4c 24 10                  	movl	%ecx, 0x10(%rsp)
   4237f: 90                           	nop
   42380: 81 f9 f8 00 00 00            	cmpl	$0xf8, %ecx
   42386: 0f 82 14 ff ff ff            	jb	0x422a0 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x60>
   4238c: eb 32                        	jmp	0x423c0 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x180>
   4238e: 66 66 66 0f 1f 84 00 00 00 00 00     	nopw	(%rax,%rax)
   42399: 66 66 66 90                  	nop
   4239d: 66 66 90                     	nop
   423a0: 89 4c 24 10                  	movl	%ecx, 0x10(%rsp)
   423a4: 48 8b cf                     	movq	%rdi, %rcx
   423a7: 48 8b f8                     	movq	%rax, %rdi
   423aa: b8 00 00 00 00               	movl	$0x0, %eax
   423af: 4c 8b d1                     	movq	%rcx, %r10
   423b2: 41 ff d2                     	callq	*%r10
   423b5: 8b 4c 24 10                  	movl	0x10(%rsp), %ecx
   423b9: ff c1                        	incl	%ecx
   423bb: 48 8b 7c 24 08               	movq	0x8(%rsp), %rdi
   423c0: 81 f9 00 01 00 00            	cmpl	$0x100, %ecx            # imm = 0x100
   423c6: 72 d8                        	jb	0x423a0 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x160>
   423c8: 48 83 c4 18                  	addq	$0x18, %rsp
   423cc: c3                           	retq
   423cd: cc                           	int3
   423ce: cc                           	int3
   423cf: cc                           	int3
   423d0: cc                           	int3
   423d1: cc                           	int3
   423d2: cc                           	int3
   423d3: cc                           	int3
   423d4: cc                           	int3
   423d5: cc                           	int3
   423d6: cc                           	int3
   423d7: cc                           	int3
   423d8: cc                           	int3
   423d9: cc                           	int3
   423da: cc                           	int3
   423db: cc                           	int3
   423dc: cc                           	int3
   423dd: cc                           	int3
   423de: cc                           	int3
   423df: cc                           	int3

# svm: Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI
00000000000423e0 <Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI>:
   423e0: 48 83 ec 08                  	subq	$0x8, %rsp
   423e4: 48 8b cf                     	movq	%rdi, %rcx
   423e7: 48 8b fe                     	movq	%rsi, %rdi
   423ea: b8 00 00 00 00               	movl	$0x0, %eax
   423ef: 4c 8b d1                     	movq	%rcx, %r10
   423f2: 48 89 0c 24                  	movq	%rcx, (%rsp)
   423f6: 41 ff d2                     	callq	*%r10
   423f9: 48 8b f8                     	movq	%rax, %rdi
   423fc: b8 00 00 00 00               	movl	$0x0, %eax
   42401: 4c 8b 14 24                  	movq	(%rsp), %r10
   42405: 41 ff d2                     	callq	*%r10
   42408: 48 8b f8                     	movq	%rax, %rdi
   4240b: b8 00 00 00 00               	movl	$0x0, %eax
   42410: 4c 8b 14 24                  	movq	(%rsp), %r10
   42414: 41 ff d2                     	callq	*%r10
   42417: 48 8b f8                     	movq	%rax, %rdi
   4241a: b8 00 00 00 00               	movl	$0x0, %eax
   4241f: 4c 8b 14 24                  	movq	(%rsp), %r10
   42423: 41 ff d2                     	callq	*%r10
   42426: 48 83 c4 08                  	addq	$0x8, %rsp
   4242a: c3                           	retq
   4242b: cc                           	int3
   4242c: cc                           	int3
   4242d: cc                           	int3
   4242e: cc                           	int3
   4242f: cc                           	int3
   42430: cc                           	int3
   42431: cc                           	int3
   42432: cc                           	int3
   42433: cc                           	int3
   42434: cc                           	int3
   42435: cc                           	int3
   42436: cc                           	int3
   42437: cc                           	int3
   42438: cc                           	int3
   42439: cc                           	int3
   4243a: cc                           	int3
   4243b: cc                           	int3
   4243c: cc                           	int3
   4243d: cc                           	int3
   4243e: cc                           	int3
   4243f: cc                           	int3

# svm: Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK
0000000000042440 <Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK>:
   42440: 48 83 ec 08                  	subq	$0x8, %rsp
   42444: 48 8b cf                     	movq	%rdi, %rcx
   42447: 48 8b fe                     	movq	%rsi, %rdi
   4244a: b8 00 00 00 00               	movl	$0x0, %eax
   4244f: 4c 8b d1                     	movq	%rcx, %r10
   42452: 48 89 0c 24                  	movq	%rcx, (%rsp)
   42456: 41 ff d2                     	callq	*%r10
   42459: 48 8b f8                     	movq	%rax, %rdi
   4245c: b8 00 00 00 00               	movl	$0x0, %eax
   42461: 4c 8b 14 24                  	movq	(%rsp), %r10
   42465: 41 ff d2                     	callq	*%r10
   42468: 48 8b f8                     	movq	%rax, %rdi
   4246b: b8 00 00 00 00               	movl	$0x0, %eax
   42470: 4c 8b 14 24                  	movq	(%rsp), %r10
   42474: 41 ff d2                     	callq	*%r10
   42477: 48 8b f8                     	movq	%rax, %rdi
   4247a: b8 00 00 00 00               	movl	$0x0, %eax
   4247f: 4c 8b 14 24                  	movq	(%rsp), %r10
   42483: 41 ff d2                     	callq	*%r10
   42486: 48 8b f8                     	movq	%rax, %rdi
   42489: b8 00 00 00 00               	movl	$0x0, %eax
   4248e: 4c 8b 14 24                  	movq	(%rsp), %r10
   42492: 41 ff d2                     	callq	*%r10
   42495: 48 8b f8                     	movq	%rax, %rdi
   42498: b8 00 00 00 00               	movl	$0x0, %eax
   4249d: 4c 8b 14 24                  	movq	(%rsp), %r10
   424a1: 41 ff d2                     	callq	*%r10
   424a4: 48 8b f8                     	movq	%rax, %rdi
   424a7: b8 00 00 00 00               	movl	$0x0, %eax
   424ac: 4c 8b 14 24                  	movq	(%rsp), %r10
   424b0: 41 ff d2                     	callq	*%r10
   424b3: 48 8b f8                     	movq	%rax, %rdi
   424b6: b8 00 00 00 00               	movl	$0x0, %eax
   424bb: 4c 8b 14 24                  	movq	(%rsp), %r10
   424bf: 90                           	nop
   424c0: 41 ff d2                     	callq	*%r10
   424c3: 48 8b f8                     	movq	%rax, %rdi
   424c6: b8 00 00 00 00               	movl	$0x0, %eax
   424cb: 4c 8b 14 24                  	movq	(%rsp), %r10
   424cf: 41 ff d2                     	callq	*%r10
   424d2: 48 8b f8                     	movq	%rax, %rdi
   424d5: b8 00 00 00 00               	movl	$0x0, %eax
   424da: 4c 8b 14 24                  	movq	(%rsp), %r10
   424de: 66 90                        	nop
   424e0: 41 ff d2                     	callq	*%r10
   424e3: 48 8b f8                     	movq	%rax, %rdi
   424e6: b8 00 00 00 00               	movl	$0x0, %eax
   424eb: 4c 8b 14 24                  	movq	(%rsp), %r10
   424ef: 41 ff d2                     	callq	*%r10
   424f2: 48 8b f8                     	movq	%rax, %rdi
   424f5: b8 00 00 00 00               	movl	$0x0, %eax
   424fa: 4c 8b 14 24                  	movq	(%rsp), %r10
   424fe: 66 90                        	nop
   42500: 41 ff d2                     	callq	*%r10
   42503: 48 8b f8                     	movq	%rax, %rdi
   42506: b8 00 00 00 00               	movl	$0x0, %eax
   4250b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4250f: 41 ff d2                     	callq	*%r10
   42512: 48 8b f8                     	movq	%rax, %rdi
   42515: b8 00 00 00 00               	movl	$0x0, %eax
   4251a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4251e: 66 90                        	nop
   42520: 41 ff d2                     	callq	*%r10
   42523: 48 8b f8                     	movq	%rax, %rdi
   42526: b8 00 00 00 00               	movl	$0x0, %eax
   4252b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4252f: 41 ff d2                     	callq	*%r10
   42532: 48 8b f8                     	movq	%rax, %rdi
   42535: b8 00 00 00 00               	movl	$0x0, %eax
   4253a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4253e: 66 90                        	nop
   42540: 41 ff d2                     	callq	*%r10
   42543: 48 8b f8                     	movq	%rax, %rdi
   42546: b8 00 00 00 00               	movl	$0x0, %eax
   4254b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4254f: 41 ff d2                     	callq	*%r10
   42552: 48 8b f8                     	movq	%rax, %rdi
   42555: b8 00 00 00 00               	movl	$0x0, %eax
   4255a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4255e: 66 90                        	nop
   42560: 41 ff d2                     	callq	*%r10
   42563: 48 8b f8                     	movq	%rax, %rdi
   42566: b8 00 00 00 00               	movl	$0x0, %eax
   4256b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4256f: 41 ff d2                     	callq	*%r10
   42572: 48 8b f8                     	movq	%rax, %rdi
   42575: b8 00 00 00 00               	movl	$0x0, %eax
   4257a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4257e: 66 90                        	nop
   42580: 41 ff d2                     	callq	*%r10
   42583: 48 8b f8                     	movq	%rax, %rdi
   42586: b8 00 00 00 00               	movl	$0x0, %eax
   4258b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4258f: 41 ff d2                     	callq	*%r10
   42592: 48 8b f8                     	movq	%rax, %rdi
   42595: b8 00 00 00 00               	movl	$0x0, %eax
   4259a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4259e: 66 90                        	nop
   425a0: 41 ff d2                     	callq	*%r10
   425a3: 48 8b f8                     	movq	%rax, %rdi
   425a6: b8 00 00 00 00               	movl	$0x0, %eax
   425ab: 4c 8b 14 24                  	movq	(%rsp), %r10
   425af: 41 ff d2                     	callq	*%r10
   425b2: 48 8b f8                     	movq	%rax, %rdi
   425b5: b8 00 00 00 00               	movl	$0x0, %eax
   425ba: 4c 8b 14 24                  	movq	(%rsp), %r10
   425be: 66 90                        	nop
   425c0: 41 ff d2                     	callq	*%r10
   425c3: 48 8b f8                     	movq	%rax, %rdi
   425c6: b8 00 00 00 00               	movl	$0x0, %eax
   425cb: 4c 8b 14 24                  	movq	(%rsp), %r10
   425cf: 41 ff d2                     	callq	*%r10
   425d2: 48 8b f8                     	movq	%rax, %rdi
   425d5: b8 00 00 00 00               	movl	$0x0, %eax
   425da: 4c 8b 14 24                  	movq	(%rsp), %r10
   425de: 66 90                        	nop
   425e0: 41 ff d2                     	callq	*%r10
   425e3: 48 8b f8                     	movq	%rax, %rdi
   425e6: b8 00 00 00 00               	movl	$0x0, %eax
   425eb: 4c 8b 14 24                  	movq	(%rsp), %r10
   425ef: 41 ff d2                     	callq	*%r10
   425f2: 48 8b f8                     	movq	%rax, %rdi
   425f5: b8 00 00 00 00               	movl	$0x0, %eax
   425fa: 4c 8b 14 24                  	movq	(%rsp), %r10
   425fe: 66 90                        	nop
   42600: 41 ff d2                     	callq	*%r10
   42603: 48 8b f8                     	movq	%rax, %rdi
   42606: b8 00 00 00 00               	movl	$0x0, %eax
   4260b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4260f: 41 ff d2                     	callq	*%r10
   42612: 48 8b f8                     	movq	%rax, %rdi
   42615: b8 00 00 00 00               	movl	$0x0, %eax
   4261a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4261e: 66 90                        	nop
   42620: 41 ff d2                     	callq	*%r10
   42623: 48 8b f8                     	movq	%rax, %rdi
   42626: b8 00 00 00 00               	movl	$0x0, %eax
   4262b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4262f: 41 ff d2                     	callq	*%r10
   42632: 48 8b f8                     	movq	%rax, %rdi
   42635: b8 00 00 00 00               	movl	$0x0, %eax
   4263a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4263e: 66 90                        	nop
   42640: 41 ff d2                     	callq	*%r10
   42643: 48 8b f8                     	movq	%rax, %rdi
   42646: b8 00 00 00 00               	movl	$0x0, %eax
   4264b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4264f: 41 ff d2                     	callq	*%r10
   42652: 48 8b f8                     	movq	%rax, %rdi
   42655: b8 00 00 00 00               	movl	$0x0, %eax
   4265a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4265e: 66 90                        	nop
   42660: 41 ff d2                     	callq	*%r10
   42663: 48 8b f8                     	movq	%rax, %rdi
   42666: b8 00 00 00 00               	movl	$0x0, %eax
   4266b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4266f: 41 ff d2                     	callq	*%r10
   42672: 48 8b f8                     	movq	%rax, %rdi
   42675: b8 00 00 00 00               	movl	$0x0, %eax
   4267a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4267e: 66 90                        	nop
   42680: 41 ff d2                     	callq	*%r10
   42683: 48 8b f8                     	movq	%rax, %rdi
   42686: b8 00 00 00 00               	movl	$0x0, %eax
   4268b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4268f: 41 ff d2                     	callq	*%r10
   42692: 48 8b f8                     	movq	%rax, %rdi
   42695: b8 00 00 00 00               	movl	$0x0, %eax
   4269a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4269e: 66 90                        	nop
   426a0: 41 ff d2                     	callq	*%r10
   426a3: 48 8b f8                     	movq	%rax, %rdi
   426a6: b8 00 00 00 00               	movl	$0x0, %eax
   426ab: 4c 8b 14 24                  	movq	(%rsp), %r10
   426af: 41 ff d2                     	callq	*%r10
   426b2: 48 8b f8                     	movq	%rax, %rdi
   426b5: b8 00 00 00 00               	movl	$0x0, %eax
   426ba: 4c 8b 14 24                  	movq	(%rsp), %r10
   426be: 66 90                        	nop
   426c0: 41 ff d2                     	callq	*%r10
   426c3: 48 8b f8                     	movq	%rax, %rdi
   426c6: b8 00 00 00 00               	movl	$0x0, %eax
   426cb: 4c 8b 14 24                  	movq	(%rsp), %r10
   426cf: 41 ff d2                     	callq	*%r10
   426d2: 48 8b f8                     	movq	%rax, %rdi
   426d5: b8 00 00 00 00               	movl	$0x0, %eax
   426da: 4c 8b 14 24                  	movq	(%rsp), %r10
   426de: 66 90                        	nop
   426e0: 41 ff d2                     	callq	*%r10
   426e3: 48 8b f8                     	movq	%rax, %rdi
   426e6: b8 00 00 00 00               	movl	$0x0, %eax
   426eb: 4c 8b 14 24                  	movq	(%rsp), %r10
   426ef: 41 ff d2                     	callq	*%r10
   426f2: 48 8b f8                     	movq	%rax, %rdi
   426f5: b8 00 00 00 00               	movl	$0x0, %eax
   426fa: 4c 8b 14 24                  	movq	(%rsp), %r10
   426fe: 66 90                        	nop
   42700: 41 ff d2                     	callq	*%r10
   42703: 48 8b f8                     	movq	%rax, %rdi
   42706: b8 00 00 00 00               	movl	$0x0, %eax
   4270b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4270f: 41 ff d2                     	callq	*%r10
   42712: 48 8b f8                     	movq	%rax, %rdi
   42715: b8 00 00 00 00               	movl	$0x0, %eax
   4271a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4271e: 66 90                        	nop
   42720: 41 ff d2                     	callq	*%r10
   42723: 48 8b f8                     	movq	%rax, %rdi
   42726: b8 00 00 00 00               	movl	$0x0, %eax
   4272b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4272f: 41 ff d2                     	callq	*%r10
   42732: 48 8b f8                     	movq	%rax, %rdi
   42735: b8 00 00 00 00               	movl	$0x0, %eax
   4273a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4273e: 66 90                        	nop
   42740: 41 ff d2                     	callq	*%r10
   42743: 48 8b f8                     	movq	%rax, %rdi
   42746: b8 00 00 00 00               	movl	$0x0, %eax
   4274b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4274f: 41 ff d2                     	callq	*%r10
   42752: 48 8b f8                     	movq	%rax, %rdi
   42755: b8 00 00 00 00               	movl	$0x0, %eax
   4275a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4275e: 66 90                        	nop
   42760: 41 ff d2                     	callq	*%r10
   42763: 48 8b f8                     	movq	%rax, %rdi
   42766: b8 00 00 00 00               	movl	$0x0, %eax
   4276b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4276f: 41 ff d2                     	callq	*%r10
   42772: 48 8b f8                     	movq	%rax, %rdi
   42775: b8 00 00 00 00               	movl	$0x0, %eax
   4277a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4277e: 66 90                        	nop
   42780: 41 ff d2                     	callq	*%r10
   42783: 48 8b f8                     	movq	%rax, %rdi
   42786: b8 00 00 00 00               	movl	$0x0, %eax
   4278b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4278f: 41 ff d2                     	callq	*%r10
   42792: 48 8b f8                     	movq	%rax, %rdi
   42795: b8 00 00 00 00               	movl	$0x0, %eax
   4279a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4279e: 66 90                        	nop
   427a0: 41 ff d2                     	callq	*%r10
   427a3: 48 8b f8                     	movq	%rax, %rdi
   427a6: b8 00 00 00 00               	movl	$0x0, %eax
   427ab: 4c 8b 14 24                  	movq	(%rsp), %r10
   427af: 41 ff d2                     	callq	*%r10
   427b2: 48 8b f8                     	movq	%rax, %rdi
   427b5: b8 00 00 00 00               	movl	$0x0, %eax
   427ba: 4c 8b 14 24                  	movq	(%rsp), %r10
   427be: 66 90                        	nop
   427c0: 41 ff d2                     	callq	*%r10
   427c3: 48 8b f8                     	movq	%rax, %rdi
   427c6: b8 00 00 00 00               	movl	$0x0, %eax
   427cb: 4c 8b 14 24                  	movq	(%rsp), %r10
   427cf: 41 ff d2                     	callq	*%r10
   427d2: 48 8b f8                     	movq	%rax, %rdi
   427d5: b8 00 00 00 00               	movl	$0x0, %eax
   427da: 4c 8b 14 24                  	movq	(%rsp), %r10
   427de: 66 90                        	nop
   427e0: 41 ff d2                     	callq	*%r10
   427e3: 48 8b f8                     	movq	%rax, %rdi
   427e6: b8 00 00 00 00               	movl	$0x0, %eax
   427eb: 4c 8b 14 24                  	movq	(%rsp), %r10
   427ef: 41 ff d2                     	callq	*%r10
   427f2: 48 8b f8                     	movq	%rax, %rdi
   427f5: b8 00 00 00 00               	movl	$0x0, %eax
   427fa: 4c 8b 14 24                  	movq	(%rsp), %r10
   427fe: 66 90                        	nop
   42800: 41 ff d2                     	callq	*%r10
   42803: 48 8b f8                     	movq	%rax, %rdi
   42806: b8 00 00 00 00               	movl	$0x0, %eax
   4280b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4280f: 41 ff d2                     	callq	*%r10
   42812: 48 8b f8                     	movq	%rax, %rdi
   42815: b8 00 00 00 00               	movl	$0x0, %eax
   4281a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4281e: 66 90                        	nop
   42820: 41 ff d2                     	callq	*%r10
   42823: 48 8b f8                     	movq	%rax, %rdi
   42826: b8 00 00 00 00               	movl	$0x0, %eax
   4282b: 4c 8b 14 24                  	movq	(%rsp), %r10
   4282f: 41 ff d2                     	callq	*%r10
   42832: 48 8b f8                     	movq	%rax, %rdi
   42835: b8 00 00 00 00               	movl	$0x0, %eax
   4283a: 4c 8b 14 24                  	movq	(%rsp), %r10
   4283e: 66 90                        	nop
   42840: 41 ff d2                     	callq	*%r10
   42843: 48 83 c4 08                  	addq	$0x8, %rsp
   42847: c3                           	retq
   42848: cc                           	int3
   42849: cc                           	int3
   4284a: cc                           	int3
   4284b: cc                           	int3
   4284c: cc                           	int3
   4284d: cc                           	int3
   4284e: cc                           	int3
   4284f: cc                           	int3
   42850: cc                           	int3
   42851: cc                           	int3
   42852: cc                           	int3
   42853: cc                           	int3
   42854: cc                           	int3
   42855: cc                           	int3
   42856: cc                           	int3
   42857: cc                           	int3
   42858: cc                           	int3
   42859: cc                           	int3
   4285a: cc                           	int3
   4285b: cc                           	int3
   4285c: cc                           	int3
   4285d: cc                           	int3
   4285e: cc                           	int3
   4285f: cc                           	int3

# svm: Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G
0000000000042cc0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>:
   42cc0: 48 83 ec 08                  	subq	$0x8, %rsp
   42cc4: 4c 8b 15 05 da 88 00         	movq	0x88da05(%rip), %r10    # 0x8d06d0 <__svm_vm_is_static_binary+0x298>
   42ccb: b8 00 00 00 00               	movl	$0x0, %eax
   42cd0: 41 ff d2                     	callq	*%r10
   42cd3: 48 83 c4 08                  	addq	$0x8, %rsp
   42cd7: c3                           	retq
   42cd8: cc                           	int3
   42cd9: cc                           	int3
   42cda: cc                           	int3
   42cdb: cc                           	int3
   42cdc: cc                           	int3
   42cdd: cc                           	int3
   42cde: cc                           	int3
   42cdf: cc                           	int3

