0000000100012820 <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71>:
100012820: d101c3e9    	sub	x9, sp, #0x70
100012824: f940078a    	ldr	x10, [x28, #0x8]
100012828: eb0a013f    	cmp	x9, x10
10001282c: 54000809    	b.ls	0x10001292c <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71+0x10c>
100012830: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100012834: 9100013f    	mov	sp, x9
100012838: b90037e1    	str	w1, [sp, #0x34]
10001283c: d0001fe3    	adrp	x3, 0x100410000 <___data>
100012840: f9416863    	ldr	x3, [x3, #0x2d0]
100012844: f90017e3    	str	x3, [sp, #0x28]
100012848: aa0203e0    	mov	x0, x2
10001284c: 2a1f03e2    	mov	w2, wzr
100012850: 14000024    	b	0x1000128e0 <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71+0xc0>
100012854: d503201f    	nop
100012858: d503201f    	nop
10001285c: d503201f    	nop
100012860: b90033e2    	str	w2, [sp, #0x30]
100012864: 9100e3ed    	add	x13, sp, #0x38
100012868: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
10001286c: f9400f9e    	ldr	x30, [x28, #0x18]
100012870: f9000dbe    	str	x30, [x13, #0x18]
100012874: f9000f8d    	str	x13, [x28, #0x18]
100012878: aa0303fe    	mov	x30, x3
10001287c: 10000029    	adr	x9, 0x100012880 <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71+0x60>
100012880: f90005a9    	str	x9, [x13, #0x8]
100012884: 910003e9    	mov	x9, sp
100012888: f90009a9    	str	x9, [x13, #0x10]
10001288c: 320007e9    	orr	w9, wzr, #0x3
100012890: 9100538a    	add	x10, x28, #0x14
100012894: 889ffd49    	stlr	w9, [x10]
100012898: d63f03c0    	blr	x30
10001289c: 320007e1    	orr	w1, wzr, #0x3
1000128a0: 320003e2    	orr	w2, wzr, #0x1
1000128a4: 91005383    	add	x3, x28, #0x14
1000128a8: 2a0103e4    	mov	w4, w1
1000128ac: 88e4fc62    	casal	w4, w2, [x3]
1000128b0: 6b01009f    	cmp	w4, w1
1000128b4: 1a9f17e1    	cset	w1, eq
1000128b8: 340001a1    	cbz	w1, 0x1000128ec <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71+0xcc>
1000128bc: f9400f81    	ldr	x1, [x28, #0x18]
1000128c0: f9400c21    	ldr	x1, [x1, #0x18]
1000128c4: f9000f81    	str	x1, [x28, #0x18]
1000128c8: b9402f81    	ldr	w1, [x28, #0x2c]
1000128cc: 350001c1    	cbnz	w1, 0x100012904 <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71+0xe4>
1000128d0: b94033e2    	ldr	w2, [sp, #0x30]
1000128d4: 11000442    	add	w2, w2, #0x1
1000128d8: f94017e3    	ldr	x3, [sp, #0x28]
1000128dc: b94037e1    	ldr	w1, [sp, #0x34]
1000128e0: 6b01005f    	cmp	w2, w1
1000128e4: 54fffbeb    	b.lt	0x100012860 <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71+0x40>
1000128e8: 1400000b    	b	0x100012914 <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71+0xf4>
1000128ec: f90013e0    	str	x0, [sp, #0x20]
1000128f0: 320003e0    	orr	w0, wzr, #0x1
1000128f4: 320003e1    	orr	w1, wzr, #0x1
1000128f8: 9405d30e    	bl	0x100187530 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
1000128fc: f94013e0    	ldr	x0, [sp, #0x20]
100012900: 17fffff4    	b	0x1000128d0 <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71+0xb0>
100012904: f90013e0    	str	x0, [sp, #0x20]
100012908: 9405d29e    	bl	0x100187380 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
10001290c: f94013e0    	ldr	x0, [sp, #0x20]
100012910: 17fffff0    	b	0x1000128d0 <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71+0xb0>
100012914: a9467bfd    	ldp	x29, x30, [sp, #0x60]
100012918: 9101c3ff    	add	sp, sp, #0x70
10001291c: b9401389    	ldr	w9, [x28, #0x10]
100012920: 7100013f    	cmp	w9, #0x0
100012924: 5400006d    	b.le	0x100012930 <_Native$$L94f46073_run_k6opIlXPAgHpfkHJM5uc71+0x110>
100012928: d65f03c0    	ret
10001292c: 14027ed1    	b	0x1000b2470 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100012930: 1405d0c4    	b	0x100186c40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100012934: cccccccc    	<unknown>
100012938: cccccccc    	<unknown>
10001293c: cccccccc    	<unknown>

