
build/upcall-records/libranges.so:	file format elf64-x86-64

Disassembly of section .text:

0000000000001640 <myna_deliver_records>:
    1640: 48 85 d2                     	testq	%rdx, %rdx
    1643: 0f 9e c0                     	setle	%al
    1646: 48 85 f6                     	testq	%rsi, %rsi
    1649: 0f 98 c1                     	sets	%cl
    164c: 08 c1                        	orb	%al, %cl
    164e: 48 c7 c0 ff ff ff ff         	movq	$-0x1, %rax
    1655: 75 4e                        	jne	0x16a5 <myna_deliver_records+0x65>
    1657: 41 57                        	pushq	%r15
    1659: 41 56                        	pushq	%r14
    165b: 41 55                        	pushq	%r13
    165d: 41 54                        	pushq	%r12
    165f: 53                           	pushq	%rbx
    1660: 48 89 d3                     	movq	%rdx, %rbx
    1663: 49 89 f6                     	movq	%rsi, %r14
    1666: 49 89 ff                     	movq	%rdi, %r15
    1669: 45 31 e4                     	xorl	%r12d, %r12d
    166c: 0f 1f 40 00                  	nopl	(%rax)
    1670: 4d 89 f5                     	movq	%r14, %r13
    1673: 4d 29 e5                     	subq	%r12, %r13
    1676: 7e 21                        	jle	0x1699 <myna_deliver_records+0x59>
    1678: 49 39 dd                     	cmpq	%rbx, %r13
    167b: 4c 0f 4d eb                  	cmovgeq	%rbx, %r13
    167f: 4c 89 e7                     	movq	%r12, %rdi
    1682: 4c 89 ee                     	movq	%r13, %rsi
    1685: 41 ff d7                     	callq	*%r15
    1688: 4d 01 ec                     	addq	%r13, %r12
    168b: 4c 39 e8                     	cmpq	%r13, %rax
    168e: 74 e0                        	je	0x1670 <myna_deliver_records+0x30>
    1690: 48 c7 c0 ff ff ff ff         	movq	$-0x1, %rax
    1697: eb 03                        	jmp	0x169c <myna_deliver_records+0x5c>
    1699: 4c 89 e0                     	movq	%r12, %rax
    169c: 5b                           	popq	%rbx
    169d: 41 5c                        	popq	%r12
    169f: 41 5d                        	popq	%r13
    16a1: 41 5e                        	popq	%r14
    16a3: 41 5f                        	popq	%r15
    16a5: c3                           	retq
    16a6: cc                           	int3
    16a7: cc                           	int3
