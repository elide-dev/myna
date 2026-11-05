# native: _direct
00000001000008fc <_direct>:
1000008fc: 7100041f    	cmp	w0, #0x1
100000900: 5400018b    	b.lt	0x100000930 <_direct+0x34>
100000904: a9be4ff4    	stp	x20, x19, [sp, #-0x20]!
100000908: a9017bfd    	stp	x29, x30, [sp, #0x10]
10000090c: 910043fd    	add	x29, sp, #0x10
100000910: aa0003f3    	mov	x19, x0
100000914: aa0103e0    	mov	x0, x1
100000918: 940000b1    	bl	0x100000bdc <_myna_bench_leaf>
10000091c: aa0003e1    	mov	x1, x0
100000920: 71000673    	subs	w19, w19, #0x1
100000924: 54ffff81    	b.ne	0x100000914 <_direct+0x18>
100000928: a9417bfd    	ldp	x29, x30, [sp, #0x10]
10000092c: a8c24ff4    	ldp	x20, x19, [sp], #0x20
100000930: aa0103e0    	mov	x0, x1
100000934: d65f03c0    	ret

# native: _indirect
0000000100000938 <_indirect>:
100000938: a9be4ff4    	stp	x20, x19, [sp, #-0x20]!
10000093c: a9017bfd    	stp	x29, x30, [sp, #0x10]
100000940: 910043fd    	add	x29, sp, #0x10
100000944: d503201f    	nop
100000948: 5803b7d4    	ldr	x20, 0x100008040 <_target>
10000094c: 7100041f    	cmp	w0, #0x1
100000950: 540000eb    	b.lt	0x10000096c <_indirect+0x34>
100000954: aa0003f3    	mov	x19, x0
100000958: aa0103e0    	mov	x0, x1
10000095c: d63f0280    	blr	x20
100000960: aa0003e1    	mov	x1, x0
100000964: 71000673    	subs	w19, w19, #0x1
100000968: 54ffff81    	b.ne	0x100000958 <_indirect+0x20>
10000096c: aa0103e0    	mov	x0, x1
100000970: a9417bfd    	ldp	x29, x30, [sp, #0x10]
100000974: a8c24ff4    	ldp	x20, x19, [sp], #0x20
100000978: d65f03c0    	ret

# native: _identity_direct
0000000100000a8c <_identity_direct>:
100000a8c: 7100041f    	cmp	w0, #0x1
100000a90: 5400018b    	b.lt	0x100000ac0 <_identity_direct+0x34>
100000a94: a9be4ff4    	stp	x20, x19, [sp, #-0x20]!
100000a98: a9017bfd    	stp	x29, x30, [sp, #0x10]
100000a9c: 910043fd    	add	x29, sp, #0x10
100000aa0: aa0003f3    	mov	x19, x0
100000aa4: aa0103e0    	mov	x0, x1
100000aa8: 940000bd    	bl	0x100000d9c <_myna_bench_identity>
100000aac: aa0003e1    	mov	x1, x0
100000ab0: 71000673    	subs	w19, w19, #0x1
100000ab4: 54ffff81    	b.ne	0x100000aa4 <_identity_direct+0x18>
100000ab8: a9417bfd    	ldp	x29, x30, [sp, #0x10]
100000abc: a8c24ff4    	ldp	x20, x19, [sp], #0x20
100000ac0: aa0103e0    	mov	x0, x1
100000ac4: d65f03c0    	ret

# native: _identity_indirect
0000000100000ac8 <_identity_indirect>:
100000ac8: a9be4ff4    	stp	x20, x19, [sp, #-0x20]!
100000acc: a9017bfd    	stp	x29, x30, [sp, #0x10]
100000ad0: 910043fd    	add	x29, sp, #0x10
100000ad4: d503201f    	nop
100000ad8: 5803ab94    	ldr	x20, 0x100008048 <_identity_target>
100000adc: 7100041f    	cmp	w0, #0x1
100000ae0: 540000eb    	b.lt	0x100000afc <_identity_indirect+0x34>
100000ae4: aa0003f3    	mov	x19, x0
100000ae8: aa0103e0    	mov	x0, x1
100000aec: d63f0280    	blr	x20
100000af0: aa0003e1    	mov	x1, x0
100000af4: 71000673    	subs	w19, w19, #0x1
100000af8: 54ffff81    	b.ne	0x100000ae8 <_identity_indirect+0x20>
100000afc: aa0103e0    	mov	x0, x1
100000b00: a9417bfd    	ldp	x29, x30, [sp, #0x10]
100000b04: a8c24ff4    	ldp	x20, x19, [sp], #0x20
100000b08: d65f03c0    	ret

# native: _identity_direct_unrolled4
0000000100000b0c <_identity_direct_unrolled4>:
100000b0c: 7100041f    	cmp	w0, #0x1
100000b10: 5400022b    	b.lt	0x100000b54 <_identity_direct_unrolled4+0x48>
100000b14: a9be4ff4    	stp	x20, x19, [sp, #-0x20]!
100000b18: a9017bfd    	stp	x29, x30, [sp, #0x10]
100000b1c: 910043fd    	add	x29, sp, #0x10
100000b20: aa0003f3    	mov	x19, x0
100000b24: 52800014    	mov	w20, #0x0               ; =0
100000b28: aa0103e0    	mov	x0, x1
100000b2c: 9400009c    	bl	0x100000d9c <_myna_bench_identity>
100000b30: 9400009b    	bl	0x100000d9c <_myna_bench_identity>
100000b34: 9400009a    	bl	0x100000d9c <_myna_bench_identity>
100000b38: 94000099    	bl	0x100000d9c <_myna_bench_identity>
100000b3c: aa0003e1    	mov	x1, x0
100000b40: 11001294    	add	w20, w20, #0x4
100000b44: 6b13029f    	cmp	w20, w19
100000b48: 54ffff0b    	b.lt	0x100000b28 <_identity_direct_unrolled4+0x1c>
100000b4c: a9417bfd    	ldp	x29, x30, [sp, #0x10]
100000b50: a8c24ff4    	ldp	x20, x19, [sp], #0x20
100000b54: aa0103e0    	mov	x0, x1
100000b58: d65f03c0    	ret

# native: _identity_indirect_unrolled4
0000000100000b5c <_identity_indirect_unrolled4>:
100000b5c: a9bd57f6    	stp	x22, x21, [sp, #-0x30]!
100000b60: a9014ff4    	stp	x20, x19, [sp, #0x10]
100000b64: a9027bfd    	stp	x29, x30, [sp, #0x20]
100000b68: 910083fd    	add	x29, sp, #0x20
100000b6c: d503201f    	nop
100000b70: 5803a6d4    	ldr	x20, 0x100008048 <_identity_target>
100000b74: 7100041f    	cmp	w0, #0x1
100000b78: 5400018b    	b.lt	0x100000ba8 <_identity_indirect_unrolled4+0x4c>
100000b7c: aa0003f3    	mov	x19, x0
100000b80: 52800015    	mov	w21, #0x0               ; =0
100000b84: aa0103e0    	mov	x0, x1
100000b88: d63f0280    	blr	x20
100000b8c: d63f0280    	blr	x20
100000b90: d63f0280    	blr	x20
100000b94: d63f0280    	blr	x20
100000b98: aa0003e1    	mov	x1, x0
100000b9c: 110012b5    	add	w21, w21, #0x4
100000ba0: 6b1302bf    	cmp	w21, w19
100000ba4: 54ffff0b    	b.lt	0x100000b84 <_identity_indirect_unrolled4+0x28>
100000ba8: aa0103e0    	mov	x0, x1
100000bac: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100000bb0: a9414ff4    	ldp	x20, x19, [sp, #0x10]
100000bb4: a8c357f6    	ldp	x22, x21, [sp], #0x30
100000bb8: d65f03c0    	ret

# native: _myna_bench_identity
0000000100000d9c <_myna_bench_identity>:
100000d9c: d65f03c0    	ret

# native: _myna_bench_identity_address
0000000100000da0 <_myna_bench_identity_address>:
100000da0: 10ffffe0    	adr	x0, 0x100000d9c <_myna_bench_identity>
100000da4: d503201f    	nop
100000da8: d65f03c0    	ret

# svm: _Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F
00000001000102d0 <_Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F>:
1000102d0: d10103e9    	sub	x9, sp, #0x40
1000102d4: f940078a    	ldr	x10, [x28, #0x8]
1000102d8: eb0a013f    	cmp	x9, x10
1000102dc: 540008c9    	b.ls	0x1000103f4 <_Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x124>
1000102e0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000102e4: 9100013f    	mov	sp, x9
1000102e8: 7100043f    	cmp	w1, #0x1
1000102ec: 5400080b    	b.lt	0x1000103ec <_Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x11c>
1000102f0: b9002fe1    	str	w1, [sp, #0x2c]
1000102f4: 52871160    	mov	w0, #0x388b             ; =14475
1000102f8: 72a000a0    	movk	w0, #0x5, lsl #16
1000102fc: 8b000f60    	add	x0, x27, x0, lsl #3
100010300: f9402403    	ldr	x3, [x0, #0x48]
100010304: f90013e3    	str	x3, [sp, #0x20]
100010308: 2a1f03e9    	mov	w9, wzr
10001030c: b9002be9    	str	w9, [sp, #0x28]
100010310: aa0203e0    	mov	x0, x2
100010314: aa0303fe    	mov	x30, x3
100010318: d63f03c0    	blr	x30
10001031c: f94013fe    	ldr	x30, [sp, #0x20]
100010320: d63f03c0    	blr	x30
100010324: f94013fe    	ldr	x30, [sp, #0x20]
100010328: d63f03c0    	blr	x30
10001032c: f94013fe    	ldr	x30, [sp, #0x20]
100010330: d63f03c0    	blr	x30
100010334: f94013fe    	ldr	x30, [sp, #0x20]
100010338: d63f03c0    	blr	x30
10001033c: f94013fe    	ldr	x30, [sp, #0x20]
100010340: d63f03c0    	blr	x30
100010344: f94013fe    	ldr	x30, [sp, #0x20]
100010348: d63f03c0    	blr	x30
10001034c: f94013fe    	ldr	x30, [sp, #0x20]
100010350: d63f03c0    	blr	x30
100010354: f94013fe    	ldr	x30, [sp, #0x20]
100010358: d63f03c0    	blr	x30
10001035c: f94013fe    	ldr	x30, [sp, #0x20]
100010360: d63f03c0    	blr	x30
100010364: f94013fe    	ldr	x30, [sp, #0x20]
100010368: d63f03c0    	blr	x30
10001036c: f94013fe    	ldr	x30, [sp, #0x20]
100010370: d63f03c0    	blr	x30
100010374: f94013fe    	ldr	x30, [sp, #0x20]
100010378: d63f03c0    	blr	x30
10001037c: f94013fe    	ldr	x30, [sp, #0x20]
100010380: d63f03c0    	blr	x30
100010384: f94013fe    	ldr	x30, [sp, #0x20]
100010388: d63f03c0    	blr	x30
10001038c: f94013fe    	ldr	x30, [sp, #0x20]
100010390: d63f03c0    	blr	x30
100010394: b9402bfd    	ldr	w29, [sp, #0x28]
100010398: 110043a1    	add	w1, w29, #0x10
10001039c: b9402fe2    	ldr	w2, [sp, #0x2c]
1000103a0: 6b02003f    	cmp	w1, w2
1000103a4: 5400018a    	b.ge	0x1000103d4 <_Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x104>
1000103a8: b9401389    	ldr	w9, [x28, #0x10]
1000103ac: 340000a9    	cbz	w9, 0x1000103c0 <_Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0xf0>
1000103b0: b9002be1    	str	w1, [sp, #0x28]
1000103b4: f94013e3    	ldr	x3, [sp, #0x20]
1000103b8: aa0003e2    	mov	x2, x0
1000103bc: 17ffffd5    	b	0x100010310 <_Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x40>
1000103c0: 9405edd4    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000103c4: b9002be1    	str	w1, [sp, #0x28]
1000103c8: f94013e3    	ldr	x3, [sp, #0x20]
1000103cc: aa0003e2    	mov	x2, x0
1000103d0: 17ffffd0    	b	0x100010310 <_Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x40>
1000103d4: a9437bfd    	ldp	x29, x30, [sp, #0x30]
1000103d8: 910103ff    	add	sp, sp, #0x40
1000103dc: b9401389    	ldr	w9, [x28, #0x10]
1000103e0: 7100013f    	cmp	w9, #0x0
1000103e4: 540000ad    	b.le	0x1000103f8 <_Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x128>
1000103e8: d65f03c0    	ret
1000103ec: aa0203e0    	mov	x0, x2
1000103f0: 17fffff9    	b	0x1000103d4 <_Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x104>
1000103f4: 14029abb    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000103f8: 1405edc6    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000103fc: cccccccc    	<unknown>

# svm: _Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH
0000000100010400 <_Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH>:
100010400: d10103e9    	sub	x9, sp, #0x40
100010404: f940078a    	ldr	x10, [x28, #0x8]
100010408: eb0a013f    	cmp	x9, x10
10001040c: 54000809    	b.ls	0x10001050c <_Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x10c>
100010410: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010414: 9100013f    	mov	sp, x9
100010418: b9002fe1    	str	w1, [sp, #0x2c]
10001041c: 7100043f    	cmp	w1, #0x1
100010420: 5400030b    	b.lt	0x100010480 <_Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x80>
100010424: 52871160    	mov	w0, #0x388b             ; =14475
100010428: 72a000a0    	movk	w0, #0x5, lsl #16
10001042c: 8b000f60    	add	x0, x27, x0, lsl #3
100010430: f9402403    	ldr	x3, [x0, #0x48]
100010434: f90013e3    	str	x3, [sp, #0x20]
100010438: aa0203e0    	mov	x0, x2
10001043c: aa0303fe    	mov	x30, x3
100010440: d63f03c0    	blr	x30
100010444: f94013fe    	ldr	x30, [sp, #0x20]
100010448: d63f03c0    	blr	x30
10001044c: f94013fe    	ldr	x30, [sp, #0x20]
100010450: d63f03c0    	blr	x30
100010454: f94013fe    	ldr	x30, [sp, #0x20]
100010458: d63f03c0    	blr	x30
10001045c: b9402fe1    	ldr	w1, [sp, #0x2c]
100010460: 7100143f    	cmp	w1, #0x5
100010464: 540001ca    	b.ge	0x10001049c <_Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x9c>
100010468: a9437bfd    	ldp	x29, x30, [sp, #0x30]
10001046c: 910103ff    	add	sp, sp, #0x40
100010470: b9401389    	ldr	w9, [x28, #0x10]
100010474: 7100013f    	cmp	w9, #0x0
100010478: 540004cd    	b.le	0x100010510 <_Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x110>
10001047c: d65f03c0    	ret
100010480: aa0203e0    	mov	x0, x2
100010484: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010488: 910103ff    	add	sp, sp, #0x40
10001048c: b9401389    	ldr	w9, [x28, #0x10]
100010490: 7100013f    	cmp	w9, #0x0
100010494: 540003ed    	b.le	0x100010510 <_Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x110>
100010498: d65f03c0    	ret
10001049c: 321e03e9    	orr	w9, wzr, #0x4
1000104a0: b9002be9    	str	w9, [sp, #0x28]
1000104a4: f94013e3    	ldr	x3, [sp, #0x20]
1000104a8: d503201f    	nop
1000104ac: d503201f    	nop
1000104b0: aa0303fe    	mov	x30, x3
1000104b4: d63f03c0    	blr	x30
1000104b8: f94013fe    	ldr	x30, [sp, #0x20]
1000104bc: d63f03c0    	blr	x30
1000104c0: f94013fe    	ldr	x30, [sp, #0x20]
1000104c4: d63f03c0    	blr	x30
1000104c8: f94013fe    	ldr	x30, [sp, #0x20]
1000104cc: d63f03c0    	blr	x30
1000104d0: b9402be1    	ldr	w1, [sp, #0x28]
1000104d4: 11001022    	add	w2, w1, #0x4
1000104d8: b9402fe3    	ldr	w3, [sp, #0x2c]
1000104dc: 6b03005f    	cmp	w2, w3
1000104e0: 54fffc4a    	b.ge	0x100010468 <_Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x68>
1000104e4: b9401389    	ldr	w9, [x28, #0x10]
1000104e8: 34000089    	cbz	w9, 0x1000104f8 <_Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0xf8>
1000104ec: b9002be2    	str	w2, [sp, #0x28]
1000104f0: f94013e3    	ldr	x3, [sp, #0x20]
1000104f4: 17ffffef    	b	0x1000104b0 <_Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0xb0>
1000104f8: f94013e4    	ldr	x4, [sp, #0x20]
1000104fc: 9405ed85    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010500: b9002be2    	str	w2, [sp, #0x28]
100010504: aa0403e3    	mov	x3, x4
100010508: 17ffffea    	b	0x1000104b0 <_Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0xb0>
10001050c: 14029a75    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010510: 1405ed80    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010514: cccccccc    	<unknown>
100010518: cccccccc    	<unknown>
10001051c: cccccccc    	<unknown>

# svm: _Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I
0000000100010520 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I>:
100010520: d10143e9    	sub	x9, sp, #0x50
100010524: f940078a    	ldr	x10, [x28, #0x8]
100010528: eb0a013f    	cmp	x9, x10
10001052c: 54000bc9    	b.ls	0x1000106a4 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x184>
100010530: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010534: 9100013f    	mov	sp, x9
100010538: b90033e1    	str	w1, [sp, #0x30]
10001053c: 7100043f    	cmp	w1, #0x1
100010540: 320003e0    	orr	w0, wzr, #0x1
100010544: 1a80d020    	csel	w0, w1, w0, le
100010548: 7100041f    	cmp	w0, #0x1
10001054c: 54000a6b    	b.lt	0x100010698 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x178>
100010550: aa0203e0    	mov	x0, x2
100010554: 94001767    	bl	0x1000162f0 <_Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
100010558: 320003e2    	orr	w2, wzr, #0x1
10001055c: b94033e1    	ldr	w1, [sp, #0x30]
100010560: 52800083    	mov	w3, #0x4                ; =4
100010564: 72b00003    	movk	w3, #0x8000, lsl #16
100010568: 320103e4    	orr	w4, wzr, #0x80000000
10001056c: 51001025    	sub	w5, w1, #0x4
100010570: 6b03003f    	cmp	w1, w3
100010574: 1a85b083    	csel	w3, w4, w5, lt
100010578: b9002be3    	str	w3, [sp, #0x28]
10001057c: 11000c64    	add	w4, w3, #0x3
100010580: b90027e4    	str	w4, [sp, #0x24]
100010584: 2a0203e1    	mov	w1, w2
100010588: d503201f    	nop
10001058c: d503201f    	nop
100010590: 6b03003f    	cmp	w1, w3
100010594: 5400062a    	b.ge	0x100010658 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x138>
100010598: 4b010085    	sub	w5, w4, w1
10001059c: 53027ca6    	lsr	w6, w5, #2
1000105a0: 121e74a5    	and	w5, w5, #0xfffffffc
1000105a4: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
1000105a8: 321203e6    	orr	w6, wzr, #0x4000
1000105ac: 1a8630a5    	csel	w5, w5, w6, lo
1000105b0: 710004bf    	cmp	w5, #0x1
1000105b4: 540004ab    	b.lt	0x100010648 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x128>
1000105b8: b90037e1    	str	w1, [sp, #0x34]
1000105bc: b9003fe5    	str	w5, [sp, #0x3c]
1000105c0: 2a1f03e2    	mov	w2, wzr
1000105c4: d503201f    	nop
1000105c8: d503201f    	nop
1000105cc: d503201f    	nop
1000105d0: 0b010047    	add	w7, w2, w1
1000105d4: b90023e7    	str	w7, [sp, #0x20]
1000105d8: b9002fe2    	str	w2, [sp, #0x2c]
1000105dc: 94001745    	bl	0x1000162f0 <_Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
1000105e0: b94023e7    	ldr	w7, [sp, #0x20]
1000105e4: 110004e1    	add	w1, w7, #0x1
1000105e8: b9003be1    	str	w1, [sp, #0x38]
1000105ec: 94001741    	bl	0x1000162f0 <_Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
1000105f0: b94023e7    	ldr	w7, [sp, #0x20]
1000105f4: 110008e1    	add	w1, w7, #0x2
1000105f8: b9003be1    	str	w1, [sp, #0x38]
1000105fc: 9400173d    	bl	0x1000162f0 <_Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
100010600: b94023e7    	ldr	w7, [sp, #0x20]
100010604: 11000ce1    	add	w1, w7, #0x3
100010608: b90023e1    	str	w1, [sp, #0x20]
10001060c: 94001739    	bl	0x1000162f0 <_Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
100010610: b9402fe2    	ldr	w2, [sp, #0x2c]
100010614: 11001042    	add	w2, w2, #0x4
100010618: b9403fe5    	ldr	w5, [sp, #0x3c]
10001061c: 6b05005f    	cmp	w2, w5
100010620: b94037e1    	ldr	w1, [sp, #0x34]
100010624: 54fffd6b    	b.lt	0x1000105d0 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0xb0>
100010628: 0b010041    	add	w1, w2, w1
10001062c: b9401389    	ldr	w9, [x28, #0x10]
100010630: 34000069    	cbz	w9, 0x10001063c <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x11c>
100010634: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100010638: 17ffffd6    	b	0x100010590 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x70>
10001063c: 9405ed35    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010640: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100010644: 17ffffd3    	b	0x100010590 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x70>
100010648: b9401389    	ldr	w9, [x28, #0x10]
10001064c: 35fffa29    	cbnz	w9, 0x100010590 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x70>
100010650: 9405ed30    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010654: 17ffffcf    	b	0x100010590 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x70>
100010658: 2a0103e2    	mov	w2, w1
10001065c: 14000006    	b	0x100010674 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x154>
100010660: b9002fe2    	str	w2, [sp, #0x2c]
100010664: 94001723    	bl	0x1000162f0 <_Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
100010668: b9402fe2    	ldr	w2, [sp, #0x2c]
10001066c: 1100045d    	add	w29, w2, #0x1
100010670: 2a1d03e2    	mov	w2, w29
100010674: b94033e1    	ldr	w1, [sp, #0x30]
100010678: 6b01005f    	cmp	w2, w1
10001067c: 54ffff2b    	b.lt	0x100010660 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x140>
100010680: a9447bfd    	ldp	x29, x30, [sp, #0x40]
100010684: 910143ff    	add	sp, sp, #0x50
100010688: b9401389    	ldr	w9, [x28, #0x10]
10001068c: 7100013f    	cmp	w9, #0x0
100010690: 540000cd    	b.le	0x1000106a8 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x188>
100010694: d65f03c0    	ret
100010698: aa0203e0    	mov	x0, x2
10001069c: 2a1f03e2    	mov	w2, wzr
1000106a0: 17ffffb0    	b	0x100010560 <_Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x40>
1000106a4: 14029a0f    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000106a8: 1405ed1a    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000106ac: cccccccc    	<unknown>

# svm: _Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D
00000001000106b0 <_Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D>:
1000106b0: d10103e9    	sub	x9, sp, #0x40
1000106b4: f940078a    	ldr	x10, [x28, #0x8]
1000106b8: eb0a013f    	cmp	x9, x10
1000106bc: 54000809    	b.ls	0x1000107bc <_Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x10c>
1000106c0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000106c4: 9100013f    	mov	sp, x9
1000106c8: b9002fe1    	str	w1, [sp, #0x2c]
1000106cc: 7100043f    	cmp	w1, #0x1
1000106d0: 5400030b    	b.lt	0x100010730 <_Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x80>
1000106d4: 52871160    	mov	w0, #0x388b             ; =14475
1000106d8: 72a000a0    	movk	w0, #0x5, lsl #16
1000106dc: 8b000f60    	add	x0, x27, x0, lsl #3
1000106e0: f9402403    	ldr	x3, [x0, #0x48]
1000106e4: f90013e3    	str	x3, [sp, #0x20]
1000106e8: aa0203e0    	mov	x0, x2
1000106ec: aa0303fe    	mov	x30, x3
1000106f0: d63f03c0    	blr	x30
1000106f4: f94013fe    	ldr	x30, [sp, #0x20]
1000106f8: d63f03c0    	blr	x30
1000106fc: f94013fe    	ldr	x30, [sp, #0x20]
100010700: d63f03c0    	blr	x30
100010704: f94013fe    	ldr	x30, [sp, #0x20]
100010708: d63f03c0    	blr	x30
10001070c: b9402fe1    	ldr	w1, [sp, #0x2c]
100010710: 7100143f    	cmp	w1, #0x5
100010714: 540001ca    	b.ge	0x10001074c <_Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x9c>
100010718: a9437bfd    	ldp	x29, x30, [sp, #0x30]
10001071c: 910103ff    	add	sp, sp, #0x40
100010720: b9401389    	ldr	w9, [x28, #0x10]
100010724: 7100013f    	cmp	w9, #0x0
100010728: 540004cd    	b.le	0x1000107c0 <_Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x110>
10001072c: d65f03c0    	ret
100010730: aa0203e0    	mov	x0, x2
100010734: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010738: 910103ff    	add	sp, sp, #0x40
10001073c: b9401389    	ldr	w9, [x28, #0x10]
100010740: 7100013f    	cmp	w9, #0x0
100010744: 540003ed    	b.le	0x1000107c0 <_Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x110>
100010748: d65f03c0    	ret
10001074c: 321e03e9    	orr	w9, wzr, #0x4
100010750: b9002be9    	str	w9, [sp, #0x28]
100010754: f94013e3    	ldr	x3, [sp, #0x20]
100010758: d503201f    	nop
10001075c: d503201f    	nop
100010760: aa0303fe    	mov	x30, x3
100010764: d63f03c0    	blr	x30
100010768: f94013fe    	ldr	x30, [sp, #0x20]
10001076c: d63f03c0    	blr	x30
100010770: f94013fe    	ldr	x30, [sp, #0x20]
100010774: d63f03c0    	blr	x30
100010778: f94013fe    	ldr	x30, [sp, #0x20]
10001077c: d63f03c0    	blr	x30
100010780: b9402be1    	ldr	w1, [sp, #0x28]
100010784: 11001022    	add	w2, w1, #0x4
100010788: b9402fe3    	ldr	w3, [sp, #0x2c]
10001078c: 6b03005f    	cmp	w2, w3
100010790: 54fffc4a    	b.ge	0x100010718 <_Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x68>
100010794: b9401389    	ldr	w9, [x28, #0x10]
100010798: 34000089    	cbz	w9, 0x1000107a8 <_Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0xf8>
10001079c: b9002be2    	str	w2, [sp, #0x28]
1000107a0: f94013e3    	ldr	x3, [sp, #0x20]
1000107a4: 17ffffef    	b	0x100010760 <_Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0xb0>
1000107a8: f94013e4    	ldr	x4, [sp, #0x20]
1000107ac: 9405ecd9    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000107b0: b9002be2    	str	w2, [sp, #0x28]
1000107b4: aa0403e3    	mov	x3, x4
1000107b8: 17ffffea    	b	0x100010760 <_Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0xb0>
1000107bc: 140299c9    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000107c0: 1405ecd4    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000107c4: cccccccc    	<unknown>
1000107c8: cccccccc    	<unknown>
1000107cc: cccccccc    	<unknown>

# svm: _Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL
00000001000107d0 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL>:
1000107d0: d10203e9    	sub	x9, sp, #0x80
1000107d4: f940078a    	ldr	x10, [x28, #0x8]
1000107d8: eb0a013f    	cmp	x9, x10
1000107dc: 54002849    	b.ls	0x100010ce4 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x514>
1000107e0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000107e4: 9100013f    	mov	sp, x9
1000107e8: b9400801    	ldr	w1, [x0, #0x8]
1000107ec: b9004be1    	str	w1, [sp, #0x48]
1000107f0: b940041e    	ldr	w30, [x0, #0x4]
1000107f4: 2a0103e2    	mov	w2, w1
1000107f8: 8b020f62    	add	x2, x27, x2, lsl #3
1000107fc: f90023e2    	str	x2, [sp, #0x40]
100010800: 340025de    	cbz	w30, 0x100010cb8 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4e8>
100010804: 2a1e03fe    	mov	w30, w30
100010808: 8b1e0f7e    	add	x30, x27, x30, lsl #3
10001080c: b94007de    	ldr	w30, [x30, #0x4]
100010810: 3400259e    	cbz	w30, 0x100010cc0 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4f0>
100010814: 2a1e03e0    	mov	w0, w30
100010818: 8b000f60    	add	x0, x27, x0, lsl #3
10001081c: d503201f    	nop
100010820: 91004009    	add	x9, x0, #0x10
100010824: 88dffd23    	ldar	w3, [x9]
100010828: 340001e3    	cbz	w3, 0x100010864 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
10001082c: 51000464    	sub	w4, w3, #0x1
100010830: 9100401e    	add	x30, x0, #0x10
100010834: 885f7fc5    	ldxr	w5, [x30]
100010838: 6b0300bf    	cmp	w5, w3
10001083c: 1a85008a    	csel	w10, w4, w5, eq
100010840: 8809ffca    	stlxr	w9, w10, [x30]
100010844: 35ffff89    	cbnz	w9, 0x100010834 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x64>
100010848: d5033bbf    	dmb	ish
10001084c: 1a9f17fe    	cset	w30, eq
100010850: 35001b9e    	cbnz	w30, 0x100010bc0 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3f0>
100010854: b9401389    	ldr	w9, [x28, #0x10]
100010858: 35fffe49    	cbnz	w9, 0x100010820 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x50>
10001085c: 9405ecad    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010860: 17fffff0    	b	0x100010820 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x50>
100010864: 52871543    	mov	w3, #0x38aa             ; =14506
100010868: 72a000a3    	movk	w3, #0x5, lsl #16
10001086c: 8b030f63    	add	x3, x27, x3, lsl #3
100010870: f9000fe3    	str	x3, [sp, #0x18]
100010874: 52871644    	mov	w4, #0x38b2             ; =14514
100010878: 72a000a4    	movk	w4, #0x5, lsl #16
10001087c: 8b040f64    	add	x4, x27, x4, lsl #3
100010880: f90013e4    	str	x4, [sp, #0x20]
100010884: 528022e5    	mov	w5, #0x117              ; =279
100010888: 72a00025    	movk	w5, #0x1, lsl #16
10001088c: b90057e5    	str	w5, [sp, #0x54]
100010890: 52988d86    	mov	w6, #0xc46c             ; =50284
100010894: 72a00026    	movk	w6, #0x1, lsl #16
100010898: 8b060f66    	add	x6, x27, x6, lsl #3
10001089c: f9001fe6    	str	x6, [sp, #0x38]
1000108a0: 52802667    	mov	w7, #0x133              ; =307
1000108a4: 72a00027    	movk	w7, #0x1, lsl #16
1000108a8: b90053e7    	str	w7, [sp, #0x50]
1000108ac: 528716a8    	mov	w8, #0x38b5             ; =14517
1000108b0: 72a000a8    	movk	w8, #0x5, lsl #16
1000108b4: b9004fe8    	str	w8, [sp, #0x4c]
1000108b8: 528716eb    	mov	w11, #0x38b7            ; =14519
1000108bc: 72a000ab    	movk	w11, #0x5, lsl #16
1000108c0: 8b0b0f6b    	add	x11, x27, x11, lsl #3
1000108c4: f9001beb    	str	x11, [sp, #0x30]
1000108c8: 52870e6c    	mov	w12, #0x3873            ; =14451
1000108cc: 72a000ac    	movk	w12, #0x5, lsl #16
1000108d0: 8b0c0f6c    	add	x12, x27, x12, lsl #3
1000108d4: f90017ec    	str	x12, [sp, #0x28]
1000108d8: b27003ed    	orr	x13, xzr, #0x10000
1000108dc: f9002fed    	str	x13, [sp, #0x58]
1000108e0: 321003ee    	orr	w14, wzr, #0x10000
1000108e4: b9006bee    	str	w14, [sp, #0x68]
1000108e8: 52850c0f    	mov	w15, #0x2860            ; =10336
1000108ec: 72a0040f    	movk	w15, #0x20, lsl #16
1000108f0: d2800110    	mov	x16, #0x8               ; =8
1000108f4: f2a00030    	movk	x16, #0x1, lsl #16
1000108f8: 2a1f03f1    	mov	w17, wzr
1000108fc: d503201f    	nop
100010900: 7108023f    	cmp	w17, #0x200
100010904: 54001c22    	b.hs	0x100010c88 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4b8>
100010908: a9438381    	ldp	x1, x0, [x28, #0x38]
10001090c: 8b100002    	add	x2, x0, x16
100010910: eb02003f    	cmp	x1, x2
100010914: 54001123    	b.lo	0x100010b38 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x368>
100010918: f9002382    	str	x2, [x28, #0x40]
10001091c: b900000f    	str	w15, [x0]
100010920: b900040e    	str	w14, [x0, #0x4]
100010924: 91002001    	add	x1, x0, #0x8
100010928: aa0d03e2    	mov	x2, x13
10001092c: b4000162    	cbz	x2, 0x100010958 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x188>
100010930: 361800a1    	tbz	w1, #0x3, 0x100010944 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x174>
100010934: d1002042    	sub	x2, x2, #0x8
100010938: f800843f    	str	xzr, [x1], #0x8
10001093c: 14000002    	b	0x100010944 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x174>
100010940: a8817c3f    	stp	xzr, xzr, [x1], #0x10
100010944: f1004042    	subs	x2, x2, #0x10
100010948: 54ffffca    	b.ge	0x100010940 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x170>
10001094c: 91004042    	add	x2, x2, #0x10
100010950: 36180042    	tbz	w2, #0x3, 0x100010958 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x188>
100010954: f800843f    	str	xzr, [x1], #0x8
100010958: d5033abf    	dmb	ishst
10001095c: cb1b0000    	sub	x0, x0, x27
100010960: d343fc00    	lsr	x0, x0, #3
100010964: 91026189    	add	x9, x12, #0x98
100010968: 889ffd20    	stlr	w0, [x9]
10001096c: b9400180    	ldr	w0, [x12]
100010970: 37080980    	tbnz	w0, #0x1, 0x100010aa0 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x2d0>
100010974: 11000633    	add	w19, w17, #0x1
100010978: 72000e3f    	tst	w17, #0xf
10001097c: 54000060    	b.eq	0x100010988 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1b8>
100010980: 2a1303f1    	mov	w17, w19
100010984: 17ffffdf    	b	0x100010900 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x130>
100010988: 91006169    	add	x9, x11, #0x18
10001098c: 88dffd20    	ldar	w0, [x9]
100010990: 6b08001f    	cmp	w0, w8
100010994: 54000180    	b.eq	0x1000109c4 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1f4>
100010998: 34000f20    	cbz	w0, 0x100010b7c <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3ac>
10001099c: 2a0003e0    	mov	w0, w0
1000109a0: 8b000f60    	add	x0, x27, x0, lsl #3
1000109a4: b9400001    	ldr	w1, [x0]
1000109a8: 53057c21    	lsr	w1, w1, #5
1000109ac: 6b07003f    	cmp	w1, w7
1000109b0: 540017e1    	b.ne	0x100010cac <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4dc>
1000109b4: 39401000    	ldrb	w0, [x0, #0x4]
1000109b8: 340000a0    	cbz	w0, 0x1000109cc <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1fc>
1000109bc: 2a1303f1    	mov	w17, w19
1000109c0: 17ffffd0    	b	0x100010900 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x130>
1000109c4: aa0603e0    	mov	x0, x6
1000109c8: 17fffffb    	b	0x1000109b4 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1e4>
1000109cc: b9402460    	ldr	w0, [x3, #0x24]
1000109d0: 340000e0    	cbz	w0, 0x1000109ec <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x21c>
1000109d4: 2a0003e0    	mov	w0, w0
1000109d8: 8b000f60    	add	x0, x27, x0, lsl #3
1000109dc: b9400000    	ldr	w0, [x0]
1000109e0: 53057c00    	lsr	w0, w0, #5
1000109e4: 6b05001f    	cmp	w0, w5
1000109e8: 54000740    	b.eq	0x100010ad0 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x300>
1000109ec: b9006ff3    	str	w19, [sp, #0x6c]
1000109f0: b90067f1    	str	w17, [sp, #0x64]
1000109f4: aa0303e0    	mov	x0, x3
1000109f8: aa0403e1    	mov	x1, x4
1000109fc: 320003e2    	orr	w2, wzr, #0x1
100010a00: 9401e41c    	bl	0x100089a70 <_GCImpl_collectWithoutAllocating_3JkbhdqsFF0bYizLQcyXX9>
100010a04: 340006a0    	cbz	w0, 0x100010ad8 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x308>
100010a08: f9400fe3    	ldr	x3, [sp, #0x18]
100010a0c: b9402460    	ldr	w0, [x3, #0x24]
100010a10: 340015e0    	cbz	w0, 0x100010ccc <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4fc>
100010a14: 2a0003e0    	mov	w0, w0
100010a18: 8b000f60    	add	x0, x27, x0, lsl #3
100010a1c: b940001e    	ldr	w30, [x0]
100010a20: 53057fc1    	lsr	w1, w30, #5
100010a24: 2a0103e1    	mov	w1, w1
100010a28: 8b010f61    	add	x1, x27, x1, lsl #3
100010a2c: b9400821    	ldr	w1, [x1, #0x8]
100010a30: 128021e2    	mov	w2, #-0x110             ; =-272
100010a34: 0b212041    	add	w1, w2, w1, uxth
100010a38: 7100303f    	cmp	w1, #0xc
100010a3c: 540014c2    	b.hs	0x100010cd4 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x504>
100010a40: 53057fde    	lsr	w30, w30, #5
100010a44: 2a1e03fe    	mov	w30, w30
100010a48: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100010a4c: f9404fde    	ldr	x30, [x30, #0x98]
100010a50: d0000361    	adrp	x1, 0x10007e000 <_CollectionPolicies_getPolicyIndex_bP8jZLnaZoEk4AVvxkBVYD+0x380>
100010a54: 911dc021    	add	x1, x1, #0x770
100010a58: cb1a0021    	sub	x1, x1, x26
100010a5c: eb0103df    	cmp	x30, x1
100010a60: 54000bc0    	b.eq	0x100010bd8 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x408>
100010a64: 8b1a03de    	add	x30, x30, x26
100010a68: d63f03c0    	blr	x30
100010a6c: b9406ff1    	ldr	w17, [sp, #0x6c]
100010a70: d2800110    	mov	x16, #0x8               ; =8
100010a74: f2a00030    	movk	x16, #0x1, lsl #16
100010a78: 52850c0f    	mov	w15, #0x2860            ; =10336
100010a7c: 72a0040f    	movk	w15, #0x20, lsl #16
100010a80: b9406bee    	ldr	w14, [sp, #0x68]
100010a84: f9402fed    	ldr	x13, [sp, #0x58]
100010a88: a942afec    	ldp	x12, x11, [sp, #0x28]
100010a8c: 29499fe8    	ldp	w8, w7, [sp, #0x4c]
100010a90: f9401fe6    	ldr	x6, [sp, #0x38]
100010a94: b94057e5    	ldr	w5, [sp, #0x54]
100010a98: a94193e3    	ldp	x3, x4, [sp, #0x18]
100010a9c: 17ffff99    	b	0x100010900 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x130>
100010aa0: aa0c03e1    	mov	x1, x12
100010aa4: 37000340    	tbnz	w0, #0x0, 0x100010b0c <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x33c>
100010aa8: b26db3e0    	mov	x0, #-0x80000           ; =-524288
100010aac: 8a000020    	and	x0, x1, x0
100010ab0: 91210002    	add	x2, x0, #0x840
100010ab4: cb020021    	sub	x1, x1, x2
100010ab8: b27d0be2    	orr	x2, xzr, #0x38
100010abc: 8b412441    	add	x1, x2, x1, lsr #9
100010ac0: 38e16802    	ldrsb	w2, [x0, x1]
100010ac4: 34fff582    	cbz	w2, 0x100010974 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1a4>
100010ac8: 3821681f    	strb	wzr, [x0, x1]
100010acc: 17ffffaa    	b	0x100010974 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1a4>
100010ad0: 2a1303f1    	mov	w17, w19
100010ad4: 17ffff8b    	b	0x100010900 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x130>
100010ad8: b9406ff1    	ldr	w17, [sp, #0x6c]
100010adc: d2800110    	mov	x16, #0x8               ; =8
100010ae0: f2a00030    	movk	x16, #0x1, lsl #16
100010ae4: 52850c0f    	mov	w15, #0x2860            ; =10336
100010ae8: 72a0040f    	movk	w15, #0x20, lsl #16
100010aec: b9406bee    	ldr	w14, [sp, #0x68]
100010af0: f9402fed    	ldr	x13, [sp, #0x58]
100010af4: a942afec    	ldp	x12, x11, [sp, #0x28]
100010af8: 29499fe8    	ldp	w8, w7, [sp, #0x4c]
100010afc: f9401fe6    	ldr	x6, [sp, #0x38]
100010b00: b94057e5    	ldr	w5, [sp, #0x54]
100010b04: a94193e3    	ldp	x3, x4, [sp, #0x18]
100010b08: 17ffff7e    	b	0x100010900 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x130>
100010b0c: f85f8020    	ldur	x0, [x1, #-0x8]
100010b10: aa0c03e2    	mov	x2, x12
100010b14: cb000020    	sub	x0, x1, x0
100010b18: 91026181    	add	x1, x12, #0x98
100010b1c: cb020021    	sub	x1, x1, x2
100010b20: b27c07e2    	orr	x2, xzr, #0x30
100010b24: 8b412441    	add	x1, x2, x1, lsr #9
100010b28: 38e16802    	ldrsb	w2, [x0, x1]
100010b2c: 34fff242    	cbz	w2, 0x100010974 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1a4>
100010b30: 3821681f    	strb	wzr, [x0, x1]
100010b34: 17ffff90    	b	0x100010974 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1a4>
100010b38: b90067f1    	str	w17, [sp, #0x64]
100010b3c: 92407de0    	and	x0, x15, #0xffffffff
100010b40: b9406be1    	ldr	w1, [sp, #0x68]
100010b44: 94026427    	bl	0x1000a9be0 <_GenScavengeAllocationSupport_slowNewArray_RH1677CN3I59XJMcBPW18F>
100010b48: b94067f1    	ldr	w17, [sp, #0x64]
100010b4c: d2800110    	mov	x16, #0x8               ; =8
100010b50: f2a00030    	movk	x16, #0x1, lsl #16
100010b54: 52850c0f    	mov	w15, #0x2860            ; =10336
100010b58: 72a0040f    	movk	w15, #0x20, lsl #16
100010b5c: b9406bee    	ldr	w14, [sp, #0x68]
100010b60: f9402fed    	ldr	x13, [sp, #0x58]
100010b64: a942afec    	ldp	x12, x11, [sp, #0x28]
100010b68: 29499fe8    	ldp	w8, w7, [sp, #0x4c]
100010b6c: f9401fe6    	ldr	x6, [sp, #0x38]
100010b70: b94057e5    	ldr	w5, [sp, #0x54]
100010b74: a94193e3    	ldp	x3, x4, [sp, #0x18]
100010b78: 17ffff79    	b	0x10001095c <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x18c>
100010b7c: b90067f1    	str	w17, [sp, #0x64]
100010b80: 94058548    	bl	0x1001720a0 <_ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
100010b84: b9404be1    	ldr	w1, [sp, #0x48]
100010b88: 34000a01    	cbz	w1, 0x100010cc8 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4f8>
100010b8c: f94023e2    	ldr	x2, [sp, #0x40]
100010b90: cb1b0000    	sub	x0, x0, x27
100010b94: d343fc00    	lsr	x0, x0, #3
100010b98: 91001049    	add	x9, x2, #0x4
100010b9c: 889ffd20    	stlr	w0, [x9]
100010ba0: b9400040    	ldr	w0, [x2]
100010ba4: 370801c0    	tbnz	w0, #0x1, 0x100010bdc <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x40c>
100010ba8: a9477bfd    	ldp	x29, x30, [sp, #0x70]
100010bac: 910203ff    	add	sp, sp, #0x80
100010bb0: b9401389    	ldr	w9, [x28, #0x10]
100010bb4: 7100013f    	cmp	w9, #0x0
100010bb8: 5400098d    	b.le	0x100010ce8 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x518>
100010bbc: d65f03c0    	ret
100010bc0: 7100047f    	cmp	w3, #0x1
100010bc4: 54ffe501    	b.ne	0x100010864 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
100010bc8: 91002009    	add	x9, x0, #0x8
100010bcc: 88dffd3e    	ldar	w30, [x9]
100010bd0: 34ffe4be    	cbz	w30, 0x100010864 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
100010bd4: 1400000d    	b	0x100010c08 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x438>
100010bd8: 9402eb72    	bl	0x1000cb9a0 <_OutOfMemoryUtil_heapSizeExceeded_tgNQlvtYRwAN6ucK4Y7ahE>
100010bdc: b26db3fd    	mov	x29, #-0x80000          ; =-524288
100010be0: f94023e2    	ldr	x2, [sp, #0x40]
100010be4: 8a1d005d    	and	x29, x2, x29
100010be8: 912103be    	add	x30, x29, #0x840
100010bec: cb1e005e    	sub	x30, x2, x30
100010bf0: b27d0be0    	orr	x0, xzr, #0x38
100010bf4: 8b5e241e    	add	x30, x0, x30, lsr #9
100010bf8: 38fe6ba0    	ldrsb	w0, [x29, x30]
100010bfc: 34fffd60    	cbz	w0, 0x100010ba8 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3d8>
100010c00: 383e6bbf    	strb	wzr, [x29, x30]
100010c04: 17ffffe9    	b	0x100010ba8 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3d8>
100010c08: 2a1e03fe    	mov	w30, w30
100010c0c: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100010c10: 910023c9    	add	x9, x30, #0x8
100010c14: 88dffd3e    	ldar	w30, [x9]
100010c18: 34ffe27e    	cbz	w30, 0x100010864 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
100010c1c: 2a1e03e0    	mov	w0, w30
100010c20: 8b000f60    	add	x0, x27, x0, lsl #3
100010c24: 91004009    	add	x9, x0, #0x10
100010c28: 88dffd3e    	ldar	w30, [x9]
100010c2c: 34ffe1de    	cbz	w30, 0x100010864 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
100010c30: 91004009    	add	x9, x0, #0x10
100010c34: 88dffd23    	ldar	w3, [x9]
100010c38: 121f787e    	and	w30, w3, #0xfffffffe
100010c3c: 91004004    	add	x4, x0, #0x10
100010c40: 885f7c85    	ldxr	w5, [x4]
100010c44: 6b0300bf    	cmp	w5, w3
100010c48: 1a8503ca    	csel	w10, w30, w5, eq
100010c4c: 8809fc8a    	stlxr	w9, w10, [x4]
100010c50: 35ffff89    	cbnz	w9, 0x100010c40 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x470>
100010c54: d5033bbf    	dmb	ish
100010c58: 1a9f17fe    	cset	w30, eq
100010c5c: 350000be    	cbnz	w30, 0x100010c70 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4a0>
100010c60: b9401389    	ldr	w9, [x28, #0x10]
100010c64: 35fffe69    	cbnz	w9, 0x100010c30 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x460>
100010c68: 9405ebaa    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010c6c: 17fffff1    	b	0x100010c30 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x460>
100010c70: b9400c00    	ldr	w0, [x0, #0xc]
100010c74: 34ffdf80    	cbz	w0, 0x100010864 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
100010c78: 2a0003e0    	mov	w0, w0
100010c7c: 8b000f60    	add	x0, x27, x0, lsl #3
100010c80: 9405e454    	bl	0x100189dd0 <_PlatformThreads_unpark_FJj4Yhv2Ja3jndtYAdmYl0>
100010c84: 17fffef8    	b	0x100010864 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
100010c88: a9477bfd    	ldp	x29, x30, [sp, #0x70]
100010c8c: 910203ff    	add	sp, sp, #0x80
100010c90: b9401389    	ldr	w9, [x28, #0x10]
100010c94: 7100013f    	cmp	w9, #0x0
100010c98: 5400028d    	b.le	0x100010ce8 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x518>
100010c9c: d65f03c0    	ret
100010ca0: 17ffffb9    	b	0x100010b84 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
100010ca4: 17ffffb8    	b	0x100010b84 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
100010ca8: 17ffffb7    	b	0x100010b84 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
100010cac: b90067f1    	str	w17, [sp, #0x64]
100010cb0: 94058334    	bl	0x100171980 <_ImplicitExceptions_createOptClassCastException_Ewnh8dGuUvIm5VBYxiDX2K>
100010cb4: 17ffffb4    	b	0x100010b84 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
100010cb8: 940584fa    	bl	0x1001720a0 <_ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
100010cbc: 17ffffb2    	b	0x100010b84 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
100010cc0: 940584f8    	bl	0x1001720a0 <_ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
100010cc4: 17ffffb0    	b	0x100010b84 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
100010cc8: 9405a036    	bl	0x100178da0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
100010ccc: 940584f5    	bl	0x1001720a0 <_ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
100010cd0: 17ffffad    	b	0x100010b84 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
100010cd4: b94067f1    	ldr	w17, [sp, #0x64]
100010cd8: 9405840e    	bl	0x100171d10 <_ImplicitExceptions_createOptIncompatibleClassChangeError_ExDFiMUY3I5T00lGxxhlnL>
100010cdc: 17ffffaa    	b	0x100010b84 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
100010ce0: 17ffffa9    	b	0x100010b84 <_Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
100010ce4: 1402987f    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010ce8: 1405eb8a    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010cec: cccccccc    	<unknown>

# svm: _Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC
0000000100010cf0 <_Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC>:
100010cf0: d100c3e9    	sub	x9, sp, #0x30
100010cf4: f940078a    	ldr	x10, [x28, #0x8]
100010cf8: eb0a013f    	cmp	x9, x10
100010cfc: 54000549    	b.ls	0x100010da4 <_Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0xb4>
100010d00: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010d04: 9100013f    	mov	sp, x9
100010d08: b9001fe1    	str	w1, [sp, #0x1c]
100010d0c: 7100043f    	cmp	w1, #0x1
100010d10: 5400018b    	b.lt	0x100010d40 <_Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0x50>
100010d14: aa0203e0    	mov	x0, x2
100010d18: 97fffd4e    	bl	0x100010250 <_Batch_batch16_7L76EVQlXq5pOOlPBaERnB>
100010d1c: b9401fe1    	ldr	w1, [sp, #0x1c]
100010d20: 7100443f    	cmp	w1, #0x11
100010d24: 540001ca    	b.ge	0x100010d5c <_Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0x6c>
100010d28: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100010d2c: 9100c3ff    	add	sp, sp, #0x30
100010d30: b9401389    	ldr	w9, [x28, #0x10]
100010d34: 7100013f    	cmp	w9, #0x0
100010d38: 5400038d    	b.le	0x100010da8 <_Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0xb8>
100010d3c: d65f03c0    	ret
100010d40: aa0203e0    	mov	x0, x2
100010d44: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100010d48: 9100c3ff    	add	sp, sp, #0x30
100010d4c: b9401389    	ldr	w9, [x28, #0x10]
100010d50: 7100013f    	cmp	w9, #0x0
100010d54: 540002ad    	b.le	0x100010da8 <_Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0xb8>
100010d58: d65f03c0    	ret
100010d5c: 321c03e9    	orr	w9, wzr, #0x10
100010d60: b9001be9    	str	w9, [sp, #0x18]
100010d64: d503201f    	nop
100010d68: d503201f    	nop
100010d6c: d503201f    	nop
100010d70: 97fffd38    	bl	0x100010250 <_Batch_batch16_7L76EVQlXq5pOOlPBaERnB>
100010d74: b9401be1    	ldr	w1, [sp, #0x18]
100010d78: 11004022    	add	w2, w1, #0x10
100010d7c: b9401fe3    	ldr	w3, [sp, #0x1c]
100010d80: 6b03005f    	cmp	w2, w3
100010d84: 54fffd2a    	b.ge	0x100010d28 <_Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0x38>
100010d88: b9401389    	ldr	w9, [x28, #0x10]
100010d8c: 34000069    	cbz	w9, 0x100010d98 <_Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0xa8>
100010d90: b9001be2    	str	w2, [sp, #0x18]
100010d94: 17fffff7    	b	0x100010d70 <_Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0x80>
100010d98: 9405eb5e    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010d9c: b9001be2    	str	w2, [sp, #0x18]
100010da0: 17fffff4    	b	0x100010d70 <_Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0x80>
100010da4: 1402984f    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010da8: 1405eb5a    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010dac: cccccccc    	<unknown>

# svm: _Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC
0000000100010db0 <_Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC>:
100010db0: d10103e9    	sub	x9, sp, #0x40
100010db4: f940078a    	ldr	x10, [x28, #0x8]
100010db8: eb0a013f    	cmp	x9, x10
100010dbc: 540008c9    	b.ls	0x100010ed4 <_Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x124>
100010dc0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010dc4: 9100013f    	mov	sp, x9
100010dc8: 7100043f    	cmp	w1, #0x1
100010dcc: 5400080b    	b.lt	0x100010ecc <_Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x11c>
100010dd0: b9002fe1    	str	w1, [sp, #0x2c]
100010dd4: 52871160    	mov	w0, #0x388b             ; =14475
100010dd8: 72a000a0    	movk	w0, #0x5, lsl #16
100010ddc: 8b000f60    	add	x0, x27, x0, lsl #3
100010de0: f9402403    	ldr	x3, [x0, #0x48]
100010de4: f90013e3    	str	x3, [sp, #0x20]
100010de8: 2a1f03e9    	mov	w9, wzr
100010dec: b9002be9    	str	w9, [sp, #0x28]
100010df0: aa0203e0    	mov	x0, x2
100010df4: aa0303fe    	mov	x30, x3
100010df8: d63f03c0    	blr	x30
100010dfc: f94013fe    	ldr	x30, [sp, #0x20]
100010e00: d63f03c0    	blr	x30
100010e04: f94013fe    	ldr	x30, [sp, #0x20]
100010e08: d63f03c0    	blr	x30
100010e0c: f94013fe    	ldr	x30, [sp, #0x20]
100010e10: d63f03c0    	blr	x30
100010e14: f94013fe    	ldr	x30, [sp, #0x20]
100010e18: d63f03c0    	blr	x30
100010e1c: f94013fe    	ldr	x30, [sp, #0x20]
100010e20: d63f03c0    	blr	x30
100010e24: f94013fe    	ldr	x30, [sp, #0x20]
100010e28: d63f03c0    	blr	x30
100010e2c: f94013fe    	ldr	x30, [sp, #0x20]
100010e30: d63f03c0    	blr	x30
100010e34: f94013fe    	ldr	x30, [sp, #0x20]
100010e38: d63f03c0    	blr	x30
100010e3c: f94013fe    	ldr	x30, [sp, #0x20]
100010e40: d63f03c0    	blr	x30
100010e44: f94013fe    	ldr	x30, [sp, #0x20]
100010e48: d63f03c0    	blr	x30
100010e4c: f94013fe    	ldr	x30, [sp, #0x20]
100010e50: d63f03c0    	blr	x30
100010e54: f94013fe    	ldr	x30, [sp, #0x20]
100010e58: d63f03c0    	blr	x30
100010e5c: f94013fe    	ldr	x30, [sp, #0x20]
100010e60: d63f03c0    	blr	x30
100010e64: f94013fe    	ldr	x30, [sp, #0x20]
100010e68: d63f03c0    	blr	x30
100010e6c: f94013fe    	ldr	x30, [sp, #0x20]
100010e70: d63f03c0    	blr	x30
100010e74: b9402bfd    	ldr	w29, [sp, #0x28]
100010e78: 110043a1    	add	w1, w29, #0x10
100010e7c: b9402fe2    	ldr	w2, [sp, #0x2c]
100010e80: 6b02003f    	cmp	w1, w2
100010e84: 5400018a    	b.ge	0x100010eb4 <_Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x104>
100010e88: b9401389    	ldr	w9, [x28, #0x10]
100010e8c: 340000a9    	cbz	w9, 0x100010ea0 <_Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0xf0>
100010e90: b9002be1    	str	w1, [sp, #0x28]
100010e94: f94013e3    	ldr	x3, [sp, #0x20]
100010e98: aa0003e2    	mov	x2, x0
100010e9c: 17ffffd5    	b	0x100010df0 <_Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x40>
100010ea0: 9405eb1c    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010ea4: b9002be1    	str	w1, [sp, #0x28]
100010ea8: f94013e3    	ldr	x3, [sp, #0x20]
100010eac: aa0003e2    	mov	x2, x0
100010eb0: 17ffffd0    	b	0x100010df0 <_Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x40>
100010eb4: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010eb8: 910103ff    	add	sp, sp, #0x40
100010ebc: b9401389    	ldr	w9, [x28, #0x10]
100010ec0: 7100013f    	cmp	w9, #0x0
100010ec4: 540000ad    	b.le	0x100010ed8 <_Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x128>
100010ec8: d65f03c0    	ret
100010ecc: aa0203e0    	mov	x0, x2
100010ed0: 17fffff9    	b	0x100010eb4 <_Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x104>
100010ed4: 14029803    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010ed8: 1405eb0e    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010edc: cccccccc    	<unknown>

# svm: _Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K
0000000100010ee0 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K>:
100010ee0: d10143e9    	sub	x9, sp, #0x50
100010ee4: f940078a    	ldr	x10, [x28, #0x8]
100010ee8: eb0a013f    	cmp	x9, x10
100010eec: 54000bc9    	b.ls	0x100011064 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x184>
100010ef0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010ef4: 9100013f    	mov	sp, x9
100010ef8: b90033e1    	str	w1, [sp, #0x30]
100010efc: 7100043f    	cmp	w1, #0x1
100010f00: 320003e0    	orr	w0, wzr, #0x1
100010f04: 1a80d020    	csel	w0, w1, w0, le
100010f08: 7100041f    	cmp	w0, #0x1
100010f0c: 54000a6b    	b.lt	0x100011058 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x178>
100010f10: aa0203e0    	mov	x0, x2
100010f14: 940003eb    	bl	0x100011ec0 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010f18: 320003e2    	orr	w2, wzr, #0x1
100010f1c: b94033e1    	ldr	w1, [sp, #0x30]
100010f20: 52800083    	mov	w3, #0x4                ; =4
100010f24: 72b00003    	movk	w3, #0x8000, lsl #16
100010f28: 320103e4    	orr	w4, wzr, #0x80000000
100010f2c: 51001025    	sub	w5, w1, #0x4
100010f30: 6b03003f    	cmp	w1, w3
100010f34: 1a85b083    	csel	w3, w4, w5, lt
100010f38: b9002be3    	str	w3, [sp, #0x28]
100010f3c: 11000c64    	add	w4, w3, #0x3
100010f40: b90027e4    	str	w4, [sp, #0x24]
100010f44: 2a0203e1    	mov	w1, w2
100010f48: d503201f    	nop
100010f4c: d503201f    	nop
100010f50: 6b03003f    	cmp	w1, w3
100010f54: 5400062a    	b.ge	0x100011018 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x138>
100010f58: 4b010085    	sub	w5, w4, w1
100010f5c: 53027ca6    	lsr	w6, w5, #2
100010f60: 121e74a5    	and	w5, w5, #0xfffffffc
100010f64: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100010f68: 321203e6    	orr	w6, wzr, #0x4000
100010f6c: 1a8630a5    	csel	w5, w5, w6, lo
100010f70: 710004bf    	cmp	w5, #0x1
100010f74: 540004ab    	b.lt	0x100011008 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x128>
100010f78: b90037e1    	str	w1, [sp, #0x34]
100010f7c: b9003fe5    	str	w5, [sp, #0x3c]
100010f80: 2a1f03e2    	mov	w2, wzr
100010f84: d503201f    	nop
100010f88: d503201f    	nop
100010f8c: d503201f    	nop
100010f90: 0b010047    	add	w7, w2, w1
100010f94: b90023e7    	str	w7, [sp, #0x20]
100010f98: b9002fe2    	str	w2, [sp, #0x2c]
100010f9c: 940003c9    	bl	0x100011ec0 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010fa0: b94023e7    	ldr	w7, [sp, #0x20]
100010fa4: 110004e1    	add	w1, w7, #0x1
100010fa8: b9003be1    	str	w1, [sp, #0x38]
100010fac: 940003c5    	bl	0x100011ec0 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010fb0: b94023e7    	ldr	w7, [sp, #0x20]
100010fb4: 110008e1    	add	w1, w7, #0x2
100010fb8: b9003be1    	str	w1, [sp, #0x38]
100010fbc: 940003c1    	bl	0x100011ec0 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010fc0: b94023e7    	ldr	w7, [sp, #0x20]
100010fc4: 11000ce1    	add	w1, w7, #0x3
100010fc8: b90023e1    	str	w1, [sp, #0x20]
100010fcc: 940003bd    	bl	0x100011ec0 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010fd0: b9402fe2    	ldr	w2, [sp, #0x2c]
100010fd4: 11001042    	add	w2, w2, #0x4
100010fd8: b9403fe5    	ldr	w5, [sp, #0x3c]
100010fdc: 6b05005f    	cmp	w2, w5
100010fe0: b94037e1    	ldr	w1, [sp, #0x34]
100010fe4: 54fffd6b    	b.lt	0x100010f90 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0xb0>
100010fe8: 0b010041    	add	w1, w2, w1
100010fec: b9401389    	ldr	w9, [x28, #0x10]
100010ff0: 34000069    	cbz	w9, 0x100010ffc <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x11c>
100010ff4: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100010ff8: 17ffffd6    	b	0x100010f50 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x70>
100010ffc: 9405eac5    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011000: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100011004: 17ffffd3    	b	0x100010f50 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x70>
100011008: b9401389    	ldr	w9, [x28, #0x10]
10001100c: 35fffa29    	cbnz	w9, 0x100010f50 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x70>
100011010: 9405eac0    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011014: 17ffffcf    	b	0x100010f50 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x70>
100011018: 2a0103e2    	mov	w2, w1
10001101c: 14000006    	b	0x100011034 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x154>
100011020: b9002fe2    	str	w2, [sp, #0x2c]
100011024: 940003a7    	bl	0x100011ec0 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100011028: b9402fe2    	ldr	w2, [sp, #0x2c]
10001102c: 1100045d    	add	w29, w2, #0x1
100011030: 2a1d03e2    	mov	w2, w29
100011034: b94033e1    	ldr	w1, [sp, #0x30]
100011038: 6b01005f    	cmp	w2, w1
10001103c: 54ffff2b    	b.lt	0x100011020 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x140>
100011040: a9447bfd    	ldp	x29, x30, [sp, #0x40]
100011044: 910143ff    	add	sp, sp, #0x50
100011048: b9401389    	ldr	w9, [x28, #0x10]
10001104c: 7100013f    	cmp	w9, #0x0
100011050: 540000cd    	b.le	0x100011068 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x188>
100011054: d65f03c0    	ret
100011058: aa0203e0    	mov	x0, x2
10001105c: 2a1f03e2    	mov	w2, wzr
100011060: 17ffffb0    	b	0x100010f20 <_Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x40>
100011064: 1402979f    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011068: 1405eaaa    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001106c: cccccccc    	<unknown>

# svm: _Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL
0000000100011070 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL>:
100011070: d10143e9    	sub	x9, sp, #0x50
100011074: f940078a    	ldr	x10, [x28, #0x8]
100011078: eb0a013f    	cmp	x9, x10
10001107c: 54000bc9    	b.ls	0x1000111f4 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x184>
100011080: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011084: 9100013f    	mov	sp, x9
100011088: b90033e1    	str	w1, [sp, #0x30]
10001108c: 7100043f    	cmp	w1, #0x1
100011090: 320003e0    	orr	w0, wzr, #0x1
100011094: 1a80d020    	csel	w0, w1, w0, le
100011098: 7100041f    	cmp	w0, #0x1
10001109c: 54000a6b    	b.lt	0x1000111e8 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x178>
1000110a0: aa0203e0    	mov	x0, x2
1000110a4: 940005e3    	bl	0x100012830 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
1000110a8: 320003e2    	orr	w2, wzr, #0x1
1000110ac: b94033e1    	ldr	w1, [sp, #0x30]
1000110b0: 52800083    	mov	w3, #0x4                ; =4
1000110b4: 72b00003    	movk	w3, #0x8000, lsl #16
1000110b8: 320103e4    	orr	w4, wzr, #0x80000000
1000110bc: 51001025    	sub	w5, w1, #0x4
1000110c0: 6b03003f    	cmp	w1, w3
1000110c4: 1a85b083    	csel	w3, w4, w5, lt
1000110c8: b9002be3    	str	w3, [sp, #0x28]
1000110cc: 11000c64    	add	w4, w3, #0x3
1000110d0: b90027e4    	str	w4, [sp, #0x24]
1000110d4: 2a0203e1    	mov	w1, w2
1000110d8: d503201f    	nop
1000110dc: d503201f    	nop
1000110e0: 6b03003f    	cmp	w1, w3
1000110e4: 5400062a    	b.ge	0x1000111a8 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x138>
1000110e8: 4b010085    	sub	w5, w4, w1
1000110ec: 53027ca6    	lsr	w6, w5, #2
1000110f0: 121e74a5    	and	w5, w5, #0xfffffffc
1000110f4: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
1000110f8: 321203e6    	orr	w6, wzr, #0x4000
1000110fc: 1a8630a5    	csel	w5, w5, w6, lo
100011100: 710004bf    	cmp	w5, #0x1
100011104: 540004ab    	b.lt	0x100011198 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x128>
100011108: b90037e1    	str	w1, [sp, #0x34]
10001110c: b9003fe5    	str	w5, [sp, #0x3c]
100011110: 2a1f03e2    	mov	w2, wzr
100011114: d503201f    	nop
100011118: d503201f    	nop
10001111c: d503201f    	nop
100011120: 0b010047    	add	w7, w2, w1
100011124: b90023e7    	str	w7, [sp, #0x20]
100011128: b9002fe2    	str	w2, [sp, #0x2c]
10001112c: 940005c1    	bl	0x100012830 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
100011130: b94023e7    	ldr	w7, [sp, #0x20]
100011134: 110004e1    	add	w1, w7, #0x1
100011138: b9003be1    	str	w1, [sp, #0x38]
10001113c: 940005bd    	bl	0x100012830 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
100011140: b94023e7    	ldr	w7, [sp, #0x20]
100011144: 110008e1    	add	w1, w7, #0x2
100011148: b9003be1    	str	w1, [sp, #0x38]
10001114c: 940005b9    	bl	0x100012830 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
100011150: b94023e7    	ldr	w7, [sp, #0x20]
100011154: 11000ce1    	add	w1, w7, #0x3
100011158: b90023e1    	str	w1, [sp, #0x20]
10001115c: 940005b5    	bl	0x100012830 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
100011160: b9402fe2    	ldr	w2, [sp, #0x2c]
100011164: 11001042    	add	w2, w2, #0x4
100011168: b9403fe5    	ldr	w5, [sp, #0x3c]
10001116c: 6b05005f    	cmp	w2, w5
100011170: b94037e1    	ldr	w1, [sp, #0x34]
100011174: 54fffd6b    	b.lt	0x100011120 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0xb0>
100011178: 0b010041    	add	w1, w2, w1
10001117c: b9401389    	ldr	w9, [x28, #0x10]
100011180: 34000069    	cbz	w9, 0x10001118c <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x11c>
100011184: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100011188: 17ffffd6    	b	0x1000110e0 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x70>
10001118c: 9405ea61    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011190: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100011194: 17ffffd3    	b	0x1000110e0 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x70>
100011198: b9401389    	ldr	w9, [x28, #0x10]
10001119c: 35fffa29    	cbnz	w9, 0x1000110e0 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x70>
1000111a0: 9405ea5c    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000111a4: 17ffffcf    	b	0x1000110e0 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x70>
1000111a8: 2a0103e2    	mov	w2, w1
1000111ac: 14000006    	b	0x1000111c4 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x154>
1000111b0: b9002fe2    	str	w2, [sp, #0x2c]
1000111b4: 9400059f    	bl	0x100012830 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
1000111b8: b9402fe2    	ldr	w2, [sp, #0x2c]
1000111bc: 1100045d    	add	w29, w2, #0x1
1000111c0: 2a1d03e2    	mov	w2, w29
1000111c4: b94033e1    	ldr	w1, [sp, #0x30]
1000111c8: 6b01005f    	cmp	w2, w1
1000111cc: 54ffff2b    	b.lt	0x1000111b0 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x140>
1000111d0: a9447bfd    	ldp	x29, x30, [sp, #0x40]
1000111d4: 910143ff    	add	sp, sp, #0x50
1000111d8: b9401389    	ldr	w9, [x28, #0x10]
1000111dc: 7100013f    	cmp	w9, #0x0
1000111e0: 540000cd    	b.le	0x1000111f8 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x188>
1000111e4: d65f03c0    	ret
1000111e8: aa0203e0    	mov	x0, x2
1000111ec: 2a1f03e2    	mov	w2, wzr
1000111f0: 17ffffb0    	b	0x1000110b0 <_Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x40>
1000111f4: 1402973b    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000111f8: 1405ea46    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000111fc: cccccccc    	<unknown>

# svm: _Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60
0000000100011200 <_Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60>:
100011200: d100c3e9    	sub	x9, sp, #0x30
100011204: f940078a    	ldr	x10, [x28, #0x8]
100011208: eb0a013f    	cmp	x9, x10
10001120c: 54000589    	b.ls	0x1000112bc <_Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0xbc>
100011210: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011214: 9100013f    	mov	sp, x9
100011218: d28cc1a0    	mov	x0, #0x660d             ; =26125
10001121c: f2a00320    	movk	x0, #0x19, lsl #16
100011220: d29e6bfd    	mov	x29, #0xf35f            ; =62303
100011224: f2a78ddd    	movk	x29, #0x3c6e, lsl #16
100011228: 2a1f03fe    	mov	w30, wzr
10001122c: d503201f    	nop
100011230: 6b0103df    	cmp	w30, w1
100011234: 5400036a    	b.ge	0x1000112a0 <_Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0xa0>
100011238: 4b1e0023    	sub	w3, w1, w30
10001123c: 7140047f    	cmp	w3, #0x1, lsl #12       ; =0x1000
100011240: 321403e4    	orr	w4, wzr, #0x1000
100011244: 1a849063    	csel	w3, w3, w4, ls
100011248: 7100007f    	cmp	w3, #0x0
10001124c: 2a1f03e4    	mov	w4, wzr
100011250: 1a84a064    	csel	w4, w3, w4, ge
100011254: 7100047f    	cmp	w3, #0x1
100011258: 5400014b    	b.lt	0x100011280 <_Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0x80>
10001125c: 92800003    	mov	x3, #-0x1               ; =-1
100011260: 8b244063    	add	x3, x3, w4, uxtw
100011264: aa1f03e5    	mov	x5, xzr
100011268: d503201f    	nop
10001126c: d503201f    	nop
100011270: 9b007442    	madd	x2, x2, x0, x29
100011274: 910004a5    	add	x5, x5, #0x1
100011278: eb05007f    	cmp	x3, x5
10001127c: 54ffffaa    	b.ge	0x100011270 <_Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0x70>
100011280: 0b0403c3    	add	w3, w30, w4
100011284: b9401389    	ldr	w9, [x28, #0x10]
100011288: 34000069    	cbz	w9, 0x100011294 <_Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0x94>
10001128c: 2a0303fe    	mov	w30, w3
100011290: 17ffffe8    	b	0x100011230 <_Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0x30>
100011294: 9405ea1f    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011298: 2a0303fe    	mov	w30, w3
10001129c: 17ffffe5    	b	0x100011230 <_Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0x30>
1000112a0: aa0203e0    	mov	x0, x2
1000112a4: a9427bfd    	ldp	x29, x30, [sp, #0x20]
1000112a8: 9100c3ff    	add	sp, sp, #0x30
1000112ac: b9401389    	ldr	w9, [x28, #0x10]
1000112b0: 7100013f    	cmp	w9, #0x0
1000112b4: 5400006d    	b.le	0x1000112c0 <_Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0xc0>
1000112b8: d65f03c0    	ret
1000112bc: 14029709    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000112c0: 1405ea14    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000112c4: cccccccc    	<unknown>
1000112c8: cccccccc    	<unknown>
1000112cc: cccccccc    	<unknown>

# svm: _Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737
00000001000112d0 <_Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737>:
1000112d0: d100c3e9    	sub	x9, sp, #0x30
1000112d4: f940078a    	ldr	x10, [x28, #0x8]
1000112d8: eb0a013f    	cmp	x9, x10
1000112dc: 54000549    	b.ls	0x100011384 <_Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0xb4>
1000112e0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000112e4: 9100013f    	mov	sp, x9
1000112e8: b9001fe1    	str	w1, [sp, #0x1c]
1000112ec: 7100043f    	cmp	w1, #0x1
1000112f0: 5400018b    	b.lt	0x100011320 <_Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0x50>
1000112f4: aa0203e0    	mov	x0, x2
1000112f8: 94000382    	bl	0x100012100 <_Floor_javaBatch64_pgMJB9JHBI7okdXsdyhwU1>
1000112fc: b9401fe1    	ldr	w1, [sp, #0x1c]
100011300: 7101043f    	cmp	w1, #0x41
100011304: 540001ca    	b.ge	0x10001133c <_Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0x6c>
100011308: a9427bfd    	ldp	x29, x30, [sp, #0x20]
10001130c: 9100c3ff    	add	sp, sp, #0x30
100011310: b9401389    	ldr	w9, [x28, #0x10]
100011314: 7100013f    	cmp	w9, #0x0
100011318: 5400038d    	b.le	0x100011388 <_Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0xb8>
10001131c: d65f03c0    	ret
100011320: aa0203e0    	mov	x0, x2
100011324: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100011328: 9100c3ff    	add	sp, sp, #0x30
10001132c: b9401389    	ldr	w9, [x28, #0x10]
100011330: 7100013f    	cmp	w9, #0x0
100011334: 540002ad    	b.le	0x100011388 <_Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0xb8>
100011338: d65f03c0    	ret
10001133c: 321a03e9    	orr	w9, wzr, #0x40
100011340: b9001be9    	str	w9, [sp, #0x18]
100011344: d503201f    	nop
100011348: d503201f    	nop
10001134c: d503201f    	nop
100011350: 9400036c    	bl	0x100012100 <_Floor_javaBatch64_pgMJB9JHBI7okdXsdyhwU1>
100011354: b9401be1    	ldr	w1, [sp, #0x18]
100011358: 11010022    	add	w2, w1, #0x40
10001135c: b9401fe3    	ldr	w3, [sp, #0x1c]
100011360: 6b03005f    	cmp	w2, w3
100011364: 54fffd2a    	b.ge	0x100011308 <_Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0x38>
100011368: b9401389    	ldr	w9, [x28, #0x10]
10001136c: 34000069    	cbz	w9, 0x100011378 <_Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0xa8>
100011370: b9001be2    	str	w2, [sp, #0x18]
100011374: 17fffff7    	b	0x100011350 <_Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0x80>
100011378: 9405e9e6    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001137c: b9001be2    	str	w2, [sp, #0x18]
100011380: 17fffff4    	b	0x100011350 <_Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0x80>
100011384: 140296d7    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011388: 1405e9e2    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001138c: cccccccc    	<unknown>

# svm: _Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I
0000000100011390 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I>:
100011390: d10143e9    	sub	x9, sp, #0x50
100011394: f940078a    	ldr	x10, [x28, #0x8]
100011398: eb0a013f    	cmp	x9, x10
10001139c: 54000f09    	b.ls	0x10001157c <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x1ec>
1000113a0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000113a4: 9100013f    	mov	sp, x9
1000113a8: b90033e1    	str	w1, [sp, #0x30]
1000113ac: 52871160    	mov	w0, #0x388b             ; =14475
1000113b0: 72a000a0    	movk	w0, #0x5, lsl #16
1000113b4: 8b000f60    	add	x0, x27, x0, lsl #3
1000113b8: f9402403    	ldr	x3, [x0, #0x48]
1000113bc: f9000fe3    	str	x3, [sp, #0x18]
1000113c0: 7100043f    	cmp	w1, #0x1
1000113c4: 320003e0    	orr	w0, wzr, #0x1
1000113c8: 1a80d020    	csel	w0, w1, w0, le
1000113cc: 7100041f    	cmp	w0, #0x1
1000113d0: 54000d0b    	b.lt	0x100011570 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x1e0>
1000113d4: aa0203e0    	mov	x0, x2
1000113d8: aa0303fe    	mov	x30, x3
1000113dc: d63f03c0    	blr	x30
1000113e0: 320003fe    	orr	w30, wzr, #0x1
1000113e4: b94033e1    	ldr	w1, [sp, #0x30]
1000113e8: 52800082    	mov	w2, #0x4                ; =4
1000113ec: 72b00002    	movk	w2, #0x8000, lsl #16
1000113f0: 320103e3    	orr	w3, wzr, #0x80000000
1000113f4: 51001024    	sub	w4, w1, #0x4
1000113f8: 6b02003f    	cmp	w1, w2
1000113fc: 1a84b062    	csel	w2, w3, w4, lt
100011400: b9002be2    	str	w2, [sp, #0x28]
100011404: 11000c43    	add	w3, w2, #0x3
100011408: b90027e3    	str	w3, [sp, #0x24]
10001140c: f9400fe4    	ldr	x4, [sp, #0x18]
100011410: 2a1e03e1    	mov	w1, w30
100011414: d503201f    	nop
100011418: d503201f    	nop
10001141c: d503201f    	nop
100011420: 6b02003f    	cmp	w1, w2
100011424: 5400078a    	b.ge	0x100011514 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x184>
100011428: 4b01007e    	sub	w30, w3, w1
10001142c: 53027fc6    	lsr	w6, w30, #2
100011430: 121e77de    	and	w30, w30, #0xfffffffc
100011434: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100011438: 321203e6    	orr	w6, wzr, #0x4000
10001143c: 1a8633c6    	csel	w6, w30, w6, lo
100011440: 710004df    	cmp	w6, #0x1
100011444: 5400060b    	b.lt	0x100011504 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x174>
100011448: b90037e1    	str	w1, [sp, #0x34]
10001144c: b9003fe6    	str	w6, [sp, #0x3c]
100011450: 2a1f03e2    	mov	w2, wzr
100011454: 14000008    	b	0x100011474 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0xe4>
100011458: d503201f    	nop
10001145c: d503201f    	nop
100011460: d503201f    	nop
100011464: d503201f    	nop
100011468: d503201f    	nop
10001146c: d503201f    	nop
100011470: f9400fe4    	ldr	x4, [sp, #0x18]
100011474: 0b010048    	add	w8, w2, w1
100011478: b90023e8    	str	w8, [sp, #0x20]
10001147c: aa0403fe    	mov	x30, x4
100011480: b9002fe2    	str	w2, [sp, #0x2c]
100011484: d63f03c0    	blr	x30
100011488: b94023e8    	ldr	w8, [sp, #0x20]
10001148c: 11000501    	add	w1, w8, #0x1
100011490: f9400ffe    	ldr	x30, [sp, #0x18]
100011494: b9003be1    	str	w1, [sp, #0x38]
100011498: d63f03c0    	blr	x30
10001149c: b94023e8    	ldr	w8, [sp, #0x20]
1000114a0: 11000901    	add	w1, w8, #0x2
1000114a4: f9400ffe    	ldr	x30, [sp, #0x18]
1000114a8: b9003be1    	str	w1, [sp, #0x38]
1000114ac: d63f03c0    	blr	x30
1000114b0: b94023e8    	ldr	w8, [sp, #0x20]
1000114b4: 11000d01    	add	w1, w8, #0x3
1000114b8: f9400ffe    	ldr	x30, [sp, #0x18]
1000114bc: b90023e1    	str	w1, [sp, #0x20]
1000114c0: d63f03c0    	blr	x30
1000114c4: b9402fe2    	ldr	w2, [sp, #0x2c]
1000114c8: 11001042    	add	w2, w2, #0x4
1000114cc: b9403fe6    	ldr	w6, [sp, #0x3c]
1000114d0: 6b06005f    	cmp	w2, w6
1000114d4: b94037e1    	ldr	w1, [sp, #0x34]
1000114d8: 54fffccb    	b.lt	0x100011470 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0xe0>
1000114dc: 0b010041    	add	w1, w2, w1
1000114e0: b9401389    	ldr	w9, [x28, #0x10]
1000114e4: 34000089    	cbz	w9, 0x1000114f4 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x164>
1000114e8: 29448be3    	ldp	w3, w2, [sp, #0x24]
1000114ec: f9400fe4    	ldr	x4, [sp, #0x18]
1000114f0: 17ffffcc    	b	0x100011420 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x90>
1000114f4: 9405e987    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000114f8: 29448be3    	ldp	w3, w2, [sp, #0x24]
1000114fc: f9400fe4    	ldr	x4, [sp, #0x18]
100011500: 17ffffc8    	b	0x100011420 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x90>
100011504: b9401389    	ldr	w9, [x28, #0x10]
100011508: 35fff8c9    	cbnz	w9, 0x100011420 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x90>
10001150c: 9405e981    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011510: 17ffffc4    	b	0x100011420 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x90>
100011514: 2a0103e2    	mov	w2, w1
100011518: aa0403e3    	mov	x3, x4
10001151c: 1400000c    	b	0x10001154c <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x1bc>
100011520: d503201f    	nop
100011524: d503201f    	nop
100011528: d503201f    	nop
10001152c: d503201f    	nop
100011530: b9002fe2    	str	w2, [sp, #0x2c]
100011534: aa0303fe    	mov	x30, x3
100011538: d63f03c0    	blr	x30
10001153c: b9402fe2    	ldr	w2, [sp, #0x2c]
100011540: 1100045d    	add	w29, w2, #0x1
100011544: 2a1d03e2    	mov	w2, w29
100011548: f9400fe3    	ldr	x3, [sp, #0x18]
10001154c: b94033e1    	ldr	w1, [sp, #0x30]
100011550: 6b01005f    	cmp	w2, w1
100011554: 54fffeeb    	b.lt	0x100011530 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x1a0>
100011558: a9447bfd    	ldp	x29, x30, [sp, #0x40]
10001155c: 910143ff    	add	sp, sp, #0x50
100011560: b9401389    	ldr	w9, [x28, #0x10]
100011564: 7100013f    	cmp	w9, #0x0
100011568: 540000cd    	b.le	0x100011580 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x1f0>
10001156c: d65f03c0    	ret
100011570: 2a1f03fe    	mov	w30, wzr
100011574: aa0203e0    	mov	x0, x2
100011578: 17ffff9c    	b	0x1000113e8 <_Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x58>
10001157c: 14029659    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011580: 1405e964    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011584: cccccccc    	<unknown>
100011588: cccccccc    	<unknown>
10001158c: cccccccc    	<unknown>

# svm: _Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4
0000000100011590 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4>:
100011590: d10143e9    	sub	x9, sp, #0x50
100011594: f940078a    	ldr	x10, [x28, #0x8]
100011598: eb0a013f    	cmp	x9, x10
10001159c: 54000bc9    	b.ls	0x100011714 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x184>
1000115a0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000115a4: 9100013f    	mov	sp, x9
1000115a8: b90033e1    	str	w1, [sp, #0x30]
1000115ac: 7100043f    	cmp	w1, #0x1
1000115b0: 320003e0    	orr	w0, wzr, #0x1
1000115b4: 1a80d020    	csel	w0, w1, w0, le
1000115b8: 7100041f    	cmp	w0, #0x1
1000115bc: 54000a6b    	b.lt	0x100011708 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x178>
1000115c0: aa0203e0    	mov	x0, x2
1000115c4: 94001353    	bl	0x100016310 <_Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
1000115c8: 320003e2    	orr	w2, wzr, #0x1
1000115cc: b94033e1    	ldr	w1, [sp, #0x30]
1000115d0: 52800083    	mov	w3, #0x4                ; =4
1000115d4: 72b00003    	movk	w3, #0x8000, lsl #16
1000115d8: 320103e4    	orr	w4, wzr, #0x80000000
1000115dc: 51001025    	sub	w5, w1, #0x4
1000115e0: 6b03003f    	cmp	w1, w3
1000115e4: 1a85b083    	csel	w3, w4, w5, lt
1000115e8: b9002be3    	str	w3, [sp, #0x28]
1000115ec: 11000c64    	add	w4, w3, #0x3
1000115f0: b90027e4    	str	w4, [sp, #0x24]
1000115f4: 2a0203e1    	mov	w1, w2
1000115f8: d503201f    	nop
1000115fc: d503201f    	nop
100011600: 6b03003f    	cmp	w1, w3
100011604: 5400062a    	b.ge	0x1000116c8 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x138>
100011608: 4b010085    	sub	w5, w4, w1
10001160c: 53027ca6    	lsr	w6, w5, #2
100011610: 121e74a5    	and	w5, w5, #0xfffffffc
100011614: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100011618: 321203e6    	orr	w6, wzr, #0x4000
10001161c: 1a8630a5    	csel	w5, w5, w6, lo
100011620: 710004bf    	cmp	w5, #0x1
100011624: 540004ab    	b.lt	0x1000116b8 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x128>
100011628: b90037e1    	str	w1, [sp, #0x34]
10001162c: b9003fe5    	str	w5, [sp, #0x3c]
100011630: 2a1f03e2    	mov	w2, wzr
100011634: d503201f    	nop
100011638: d503201f    	nop
10001163c: d503201f    	nop
100011640: 0b010047    	add	w7, w2, w1
100011644: b90023e7    	str	w7, [sp, #0x20]
100011648: b9002fe2    	str	w2, [sp, #0x2c]
10001164c: 94001331    	bl	0x100016310 <_Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
100011650: b94023e7    	ldr	w7, [sp, #0x20]
100011654: 110004e1    	add	w1, w7, #0x1
100011658: b9003be1    	str	w1, [sp, #0x38]
10001165c: 9400132d    	bl	0x100016310 <_Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
100011660: b94023e7    	ldr	w7, [sp, #0x20]
100011664: 110008e1    	add	w1, w7, #0x2
100011668: b9003be1    	str	w1, [sp, #0x38]
10001166c: 94001329    	bl	0x100016310 <_Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
100011670: b94023e7    	ldr	w7, [sp, #0x20]
100011674: 11000ce1    	add	w1, w7, #0x3
100011678: b90023e1    	str	w1, [sp, #0x20]
10001167c: 94001325    	bl	0x100016310 <_Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
100011680: b9402fe2    	ldr	w2, [sp, #0x2c]
100011684: 11001042    	add	w2, w2, #0x4
100011688: b9403fe5    	ldr	w5, [sp, #0x3c]
10001168c: 6b05005f    	cmp	w2, w5
100011690: b94037e1    	ldr	w1, [sp, #0x34]
100011694: 54fffd6b    	b.lt	0x100011640 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0xb0>
100011698: 0b010041    	add	w1, w2, w1
10001169c: b9401389    	ldr	w9, [x28, #0x10]
1000116a0: 34000069    	cbz	w9, 0x1000116ac <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x11c>
1000116a4: 29448fe4    	ldp	w4, w3, [sp, #0x24]
1000116a8: 17ffffd6    	b	0x100011600 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x70>
1000116ac: 9405e919    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000116b0: 29448fe4    	ldp	w4, w3, [sp, #0x24]
1000116b4: 17ffffd3    	b	0x100011600 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x70>
1000116b8: b9401389    	ldr	w9, [x28, #0x10]
1000116bc: 35fffa29    	cbnz	w9, 0x100011600 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x70>
1000116c0: 9405e914    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000116c4: 17ffffcf    	b	0x100011600 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x70>
1000116c8: 2a0103e2    	mov	w2, w1
1000116cc: 14000006    	b	0x1000116e4 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x154>
1000116d0: b9002fe2    	str	w2, [sp, #0x2c]
1000116d4: 9400130f    	bl	0x100016310 <_Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
1000116d8: b9402fe2    	ldr	w2, [sp, #0x2c]
1000116dc: 1100045d    	add	w29, w2, #0x1
1000116e0: 2a1d03e2    	mov	w2, w29
1000116e4: b94033e1    	ldr	w1, [sp, #0x30]
1000116e8: 6b01005f    	cmp	w2, w1
1000116ec: 54ffff2b    	b.lt	0x1000116d0 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x140>
1000116f0: a9447bfd    	ldp	x29, x30, [sp, #0x40]
1000116f4: 910143ff    	add	sp, sp, #0x50
1000116f8: b9401389    	ldr	w9, [x28, #0x10]
1000116fc: 7100013f    	cmp	w9, #0x0
100011700: 540000cd    	b.le	0x100011718 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x188>
100011704: d65f03c0    	ret
100011708: aa0203e0    	mov	x0, x2
10001170c: 2a1f03e2    	mov	w2, wzr
100011710: 17ffffb0    	b	0x1000115d0 <_Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x40>
100011714: 140295f3    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011718: 1405e8fe    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001171c: cccccccc    	<unknown>

# svm: _Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K
0000000100011720 <_Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K>:
100011720: d100c3e9    	sub	x9, sp, #0x30
100011724: f940078a    	ldr	x10, [x28, #0x8]
100011728: eb0a013f    	cmp	x9, x10
10001172c: 54000549    	b.ls	0x1000117d4 <_Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0xb4>
100011730: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011734: 9100013f    	mov	sp, x9
100011738: b9001fe1    	str	w1, [sp, #0x1c]
10001173c: 7100043f    	cmp	w1, #0x1
100011740: 5400018b    	b.lt	0x100011770 <_Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0x50>
100011744: aa0203e0    	mov	x0, x2
100011748: 97fffaca    	bl	0x100010270 <_Batch_batch256_DscONwqsjcJ5uLVvo6oz1L>
10001174c: b9401fe1    	ldr	w1, [sp, #0x1c]
100011750: 7104043f    	cmp	w1, #0x101
100011754: 540001ca    	b.ge	0x10001178c <_Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0x6c>
100011758: a9427bfd    	ldp	x29, x30, [sp, #0x20]
10001175c: 9100c3ff    	add	sp, sp, #0x30
100011760: b9401389    	ldr	w9, [x28, #0x10]
100011764: 7100013f    	cmp	w9, #0x0
100011768: 5400038d    	b.le	0x1000117d8 <_Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0xb8>
10001176c: d65f03c0    	ret
100011770: aa0203e0    	mov	x0, x2
100011774: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100011778: 9100c3ff    	add	sp, sp, #0x30
10001177c: b9401389    	ldr	w9, [x28, #0x10]
100011780: 7100013f    	cmp	w9, #0x0
100011784: 540002ad    	b.le	0x1000117d8 <_Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0xb8>
100011788: d65f03c0    	ret
10001178c: 321803e9    	orr	w9, wzr, #0x100
100011790: b9001be9    	str	w9, [sp, #0x18]
100011794: d503201f    	nop
100011798: d503201f    	nop
10001179c: d503201f    	nop
1000117a0: 97fffab4    	bl	0x100010270 <_Batch_batch256_DscONwqsjcJ5uLVvo6oz1L>
1000117a4: b9401be1    	ldr	w1, [sp, #0x18]
1000117a8: 11040022    	add	w2, w1, #0x100
1000117ac: b9401fe3    	ldr	w3, [sp, #0x1c]
1000117b0: 6b03005f    	cmp	w2, w3
1000117b4: 54fffd2a    	b.ge	0x100011758 <_Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0x38>
1000117b8: b9401389    	ldr	w9, [x28, #0x10]
1000117bc: 34000069    	cbz	w9, 0x1000117c8 <_Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0xa8>
1000117c0: b9001be2    	str	w2, [sp, #0x18]
1000117c4: 17fffff7    	b	0x1000117a0 <_Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0x80>
1000117c8: 9405e8d2    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000117cc: b9001be2    	str	w2, [sp, #0x18]
1000117d0: 17fffff4    	b	0x1000117a0 <_Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0x80>
1000117d4: 140295c3    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000117d8: 1405e8ce    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000117dc: cccccccc    	<unknown>

# svm: _Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9
00000001000117e0 <_Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9>:
1000117e0: d100c3e9    	sub	x9, sp, #0x30
1000117e4: f940078a    	ldr	x10, [x28, #0x8]
1000117e8: eb0a013f    	cmp	x9, x10
1000117ec: 54000549    	b.ls	0x100011894 <_Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0xb4>
1000117f0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000117f4: 9100013f    	mov	sp, x9
1000117f8: b9001fe1    	str	w1, [sp, #0x1c]
1000117fc: 7100043f    	cmp	w1, #0x1
100011800: 5400018b    	b.lt	0x100011830 <_Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0x50>
100011804: aa0203e0    	mov	x0, x2
100011808: 940001de    	bl	0x100011f80 <_Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0>
10001180c: b9401fe1    	ldr	w1, [sp, #0x1c]
100011810: 7104043f    	cmp	w1, #0x101
100011814: 540001ca    	b.ge	0x10001184c <_Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0x6c>
100011818: a9427bfd    	ldp	x29, x30, [sp, #0x20]
10001181c: 9100c3ff    	add	sp, sp, #0x30
100011820: b9401389    	ldr	w9, [x28, #0x10]
100011824: 7100013f    	cmp	w9, #0x0
100011828: 5400038d    	b.le	0x100011898 <_Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0xb8>
10001182c: d65f03c0    	ret
100011830: aa0203e0    	mov	x0, x2
100011834: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100011838: 9100c3ff    	add	sp, sp, #0x30
10001183c: b9401389    	ldr	w9, [x28, #0x10]
100011840: 7100013f    	cmp	w9, #0x0
100011844: 540002ad    	b.le	0x100011898 <_Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0xb8>
100011848: d65f03c0    	ret
10001184c: 321803e9    	orr	w9, wzr, #0x100
100011850: b9001be9    	str	w9, [sp, #0x18]
100011854: d503201f    	nop
100011858: d503201f    	nop
10001185c: d503201f    	nop
100011860: 940001c8    	bl	0x100011f80 <_Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0>
100011864: b9401be1    	ldr	w1, [sp, #0x18]
100011868: 11040022    	add	w2, w1, #0x100
10001186c: b9401fe3    	ldr	w3, [sp, #0x1c]
100011870: 6b03005f    	cmp	w2, w3
100011874: 54fffd2a    	b.ge	0x100011818 <_Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0x38>
100011878: b9401389    	ldr	w9, [x28, #0x10]
10001187c: 34000069    	cbz	w9, 0x100011888 <_Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0xa8>
100011880: b9001be2    	str	w2, [sp, #0x18]
100011884: 17fffff7    	b	0x100011860 <_Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0x80>
100011888: 9405e8a2    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001188c: b9001be2    	str	w2, [sp, #0x18]
100011890: 17fffff4    	b	0x100011860 <_Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0x80>
100011894: 14029593    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011898: 1405e89e    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001189c: cccccccc    	<unknown>

# svm: _Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK
00000001000118a0 <_Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK>:
1000118a0: d100c3e9    	sub	x9, sp, #0x30
1000118a4: f940078a    	ldr	x10, [x28, #0x8]
1000118a8: eb0a013f    	cmp	x9, x10
1000118ac: 54000549    	b.ls	0x100011954 <_Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0xb4>
1000118b0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000118b4: 9100013f    	mov	sp, x9
1000118b8: b9001fe1    	str	w1, [sp, #0x1c]
1000118bc: 7100043f    	cmp	w1, #0x1
1000118c0: 5400018b    	b.lt	0x1000118f0 <_Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0x50>
1000118c4: aa0203e0    	mov	x0, x2
1000118c8: 97fffa72    	bl	0x100010290 <_Batch_batch4_38OjNnaTu60qxbWBQgs1KG>
1000118cc: b9401fe1    	ldr	w1, [sp, #0x1c]
1000118d0: 7100143f    	cmp	w1, #0x5
1000118d4: 540001ca    	b.ge	0x10001190c <_Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0x6c>
1000118d8: a9427bfd    	ldp	x29, x30, [sp, #0x20]
1000118dc: 9100c3ff    	add	sp, sp, #0x30
1000118e0: b9401389    	ldr	w9, [x28, #0x10]
1000118e4: 7100013f    	cmp	w9, #0x0
1000118e8: 5400038d    	b.le	0x100011958 <_Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0xb8>
1000118ec: d65f03c0    	ret
1000118f0: aa0203e0    	mov	x0, x2
1000118f4: a9427bfd    	ldp	x29, x30, [sp, #0x20]
1000118f8: 9100c3ff    	add	sp, sp, #0x30
1000118fc: b9401389    	ldr	w9, [x28, #0x10]
100011900: 7100013f    	cmp	w9, #0x0
100011904: 540002ad    	b.le	0x100011958 <_Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0xb8>
100011908: d65f03c0    	ret
10001190c: 321e03e9    	orr	w9, wzr, #0x4
100011910: b9001be9    	str	w9, [sp, #0x18]
100011914: d503201f    	nop
100011918: d503201f    	nop
10001191c: d503201f    	nop
100011920: 97fffa5c    	bl	0x100010290 <_Batch_batch4_38OjNnaTu60qxbWBQgs1KG>
100011924: b9401be1    	ldr	w1, [sp, #0x18]
100011928: 11001022    	add	w2, w1, #0x4
10001192c: b9401fe3    	ldr	w3, [sp, #0x1c]
100011930: 6b03005f    	cmp	w2, w3
100011934: 54fffd2a    	b.ge	0x1000118d8 <_Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0x38>
100011938: b9401389    	ldr	w9, [x28, #0x10]
10001193c: 34000069    	cbz	w9, 0x100011948 <_Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0xa8>
100011940: b9001be2    	str	w2, [sp, #0x18]
100011944: 17fffff7    	b	0x100011920 <_Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0x80>
100011948: 9405e872    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001194c: b9001be2    	str	w2, [sp, #0x18]
100011950: 17fffff4    	b	0x100011920 <_Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0x80>
100011954: 14029563    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011958: 1405e86e    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001195c: cccccccc    	<unknown>

# svm: _Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB
0000000100011960 <_Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB>:
100011960: d100c3e9    	sub	x9, sp, #0x30
100011964: f940078a    	ldr	x10, [x28, #0x8]
100011968: eb0a013f    	cmp	x9, x10
10001196c: 54000549    	b.ls	0x100011a14 <_Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0xb4>
100011970: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011974: 9100013f    	mov	sp, x9
100011978: b9001fe1    	str	w1, [sp, #0x1c]
10001197c: 7100043f    	cmp	w1, #0x1
100011980: 5400018b    	b.lt	0x1000119b0 <_Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0x50>
100011984: aa0203e0    	mov	x0, x2
100011988: 97fffa4a    	bl	0x1000102b0 <_Batch_batch64_xRVK4bfrby1czdEDQHBCYB>
10001198c: b9401fe1    	ldr	w1, [sp, #0x1c]
100011990: 7101043f    	cmp	w1, #0x41
100011994: 540001ca    	b.ge	0x1000119cc <_Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0x6c>
100011998: a9427bfd    	ldp	x29, x30, [sp, #0x20]
10001199c: 9100c3ff    	add	sp, sp, #0x30
1000119a0: b9401389    	ldr	w9, [x28, #0x10]
1000119a4: 7100013f    	cmp	w9, #0x0
1000119a8: 5400038d    	b.le	0x100011a18 <_Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0xb8>
1000119ac: d65f03c0    	ret
1000119b0: aa0203e0    	mov	x0, x2
1000119b4: a9427bfd    	ldp	x29, x30, [sp, #0x20]
1000119b8: 9100c3ff    	add	sp, sp, #0x30
1000119bc: b9401389    	ldr	w9, [x28, #0x10]
1000119c0: 7100013f    	cmp	w9, #0x0
1000119c4: 540002ad    	b.le	0x100011a18 <_Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0xb8>
1000119c8: d65f03c0    	ret
1000119cc: 321a03e9    	orr	w9, wzr, #0x40
1000119d0: b9001be9    	str	w9, [sp, #0x18]
1000119d4: d503201f    	nop
1000119d8: d503201f    	nop
1000119dc: d503201f    	nop
1000119e0: 97fffa34    	bl	0x1000102b0 <_Batch_batch64_xRVK4bfrby1czdEDQHBCYB>
1000119e4: b9401be1    	ldr	w1, [sp, #0x18]
1000119e8: 11010022    	add	w2, w1, #0x40
1000119ec: b9401fe3    	ldr	w3, [sp, #0x1c]
1000119f0: 6b03005f    	cmp	w2, w3
1000119f4: 54fffd2a    	b.ge	0x100011998 <_Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0x38>
1000119f8: b9401389    	ldr	w9, [x28, #0x10]
1000119fc: 34000069    	cbz	w9, 0x100011a08 <_Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0xa8>
100011a00: b9001be2    	str	w2, [sp, #0x18]
100011a04: 17fffff7    	b	0x1000119e0 <_Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0x80>
100011a08: 9405e842    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011a0c: b9001be2    	str	w2, [sp, #0x18]
100011a10: 17fffff4    	b	0x1000119e0 <_Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0x80>
100011a14: 14029533    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011a18: 1405e83e    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011a1c: cccccccc    	<unknown>

# svm: _Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3
0000000100011a20 <_Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3>:
100011a20: d100c3e9    	sub	x9, sp, #0x30
100011a24: f940078a    	ldr	x10, [x28, #0x8]
100011a28: eb0a013f    	cmp	x9, x10
100011a2c: 54000549    	b.ls	0x100011ad4 <_Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0xb4>
100011a30: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011a34: 9100013f    	mov	sp, x9
100011a38: b9001fe1    	str	w1, [sp, #0x1c]
100011a3c: 7100043f    	cmp	w1, #0x1
100011a40: 5400018b    	b.lt	0x100011a70 <_Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0x50>
100011a44: aa0203e0    	mov	x0, x2
100011a48: 9400019e    	bl	0x1000120c0 <_Floor_javaBatch4_f1KINYClC5BmmtfwLbXF1F>
100011a4c: b9401fe1    	ldr	w1, [sp, #0x1c]
100011a50: 7100143f    	cmp	w1, #0x5
100011a54: 540001ca    	b.ge	0x100011a8c <_Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0x6c>
100011a58: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100011a5c: 9100c3ff    	add	sp, sp, #0x30
100011a60: b9401389    	ldr	w9, [x28, #0x10]
100011a64: 7100013f    	cmp	w9, #0x0
100011a68: 5400038d    	b.le	0x100011ad8 <_Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0xb8>
100011a6c: d65f03c0    	ret
100011a70: aa0203e0    	mov	x0, x2
100011a74: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100011a78: 9100c3ff    	add	sp, sp, #0x30
100011a7c: b9401389    	ldr	w9, [x28, #0x10]
100011a80: 7100013f    	cmp	w9, #0x0
100011a84: 540002ad    	b.le	0x100011ad8 <_Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0xb8>
100011a88: d65f03c0    	ret
100011a8c: 321e03e9    	orr	w9, wzr, #0x4
100011a90: b9001be9    	str	w9, [sp, #0x18]
100011a94: d503201f    	nop
100011a98: d503201f    	nop
100011a9c: d503201f    	nop
100011aa0: 94000188    	bl	0x1000120c0 <_Floor_javaBatch4_f1KINYClC5BmmtfwLbXF1F>
100011aa4: b9401be1    	ldr	w1, [sp, #0x18]
100011aa8: 11001022    	add	w2, w1, #0x4
100011aac: b9401fe3    	ldr	w3, [sp, #0x1c]
100011ab0: 6b03005f    	cmp	w2, w3
100011ab4: 54fffd2a    	b.ge	0x100011a58 <_Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0x38>
100011ab8: b9401389    	ldr	w9, [x28, #0x10]
100011abc: 34000069    	cbz	w9, 0x100011ac8 <_Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0xa8>
100011ac0: b9001be2    	str	w2, [sp, #0x18]
100011ac4: 17fffff7    	b	0x100011aa0 <_Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0x80>
100011ac8: 9405e812    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011acc: b9001be2    	str	w2, [sp, #0x18]
100011ad0: 17fffff4    	b	0x100011aa0 <_Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0x80>
100011ad4: 14029503    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011ad8: 1405e80e    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011adc: cccccccc    	<unknown>

# svm: _Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH
0000000100011ae0 <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH>:
100011ae0: d101c3e9    	sub	x9, sp, #0x70
100011ae4: f940078a    	ldr	x10, [x28, #0x8]
100011ae8: eb0a013f    	cmp	x9, x10
100011aec: 54000869    	b.ls	0x100011bf8 <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x118>
100011af0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011af4: 9100013f    	mov	sp, x9
100011af8: b90037e1    	str	w1, [sp, #0x34]
100011afc: f00020e3    	adrp	x3, 0x100430000 <___data>
100011b00: f9431063    	ldr	x3, [x3, #0x620]
100011b04: f90017e3    	str	x3, [sp, #0x28]
100011b08: aa0203e0    	mov	x0, x2
100011b0c: 2a1f03e2    	mov	w2, wzr
100011b10: 14000027    	b	0x100011bac <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0xcc>
100011b14: d503201f    	nop
100011b18: d503201f    	nop
100011b1c: d503201f    	nop
100011b20: b90033e2    	str	w2, [sp, #0x30]
100011b24: 9100e3ed    	add	x13, sp, #0x38
100011b28: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
100011b2c: f9400f9e    	ldr	x30, [x28, #0x18]
100011b30: f9000dbe    	str	x30, [x13, #0x18]
100011b34: f9000f8d    	str	x13, [x28, #0x18]
100011b38: aa0303fe    	mov	x30, x3
100011b3c: 10000029    	adr	x9, 0x100011b40 <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x60>
100011b40: f90005a9    	str	x9, [x13, #0x8]
100011b44: 910003e9    	mov	x9, sp
100011b48: f90009a9    	str	x9, [x13, #0x10]
100011b4c: 320007e9    	orr	w9, wzr, #0x3
100011b50: 9100538a    	add	x10, x28, #0x14
100011b54: 889ffd49    	stlr	w9, [x10]
100011b58: d63f03c0    	blr	x30
100011b5c: 320007e1    	orr	w1, wzr, #0x3
100011b60: 320003e2    	orr	w2, wzr, #0x1
100011b64: 91005383    	add	x3, x28, #0x14
100011b68: 885f7c64    	ldxr	w4, [x3]
100011b6c: 6b01009f    	cmp	w4, w1
100011b70: 1a84004a    	csel	w10, w2, w4, eq
100011b74: 8809fc6a    	stlxr	w9, w10, [x3]
100011b78: 35ffff89    	cbnz	w9, 0x100011b68 <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x88>
100011b7c: d5033bbf    	dmb	ish
100011b80: 1a9f17e1    	cset	w1, eq
100011b84: 340001a1    	cbz	w1, 0x100011bb8 <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0xd8>
100011b88: f9400f81    	ldr	x1, [x28, #0x18]
100011b8c: f9400c21    	ldr	x1, [x1, #0x18]
100011b90: f9000f81    	str	x1, [x28, #0x18]
100011b94: b9402f81    	ldr	w1, [x28, #0x2c]
100011b98: 350001c1    	cbnz	w1, 0x100011bd0 <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0xf0>
100011b9c: b94033e2    	ldr	w2, [sp, #0x30]
100011ba0: 11000442    	add	w2, w2, #0x1
100011ba4: f94017e3    	ldr	x3, [sp, #0x28]
100011ba8: b94037e1    	ldr	w1, [sp, #0x34]
100011bac: 6b01005f    	cmp	w2, w1
100011bb0: 54fffb8b    	b.lt	0x100011b20 <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x40>
100011bb4: 1400000b    	b	0x100011be0 <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x100>
100011bb8: f90013e0    	str	x0, [sp, #0x20]
100011bbc: 320003e0    	orr	w0, wzr, #0x1
100011bc0: 320003e1    	orr	w1, wzr, #0x1
100011bc4: 9405ea0f    	bl	0x10018c400 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
100011bc8: f94013e0    	ldr	x0, [sp, #0x20]
100011bcc: 17fffff4    	b	0x100011b9c <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0xbc>
100011bd0: f90013e0    	str	x0, [sp, #0x20]
100011bd4: 9405e99f    	bl	0x10018c250 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
100011bd8: f94013e0    	ldr	x0, [sp, #0x20]
100011bdc: 17fffff0    	b	0x100011b9c <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0xbc>
100011be0: a9467bfd    	ldp	x29, x30, [sp, #0x60]
100011be4: 9101c3ff    	add	sp, sp, #0x70
100011be8: b9401389    	ldr	w9, [x28, #0x10]
100011bec: 7100013f    	cmp	w9, #0x0
100011bf0: 5400006d    	b.le	0x100011bfc <_Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x11c>
100011bf4: d65f03c0    	ret
100011bf8: 140294ba    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011bfc: 1405e7c5    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>

# svm: _Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB
0000000100011c00 <_Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB>:
100011c00: d100c3e9    	sub	x9, sp, #0x30
100011c04: f940078a    	ldr	x10, [x28, #0x8]
100011c08: eb0a013f    	cmp	x9, x10
100011c0c: 54000549    	b.ls	0x100011cb4 <_Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0xb4>
100011c10: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011c14: 9100013f    	mov	sp, x9
100011c18: b9001fe1    	str	w1, [sp, #0x1c]
100011c1c: 7100043f    	cmp	w1, #0x1
100011c20: 5400018b    	b.lt	0x100011c50 <_Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0x50>
100011c24: aa0203e0    	mov	x0, x2
100011c28: 940000ae    	bl	0x100011ee0 <_Floor_javaBatch16_y3MnyKV1Au2DVE8WunT2mC>
100011c2c: b9401fe1    	ldr	w1, [sp, #0x1c]
100011c30: 7100443f    	cmp	w1, #0x11
100011c34: 540001ca    	b.ge	0x100011c6c <_Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0x6c>
100011c38: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100011c3c: 9100c3ff    	add	sp, sp, #0x30
100011c40: b9401389    	ldr	w9, [x28, #0x10]
100011c44: 7100013f    	cmp	w9, #0x0
100011c48: 5400038d    	b.le	0x100011cb8 <_Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0xb8>
100011c4c: d65f03c0    	ret
100011c50: aa0203e0    	mov	x0, x2
100011c54: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100011c58: 9100c3ff    	add	sp, sp, #0x30
100011c5c: b9401389    	ldr	w9, [x28, #0x10]
100011c60: 7100013f    	cmp	w9, #0x0
100011c64: 540002ad    	b.le	0x100011cb8 <_Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0xb8>
100011c68: d65f03c0    	ret
100011c6c: 321c03e9    	orr	w9, wzr, #0x10
100011c70: b9001be9    	str	w9, [sp, #0x18]
100011c74: d503201f    	nop
100011c78: d503201f    	nop
100011c7c: d503201f    	nop
100011c80: 94000098    	bl	0x100011ee0 <_Floor_javaBatch16_y3MnyKV1Au2DVE8WunT2mC>
100011c84: b9401be1    	ldr	w1, [sp, #0x18]
100011c88: 11004022    	add	w2, w1, #0x10
100011c8c: b9401fe3    	ldr	w3, [sp, #0x1c]
100011c90: 6b03005f    	cmp	w2, w3
100011c94: 54fffd2a    	b.ge	0x100011c38 <_Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0x38>
100011c98: b9401389    	ldr	w9, [x28, #0x10]
100011c9c: 34000069    	cbz	w9, 0x100011ca8 <_Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0xa8>
100011ca0: b9001be2    	str	w2, [sp, #0x18]
100011ca4: 17fffff7    	b	0x100011c80 <_Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0x80>
100011ca8: 9405e79a    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011cac: b9001be2    	str	w2, [sp, #0x18]
100011cb0: 17fffff4    	b	0x100011c80 <_Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0x80>
100011cb4: 1402948b    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011cb8: 1405e796    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011cbc: cccccccc    	<unknown>

# svm: _Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L
0000000100011cc0 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L>:
100011cc0: d10143e9    	sub	x9, sp, #0x50
100011cc4: f940078a    	ldr	x10, [x28, #0x8]
100011cc8: eb0a013f    	cmp	x9, x10
100011ccc: 54000f09    	b.ls	0x100011eac <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x1ec>
100011cd0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011cd4: 9100013f    	mov	sp, x9
100011cd8: b90033e1    	str	w1, [sp, #0x30]
100011cdc: 52871160    	mov	w0, #0x388b             ; =14475
100011ce0: 72a000a0    	movk	w0, #0x5, lsl #16
100011ce4: 8b000f60    	add	x0, x27, x0, lsl #3
100011ce8: f9402403    	ldr	x3, [x0, #0x48]
100011cec: f9000fe3    	str	x3, [sp, #0x18]
100011cf0: 7100043f    	cmp	w1, #0x1
100011cf4: 320003e0    	orr	w0, wzr, #0x1
100011cf8: 1a80d020    	csel	w0, w1, w0, le
100011cfc: 7100041f    	cmp	w0, #0x1
100011d00: 54000d0b    	b.lt	0x100011ea0 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x1e0>
100011d04: aa0203e0    	mov	x0, x2
100011d08: aa0303fe    	mov	x30, x3
100011d0c: d63f03c0    	blr	x30
100011d10: 320003fe    	orr	w30, wzr, #0x1
100011d14: b94033e1    	ldr	w1, [sp, #0x30]
100011d18: 52800082    	mov	w2, #0x4                ; =4
100011d1c: 72b00002    	movk	w2, #0x8000, lsl #16
100011d20: 320103e3    	orr	w3, wzr, #0x80000000
100011d24: 51001024    	sub	w4, w1, #0x4
100011d28: 6b02003f    	cmp	w1, w2
100011d2c: 1a84b062    	csel	w2, w3, w4, lt
100011d30: b9002be2    	str	w2, [sp, #0x28]
100011d34: 11000c43    	add	w3, w2, #0x3
100011d38: b90027e3    	str	w3, [sp, #0x24]
100011d3c: f9400fe4    	ldr	x4, [sp, #0x18]
100011d40: 2a1e03e1    	mov	w1, w30
100011d44: d503201f    	nop
100011d48: d503201f    	nop
100011d4c: d503201f    	nop
100011d50: 6b02003f    	cmp	w1, w2
100011d54: 5400078a    	b.ge	0x100011e44 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x184>
100011d58: 4b01007e    	sub	w30, w3, w1
100011d5c: 53027fc6    	lsr	w6, w30, #2
100011d60: 121e77de    	and	w30, w30, #0xfffffffc
100011d64: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100011d68: 321203e6    	orr	w6, wzr, #0x4000
100011d6c: 1a8633c6    	csel	w6, w30, w6, lo
100011d70: 710004df    	cmp	w6, #0x1
100011d74: 5400060b    	b.lt	0x100011e34 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x174>
100011d78: b90037e1    	str	w1, [sp, #0x34]
100011d7c: b9003fe6    	str	w6, [sp, #0x3c]
100011d80: 2a1f03e2    	mov	w2, wzr
100011d84: 14000008    	b	0x100011da4 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0xe4>
100011d88: d503201f    	nop
100011d8c: d503201f    	nop
100011d90: d503201f    	nop
100011d94: d503201f    	nop
100011d98: d503201f    	nop
100011d9c: d503201f    	nop
100011da0: f9400fe4    	ldr	x4, [sp, #0x18]
100011da4: 0b010048    	add	w8, w2, w1
100011da8: b90023e8    	str	w8, [sp, #0x20]
100011dac: aa0403fe    	mov	x30, x4
100011db0: b9002fe2    	str	w2, [sp, #0x2c]
100011db4: d63f03c0    	blr	x30
100011db8: b94023e8    	ldr	w8, [sp, #0x20]
100011dbc: 11000501    	add	w1, w8, #0x1
100011dc0: f9400ffe    	ldr	x30, [sp, #0x18]
100011dc4: b9003be1    	str	w1, [sp, #0x38]
100011dc8: d63f03c0    	blr	x30
100011dcc: b94023e8    	ldr	w8, [sp, #0x20]
100011dd0: 11000901    	add	w1, w8, #0x2
100011dd4: f9400ffe    	ldr	x30, [sp, #0x18]
100011dd8: b9003be1    	str	w1, [sp, #0x38]
100011ddc: d63f03c0    	blr	x30
100011de0: b94023e8    	ldr	w8, [sp, #0x20]
100011de4: 11000d01    	add	w1, w8, #0x3
100011de8: f9400ffe    	ldr	x30, [sp, #0x18]
100011dec: b90023e1    	str	w1, [sp, #0x20]
100011df0: d63f03c0    	blr	x30
100011df4: b9402fe2    	ldr	w2, [sp, #0x2c]
100011df8: 11001042    	add	w2, w2, #0x4
100011dfc: b9403fe6    	ldr	w6, [sp, #0x3c]
100011e00: 6b06005f    	cmp	w2, w6
100011e04: b94037e1    	ldr	w1, [sp, #0x34]
100011e08: 54fffccb    	b.lt	0x100011da0 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0xe0>
100011e0c: 0b010041    	add	w1, w2, w1
100011e10: b9401389    	ldr	w9, [x28, #0x10]
100011e14: 34000089    	cbz	w9, 0x100011e24 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x164>
100011e18: 29448be3    	ldp	w3, w2, [sp, #0x24]
100011e1c: f9400fe4    	ldr	x4, [sp, #0x18]
100011e20: 17ffffcc    	b	0x100011d50 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x90>
100011e24: 9405e73b    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011e28: 29448be3    	ldp	w3, w2, [sp, #0x24]
100011e2c: f9400fe4    	ldr	x4, [sp, #0x18]
100011e30: 17ffffc8    	b	0x100011d50 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x90>
100011e34: b9401389    	ldr	w9, [x28, #0x10]
100011e38: 35fff8c9    	cbnz	w9, 0x100011d50 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x90>
100011e3c: 9405e735    	bl	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011e40: 17ffffc4    	b	0x100011d50 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x90>
100011e44: 2a0103e2    	mov	w2, w1
100011e48: aa0403e3    	mov	x3, x4
100011e4c: 1400000c    	b	0x100011e7c <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x1bc>
100011e50: d503201f    	nop
100011e54: d503201f    	nop
100011e58: d503201f    	nop
100011e5c: d503201f    	nop
100011e60: b9002fe2    	str	w2, [sp, #0x2c]
100011e64: aa0303fe    	mov	x30, x3
100011e68: d63f03c0    	blr	x30
100011e6c: b9402fe2    	ldr	w2, [sp, #0x2c]
100011e70: 1100045d    	add	w29, w2, #0x1
100011e74: 2a1d03e2    	mov	w2, w29
100011e78: f9400fe3    	ldr	x3, [sp, #0x18]
100011e7c: b94033e1    	ldr	w1, [sp, #0x30]
100011e80: 6b01005f    	cmp	w2, w1
100011e84: 54fffeeb    	b.lt	0x100011e60 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x1a0>
100011e88: a9447bfd    	ldp	x29, x30, [sp, #0x40]
100011e8c: 910143ff    	add	sp, sp, #0x50
100011e90: b9401389    	ldr	w9, [x28, #0x10]
100011e94: 7100013f    	cmp	w9, #0x0
100011e98: 540000cd    	b.le	0x100011eb0 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x1f0>
100011e9c: d65f03c0    	ret
100011ea0: 2a1f03fe    	mov	w30, wzr
100011ea4: aa0203e0    	mov	x0, x2
100011ea8: 17ffff9c    	b	0x100011d18 <_Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x58>
100011eac: 1402940d    	b	0x1000b6ee0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011eb0: 1405e718    	b	0x10018bb10 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011eb4: cccccccc    	<unknown>
100011eb8: cccccccc    	<unknown>
100011ebc: cccccccc    	<unknown>

# svm: _Floor_javaBatch16_y3MnyKV1Au2DVE8WunT2mC
0000000100011ee0 <_Floor_javaBatch16_y3MnyKV1Au2DVE8WunT2mC>:
100011ee0: d100c3ff    	sub	sp, sp, #0x30
100011ee4: a9027bfd    	stp	x29, x30, [sp, #0x20]
100011ee8: f00020e1    	adrp	x1, 0x100430000 <___data>
100011eec: f9413c21    	ldr	x1, [x1, #0x278]
100011ef0: aa0103fe    	mov	x30, x1
100011ef4: f9000fe1    	str	x1, [sp, #0x18]
100011ef8: d63f03c0    	blr	x30
100011efc: f9400ffe    	ldr	x30, [sp, #0x18]
100011f00: d63f03c0    	blr	x30
100011f04: f9400ffe    	ldr	x30, [sp, #0x18]
100011f08: d63f03c0    	blr	x30
100011f0c: f9400ffe    	ldr	x30, [sp, #0x18]
100011f10: d63f03c0    	blr	x30
100011f14: f9400ffe    	ldr	x30, [sp, #0x18]
100011f18: d63f03c0    	blr	x30
100011f1c: f9400ffe    	ldr	x30, [sp, #0x18]
100011f20: d63f03c0    	blr	x30
100011f24: f9400ffe    	ldr	x30, [sp, #0x18]
100011f28: d63f03c0    	blr	x30
100011f2c: f9400ffe    	ldr	x30, [sp, #0x18]
100011f30: d63f03c0    	blr	x30
100011f34: f9400ffe    	ldr	x30, [sp, #0x18]
100011f38: d63f03c0    	blr	x30
100011f3c: f9400ffe    	ldr	x30, [sp, #0x18]
100011f40: d63f03c0    	blr	x30
100011f44: f9400ffe    	ldr	x30, [sp, #0x18]
100011f48: d63f03c0    	blr	x30
100011f4c: f9400ffe    	ldr	x30, [sp, #0x18]
100011f50: d63f03c0    	blr	x30
100011f54: f9400ffe    	ldr	x30, [sp, #0x18]
100011f58: d63f03c0    	blr	x30
100011f5c: f9400ffe    	ldr	x30, [sp, #0x18]
100011f60: d63f03c0    	blr	x30
100011f64: f9400ffe    	ldr	x30, [sp, #0x18]
100011f68: d63f03c0    	blr	x30
100011f6c: f9400ffe    	ldr	x30, [sp, #0x18]
100011f70: d63f03c0    	blr	x30
100011f74: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100011f78: 9100c3ff    	add	sp, sp, #0x30
100011f7c: d65f03c0    	ret

# svm: _Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0
0000000100011f80 <_Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0>:
100011f80: d100c3ff    	sub	sp, sp, #0x30
100011f84: a9027bfd    	stp	x29, x30, [sp, #0x20]
100011f88: f00020e1    	adrp	x1, 0x100430000 <___data>
100011f8c: f9413c21    	ldr	x1, [x1, #0x278]
100011f90: f9000be1    	str	x1, [sp, #0x10]
100011f94: 2a1f03e2    	mov	w2, wzr
100011f98: 14000006    	b	0x100011fb0 <_Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0x30>
100011f9c: d503201f    	nop
100011fa0: aa0103fe    	mov	x30, x1
100011fa4: d63f03c0    	blr	x30
100011fa8: 320003e2    	orr	w2, wzr, #0x1
100011fac: f9400be1    	ldr	x1, [sp, #0x10]
100011fb0: 34ffff82    	cbz	w2, 0x100011fa0 <_Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0x20>
100011fb4: 2a0203e1    	mov	w1, w2
100011fb8: 14000029    	b	0x10001205c <_Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0xdc>
100011fbc: d503201f    	nop
100011fc0: aa0203fe    	mov	x30, x2
100011fc4: d63f03c0    	blr	x30
100011fc8: b9401be1    	ldr	w1, [sp, #0x18]
100011fcc: 11000422    	add	w2, w1, #0x1
100011fd0: f9400bfe    	ldr	x30, [sp, #0x10]
100011fd4: b9001fe2    	str	w2, [sp, #0x1c]
100011fd8: d63f03c0    	blr	x30
100011fdc: b9401be1    	ldr	w1, [sp, #0x18]
100011fe0: 11000822    	add	w2, w1, #0x2
100011fe4: f9400bfe    	ldr	x30, [sp, #0x10]
100011fe8: b9001fe2    	str	w2, [sp, #0x1c]
100011fec: d63f03c0    	blr	x30
100011ff0: b9401be1    	ldr	w1, [sp, #0x18]
100011ff4: 11000c22    	add	w2, w1, #0x3
100011ff8: f9400bfe    	ldr	x30, [sp, #0x10]
100011ffc: b9001fe2    	str	w2, [sp, #0x1c]
100012000: d63f03c0    	blr	x30
100012004: b9401be1    	ldr	w1, [sp, #0x18]
100012008: 11001022    	add	w2, w1, #0x4
10001200c: f9400bfe    	ldr	x30, [sp, #0x10]
100012010: b9001fe2    	str	w2, [sp, #0x1c]
100012014: d63f03c0    	blr	x30
100012018: b9401be1    	ldr	w1, [sp, #0x18]
10001201c: 11001422    	add	w2, w1, #0x5
100012020: f9400bfe    	ldr	x30, [sp, #0x10]
100012024: b9001fe2    	str	w2, [sp, #0x1c]
100012028: d63f03c0    	blr	x30
10001202c: b9401be1    	ldr	w1, [sp, #0x18]
100012030: 11001822    	add	w2, w1, #0x6
100012034: f9400bfe    	ldr	x30, [sp, #0x10]
100012038: b9001fe2    	str	w2, [sp, #0x1c]
10001203c: d63f03c0    	blr	x30
100012040: b9401be1    	ldr	w1, [sp, #0x18]
100012044: 11001c22    	add	w2, w1, #0x7
100012048: f9400bfe    	ldr	x30, [sp, #0x10]
10001204c: b9001fe2    	str	w2, [sp, #0x1c]
100012050: d63f03c0    	blr	x30
100012054: b9401be1    	ldr	w1, [sp, #0x18]
100012058: 11002021    	add	w1, w1, #0x8
10001205c: f9400be2    	ldr	x2, [sp, #0x10]
100012060: b9001be1    	str	w1, [sp, #0x18]
100012064: 7103e03f    	cmp	w1, #0xf8
100012068: 54fffac3    	b.lo	0x100011fc0 <_Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0x40>
10001206c: 1400000d    	b	0x1000120a0 <_Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0x120>
100012070: d503201f    	nop
100012074: d503201f    	nop
100012078: d503201f    	nop
10001207c: d503201f    	nop
100012080: b9001be1    	str	w1, [sp, #0x18]
100012084: aa0203fe    	mov	x30, x2
100012088: d63f03c0    	blr	x30
10001208c: b9401be1    	ldr	w1, [sp, #0x18]
100012090: 1100043d    	add	w29, w1, #0x1
100012094: 2a1d03e1    	mov	w1, w29
100012098: f9400be2    	ldr	x2, [sp, #0x10]
10001209c: d503201f    	nop
1000120a0: 7104003f    	cmp	w1, #0x100
1000120a4: 54fffee3    	b.lo	0x100012080 <_Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0x100>
1000120a8: a9427bfd    	ldp	x29, x30, [sp, #0x20]
1000120ac: 9100c3ff    	add	sp, sp, #0x30
1000120b0: d65f03c0    	ret
1000120b4: cccccccc    	<unknown>
1000120b8: cccccccc    	<unknown>
1000120bc: cccccccc    	<unknown>

# svm: _Floor_javaBatch4_f1KINYClC5BmmtfwLbXF1F
00000001000120c0 <_Floor_javaBatch4_f1KINYClC5BmmtfwLbXF1F>:
1000120c0: d100c3ff    	sub	sp, sp, #0x30
1000120c4: a9027bfd    	stp	x29, x30, [sp, #0x20]
1000120c8: d00020e1    	adrp	x1, 0x100430000 <___data>
1000120cc: f9413c21    	ldr	x1, [x1, #0x278]
1000120d0: aa0103fe    	mov	x30, x1
1000120d4: f9000fe1    	str	x1, [sp, #0x18]
1000120d8: d63f03c0    	blr	x30
1000120dc: f9400ffe    	ldr	x30, [sp, #0x18]
1000120e0: d63f03c0    	blr	x30
1000120e4: f9400ffe    	ldr	x30, [sp, #0x18]
1000120e8: d63f03c0    	blr	x30
1000120ec: f9400ffe    	ldr	x30, [sp, #0x18]
1000120f0: d63f03c0    	blr	x30
1000120f4: a9427bfd    	ldp	x29, x30, [sp, #0x20]
1000120f8: 9100c3ff    	add	sp, sp, #0x30
1000120fc: d65f03c0    	ret

# svm: _Floor_javaBatch64_pgMJB9JHBI7okdXsdyhwU1
0000000100012100 <_Floor_javaBatch64_pgMJB9JHBI7okdXsdyhwU1>:
100012100: d100c3ff    	sub	sp, sp, #0x30
100012104: a9027bfd    	stp	x29, x30, [sp, #0x20]
100012108: d00020e1    	adrp	x1, 0x100430000 <___data>
10001210c: f9413c21    	ldr	x1, [x1, #0x278]
100012110: aa0103fe    	mov	x30, x1
100012114: f9000fe1    	str	x1, [sp, #0x18]
100012118: d63f03c0    	blr	x30
10001211c: f9400ffe    	ldr	x30, [sp, #0x18]
100012120: d63f03c0    	blr	x30
100012124: f9400ffe    	ldr	x30, [sp, #0x18]
100012128: d63f03c0    	blr	x30
10001212c: f9400ffe    	ldr	x30, [sp, #0x18]
100012130: d63f03c0    	blr	x30
100012134: f9400ffe    	ldr	x30, [sp, #0x18]
100012138: d63f03c0    	blr	x30
10001213c: f9400ffe    	ldr	x30, [sp, #0x18]
100012140: d63f03c0    	blr	x30
100012144: f9400ffe    	ldr	x30, [sp, #0x18]
100012148: d63f03c0    	blr	x30
10001214c: f9400ffe    	ldr	x30, [sp, #0x18]
100012150: d63f03c0    	blr	x30
100012154: f9400ffe    	ldr	x30, [sp, #0x18]
100012158: d63f03c0    	blr	x30
10001215c: f9400ffe    	ldr	x30, [sp, #0x18]
100012160: d63f03c0    	blr	x30
100012164: f9400ffe    	ldr	x30, [sp, #0x18]
100012168: d63f03c0    	blr	x30
10001216c: f9400ffe    	ldr	x30, [sp, #0x18]
100012170: d63f03c0    	blr	x30
100012174: f9400ffe    	ldr	x30, [sp, #0x18]
100012178: d63f03c0    	blr	x30
10001217c: f9400ffe    	ldr	x30, [sp, #0x18]
100012180: d63f03c0    	blr	x30
100012184: f9400ffe    	ldr	x30, [sp, #0x18]
100012188: d63f03c0    	blr	x30
10001218c: f9400ffe    	ldr	x30, [sp, #0x18]
100012190: d63f03c0    	blr	x30
100012194: f9400ffe    	ldr	x30, [sp, #0x18]
100012198: d63f03c0    	blr	x30
10001219c: f9400ffe    	ldr	x30, [sp, #0x18]
1000121a0: d63f03c0    	blr	x30
1000121a4: f9400ffe    	ldr	x30, [sp, #0x18]
1000121a8: d63f03c0    	blr	x30
1000121ac: f9400ffe    	ldr	x30, [sp, #0x18]
1000121b0: d63f03c0    	blr	x30
1000121b4: f9400ffe    	ldr	x30, [sp, #0x18]
1000121b8: d63f03c0    	blr	x30
1000121bc: f9400ffe    	ldr	x30, [sp, #0x18]
1000121c0: d63f03c0    	blr	x30
1000121c4: f9400ffe    	ldr	x30, [sp, #0x18]
1000121c8: d63f03c0    	blr	x30
1000121cc: f9400ffe    	ldr	x30, [sp, #0x18]
1000121d0: d63f03c0    	blr	x30
1000121d4: f9400ffe    	ldr	x30, [sp, #0x18]
1000121d8: d63f03c0    	blr	x30
1000121dc: f9400ffe    	ldr	x30, [sp, #0x18]
1000121e0: d63f03c0    	blr	x30
1000121e4: f9400ffe    	ldr	x30, [sp, #0x18]
1000121e8: d63f03c0    	blr	x30
1000121ec: f9400ffe    	ldr	x30, [sp, #0x18]
1000121f0: d63f03c0    	blr	x30
1000121f4: f9400ffe    	ldr	x30, [sp, #0x18]
1000121f8: d63f03c0    	blr	x30
1000121fc: f9400ffe    	ldr	x30, [sp, #0x18]
100012200: d63f03c0    	blr	x30
100012204: f9400ffe    	ldr	x30, [sp, #0x18]
100012208: d63f03c0    	blr	x30
10001220c: f9400ffe    	ldr	x30, [sp, #0x18]
100012210: d63f03c0    	blr	x30
100012214: f9400ffe    	ldr	x30, [sp, #0x18]
100012218: d63f03c0    	blr	x30
10001221c: f9400ffe    	ldr	x30, [sp, #0x18]
100012220: d63f03c0    	blr	x30
100012224: f9400ffe    	ldr	x30, [sp, #0x18]
100012228: d63f03c0    	blr	x30
10001222c: f9400ffe    	ldr	x30, [sp, #0x18]
100012230: d63f03c0    	blr	x30
100012234: f9400ffe    	ldr	x30, [sp, #0x18]
100012238: d63f03c0    	blr	x30
10001223c: f9400ffe    	ldr	x30, [sp, #0x18]
100012240: d63f03c0    	blr	x30
100012244: f9400ffe    	ldr	x30, [sp, #0x18]
100012248: d63f03c0    	blr	x30
10001224c: f9400ffe    	ldr	x30, [sp, #0x18]
100012250: d63f03c0    	blr	x30
100012254: f9400ffe    	ldr	x30, [sp, #0x18]
100012258: d63f03c0    	blr	x30
10001225c: f9400ffe    	ldr	x30, [sp, #0x18]
100012260: d63f03c0    	blr	x30
100012264: f9400ffe    	ldr	x30, [sp, #0x18]
100012268: d63f03c0    	blr	x30
10001226c: f9400ffe    	ldr	x30, [sp, #0x18]
100012270: d63f03c0    	blr	x30
100012274: f9400ffe    	ldr	x30, [sp, #0x18]
100012278: d63f03c0    	blr	x30
10001227c: f9400ffe    	ldr	x30, [sp, #0x18]
100012280: d63f03c0    	blr	x30
100012284: f9400ffe    	ldr	x30, [sp, #0x18]
100012288: d63f03c0    	blr	x30
10001228c: f9400ffe    	ldr	x30, [sp, #0x18]
100012290: d63f03c0    	blr	x30
100012294: f9400ffe    	ldr	x30, [sp, #0x18]
100012298: d63f03c0    	blr	x30
10001229c: f9400ffe    	ldr	x30, [sp, #0x18]
1000122a0: d63f03c0    	blr	x30
1000122a4: f9400ffe    	ldr	x30, [sp, #0x18]
1000122a8: d63f03c0    	blr	x30
1000122ac: f9400ffe    	ldr	x30, [sp, #0x18]
1000122b0: d63f03c0    	blr	x30
1000122b4: f9400ffe    	ldr	x30, [sp, #0x18]
1000122b8: d63f03c0    	blr	x30
1000122bc: f9400ffe    	ldr	x30, [sp, #0x18]
1000122c0: d63f03c0    	blr	x30
1000122c4: f9400ffe    	ldr	x30, [sp, #0x18]
1000122c8: d63f03c0    	blr	x30
1000122cc: f9400ffe    	ldr	x30, [sp, #0x18]
1000122d0: d63f03c0    	blr	x30
1000122d4: f9400ffe    	ldr	x30, [sp, #0x18]
1000122d8: d63f03c0    	blr	x30
1000122dc: f9400ffe    	ldr	x30, [sp, #0x18]
1000122e0: d63f03c0    	blr	x30
1000122e4: f9400ffe    	ldr	x30, [sp, #0x18]
1000122e8: d63f03c0    	blr	x30
1000122ec: f9400ffe    	ldr	x30, [sp, #0x18]
1000122f0: d63f03c0    	blr	x30
1000122f4: f9400ffe    	ldr	x30, [sp, #0x18]
1000122f8: d63f03c0    	blr	x30
1000122fc: f9400ffe    	ldr	x30, [sp, #0x18]
100012300: d63f03c0    	blr	x30
100012304: f9400ffe    	ldr	x30, [sp, #0x18]
100012308: d63f03c0    	blr	x30
10001230c: f9400ffe    	ldr	x30, [sp, #0x18]
100012310: d63f03c0    	blr	x30
100012314: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100012318: 9100c3ff    	add	sp, sp, #0x30
10001231c: d65f03c0    	ret

# svm: _Generated_leaf_A7zZco9JWUE1TvDDMvfDp7
00000001000162f0 <_Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>:
1000162f0: d10083ff    	sub	sp, sp, #0x20
1000162f4: a9017bfd    	stp	x29, x30, [sp, #0x10]
1000162f8: d00020de    	adrp	x30, 0x100430000 <___data>
1000162fc: f9413fde    	ldr	x30, [x30, #0x278]
100016300: d63f03c0    	blr	x30
100016304: a9417bfd    	ldp	x29, x30, [sp, #0x10]
100016308: 910083ff    	add	sp, sp, #0x20
10001630c: d65f03c0    	ret

# svm: _Identity_leaf_gOd6suzlEz5e8qvz4k8wcB
0000000100016310 <_Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>:
100016310: d10083ff    	sub	sp, sp, #0x20
100016314: a9017bfd    	stp	x29, x30, [sp, #0x10]
100016318: d00020de    	adrp	x30, 0x100430000 <___data>
10001631c: f94093de    	ldr	x30, [x30, #0x120]
100016320: d63f03c0    	blr	x30
100016324: a9417bfd    	ldp	x29, x30, [sp, #0x10]
100016328: 910083ff    	add	sp, sp, #0x20
10001632c: d65f03c0    	ret

