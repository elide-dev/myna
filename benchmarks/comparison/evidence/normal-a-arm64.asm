00000001000123e0 <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5>:
1000123e0: d101c3e9    	sub	x9, sp, #0x70
1000123e4: f940078a    	ldr	x10, [x28, #0x8]
1000123e8: eb0a013f    	cmp	x9, x10
1000123ec: 54000809    	b.ls	0x1000124ec <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5+0x10c>
1000123f0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000123f4: 9100013f    	mov	sp, x9
1000123f8: b90037e1    	str	w1, [sp, #0x34]
1000123fc: d0001fe3    	adrp	x3, 0x100410000 <___data>
100012400: f9416863    	ldr	x3, [x3, #0x2d0]
100012404: f90017e3    	str	x3, [sp, #0x28]
100012408: aa0203e0    	mov	x0, x2
10001240c: 2a1f03e2    	mov	w2, wzr
100012410: 14000024    	b	0x1000124a0 <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5+0xc0>
100012414: d503201f    	nop
100012418: d503201f    	nop
10001241c: d503201f    	nop
100012420: b90033e2    	str	w2, [sp, #0x30]
100012424: 9100e3ed    	add	x13, sp, #0x38
100012428: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
10001242c: f9400f9e    	ldr	x30, [x28, #0x18]
100012430: f9000dbe    	str	x30, [x13, #0x18]
100012434: f9000f8d    	str	x13, [x28, #0x18]
100012438: aa0303fe    	mov	x30, x3
10001243c: 10000029    	adr	x9, 0x100012440 <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5+0x60>
100012440: f90005a9    	str	x9, [x13, #0x8]
100012444: 910003e9    	mov	x9, sp
100012448: f90009a9    	str	x9, [x13, #0x10]
10001244c: 320007e9    	orr	w9, wzr, #0x3
100012450: 9100538a    	add	x10, x28, #0x14
100012454: 889ffd49    	stlr	w9, [x10]
100012458: d63f03c0    	blr	x30
10001245c: 320007e1    	orr	w1, wzr, #0x3
100012460: 320003e2    	orr	w2, wzr, #0x1
100012464: 91005383    	add	x3, x28, #0x14
100012468: 2a0103e4    	mov	w4, w1
10001246c: 88e4fc62    	casal	w4, w2, [x3]
100012470: 6b01009f    	cmp	w4, w1
100012474: 1a9f17e1    	cset	w1, eq
100012478: 340001a1    	cbz	w1, 0x1000124ac <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5+0xcc>
10001247c: f9400f81    	ldr	x1, [x28, #0x18]
100012480: f9400c21    	ldr	x1, [x1, #0x18]
100012484: f9000f81    	str	x1, [x28, #0x18]
100012488: b9402f81    	ldr	w1, [x28, #0x2c]
10001248c: 350001c1    	cbnz	w1, 0x1000124c4 <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5+0xe4>
100012490: b94033e2    	ldr	w2, [sp, #0x30]
100012494: 11000442    	add	w2, w2, #0x1
100012498: f94017e3    	ldr	x3, [sp, #0x28]
10001249c: b94037e1    	ldr	w1, [sp, #0x34]
1000124a0: 6b01005f    	cmp	w2, w1
1000124a4: 54fffbeb    	b.lt	0x100012420 <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5+0x40>
1000124a8: 1400000b    	b	0x1000124d4 <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5+0xf4>
1000124ac: f90013e0    	str	x0, [sp, #0x20]
1000124b0: 320003e0    	orr	w0, wzr, #0x1
1000124b4: 320003e1    	orr	w1, wzr, #0x1
1000124b8: 9405d41e    	bl	0x100187530 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
1000124bc: f94013e0    	ldr	x0, [sp, #0x20]
1000124c0: 17fffff4    	b	0x100012490 <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5+0xb0>
1000124c4: f90013e0    	str	x0, [sp, #0x20]
1000124c8: 9405d3ae    	bl	0x100187380 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
1000124cc: f94013e0    	ldr	x0, [sp, #0x20]
1000124d0: 17fffff0    	b	0x100012490 <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5+0xb0>
1000124d4: a9467bfd    	ldp	x29, x30, [sp, #0x60]
1000124d8: 9101c3ff    	add	sp, sp, #0x70
1000124dc: b9401389    	ldr	w9, [x28, #0x10]
1000124e0: 7100013f    	cmp	w9, #0x0
1000124e4: 5400006d    	b.le	0x1000124f0 <_Native$$L564382a4_run_JvOcpPasxkCBdFCf6XfXu5+0x110>
1000124e8: d65f03c0    	ret
1000124ec: 14027fe1    	b	0x1000b2470 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000124f0: 1405d1d4    	b	0x100186c40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000124f4: cccccccc    	<unknown>
1000124f8: cccccccc    	<unknown>
1000124fc: cccccccc    	<unknown>

