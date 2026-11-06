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

# svm: _Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG
0000000100010250 <_Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG>:
100010250: d10103e9    	sub	x9, sp, #0x40
100010254: f940078a    	ldr	x10, [x28, #0x8]
100010258: eb0a013f    	cmp	x9, x10
10001025c: 54000689    	b.ls	0x10001032c <_Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0xdc>
100010260: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010264: 9100013f    	mov	sp, x9
100010268: b9002fe1    	str	w1, [sp, #0x2c]
10001026c: 7100043f    	cmp	w1, #0x1
100010270: 5400022b    	b.lt	0x1000102b4 <_Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x64>
100010274: 52873323    	mov	w3, #0x3999             ; =14745
100010278: 72a000a3    	movk	w3, #0x5, lsl #16
10001027c: 8b030f63    	add	x3, x27, x3, lsl #3
100010280: f90013e3    	str	x3, [sp, #0x20]
100010284: f9401460    	ldr	x0, [x3, #0x28]
100010288: aa0203e1    	mov	x1, x2
10001028c: 94000795    	bl	0x1000120e0 <_Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1>
100010290: b9402fe1    	ldr	w1, [sp, #0x2c]
100010294: 7104043f    	cmp	w1, #0x101
100010298: 540001ca    	b.ge	0x1000102d0 <_Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x80>
10001029c: a9437bfd    	ldp	x29, x30, [sp, #0x30]
1000102a0: 910103ff    	add	sp, sp, #0x40
1000102a4: b9401389    	ldr	w9, [x28, #0x10]
1000102a8: 7100013f    	cmp	w9, #0x0
1000102ac: 5400042d    	b.le	0x100010330 <_Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0xe0>
1000102b0: d65f03c0    	ret
1000102b4: aa0203e0    	mov	x0, x2
1000102b8: a9437bfd    	ldp	x29, x30, [sp, #0x30]
1000102bc: 910103ff    	add	sp, sp, #0x40
1000102c0: b9401389    	ldr	w9, [x28, #0x10]
1000102c4: 7100013f    	cmp	w9, #0x0
1000102c8: 5400034d    	b.le	0x100010330 <_Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0xe0>
1000102cc: d65f03c0    	ret
1000102d0: 321803e9    	orr	w9, wzr, #0x100
1000102d4: b9002be9    	str	w9, [sp, #0x28]
1000102d8: f94013e3    	ldr	x3, [sp, #0x20]
1000102dc: aa0003e1    	mov	x1, x0
1000102e0: f9401460    	ldr	x0, [x3, #0x28]
1000102e4: 9400077f    	bl	0x1000120e0 <_Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1>
1000102e8: b9402be1    	ldr	w1, [sp, #0x28]
1000102ec: 11040022    	add	w2, w1, #0x100
1000102f0: b9402fe3    	ldr	w3, [sp, #0x2c]
1000102f4: 6b03005f    	cmp	w2, w3
1000102f8: 54fffd2a    	b.ge	0x10001029c <_Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x4c>
1000102fc: b9401389    	ldr	w9, [x28, #0x10]
100010300: 340000a9    	cbz	w9, 0x100010314 <_Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0xc4>
100010304: aa0003e1    	mov	x1, x0
100010308: b9002be2    	str	w2, [sp, #0x28]
10001030c: f94013e3    	ldr	x3, [sp, #0x20]
100010310: 17fffff4    	b	0x1000102e0 <_Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x90>
100010314: f94013e4    	ldr	x4, [sp, #0x20]
100010318: 9405ee8a    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001031c: aa0003e1    	mov	x1, x0
100010320: b9002be2    	str	w2, [sp, #0x28]
100010324: aa0403e3    	mov	x3, x4
100010328: 17ffffee    	b	0x1000102e0 <_Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x90>
10001032c: 14029b79    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010330: 1405ee84    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010334: cccccccc    	<unknown>
100010338: cccccccc    	<unknown>
10001033c: cccccccc    	<unknown>

# svm: _Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6
0000000100010340 <_Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6>:
100010340: d10103e9    	sub	x9, sp, #0x40
100010344: f940078a    	ldr	x10, [x28, #0x8]
100010348: eb0a013f    	cmp	x9, x10
10001034c: 54000649    	b.ls	0x100010414 <_Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0xd4>
100010350: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010354: 9100013f    	mov	sp, x9
100010358: b9002fe1    	str	w1, [sp, #0x2c]
10001035c: 7100043f    	cmp	w1, #0x1
100010360: 5400020b    	b.lt	0x1000103a0 <_Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x60>
100010364: 90002083    	adrp	x3, 0x100420000 <___data>
100010368: f9437863    	ldr	x3, [x3, #0x6f0]
10001036c: f90013e3    	str	x3, [sp, #0x20]
100010370: aa0203e0    	mov	x0, x2
100010374: aa0303fe    	mov	x30, x3
100010378: d63f03c0    	blr	x30
10001037c: b9402fe1    	ldr	w1, [sp, #0x2c]
100010380: 7100443f    	cmp	w1, #0x11
100010384: 540001ca    	b.ge	0x1000103bc <_Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x7c>
100010388: a9437bfd    	ldp	x29, x30, [sp, #0x30]
10001038c: 910103ff    	add	sp, sp, #0x40
100010390: b9401389    	ldr	w9, [x28, #0x10]
100010394: 7100013f    	cmp	w9, #0x0
100010398: 5400040d    	b.le	0x100010418 <_Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0xd8>
10001039c: d65f03c0    	ret
1000103a0: aa0203e0    	mov	x0, x2
1000103a4: a9437bfd    	ldp	x29, x30, [sp, #0x30]
1000103a8: 910103ff    	add	sp, sp, #0x40
1000103ac: b9401389    	ldr	w9, [x28, #0x10]
1000103b0: 7100013f    	cmp	w9, #0x0
1000103b4: 5400032d    	b.le	0x100010418 <_Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0xd8>
1000103b8: d65f03c0    	ret
1000103bc: 321c03e9    	orr	w9, wzr, #0x10
1000103c0: b9002be9    	str	w9, [sp, #0x28]
1000103c4: f94013e3    	ldr	x3, [sp, #0x20]
1000103c8: d503201f    	nop
1000103cc: d503201f    	nop
1000103d0: aa0303fe    	mov	x30, x3
1000103d4: d63f03c0    	blr	x30
1000103d8: b9402be1    	ldr	w1, [sp, #0x28]
1000103dc: 11004022    	add	w2, w1, #0x10
1000103e0: b9402fe3    	ldr	w3, [sp, #0x2c]
1000103e4: 6b03005f    	cmp	w2, w3
1000103e8: 54fffd0a    	b.ge	0x100010388 <_Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x48>
1000103ec: b9401389    	ldr	w9, [x28, #0x10]
1000103f0: 34000089    	cbz	w9, 0x100010400 <_Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0xc0>
1000103f4: b9002be2    	str	w2, [sp, #0x28]
1000103f8: f94013e3    	ldr	x3, [sp, #0x20]
1000103fc: 17fffff5    	b	0x1000103d0 <_Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x90>
100010400: f94013e4    	ldr	x4, [sp, #0x20]
100010404: 9405ee4f    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010408: b9002be2    	str	w2, [sp, #0x28]
10001040c: aa0403e3    	mov	x3, x4
100010410: 17fffff0    	b	0x1000103d0 <_Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x90>
100010414: 14029b3f    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010418: 1405ee4a    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001041c: cccccccc    	<unknown>

# svm: _Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF
0000000100010420 <_Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF>:
100010420: d10103e9    	sub	x9, sp, #0x40
100010424: f940078a    	ldr	x10, [x28, #0x8]
100010428: eb0a013f    	cmp	x9, x10
10001042c: 54000689    	b.ls	0x1000104fc <_Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0xdc>
100010430: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010434: 9100013f    	mov	sp, x9
100010438: b9002fe1    	str	w1, [sp, #0x2c]
10001043c: 7100043f    	cmp	w1, #0x1
100010440: 5400022b    	b.lt	0x100010484 <_Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x64>
100010444: 52873323    	mov	w3, #0x3999             ; =14745
100010448: 72a000a3    	movk	w3, #0x5, lsl #16
10001044c: 8b030f63    	add	x3, x27, x3, lsl #3
100010450: f90013e3    	str	x3, [sp, #0x20]
100010454: f9401460    	ldr	x0, [x3, #0x28]
100010458: aa0203e1    	mov	x1, x2
10001045c: 94000779    	bl	0x100012240 <_Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI>
100010460: b9402fe1    	ldr	w1, [sp, #0x2c]
100010464: 7100143f    	cmp	w1, #0x5
100010468: 540001ca    	b.ge	0x1000104a0 <_Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x80>
10001046c: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010470: 910103ff    	add	sp, sp, #0x40
100010474: b9401389    	ldr	w9, [x28, #0x10]
100010478: 7100013f    	cmp	w9, #0x0
10001047c: 5400042d    	b.le	0x100010500 <_Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0xe0>
100010480: d65f03c0    	ret
100010484: aa0203e0    	mov	x0, x2
100010488: a9437bfd    	ldp	x29, x30, [sp, #0x30]
10001048c: 910103ff    	add	sp, sp, #0x40
100010490: b9401389    	ldr	w9, [x28, #0x10]
100010494: 7100013f    	cmp	w9, #0x0
100010498: 5400034d    	b.le	0x100010500 <_Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0xe0>
10001049c: d65f03c0    	ret
1000104a0: 321e03e9    	orr	w9, wzr, #0x4
1000104a4: b9002be9    	str	w9, [sp, #0x28]
1000104a8: f94013e3    	ldr	x3, [sp, #0x20]
1000104ac: aa0003e1    	mov	x1, x0
1000104b0: f9401460    	ldr	x0, [x3, #0x28]
1000104b4: 94000763    	bl	0x100012240 <_Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI>
1000104b8: b9402be1    	ldr	w1, [sp, #0x28]
1000104bc: 11001022    	add	w2, w1, #0x4
1000104c0: b9402fe3    	ldr	w3, [sp, #0x2c]
1000104c4: 6b03005f    	cmp	w2, w3
1000104c8: 54fffd2a    	b.ge	0x10001046c <_Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x4c>
1000104cc: b9401389    	ldr	w9, [x28, #0x10]
1000104d0: 340000a9    	cbz	w9, 0x1000104e4 <_Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0xc4>
1000104d4: aa0003e1    	mov	x1, x0
1000104d8: b9002be2    	str	w2, [sp, #0x28]
1000104dc: f94013e3    	ldr	x3, [sp, #0x20]
1000104e0: 17fffff4    	b	0x1000104b0 <_Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x90>
1000104e4: f94013e4    	ldr	x4, [sp, #0x20]
1000104e8: 9405ee16    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000104ec: aa0003e1    	mov	x1, x0
1000104f0: b9002be2    	str	w2, [sp, #0x28]
1000104f4: aa0403e3    	mov	x3, x4
1000104f8: 17ffffee    	b	0x1000104b0 <_Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x90>
1000104fc: 14029b05    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010500: 1405ee10    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010504: cccccccc    	<unknown>
100010508: cccccccc    	<unknown>
10001050c: cccccccc    	<unknown>

# svm: _Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE
0000000100010510 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE>:
100010510: d10143e9    	sub	x9, sp, #0x50
100010514: f940078a    	ldr	x10, [x28, #0x8]
100010518: eb0a013f    	cmp	x9, x10
10001051c: 54000bc9    	b.ls	0x100010694 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x184>
100010520: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010524: 9100013f    	mov	sp, x9
100010528: b90033e1    	str	w1, [sp, #0x30]
10001052c: 7100043f    	cmp	w1, #0x1
100010530: 320003e0    	orr	w0, wzr, #0x1
100010534: 1a80d020    	csel	w0, w1, w0, le
100010538: 7100041f    	cmp	w0, #0x1
10001053c: 54000a6b    	b.lt	0x100010688 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x178>
100010540: aa0203e0    	mov	x0, x2
100010544: 9400091b    	bl	0x1000129b0 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
100010548: 320003e2    	orr	w2, wzr, #0x1
10001054c: b94033e1    	ldr	w1, [sp, #0x30]
100010550: 52800083    	mov	w3, #0x4                ; =4
100010554: 72b00003    	movk	w3, #0x8000, lsl #16
100010558: 320103e4    	orr	w4, wzr, #0x80000000
10001055c: 51001025    	sub	w5, w1, #0x4
100010560: 6b03003f    	cmp	w1, w3
100010564: 1a85b083    	csel	w3, w4, w5, lt
100010568: b9002be3    	str	w3, [sp, #0x28]
10001056c: 11000c64    	add	w4, w3, #0x3
100010570: b90027e4    	str	w4, [sp, #0x24]
100010574: 2a0203e1    	mov	w1, w2
100010578: d503201f    	nop
10001057c: d503201f    	nop
100010580: 6b03003f    	cmp	w1, w3
100010584: 5400062a    	b.ge	0x100010648 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x138>
100010588: 4b010085    	sub	w5, w4, w1
10001058c: 53027ca6    	lsr	w6, w5, #2
100010590: 121e74a5    	and	w5, w5, #0xfffffffc
100010594: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100010598: 321203e6    	orr	w6, wzr, #0x4000
10001059c: 1a8630a5    	csel	w5, w5, w6, lo
1000105a0: 710004bf    	cmp	w5, #0x1
1000105a4: 540004ab    	b.lt	0x100010638 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x128>
1000105a8: b90037e1    	str	w1, [sp, #0x34]
1000105ac: b9003fe5    	str	w5, [sp, #0x3c]
1000105b0: 2a1f03e2    	mov	w2, wzr
1000105b4: d503201f    	nop
1000105b8: d503201f    	nop
1000105bc: d503201f    	nop
1000105c0: 0b010047    	add	w7, w2, w1
1000105c4: b90023e7    	str	w7, [sp, #0x20]
1000105c8: b9002fe2    	str	w2, [sp, #0x2c]
1000105cc: 940008f9    	bl	0x1000129b0 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
1000105d0: b94023e7    	ldr	w7, [sp, #0x20]
1000105d4: 110004e1    	add	w1, w7, #0x1
1000105d8: b9003be1    	str	w1, [sp, #0x38]
1000105dc: 940008f5    	bl	0x1000129b0 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
1000105e0: b94023e7    	ldr	w7, [sp, #0x20]
1000105e4: 110008e1    	add	w1, w7, #0x2
1000105e8: b9003be1    	str	w1, [sp, #0x38]
1000105ec: 940008f1    	bl	0x1000129b0 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
1000105f0: b94023e7    	ldr	w7, [sp, #0x20]
1000105f4: 11000ce1    	add	w1, w7, #0x3
1000105f8: b90023e1    	str	w1, [sp, #0x20]
1000105fc: 940008ed    	bl	0x1000129b0 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
100010600: b9402fe2    	ldr	w2, [sp, #0x2c]
100010604: 11001042    	add	w2, w2, #0x4
100010608: b9403fe5    	ldr	w5, [sp, #0x3c]
10001060c: 6b05005f    	cmp	w2, w5
100010610: b94037e1    	ldr	w1, [sp, #0x34]
100010614: 54fffd6b    	b.lt	0x1000105c0 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0xb0>
100010618: 0b010041    	add	w1, w2, w1
10001061c: b9401389    	ldr	w9, [x28, #0x10]
100010620: 34000069    	cbz	w9, 0x10001062c <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x11c>
100010624: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100010628: 17ffffd6    	b	0x100010580 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x70>
10001062c: 9405edc5    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010630: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100010634: 17ffffd3    	b	0x100010580 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x70>
100010638: b9401389    	ldr	w9, [x28, #0x10]
10001063c: 35fffa29    	cbnz	w9, 0x100010580 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x70>
100010640: 9405edc0    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010644: 17ffffcf    	b	0x100010580 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x70>
100010648: 2a0103e2    	mov	w2, w1
10001064c: 14000006    	b	0x100010664 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x154>
100010650: b9002fe2    	str	w2, [sp, #0x2c]
100010654: 940008d7    	bl	0x1000129b0 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
100010658: b9402fe2    	ldr	w2, [sp, #0x2c]
10001065c: 1100045d    	add	w29, w2, #0x1
100010660: 2a1d03e2    	mov	w2, w29
100010664: b94033e1    	ldr	w1, [sp, #0x30]
100010668: 6b01005f    	cmp	w2, w1
10001066c: 54ffff2b    	b.lt	0x100010650 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x140>
100010670: a9447bfd    	ldp	x29, x30, [sp, #0x40]
100010674: 910143ff    	add	sp, sp, #0x50
100010678: b9401389    	ldr	w9, [x28, #0x10]
10001067c: 7100013f    	cmp	w9, #0x0
100010680: 540000cd    	b.le	0x100010698 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x188>
100010684: d65f03c0    	ret
100010688: aa0203e0    	mov	x0, x2
10001068c: 2a1f03e2    	mov	w2, wzr
100010690: 17ffffb0    	b	0x100010550 <_Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x40>
100010694: 14029a9f    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010698: 1405edaa    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001069c: cccccccc    	<unknown>

# svm: _Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669
00000001000106a0 <_Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669>:
1000106a0: d10103e9    	sub	x9, sp, #0x40
1000106a4: f940078a    	ldr	x10, [x28, #0x8]
1000106a8: eb0a013f    	cmp	x9, x10
1000106ac: 54000809    	b.ls	0x1000107ac <_Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x10c>
1000106b0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000106b4: 9100013f    	mov	sp, x9
1000106b8: b9002fe1    	str	w1, [sp, #0x2c]
1000106bc: 7100043f    	cmp	w1, #0x1
1000106c0: 5400030b    	b.lt	0x100010720 <_Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x80>
1000106c4: 52873320    	mov	w0, #0x3999             ; =14745
1000106c8: 72a000a0    	movk	w0, #0x5, lsl #16
1000106cc: 8b000f60    	add	x0, x27, x0, lsl #3
1000106d0: f9401803    	ldr	x3, [x0, #0x30]
1000106d4: f90013e3    	str	x3, [sp, #0x20]
1000106d8: aa0203e0    	mov	x0, x2
1000106dc: aa0303fe    	mov	x30, x3
1000106e0: d63f03c0    	blr	x30
1000106e4: f94013fe    	ldr	x30, [sp, #0x20]
1000106e8: d63f03c0    	blr	x30
1000106ec: f94013fe    	ldr	x30, [sp, #0x20]
1000106f0: d63f03c0    	blr	x30
1000106f4: f94013fe    	ldr	x30, [sp, #0x20]
1000106f8: d63f03c0    	blr	x30
1000106fc: b9402fe1    	ldr	w1, [sp, #0x2c]
100010700: 7100143f    	cmp	w1, #0x5
100010704: 540001ca    	b.ge	0x10001073c <_Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x9c>
100010708: a9437bfd    	ldp	x29, x30, [sp, #0x30]
10001070c: 910103ff    	add	sp, sp, #0x40
100010710: b9401389    	ldr	w9, [x28, #0x10]
100010714: 7100013f    	cmp	w9, #0x0
100010718: 540004cd    	b.le	0x1000107b0 <_Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x110>
10001071c: d65f03c0    	ret
100010720: aa0203e0    	mov	x0, x2
100010724: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010728: 910103ff    	add	sp, sp, #0x40
10001072c: b9401389    	ldr	w9, [x28, #0x10]
100010730: 7100013f    	cmp	w9, #0x0
100010734: 540003ed    	b.le	0x1000107b0 <_Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x110>
100010738: d65f03c0    	ret
10001073c: 321e03e9    	orr	w9, wzr, #0x4
100010740: b9002be9    	str	w9, [sp, #0x28]
100010744: f94013e3    	ldr	x3, [sp, #0x20]
100010748: d503201f    	nop
10001074c: d503201f    	nop
100010750: aa0303fe    	mov	x30, x3
100010754: d63f03c0    	blr	x30
100010758: f94013fe    	ldr	x30, [sp, #0x20]
10001075c: d63f03c0    	blr	x30
100010760: f94013fe    	ldr	x30, [sp, #0x20]
100010764: d63f03c0    	blr	x30
100010768: f94013fe    	ldr	x30, [sp, #0x20]
10001076c: d63f03c0    	blr	x30
100010770: b9402be1    	ldr	w1, [sp, #0x28]
100010774: 11001022    	add	w2, w1, #0x4
100010778: b9402fe3    	ldr	w3, [sp, #0x2c]
10001077c: 6b03005f    	cmp	w2, w3
100010780: 54fffc4a    	b.ge	0x100010708 <_Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x68>
100010784: b9401389    	ldr	w9, [x28, #0x10]
100010788: 34000089    	cbz	w9, 0x100010798 <_Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0xf8>
10001078c: b9002be2    	str	w2, [sp, #0x28]
100010790: f94013e3    	ldr	x3, [sp, #0x20]
100010794: 17ffffef    	b	0x100010750 <_Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0xb0>
100010798: f94013e4    	ldr	x4, [sp, #0x20]
10001079c: 9405ed69    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000107a0: b9002be2    	str	w2, [sp, #0x28]
1000107a4: aa0403e3    	mov	x3, x4
1000107a8: 17ffffea    	b	0x100010750 <_Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0xb0>
1000107ac: 14029a59    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000107b0: 1405ed64    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000107b4: cccccccc    	<unknown>
1000107b8: cccccccc    	<unknown>
1000107bc: cccccccc    	<unknown>

# svm: _Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4
00000001000107c0 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4>:
1000107c0: d10143e9    	sub	x9, sp, #0x50
1000107c4: f940078a    	ldr	x10, [x28, #0x8]
1000107c8: eb0a013f    	cmp	x9, x10
1000107cc: 54000f09    	b.ls	0x1000109ac <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1ec>
1000107d0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000107d4: 9100013f    	mov	sp, x9
1000107d8: b90033e1    	str	w1, [sp, #0x30]
1000107dc: 52873320    	mov	w0, #0x3999             ; =14745
1000107e0: 72a000a0    	movk	w0, #0x5, lsl #16
1000107e4: 8b000f60    	add	x0, x27, x0, lsl #3
1000107e8: f9401803    	ldr	x3, [x0, #0x30]
1000107ec: f9000fe3    	str	x3, [sp, #0x18]
1000107f0: 7100043f    	cmp	w1, #0x1
1000107f4: 320003e0    	orr	w0, wzr, #0x1
1000107f8: 1a80d020    	csel	w0, w1, w0, le
1000107fc: 7100041f    	cmp	w0, #0x1
100010800: 54000d0b    	b.lt	0x1000109a0 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1e0>
100010804: aa0203e0    	mov	x0, x2
100010808: aa0303fe    	mov	x30, x3
10001080c: d63f03c0    	blr	x30
100010810: 320003fe    	orr	w30, wzr, #0x1
100010814: b94033e1    	ldr	w1, [sp, #0x30]
100010818: 52800082    	mov	w2, #0x4                ; =4
10001081c: 72b00002    	movk	w2, #0x8000, lsl #16
100010820: 320103e3    	orr	w3, wzr, #0x80000000
100010824: 51001024    	sub	w4, w1, #0x4
100010828: 6b02003f    	cmp	w1, w2
10001082c: 1a84b062    	csel	w2, w3, w4, lt
100010830: b9002be2    	str	w2, [sp, #0x28]
100010834: 11000c43    	add	w3, w2, #0x3
100010838: b90027e3    	str	w3, [sp, #0x24]
10001083c: f9400fe4    	ldr	x4, [sp, #0x18]
100010840: 2a1e03e1    	mov	w1, w30
100010844: d503201f    	nop
100010848: d503201f    	nop
10001084c: d503201f    	nop
100010850: 6b02003f    	cmp	w1, w2
100010854: 5400078a    	b.ge	0x100010944 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x184>
100010858: 4b01007e    	sub	w30, w3, w1
10001085c: 53027fc6    	lsr	w6, w30, #2
100010860: 121e77de    	and	w30, w30, #0xfffffffc
100010864: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100010868: 321203e6    	orr	w6, wzr, #0x4000
10001086c: 1a8633c6    	csel	w6, w30, w6, lo
100010870: 710004df    	cmp	w6, #0x1
100010874: 5400060b    	b.lt	0x100010934 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x174>
100010878: b90037e1    	str	w1, [sp, #0x34]
10001087c: b9003fe6    	str	w6, [sp, #0x3c]
100010880: 2a1f03e2    	mov	w2, wzr
100010884: 14000008    	b	0x1000108a4 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0xe4>
100010888: d503201f    	nop
10001088c: d503201f    	nop
100010890: d503201f    	nop
100010894: d503201f    	nop
100010898: d503201f    	nop
10001089c: d503201f    	nop
1000108a0: f9400fe4    	ldr	x4, [sp, #0x18]
1000108a4: 0b010048    	add	w8, w2, w1
1000108a8: b90023e8    	str	w8, [sp, #0x20]
1000108ac: aa0403fe    	mov	x30, x4
1000108b0: b9002fe2    	str	w2, [sp, #0x2c]
1000108b4: d63f03c0    	blr	x30
1000108b8: b94023e8    	ldr	w8, [sp, #0x20]
1000108bc: 11000501    	add	w1, w8, #0x1
1000108c0: f9400ffe    	ldr	x30, [sp, #0x18]
1000108c4: b9003be1    	str	w1, [sp, #0x38]
1000108c8: d63f03c0    	blr	x30
1000108cc: b94023e8    	ldr	w8, [sp, #0x20]
1000108d0: 11000901    	add	w1, w8, #0x2
1000108d4: f9400ffe    	ldr	x30, [sp, #0x18]
1000108d8: b9003be1    	str	w1, [sp, #0x38]
1000108dc: d63f03c0    	blr	x30
1000108e0: b94023e8    	ldr	w8, [sp, #0x20]
1000108e4: 11000d01    	add	w1, w8, #0x3
1000108e8: f9400ffe    	ldr	x30, [sp, #0x18]
1000108ec: b90023e1    	str	w1, [sp, #0x20]
1000108f0: d63f03c0    	blr	x30
1000108f4: b9402fe2    	ldr	w2, [sp, #0x2c]
1000108f8: 11001042    	add	w2, w2, #0x4
1000108fc: b9403fe6    	ldr	w6, [sp, #0x3c]
100010900: 6b06005f    	cmp	w2, w6
100010904: b94037e1    	ldr	w1, [sp, #0x34]
100010908: 54fffccb    	b.lt	0x1000108a0 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0xe0>
10001090c: 0b010041    	add	w1, w2, w1
100010910: b9401389    	ldr	w9, [x28, #0x10]
100010914: 34000089    	cbz	w9, 0x100010924 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x164>
100010918: 29448be3    	ldp	w3, w2, [sp, #0x24]
10001091c: f9400fe4    	ldr	x4, [sp, #0x18]
100010920: 17ffffcc    	b	0x100010850 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x90>
100010924: 9405ed07    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010928: 29448be3    	ldp	w3, w2, [sp, #0x24]
10001092c: f9400fe4    	ldr	x4, [sp, #0x18]
100010930: 17ffffc8    	b	0x100010850 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x90>
100010934: b9401389    	ldr	w9, [x28, #0x10]
100010938: 35fff8c9    	cbnz	w9, 0x100010850 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x90>
10001093c: 9405ed01    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010940: 17ffffc4    	b	0x100010850 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x90>
100010944: 2a0103e2    	mov	w2, w1
100010948: aa0403e3    	mov	x3, x4
10001094c: 1400000c    	b	0x10001097c <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1bc>
100010950: d503201f    	nop
100010954: d503201f    	nop
100010958: d503201f    	nop
10001095c: d503201f    	nop
100010960: b9002fe2    	str	w2, [sp, #0x2c]
100010964: aa0303fe    	mov	x30, x3
100010968: d63f03c0    	blr	x30
10001096c: b9402fe2    	ldr	w2, [sp, #0x2c]
100010970: 1100045d    	add	w29, w2, #0x1
100010974: 2a1d03e2    	mov	w2, w29
100010978: f9400fe3    	ldr	x3, [sp, #0x18]
10001097c: b94033e1    	ldr	w1, [sp, #0x30]
100010980: 6b01005f    	cmp	w2, w1
100010984: 54fffeeb    	b.lt	0x100010960 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1a0>
100010988: a9447bfd    	ldp	x29, x30, [sp, #0x40]
10001098c: 910143ff    	add	sp, sp, #0x50
100010990: b9401389    	ldr	w9, [x28, #0x10]
100010994: 7100013f    	cmp	w9, #0x0
100010998: 540000cd    	b.le	0x1000109b0 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1f0>
10001099c: d65f03c0    	ret
1000109a0: 2a1f03fe    	mov	w30, wzr
1000109a4: aa0203e0    	mov	x0, x2
1000109a8: 17ffff9c    	b	0x100010818 <_Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x58>
1000109ac: 140299d9    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000109b0: 1405ece4    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000109b4: cccccccc    	<unknown>
1000109b8: cccccccc    	<unknown>
1000109bc: cccccccc    	<unknown>

# svm: _Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5
00000001000109c0 <_Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5>:
1000109c0: d10103e9    	sub	x9, sp, #0x40
1000109c4: f940078a    	ldr	x10, [x28, #0x8]
1000109c8: eb0a013f    	cmp	x9, x10
1000109cc: 54000809    	b.ls	0x100010acc <_Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x10c>
1000109d0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000109d4: 9100013f    	mov	sp, x9
1000109d8: b9002fe1    	str	w1, [sp, #0x2c]
1000109dc: 7100043f    	cmp	w1, #0x1
1000109e0: 5400030b    	b.lt	0x100010a40 <_Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x80>
1000109e4: 52873320    	mov	w0, #0x3999             ; =14745
1000109e8: 72a000a0    	movk	w0, #0x5, lsl #16
1000109ec: 8b000f60    	add	x0, x27, x0, lsl #3
1000109f0: f9401803    	ldr	x3, [x0, #0x30]
1000109f4: f90013e3    	str	x3, [sp, #0x20]
1000109f8: aa0203e0    	mov	x0, x2
1000109fc: aa0303fe    	mov	x30, x3
100010a00: d63f03c0    	blr	x30
100010a04: f94013fe    	ldr	x30, [sp, #0x20]
100010a08: d63f03c0    	blr	x30
100010a0c: f94013fe    	ldr	x30, [sp, #0x20]
100010a10: d63f03c0    	blr	x30
100010a14: f94013fe    	ldr	x30, [sp, #0x20]
100010a18: d63f03c0    	blr	x30
100010a1c: b9402fe1    	ldr	w1, [sp, #0x2c]
100010a20: 7100143f    	cmp	w1, #0x5
100010a24: 540001ca    	b.ge	0x100010a5c <_Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x9c>
100010a28: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010a2c: 910103ff    	add	sp, sp, #0x40
100010a30: b9401389    	ldr	w9, [x28, #0x10]
100010a34: 7100013f    	cmp	w9, #0x0
100010a38: 540004cd    	b.le	0x100010ad0 <_Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x110>
100010a3c: d65f03c0    	ret
100010a40: aa0203e0    	mov	x0, x2
100010a44: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010a48: 910103ff    	add	sp, sp, #0x40
100010a4c: b9401389    	ldr	w9, [x28, #0x10]
100010a50: 7100013f    	cmp	w9, #0x0
100010a54: 540003ed    	b.le	0x100010ad0 <_Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x110>
100010a58: d65f03c0    	ret
100010a5c: 321e03e9    	orr	w9, wzr, #0x4
100010a60: b9002be9    	str	w9, [sp, #0x28]
100010a64: f94013e3    	ldr	x3, [sp, #0x20]
100010a68: d503201f    	nop
100010a6c: d503201f    	nop
100010a70: aa0303fe    	mov	x30, x3
100010a74: d63f03c0    	blr	x30
100010a78: f94013fe    	ldr	x30, [sp, #0x20]
100010a7c: d63f03c0    	blr	x30
100010a80: f94013fe    	ldr	x30, [sp, #0x20]
100010a84: d63f03c0    	blr	x30
100010a88: f94013fe    	ldr	x30, [sp, #0x20]
100010a8c: d63f03c0    	blr	x30
100010a90: b9402be1    	ldr	w1, [sp, #0x28]
100010a94: 11001022    	add	w2, w1, #0x4
100010a98: b9402fe3    	ldr	w3, [sp, #0x2c]
100010a9c: 6b03005f    	cmp	w2, w3
100010aa0: 54fffc4a    	b.ge	0x100010a28 <_Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x68>
100010aa4: b9401389    	ldr	w9, [x28, #0x10]
100010aa8: 34000089    	cbz	w9, 0x100010ab8 <_Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0xf8>
100010aac: b9002be2    	str	w2, [sp, #0x28]
100010ab0: f94013e3    	ldr	x3, [sp, #0x20]
100010ab4: 17ffffef    	b	0x100010a70 <_Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0xb0>
100010ab8: f94013e4    	ldr	x4, [sp, #0x20]
100010abc: 9405eca1    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010ac0: b9002be2    	str	w2, [sp, #0x28]
100010ac4: aa0403e3    	mov	x3, x4
100010ac8: 17ffffea    	b	0x100010a70 <_Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0xb0>
100010acc: 14029991    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010ad0: 1405ec9c    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010ad4: cccccccc    	<unknown>
100010ad8: cccccccc    	<unknown>
100010adc: cccccccc    	<unknown>

# svm: _Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB
0000000100010ae0 <_Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB>:
100010ae0: d10103e9    	sub	x9, sp, #0x40
100010ae4: f940078a    	ldr	x10, [x28, #0x8]
100010ae8: eb0a013f    	cmp	x9, x10
100010aec: 54000649    	b.ls	0x100010bb4 <_Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0xd4>
100010af0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010af4: 9100013f    	mov	sp, x9
100010af8: b9002fe1    	str	w1, [sp, #0x2c]
100010afc: 7100043f    	cmp	w1, #0x1
100010b00: 5400020b    	b.lt	0x100010b40 <_Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x60>
100010b04: 90002083    	adrp	x3, 0x100420000 <___data>
100010b08: f9416463    	ldr	x3, [x3, #0x2c8]
100010b0c: f90013e3    	str	x3, [sp, #0x20]
100010b10: aa0203e0    	mov	x0, x2
100010b14: aa0303fe    	mov	x30, x3
100010b18: d63f03c0    	blr	x30
100010b1c: b9402fe1    	ldr	w1, [sp, #0x2c]
100010b20: 7104043f    	cmp	w1, #0x101
100010b24: 540001ca    	b.ge	0x100010b5c <_Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x7c>
100010b28: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010b2c: 910103ff    	add	sp, sp, #0x40
100010b30: b9401389    	ldr	w9, [x28, #0x10]
100010b34: 7100013f    	cmp	w9, #0x0
100010b38: 5400040d    	b.le	0x100010bb8 <_Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0xd8>
100010b3c: d65f03c0    	ret
100010b40: aa0203e0    	mov	x0, x2
100010b44: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010b48: 910103ff    	add	sp, sp, #0x40
100010b4c: b9401389    	ldr	w9, [x28, #0x10]
100010b50: 7100013f    	cmp	w9, #0x0
100010b54: 5400032d    	b.le	0x100010bb8 <_Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0xd8>
100010b58: d65f03c0    	ret
100010b5c: 321803e9    	orr	w9, wzr, #0x100
100010b60: b9002be9    	str	w9, [sp, #0x28]
100010b64: f94013e3    	ldr	x3, [sp, #0x20]
100010b68: d503201f    	nop
100010b6c: d503201f    	nop
100010b70: aa0303fe    	mov	x30, x3
100010b74: d63f03c0    	blr	x30
100010b78: b9402be1    	ldr	w1, [sp, #0x28]
100010b7c: 11040022    	add	w2, w1, #0x100
100010b80: b9402fe3    	ldr	w3, [sp, #0x2c]
100010b84: 6b03005f    	cmp	w2, w3
100010b88: 54fffd0a    	b.ge	0x100010b28 <_Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x48>
100010b8c: b9401389    	ldr	w9, [x28, #0x10]
100010b90: 34000089    	cbz	w9, 0x100010ba0 <_Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0xc0>
100010b94: b9002be2    	str	w2, [sp, #0x28]
100010b98: f94013e3    	ldr	x3, [sp, #0x20]
100010b9c: 17fffff5    	b	0x100010b70 <_Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x90>
100010ba0: f94013e4    	ldr	x4, [sp, #0x20]
100010ba4: 9405ec67    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010ba8: b9002be2    	str	w2, [sp, #0x28]
100010bac: aa0403e3    	mov	x3, x4
100010bb0: 17fffff0    	b	0x100010b70 <_Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x90>
100010bb4: 14029957    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010bb8: 1405ec62    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010bbc: cccccccc    	<unknown>

# svm: _Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G
0000000100010bc0 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G>:
100010bc0: d10143e9    	sub	x9, sp, #0x50
100010bc4: f940078a    	ldr	x10, [x28, #0x8]
100010bc8: eb0a013f    	cmp	x9, x10
100010bcc: 54000bc9    	b.ls	0x100010d44 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x184>
100010bd0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010bd4: 9100013f    	mov	sp, x9
100010bd8: b90033e1    	str	w1, [sp, #0x30]
100010bdc: 7100043f    	cmp	w1, #0x1
100010be0: 320003e0    	orr	w0, wzr, #0x1
100010be4: 1a80d020    	csel	w0, w1, w0, le
100010be8: 7100041f    	cmp	w0, #0x1
100010bec: 54000a6b    	b.lt	0x100010d38 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x178>
100010bf0: aa0203e0    	mov	x0, x2
100010bf4: 9400050b    	bl	0x100012020 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010bf8: 320003e2    	orr	w2, wzr, #0x1
100010bfc: b94033e1    	ldr	w1, [sp, #0x30]
100010c00: 52800083    	mov	w3, #0x4                ; =4
100010c04: 72b00003    	movk	w3, #0x8000, lsl #16
100010c08: 320103e4    	orr	w4, wzr, #0x80000000
100010c0c: 51001025    	sub	w5, w1, #0x4
100010c10: 6b03003f    	cmp	w1, w3
100010c14: 1a85b083    	csel	w3, w4, w5, lt
100010c18: b9002be3    	str	w3, [sp, #0x28]
100010c1c: 11000c64    	add	w4, w3, #0x3
100010c20: b90027e4    	str	w4, [sp, #0x24]
100010c24: 2a0203e1    	mov	w1, w2
100010c28: d503201f    	nop
100010c2c: d503201f    	nop
100010c30: 6b03003f    	cmp	w1, w3
100010c34: 5400062a    	b.ge	0x100010cf8 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x138>
100010c38: 4b010085    	sub	w5, w4, w1
100010c3c: 53027ca6    	lsr	w6, w5, #2
100010c40: 121e74a5    	and	w5, w5, #0xfffffffc
100010c44: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100010c48: 321203e6    	orr	w6, wzr, #0x4000
100010c4c: 1a8630a5    	csel	w5, w5, w6, lo
100010c50: 710004bf    	cmp	w5, #0x1
100010c54: 540004ab    	b.lt	0x100010ce8 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x128>
100010c58: b90037e1    	str	w1, [sp, #0x34]
100010c5c: b9003fe5    	str	w5, [sp, #0x3c]
100010c60: 2a1f03e2    	mov	w2, wzr
100010c64: d503201f    	nop
100010c68: d503201f    	nop
100010c6c: d503201f    	nop
100010c70: 0b010047    	add	w7, w2, w1
100010c74: b90023e7    	str	w7, [sp, #0x20]
100010c78: b9002fe2    	str	w2, [sp, #0x2c]
100010c7c: 940004e9    	bl	0x100012020 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010c80: b94023e7    	ldr	w7, [sp, #0x20]
100010c84: 110004e1    	add	w1, w7, #0x1
100010c88: b9003be1    	str	w1, [sp, #0x38]
100010c8c: 940004e5    	bl	0x100012020 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010c90: b94023e7    	ldr	w7, [sp, #0x20]
100010c94: 110008e1    	add	w1, w7, #0x2
100010c98: b9003be1    	str	w1, [sp, #0x38]
100010c9c: 940004e1    	bl	0x100012020 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010ca0: b94023e7    	ldr	w7, [sp, #0x20]
100010ca4: 11000ce1    	add	w1, w7, #0x3
100010ca8: b90023e1    	str	w1, [sp, #0x20]
100010cac: 940004dd    	bl	0x100012020 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010cb0: b9402fe2    	ldr	w2, [sp, #0x2c]
100010cb4: 11001042    	add	w2, w2, #0x4
100010cb8: b9403fe5    	ldr	w5, [sp, #0x3c]
100010cbc: 6b05005f    	cmp	w2, w5
100010cc0: b94037e1    	ldr	w1, [sp, #0x34]
100010cc4: 54fffd6b    	b.lt	0x100010c70 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0xb0>
100010cc8: 0b010041    	add	w1, w2, w1
100010ccc: b9401389    	ldr	w9, [x28, #0x10]
100010cd0: 34000069    	cbz	w9, 0x100010cdc <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x11c>
100010cd4: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100010cd8: 17ffffd6    	b	0x100010c30 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x70>
100010cdc: 9405ec19    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010ce0: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100010ce4: 17ffffd3    	b	0x100010c30 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x70>
100010ce8: b9401389    	ldr	w9, [x28, #0x10]
100010cec: 35fffa29    	cbnz	w9, 0x100010c30 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x70>
100010cf0: 9405ec14    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010cf4: 17ffffcf    	b	0x100010c30 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x70>
100010cf8: 2a0103e2    	mov	w2, w1
100010cfc: 14000006    	b	0x100010d14 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x154>
100010d00: b9002fe2    	str	w2, [sp, #0x2c]
100010d04: 940004c7    	bl	0x100012020 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>
100010d08: b9402fe2    	ldr	w2, [sp, #0x2c]
100010d0c: 1100045d    	add	w29, w2, #0x1
100010d10: 2a1d03e2    	mov	w2, w29
100010d14: b94033e1    	ldr	w1, [sp, #0x30]
100010d18: 6b01005f    	cmp	w2, w1
100010d1c: 54ffff2b    	b.lt	0x100010d00 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x140>
100010d20: a9447bfd    	ldp	x29, x30, [sp, #0x40]
100010d24: 910143ff    	add	sp, sp, #0x50
100010d28: b9401389    	ldr	w9, [x28, #0x10]
100010d2c: 7100013f    	cmp	w9, #0x0
100010d30: 540000cd    	b.le	0x100010d48 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x188>
100010d34: d65f03c0    	ret
100010d38: aa0203e0    	mov	x0, x2
100010d3c: 2a1f03e2    	mov	w2, wzr
100010d40: 17ffffb0    	b	0x100010c00 <_Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x40>
100010d44: 140298f3    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010d48: 1405ebfe    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010d4c: cccccccc    	<unknown>

# svm: _Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7
0000000100010d50 <_Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7>:
100010d50: d10103e9    	sub	x9, sp, #0x40
100010d54: f940078a    	ldr	x10, [x28, #0x8]
100010d58: eb0a013f    	cmp	x9, x10
100010d5c: 54000689    	b.ls	0x100010e2c <_Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0xdc>
100010d60: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010d64: 9100013f    	mov	sp, x9
100010d68: b9002fe1    	str	w1, [sp, #0x2c]
100010d6c: 7100043f    	cmp	w1, #0x1
100010d70: 5400022b    	b.lt	0x100010db4 <_Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x64>
100010d74: 52873323    	mov	w3, #0x3999             ; =14745
100010d78: 72a000a3    	movk	w3, #0x5, lsl #16
100010d7c: 8b030f63    	add	x3, x27, x3, lsl #3
100010d80: f90013e3    	str	x3, [sp, #0x20]
100010d84: f9401460    	ldr	x0, [x3, #0x28]
100010d88: aa0203e1    	mov	x1, x2
100010d8c: 9400053d    	bl	0x100012280 <_Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK>
100010d90: b9402fe1    	ldr	w1, [sp, #0x2c]
100010d94: 7101043f    	cmp	w1, #0x41
100010d98: 540001ca    	b.ge	0x100010dd0 <_Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x80>
100010d9c: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010da0: 910103ff    	add	sp, sp, #0x40
100010da4: b9401389    	ldr	w9, [x28, #0x10]
100010da8: 7100013f    	cmp	w9, #0x0
100010dac: 5400042d    	b.le	0x100010e30 <_Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0xe0>
100010db0: d65f03c0    	ret
100010db4: aa0203e0    	mov	x0, x2
100010db8: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100010dbc: 910103ff    	add	sp, sp, #0x40
100010dc0: b9401389    	ldr	w9, [x28, #0x10]
100010dc4: 7100013f    	cmp	w9, #0x0
100010dc8: 5400034d    	b.le	0x100010e30 <_Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0xe0>
100010dcc: d65f03c0    	ret
100010dd0: 321a03e9    	orr	w9, wzr, #0x40
100010dd4: b9002be9    	str	w9, [sp, #0x28]
100010dd8: f94013e3    	ldr	x3, [sp, #0x20]
100010ddc: aa0003e1    	mov	x1, x0
100010de0: f9401460    	ldr	x0, [x3, #0x28]
100010de4: 94000527    	bl	0x100012280 <_Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK>
100010de8: b9402be1    	ldr	w1, [sp, #0x28]
100010dec: 11010022    	add	w2, w1, #0x40
100010df0: b9402fe3    	ldr	w3, [sp, #0x2c]
100010df4: 6b03005f    	cmp	w2, w3
100010df8: 54fffd2a    	b.ge	0x100010d9c <_Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x4c>
100010dfc: b9401389    	ldr	w9, [x28, #0x10]
100010e00: 340000a9    	cbz	w9, 0x100010e14 <_Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0xc4>
100010e04: aa0003e1    	mov	x1, x0
100010e08: b9002be2    	str	w2, [sp, #0x28]
100010e0c: f94013e3    	ldr	x3, [sp, #0x20]
100010e10: 17fffff4    	b	0x100010de0 <_Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x90>
100010e14: f94013e4    	ldr	x4, [sp, #0x20]
100010e18: 9405ebca    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010e1c: aa0003e1    	mov	x1, x0
100010e20: b9002be2    	str	w2, [sp, #0x28]
100010e24: aa0403e3    	mov	x3, x4
100010e28: 17ffffee    	b	0x100010de0 <_Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x90>
100010e2c: 140298b9    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010e30: 1405ebc4    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010e34: cccccccc    	<unknown>
100010e38: cccccccc    	<unknown>
100010e3c: cccccccc    	<unknown>

# svm: _Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI
0000000100010e40 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI>:
100010e40: d10203e9    	sub	x9, sp, #0x80
100010e44: f940078a    	ldr	x10, [x28, #0x8]
100010e48: eb0a013f    	cmp	x9, x10
100010e4c: 54002849    	b.ls	0x100011354 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x514>
100010e50: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100010e54: 9100013f    	mov	sp, x9
100010e58: b9400801    	ldr	w1, [x0, #0x8]
100010e5c: b9004be1    	str	w1, [sp, #0x48]
100010e60: b940041e    	ldr	w30, [x0, #0x4]
100010e64: 2a0103e2    	mov	w2, w1
100010e68: 8b020f62    	add	x2, x27, x2, lsl #3
100010e6c: f90023e2    	str	x2, [sp, #0x40]
100010e70: 340025de    	cbz	w30, 0x100011328 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4e8>
100010e74: 2a1e03fe    	mov	w30, w30
100010e78: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100010e7c: b94007de    	ldr	w30, [x30, #0x4]
100010e80: 3400259e    	cbz	w30, 0x100011330 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4f0>
100010e84: 2a1e03e0    	mov	w0, w30
100010e88: 8b000f60    	add	x0, x27, x0, lsl #3
100010e8c: d503201f    	nop
100010e90: 91004009    	add	x9, x0, #0x10
100010e94: 88dffd23    	ldar	w3, [x9]
100010e98: 340001e3    	cbz	w3, 0x100010ed4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
100010e9c: 51000464    	sub	w4, w3, #0x1
100010ea0: 9100401e    	add	x30, x0, #0x10
100010ea4: 885f7fc5    	ldxr	w5, [x30]
100010ea8: 6b0300bf    	cmp	w5, w3
100010eac: 1a85008a    	csel	w10, w4, w5, eq
100010eb0: 8809ffca    	stlxr	w9, w10, [x30]
100010eb4: 35ffff89    	cbnz	w9, 0x100010ea4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x64>
100010eb8: d5033bbf    	dmb	ish
100010ebc: 1a9f17fe    	cset	w30, eq
100010ec0: 35001b9e    	cbnz	w30, 0x100011230 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3f0>
100010ec4: b9401389    	ldr	w9, [x28, #0x10]
100010ec8: 35fffe49    	cbnz	w9, 0x100010e90 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x50>
100010ecc: 9405eb9d    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010ed0: 17fffff0    	b	0x100010e90 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x50>
100010ed4: 52873743    	mov	w3, #0x39ba             ; =14778
100010ed8: 72a000a3    	movk	w3, #0x5, lsl #16
100010edc: 8b030f63    	add	x3, x27, x3, lsl #3
100010ee0: f9000fe3    	str	x3, [sp, #0x18]
100010ee4: 52873844    	mov	w4, #0x39c2             ; =14786
100010ee8: 72a000a4    	movk	w4, #0x5, lsl #16
100010eec: 8b040f64    	add	x4, x27, x4, lsl #3
100010ef0: f90013e4    	str	x4, [sp, #0x20]
100010ef4: 528022e5    	mov	w5, #0x117              ; =279
100010ef8: 72a00025    	movk	w5, #0x1, lsl #16
100010efc: b90057e5    	str	w5, [sp, #0x54]
100010f00: 52989906    	mov	w6, #0xc4c8             ; =50376
100010f04: 72a00026    	movk	w6, #0x1, lsl #16
100010f08: 8b060f66    	add	x6, x27, x6, lsl #3
100010f0c: f9001fe6    	str	x6, [sp, #0x38]
100010f10: 52802667    	mov	w7, #0x133              ; =307
100010f14: 72a00027    	movk	w7, #0x1, lsl #16
100010f18: b90053e7    	str	w7, [sp, #0x50]
100010f1c: 528738a8    	mov	w8, #0x39c5             ; =14789
100010f20: 72a000a8    	movk	w8, #0x5, lsl #16
100010f24: b9004fe8    	str	w8, [sp, #0x4c]
100010f28: 528738eb    	mov	w11, #0x39c7            ; =14791
100010f2c: 72a000ab    	movk	w11, #0x5, lsl #16
100010f30: 8b0b0f6b    	add	x11, x27, x11, lsl #3
100010f34: f9001beb    	str	x11, [sp, #0x30]
100010f38: 5287302c    	mov	w12, #0x3981            ; =14721
100010f3c: 72a000ac    	movk	w12, #0x5, lsl #16
100010f40: 8b0c0f6c    	add	x12, x27, x12, lsl #3
100010f44: f90017ec    	str	x12, [sp, #0x28]
100010f48: b27003ed    	orr	x13, xzr, #0x10000
100010f4c: f9002fed    	str	x13, [sp, #0x58]
100010f50: 321003ee    	orr	w14, wzr, #0x10000
100010f54: b9006bee    	str	w14, [sp, #0x68]
100010f58: 52850c0f    	mov	w15, #0x2860            ; =10336
100010f5c: 72a0040f    	movk	w15, #0x20, lsl #16
100010f60: d2800110    	mov	x16, #0x8               ; =8
100010f64: f2a00030    	movk	x16, #0x1, lsl #16
100010f68: 2a1f03f1    	mov	w17, wzr
100010f6c: d503201f    	nop
100010f70: 7108023f    	cmp	w17, #0x200
100010f74: 54001c22    	b.hs	0x1000112f8 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4b8>
100010f78: a9438381    	ldp	x1, x0, [x28, #0x38]
100010f7c: 8b100002    	add	x2, x0, x16
100010f80: eb02003f    	cmp	x1, x2
100010f84: 54001123    	b.lo	0x1000111a8 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x368>
100010f88: f9002382    	str	x2, [x28, #0x40]
100010f8c: b900000f    	str	w15, [x0]
100010f90: b900040e    	str	w14, [x0, #0x4]
100010f94: 91002001    	add	x1, x0, #0x8
100010f98: aa0d03e2    	mov	x2, x13
100010f9c: b4000162    	cbz	x2, 0x100010fc8 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x188>
100010fa0: 361800a1    	tbz	w1, #0x3, 0x100010fb4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x174>
100010fa4: d1002042    	sub	x2, x2, #0x8
100010fa8: f800843f    	str	xzr, [x1], #0x8
100010fac: 14000002    	b	0x100010fb4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x174>
100010fb0: a8817c3f    	stp	xzr, xzr, [x1], #0x10
100010fb4: f1004042    	subs	x2, x2, #0x10
100010fb8: 54ffffca    	b.ge	0x100010fb0 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x170>
100010fbc: 91004042    	add	x2, x2, #0x10
100010fc0: 36180042    	tbz	w2, #0x3, 0x100010fc8 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x188>
100010fc4: f800843f    	str	xzr, [x1], #0x8
100010fc8: d5033abf    	dmb	ishst
100010fcc: cb1b0000    	sub	x0, x0, x27
100010fd0: d343fc00    	lsr	x0, x0, #3
100010fd4: 91028189    	add	x9, x12, #0xa0
100010fd8: 889ffd20    	stlr	w0, [x9]
100010fdc: b9400180    	ldr	w0, [x12]
100010fe0: 37080980    	tbnz	w0, #0x1, 0x100011110 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2d0>
100010fe4: 11000633    	add	w19, w17, #0x1
100010fe8: 72000e3f    	tst	w17, #0xf
100010fec: 54000060    	b.eq	0x100010ff8 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1b8>
100010ff0: 2a1303f1    	mov	w17, w19
100010ff4: 17ffffdf    	b	0x100010f70 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x130>
100010ff8: 91006169    	add	x9, x11, #0x18
100010ffc: 88dffd20    	ldar	w0, [x9]
100011000: 6b08001f    	cmp	w0, w8
100011004: 54000180    	b.eq	0x100011034 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1f4>
100011008: 34000f20    	cbz	w0, 0x1000111ec <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3ac>
10001100c: 2a0003e0    	mov	w0, w0
100011010: 8b000f60    	add	x0, x27, x0, lsl #3
100011014: b9400001    	ldr	w1, [x0]
100011018: 53057c21    	lsr	w1, w1, #5
10001101c: 6b07003f    	cmp	w1, w7
100011020: 540017e1    	b.ne	0x10001131c <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4dc>
100011024: 39401000    	ldrb	w0, [x0, #0x4]
100011028: 340000a0    	cbz	w0, 0x10001103c <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1fc>
10001102c: 2a1303f1    	mov	w17, w19
100011030: 17ffffd0    	b	0x100010f70 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x130>
100011034: aa0603e0    	mov	x0, x6
100011038: 17fffffb    	b	0x100011024 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1e4>
10001103c: b9402460    	ldr	w0, [x3, #0x24]
100011040: 340000e0    	cbz	w0, 0x10001105c <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x21c>
100011044: 2a0003e0    	mov	w0, w0
100011048: 8b000f60    	add	x0, x27, x0, lsl #3
10001104c: b9400000    	ldr	w0, [x0]
100011050: 53057c00    	lsr	w0, w0, #5
100011054: 6b05001f    	cmp	w0, w5
100011058: 54000740    	b.eq	0x100011140 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x300>
10001105c: b9006ff3    	str	w19, [sp, #0x6c]
100011060: b90067f1    	str	w17, [sp, #0x64]
100011064: aa0303e0    	mov	x0, x3
100011068: aa0403e1    	mov	x1, x4
10001106c: 320003e2    	orr	w2, wzr, #0x1
100011070: 9401e2a4    	bl	0x100089b00 <_GCImpl_collectWithoutAllocating_3JkbhdqsFF0bYizLQcyXX9>
100011074: 340006a0    	cbz	w0, 0x100011148 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x308>
100011078: f9400fe3    	ldr	x3, [sp, #0x18]
10001107c: b9402460    	ldr	w0, [x3, #0x24]
100011080: 340015e0    	cbz	w0, 0x10001133c <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4fc>
100011084: 2a0003e0    	mov	w0, w0
100011088: 8b000f60    	add	x0, x27, x0, lsl #3
10001108c: b940001e    	ldr	w30, [x0]
100011090: 53057fc1    	lsr	w1, w30, #5
100011094: 2a0103e1    	mov	w1, w1
100011098: 8b010f61    	add	x1, x27, x1, lsl #3
10001109c: b9400821    	ldr	w1, [x1, #0x8]
1000110a0: 12802262    	mov	w2, #-0x114             ; =-276
1000110a4: 0b212041    	add	w1, w2, w1, uxth
1000110a8: 7100303f    	cmp	w1, #0xc
1000110ac: 540014c2    	b.hs	0x100011344 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x504>
1000110b0: 53057fde    	lsr	w30, w30, #5
1000110b4: 2a1e03fe    	mov	w30, w30
1000110b8: 8b1e0f7e    	add	x30, x27, x30, lsl #3
1000110bc: f9404fde    	ldr	x30, [x30, #0x98]
1000110c0: b0000361    	adrp	x1, 0x10007e000 <_CollectionPolicies_getPolicyIndex_bP8jZLnaZoEk4AVvxkBVYD+0x2f0>
1000110c4: 91200021    	add	x1, x1, #0x800
1000110c8: cb1a0021    	sub	x1, x1, x26
1000110cc: eb0103df    	cmp	x30, x1
1000110d0: 54000bc0    	b.eq	0x100011248 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x408>
1000110d4: 8b1a03de    	add	x30, x30, x26
1000110d8: d63f03c0    	blr	x30
1000110dc: b9406ff1    	ldr	w17, [sp, #0x6c]
1000110e0: d2800110    	mov	x16, #0x8               ; =8
1000110e4: f2a00030    	movk	x16, #0x1, lsl #16
1000110e8: 52850c0f    	mov	w15, #0x2860            ; =10336
1000110ec: 72a0040f    	movk	w15, #0x20, lsl #16
1000110f0: b9406bee    	ldr	w14, [sp, #0x68]
1000110f4: f9402fed    	ldr	x13, [sp, #0x58]
1000110f8: a942afec    	ldp	x12, x11, [sp, #0x28]
1000110fc: 29499fe8    	ldp	w8, w7, [sp, #0x4c]
100011100: f9401fe6    	ldr	x6, [sp, #0x38]
100011104: b94057e5    	ldr	w5, [sp, #0x54]
100011108: a94193e3    	ldp	x3, x4, [sp, #0x18]
10001110c: 17ffff99    	b	0x100010f70 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x130>
100011110: aa0c03e1    	mov	x1, x12
100011114: 37000340    	tbnz	w0, #0x0, 0x10001117c <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x33c>
100011118: b26db3e0    	mov	x0, #-0x80000           ; =-524288
10001111c: 8a000020    	and	x0, x1, x0
100011120: 91210002    	add	x2, x0, #0x840
100011124: cb020021    	sub	x1, x1, x2
100011128: b27d0be2    	orr	x2, xzr, #0x38
10001112c: 8b412441    	add	x1, x2, x1, lsr #9
100011130: 38e16802    	ldrsb	w2, [x0, x1]
100011134: 34fff582    	cbz	w2, 0x100010fe4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1a4>
100011138: 3821681f    	strb	wzr, [x0, x1]
10001113c: 17ffffaa    	b	0x100010fe4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1a4>
100011140: 2a1303f1    	mov	w17, w19
100011144: 17ffff8b    	b	0x100010f70 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x130>
100011148: b9406ff1    	ldr	w17, [sp, #0x6c]
10001114c: d2800110    	mov	x16, #0x8               ; =8
100011150: f2a00030    	movk	x16, #0x1, lsl #16
100011154: 52850c0f    	mov	w15, #0x2860            ; =10336
100011158: 72a0040f    	movk	w15, #0x20, lsl #16
10001115c: b9406bee    	ldr	w14, [sp, #0x68]
100011160: f9402fed    	ldr	x13, [sp, #0x58]
100011164: a942afec    	ldp	x12, x11, [sp, #0x28]
100011168: 29499fe8    	ldp	w8, w7, [sp, #0x4c]
10001116c: f9401fe6    	ldr	x6, [sp, #0x38]
100011170: b94057e5    	ldr	w5, [sp, #0x54]
100011174: a94193e3    	ldp	x3, x4, [sp, #0x18]
100011178: 17ffff7e    	b	0x100010f70 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x130>
10001117c: f85f8020    	ldur	x0, [x1, #-0x8]
100011180: aa0c03e2    	mov	x2, x12
100011184: cb000020    	sub	x0, x1, x0
100011188: 91028181    	add	x1, x12, #0xa0
10001118c: cb020021    	sub	x1, x1, x2
100011190: b27c07e2    	orr	x2, xzr, #0x30
100011194: 8b412441    	add	x1, x2, x1, lsr #9
100011198: 38e16802    	ldrsb	w2, [x0, x1]
10001119c: 34fff242    	cbz	w2, 0x100010fe4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1a4>
1000111a0: 3821681f    	strb	wzr, [x0, x1]
1000111a4: 17ffff90    	b	0x100010fe4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1a4>
1000111a8: b90067f1    	str	w17, [sp, #0x64]
1000111ac: 92407de0    	and	x0, x15, #0xffffffff
1000111b0: b9406be1    	ldr	w1, [sp, #0x68]
1000111b4: 94026317    	bl	0x1000a9e10 <_GenScavengeAllocationSupport_slowNewArray_RH1677CN3I59XJMcBPW18F>
1000111b8: b94067f1    	ldr	w17, [sp, #0x64]
1000111bc: d2800110    	mov	x16, #0x8               ; =8
1000111c0: f2a00030    	movk	x16, #0x1, lsl #16
1000111c4: 52850c0f    	mov	w15, #0x2860            ; =10336
1000111c8: 72a0040f    	movk	w15, #0x20, lsl #16
1000111cc: b9406bee    	ldr	w14, [sp, #0x68]
1000111d0: f9402fed    	ldr	x13, [sp, #0x58]
1000111d4: a942afec    	ldp	x12, x11, [sp, #0x28]
1000111d8: 29499fe8    	ldp	w8, w7, [sp, #0x4c]
1000111dc: f9401fe6    	ldr	x6, [sp, #0x38]
1000111e0: b94057e5    	ldr	w5, [sp, #0x54]
1000111e4: a94193e3    	ldp	x3, x4, [sp, #0x18]
1000111e8: 17ffff79    	b	0x100010fcc <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x18c>
1000111ec: b90067f1    	str	w17, [sp, #0x64]
1000111f0: 94058438    	bl	0x1001722d0 <_ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
1000111f4: b9404be1    	ldr	w1, [sp, #0x48]
1000111f8: 34000a01    	cbz	w1, 0x100011338 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4f8>
1000111fc: f94023e2    	ldr	x2, [sp, #0x40]
100011200: cb1b0000    	sub	x0, x0, x27
100011204: d343fc00    	lsr	x0, x0, #3
100011208: 91001049    	add	x9, x2, #0x4
10001120c: 889ffd20    	stlr	w0, [x9]
100011210: b9400040    	ldr	w0, [x2]
100011214: 370801c0    	tbnz	w0, #0x1, 0x10001124c <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x40c>
100011218: a9477bfd    	ldp	x29, x30, [sp, #0x70]
10001121c: 910203ff    	add	sp, sp, #0x80
100011220: b9401389    	ldr	w9, [x28, #0x10]
100011224: 7100013f    	cmp	w9, #0x0
100011228: 5400098d    	b.le	0x100011358 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x518>
10001122c: d65f03c0    	ret
100011230: 7100047f    	cmp	w3, #0x1
100011234: 54ffe501    	b.ne	0x100010ed4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
100011238: 91002009    	add	x9, x0, #0x8
10001123c: 88dffd3e    	ldar	w30, [x9]
100011240: 34ffe4be    	cbz	w30, 0x100010ed4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
100011244: 1400000d    	b	0x100011278 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x438>
100011248: 9402ea62    	bl	0x1000cbbd0 <_OutOfMemoryUtil_heapSizeExceeded_tgNQlvtYRwAN6ucK4Y7ahE>
10001124c: b26db3fd    	mov	x29, #-0x80000          ; =-524288
100011250: f94023e2    	ldr	x2, [sp, #0x40]
100011254: 8a1d005d    	and	x29, x2, x29
100011258: 912103be    	add	x30, x29, #0x840
10001125c: cb1e005e    	sub	x30, x2, x30
100011260: b27d0be0    	orr	x0, xzr, #0x38
100011264: 8b5e241e    	add	x30, x0, x30, lsr #9
100011268: 38fe6ba0    	ldrsb	w0, [x29, x30]
10001126c: 34fffd60    	cbz	w0, 0x100011218 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3d8>
100011270: 383e6bbf    	strb	wzr, [x29, x30]
100011274: 17ffffe9    	b	0x100011218 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3d8>
100011278: 2a1e03fe    	mov	w30, w30
10001127c: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100011280: 910023c9    	add	x9, x30, #0x8
100011284: 88dffd3e    	ldar	w30, [x9]
100011288: 34ffe27e    	cbz	w30, 0x100010ed4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
10001128c: 2a1e03e0    	mov	w0, w30
100011290: 8b000f60    	add	x0, x27, x0, lsl #3
100011294: 91004009    	add	x9, x0, #0x10
100011298: 88dffd3e    	ldar	w30, [x9]
10001129c: 34ffe1de    	cbz	w30, 0x100010ed4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
1000112a0: 91004009    	add	x9, x0, #0x10
1000112a4: 88dffd23    	ldar	w3, [x9]
1000112a8: 121f787e    	and	w30, w3, #0xfffffffe
1000112ac: 91004004    	add	x4, x0, #0x10
1000112b0: 885f7c85    	ldxr	w5, [x4]
1000112b4: 6b0300bf    	cmp	w5, w3
1000112b8: 1a8503ca    	csel	w10, w30, w5, eq
1000112bc: 8809fc8a    	stlxr	w9, w10, [x4]
1000112c0: 35ffff89    	cbnz	w9, 0x1000112b0 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x470>
1000112c4: d5033bbf    	dmb	ish
1000112c8: 1a9f17fe    	cset	w30, eq
1000112cc: 350000be    	cbnz	w30, 0x1000112e0 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4a0>
1000112d0: b9401389    	ldr	w9, [x28, #0x10]
1000112d4: 35fffe69    	cbnz	w9, 0x1000112a0 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x460>
1000112d8: 9405ea9a    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000112dc: 17fffff1    	b	0x1000112a0 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x460>
1000112e0: b9400c00    	ldr	w0, [x0, #0xc]
1000112e4: 34ffdf80    	cbz	w0, 0x100010ed4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
1000112e8: 2a0003e0    	mov	w0, w0
1000112ec: 8b000f60    	add	x0, x27, x0, lsl #3
1000112f0: 9405e344    	bl	0x10018a000 <_PlatformThreads_unpark_FJj4Yhv2Ja3jndtYAdmYl0>
1000112f4: 17fffef8    	b	0x100010ed4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
1000112f8: a9477bfd    	ldp	x29, x30, [sp, #0x70]
1000112fc: 910203ff    	add	sp, sp, #0x80
100011300: b9401389    	ldr	w9, [x28, #0x10]
100011304: 7100013f    	cmp	w9, #0x0
100011308: 5400028d    	b.le	0x100011358 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x518>
10001130c: d65f03c0    	ret
100011310: 17ffffb9    	b	0x1000111f4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
100011314: 17ffffb8    	b	0x1000111f4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
100011318: 17ffffb7    	b	0x1000111f4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
10001131c: b90067f1    	str	w17, [sp, #0x64]
100011320: 94058224    	bl	0x100171bb0 <_ImplicitExceptions_createOptClassCastException_Ewnh8dGuUvIm5VBYxiDX2K>
100011324: 17ffffb4    	b	0x1000111f4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
100011328: 940583ea    	bl	0x1001722d0 <_ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
10001132c: 17ffffb2    	b	0x1000111f4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
100011330: 940583e8    	bl	0x1001722d0 <_ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
100011334: 17ffffb0    	b	0x1000111f4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
100011338: 94059f26    	bl	0x100178fd0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
10001133c: 940583e5    	bl	0x1001722d0 <_ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
100011340: 17ffffad    	b	0x1000111f4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
100011344: b94067f1    	ldr	w17, [sp, #0x64]
100011348: 940582fe    	bl	0x100171f40 <_ImplicitExceptions_createOptIncompatibleClassChangeError_ExDFiMUY3I5T00lGxxhlnL>
10001134c: 17ffffaa    	b	0x1000111f4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
100011350: 17ffffa9    	b	0x1000111f4 <_Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
100011354: 1402976f    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011358: 1405ea7a    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001135c: cccccccc    	<unknown>

# svm: _Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI
0000000100011360 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI>:
100011360: d10143e9    	sub	x9, sp, #0x50
100011364: f940078a    	ldr	x10, [x28, #0x8]
100011368: eb0a013f    	cmp	x9, x10
10001136c: 54000f09    	b.ls	0x10001154c <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1ec>
100011370: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011374: 9100013f    	mov	sp, x9
100011378: b90033e1    	str	w1, [sp, #0x30]
10001137c: 52873320    	mov	w0, #0x3999             ; =14745
100011380: 72a000a0    	movk	w0, #0x5, lsl #16
100011384: 8b000f60    	add	x0, x27, x0, lsl #3
100011388: f9401803    	ldr	x3, [x0, #0x30]
10001138c: f9000fe3    	str	x3, [sp, #0x18]
100011390: 7100043f    	cmp	w1, #0x1
100011394: 320003e0    	orr	w0, wzr, #0x1
100011398: 1a80d020    	csel	w0, w1, w0, le
10001139c: 7100041f    	cmp	w0, #0x1
1000113a0: 54000d0b    	b.lt	0x100011540 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1e0>
1000113a4: aa0203e0    	mov	x0, x2
1000113a8: aa0303fe    	mov	x30, x3
1000113ac: d63f03c0    	blr	x30
1000113b0: 320003fe    	orr	w30, wzr, #0x1
1000113b4: b94033e1    	ldr	w1, [sp, #0x30]
1000113b8: 52800082    	mov	w2, #0x4                ; =4
1000113bc: 72b00002    	movk	w2, #0x8000, lsl #16
1000113c0: 320103e3    	orr	w3, wzr, #0x80000000
1000113c4: 51001024    	sub	w4, w1, #0x4
1000113c8: 6b02003f    	cmp	w1, w2
1000113cc: 1a84b062    	csel	w2, w3, w4, lt
1000113d0: b9002be2    	str	w2, [sp, #0x28]
1000113d4: 11000c43    	add	w3, w2, #0x3
1000113d8: b90027e3    	str	w3, [sp, #0x24]
1000113dc: f9400fe4    	ldr	x4, [sp, #0x18]
1000113e0: 2a1e03e1    	mov	w1, w30
1000113e4: d503201f    	nop
1000113e8: d503201f    	nop
1000113ec: d503201f    	nop
1000113f0: 6b02003f    	cmp	w1, w2
1000113f4: 5400078a    	b.ge	0x1000114e4 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x184>
1000113f8: 4b01007e    	sub	w30, w3, w1
1000113fc: 53027fc6    	lsr	w6, w30, #2
100011400: 121e77de    	and	w30, w30, #0xfffffffc
100011404: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100011408: 321203e6    	orr	w6, wzr, #0x4000
10001140c: 1a8633c6    	csel	w6, w30, w6, lo
100011410: 710004df    	cmp	w6, #0x1
100011414: 5400060b    	b.lt	0x1000114d4 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x174>
100011418: b90037e1    	str	w1, [sp, #0x34]
10001141c: b9003fe6    	str	w6, [sp, #0x3c]
100011420: 2a1f03e2    	mov	w2, wzr
100011424: 14000008    	b	0x100011444 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0xe4>
100011428: d503201f    	nop
10001142c: d503201f    	nop
100011430: d503201f    	nop
100011434: d503201f    	nop
100011438: d503201f    	nop
10001143c: d503201f    	nop
100011440: f9400fe4    	ldr	x4, [sp, #0x18]
100011444: 0b010048    	add	w8, w2, w1
100011448: b90023e8    	str	w8, [sp, #0x20]
10001144c: aa0403fe    	mov	x30, x4
100011450: b9002fe2    	str	w2, [sp, #0x2c]
100011454: d63f03c0    	blr	x30
100011458: b94023e8    	ldr	w8, [sp, #0x20]
10001145c: 11000501    	add	w1, w8, #0x1
100011460: f9400ffe    	ldr	x30, [sp, #0x18]
100011464: b9003be1    	str	w1, [sp, #0x38]
100011468: d63f03c0    	blr	x30
10001146c: b94023e8    	ldr	w8, [sp, #0x20]
100011470: 11000901    	add	w1, w8, #0x2
100011474: f9400ffe    	ldr	x30, [sp, #0x18]
100011478: b9003be1    	str	w1, [sp, #0x38]
10001147c: d63f03c0    	blr	x30
100011480: b94023e8    	ldr	w8, [sp, #0x20]
100011484: 11000d01    	add	w1, w8, #0x3
100011488: f9400ffe    	ldr	x30, [sp, #0x18]
10001148c: b90023e1    	str	w1, [sp, #0x20]
100011490: d63f03c0    	blr	x30
100011494: b9402fe2    	ldr	w2, [sp, #0x2c]
100011498: 11001042    	add	w2, w2, #0x4
10001149c: b9403fe6    	ldr	w6, [sp, #0x3c]
1000114a0: 6b06005f    	cmp	w2, w6
1000114a4: b94037e1    	ldr	w1, [sp, #0x34]
1000114a8: 54fffccb    	b.lt	0x100011440 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0xe0>
1000114ac: 0b010041    	add	w1, w2, w1
1000114b0: b9401389    	ldr	w9, [x28, #0x10]
1000114b4: 34000089    	cbz	w9, 0x1000114c4 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x164>
1000114b8: 29448be3    	ldp	w3, w2, [sp, #0x24]
1000114bc: f9400fe4    	ldr	x4, [sp, #0x18]
1000114c0: 17ffffcc    	b	0x1000113f0 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x90>
1000114c4: 9405ea1f    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000114c8: 29448be3    	ldp	w3, w2, [sp, #0x24]
1000114cc: f9400fe4    	ldr	x4, [sp, #0x18]
1000114d0: 17ffffc8    	b	0x1000113f0 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x90>
1000114d4: b9401389    	ldr	w9, [x28, #0x10]
1000114d8: 35fff8c9    	cbnz	w9, 0x1000113f0 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x90>
1000114dc: 9405ea19    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000114e0: 17ffffc4    	b	0x1000113f0 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x90>
1000114e4: 2a0103e2    	mov	w2, w1
1000114e8: aa0403e3    	mov	x3, x4
1000114ec: 1400000c    	b	0x10001151c <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1bc>
1000114f0: d503201f    	nop
1000114f4: d503201f    	nop
1000114f8: d503201f    	nop
1000114fc: d503201f    	nop
100011500: b9002fe2    	str	w2, [sp, #0x2c]
100011504: aa0303fe    	mov	x30, x3
100011508: d63f03c0    	blr	x30
10001150c: b9402fe2    	ldr	w2, [sp, #0x2c]
100011510: 1100045d    	add	w29, w2, #0x1
100011514: 2a1d03e2    	mov	w2, w29
100011518: f9400fe3    	ldr	x3, [sp, #0x18]
10001151c: b94033e1    	ldr	w1, [sp, #0x30]
100011520: 6b01005f    	cmp	w2, w1
100011524: 54fffeeb    	b.lt	0x100011500 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1a0>
100011528: a9447bfd    	ldp	x29, x30, [sp, #0x40]
10001152c: 910143ff    	add	sp, sp, #0x50
100011530: b9401389    	ldr	w9, [x28, #0x10]
100011534: 7100013f    	cmp	w9, #0x0
100011538: 540000cd    	b.le	0x100011550 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1f0>
10001153c: d65f03c0    	ret
100011540: 2a1f03fe    	mov	w30, wzr
100011544: aa0203e0    	mov	x0, x2
100011548: 17ffff9c    	b	0x1000113b8 <_Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x58>
10001154c: 140296f1    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011550: 1405e9fc    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011554: cccccccc    	<unknown>
100011558: cccccccc    	<unknown>
10001155c: cccccccc    	<unknown>

# svm: _Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15
0000000100011560 <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15>:
100011560: d101c3e9    	sub	x9, sp, #0x70
100011564: f940078a    	ldr	x10, [x28, #0x8]
100011568: eb0a013f    	cmp	x9, x10
10001156c: 54000869    	b.ls	0x100011678 <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x118>
100011570: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011574: 9100013f    	mov	sp, x9
100011578: b90037e1    	str	w1, [sp, #0x34]
10001157c: f0002063    	adrp	x3, 0x100420000 <___data>
100011580: f9419863    	ldr	x3, [x3, #0x330]
100011584: f90017e3    	str	x3, [sp, #0x28]
100011588: aa0203e0    	mov	x0, x2
10001158c: 2a1f03e2    	mov	w2, wzr
100011590: 14000027    	b	0x10001162c <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xcc>
100011594: d503201f    	nop
100011598: d503201f    	nop
10001159c: d503201f    	nop
1000115a0: b90033e2    	str	w2, [sp, #0x30]
1000115a4: 9100e3ed    	add	x13, sp, #0x38
1000115a8: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
1000115ac: f9400f9e    	ldr	x30, [x28, #0x18]
1000115b0: f9000dbe    	str	x30, [x13, #0x18]
1000115b4: f9000f8d    	str	x13, [x28, #0x18]
1000115b8: aa0303fe    	mov	x30, x3
1000115bc: 10000029    	adr	x9, 0x1000115c0 <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x60>
1000115c0: f90005a9    	str	x9, [x13, #0x8]
1000115c4: 910003e9    	mov	x9, sp
1000115c8: f90009a9    	str	x9, [x13, #0x10]
1000115cc: 320007e9    	orr	w9, wzr, #0x3
1000115d0: 9100538a    	add	x10, x28, #0x14
1000115d4: 889ffd49    	stlr	w9, [x10]
1000115d8: d63f03c0    	blr	x30
1000115dc: 320007e1    	orr	w1, wzr, #0x3
1000115e0: 320003e2    	orr	w2, wzr, #0x1
1000115e4: 91005383    	add	x3, x28, #0x14
1000115e8: 885f7c64    	ldxr	w4, [x3]
1000115ec: 6b01009f    	cmp	w4, w1
1000115f0: 1a84004a    	csel	w10, w2, w4, eq
1000115f4: 8809fc6a    	stlxr	w9, w10, [x3]
1000115f8: 35ffff89    	cbnz	w9, 0x1000115e8 <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x88>
1000115fc: d5033bbf    	dmb	ish
100011600: 1a9f17e1    	cset	w1, eq
100011604: 340001a1    	cbz	w1, 0x100011638 <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xd8>
100011608: f9400f81    	ldr	x1, [x28, #0x18]
10001160c: f9400c21    	ldr	x1, [x1, #0x18]
100011610: f9000f81    	str	x1, [x28, #0x18]
100011614: b9402f81    	ldr	w1, [x28, #0x2c]
100011618: 350001c1    	cbnz	w1, 0x100011650 <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xf0>
10001161c: b94033e2    	ldr	w2, [sp, #0x30]
100011620: 11000442    	add	w2, w2, #0x1
100011624: f94017e3    	ldr	x3, [sp, #0x28]
100011628: b94037e1    	ldr	w1, [sp, #0x34]
10001162c: 6b01005f    	cmp	w2, w1
100011630: 54fffb8b    	b.lt	0x1000115a0 <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x40>
100011634: 1400000b    	b	0x100011660 <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x100>
100011638: f90013e0    	str	x0, [sp, #0x20]
10001163c: 320003e0    	orr	w0, wzr, #0x1
100011640: 320003e1    	orr	w1, wzr, #0x1
100011644: 9405ebfb    	bl	0x10018c630 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
100011648: f94013e0    	ldr	x0, [sp, #0x20]
10001164c: 17fffff4    	b	0x10001161c <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xbc>
100011650: f90013e0    	str	x0, [sp, #0x20]
100011654: 9405eb8b    	bl	0x10018c480 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
100011658: f94013e0    	ldr	x0, [sp, #0x20]
10001165c: 17fffff0    	b	0x10001161c <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xbc>
100011660: a9467bfd    	ldp	x29, x30, [sp, #0x60]
100011664: 9101c3ff    	add	sp, sp, #0x70
100011668: b9401389    	ldr	w9, [x28, #0x10]
10001166c: 7100013f    	cmp	w9, #0x0
100011670: 5400006d    	b.le	0x10001167c <_Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x11c>
100011674: d65f03c0    	ret
100011678: 140296a6    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
10001167c: 1405e9b1    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>

# svm: _Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH
0000000100011680 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH>:
100011680: d10143e9    	sub	x9, sp, #0x50
100011684: f940078a    	ldr	x10, [x28, #0x8]
100011688: eb0a013f    	cmp	x9, x10
10001168c: 54000e09    	b.ls	0x10001184c <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x1cc>
100011690: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011694: 9100013f    	mov	sp, x9
100011698: b90033e1    	str	w1, [sp, #0x30]
10001169c: f0002063    	adrp	x3, 0x100420000 <___data>
1000116a0: f940b863    	ldr	x3, [x3, #0x170]
1000116a4: f9000fe3    	str	x3, [sp, #0x18]
1000116a8: 7100043f    	cmp	w1, #0x1
1000116ac: 320003e0    	orr	w0, wzr, #0x1
1000116b0: 1a80d020    	csel	w0, w1, w0, le
1000116b4: 7100041f    	cmp	w0, #0x1
1000116b8: 54000c4b    	b.lt	0x100011840 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x1c0>
1000116bc: aa0203e0    	mov	x0, x2
1000116c0: aa0303fe    	mov	x30, x3
1000116c4: d63f03c0    	blr	x30
1000116c8: 320003fe    	orr	w30, wzr, #0x1
1000116cc: b94033e1    	ldr	w1, [sp, #0x30]
1000116d0: 52800082    	mov	w2, #0x4                ; =4
1000116d4: 72b00002    	movk	w2, #0x8000, lsl #16
1000116d8: 320103e3    	orr	w3, wzr, #0x80000000
1000116dc: 51001024    	sub	w4, w1, #0x4
1000116e0: 6b02003f    	cmp	w1, w2
1000116e4: 1a84b062    	csel	w2, w3, w4, lt
1000116e8: b9002be2    	str	w2, [sp, #0x28]
1000116ec: 11000c43    	add	w3, w2, #0x3
1000116f0: b90027e3    	str	w3, [sp, #0x24]
1000116f4: f9400fe4    	ldr	x4, [sp, #0x18]
1000116f8: 2a1e03e1    	mov	w1, w30
1000116fc: d503201f    	nop
100011700: 6b02003f    	cmp	w1, w2
100011704: 5400070a    	b.ge	0x1000117e4 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x164>
100011708: 4b01007e    	sub	w30, w3, w1
10001170c: 53027fc6    	lsr	w6, w30, #2
100011710: 121e77de    	and	w30, w30, #0xfffffffc
100011714: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100011718: 321203e6    	orr	w6, wzr, #0x4000
10001171c: 1a8633c6    	csel	w6, w30, w6, lo
100011720: 710004df    	cmp	w6, #0x1
100011724: 5400058b    	b.lt	0x1000117d4 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x154>
100011728: b90037e1    	str	w1, [sp, #0x34]
10001172c: b9003fe6    	str	w6, [sp, #0x3c]
100011730: 2a1f03e2    	mov	w2, wzr
100011734: 14000004    	b	0x100011744 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0xc4>
100011738: d503201f    	nop
10001173c: d503201f    	nop
100011740: f9400fe4    	ldr	x4, [sp, #0x18]
100011744: 0b010048    	add	w8, w2, w1
100011748: b90023e8    	str	w8, [sp, #0x20]
10001174c: aa0403fe    	mov	x30, x4
100011750: b9002fe2    	str	w2, [sp, #0x2c]
100011754: d63f03c0    	blr	x30
100011758: b94023e8    	ldr	w8, [sp, #0x20]
10001175c: 11000501    	add	w1, w8, #0x1
100011760: f9400ffe    	ldr	x30, [sp, #0x18]
100011764: b9003be1    	str	w1, [sp, #0x38]
100011768: d63f03c0    	blr	x30
10001176c: b94023e8    	ldr	w8, [sp, #0x20]
100011770: 11000901    	add	w1, w8, #0x2
100011774: f9400ffe    	ldr	x30, [sp, #0x18]
100011778: b9003be1    	str	w1, [sp, #0x38]
10001177c: d63f03c0    	blr	x30
100011780: b94023e8    	ldr	w8, [sp, #0x20]
100011784: 11000d01    	add	w1, w8, #0x3
100011788: f9400ffe    	ldr	x30, [sp, #0x18]
10001178c: b90023e1    	str	w1, [sp, #0x20]
100011790: d63f03c0    	blr	x30
100011794: b9402fe2    	ldr	w2, [sp, #0x2c]
100011798: 11001042    	add	w2, w2, #0x4
10001179c: b9403fe6    	ldr	w6, [sp, #0x3c]
1000117a0: 6b06005f    	cmp	w2, w6
1000117a4: b94037e1    	ldr	w1, [sp, #0x34]
1000117a8: 54fffccb    	b.lt	0x100011740 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0xc0>
1000117ac: 0b010041    	add	w1, w2, w1
1000117b0: b9401389    	ldr	w9, [x28, #0x10]
1000117b4: 34000089    	cbz	w9, 0x1000117c4 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x144>
1000117b8: 29448be3    	ldp	w3, w2, [sp, #0x24]
1000117bc: f9400fe4    	ldr	x4, [sp, #0x18]
1000117c0: 17ffffd0    	b	0x100011700 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x80>
1000117c4: 9405e95f    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000117c8: 29448be3    	ldp	w3, w2, [sp, #0x24]
1000117cc: f9400fe4    	ldr	x4, [sp, #0x18]
1000117d0: 17ffffcc    	b	0x100011700 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x80>
1000117d4: b9401389    	ldr	w9, [x28, #0x10]
1000117d8: 35fff949    	cbnz	w9, 0x100011700 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x80>
1000117dc: 9405e959    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000117e0: 17ffffc8    	b	0x100011700 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x80>
1000117e4: 2a0103e2    	mov	w2, w1
1000117e8: aa0403e3    	mov	x3, x4
1000117ec: 1400000c    	b	0x10001181c <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x19c>
1000117f0: d503201f    	nop
1000117f4: d503201f    	nop
1000117f8: d503201f    	nop
1000117fc: d503201f    	nop
100011800: b9002fe2    	str	w2, [sp, #0x2c]
100011804: aa0303fe    	mov	x30, x3
100011808: d63f03c0    	blr	x30
10001180c: b9402fe2    	ldr	w2, [sp, #0x2c]
100011810: 1100045d    	add	w29, w2, #0x1
100011814: 2a1d03e2    	mov	w2, w29
100011818: f9400fe3    	ldr	x3, [sp, #0x18]
10001181c: b94033e1    	ldr	w1, [sp, #0x30]
100011820: 6b01005f    	cmp	w2, w1
100011824: 54fffeeb    	b.lt	0x100011800 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x180>
100011828: a9447bfd    	ldp	x29, x30, [sp, #0x40]
10001182c: 910143ff    	add	sp, sp, #0x50
100011830: b9401389    	ldr	w9, [x28, #0x10]
100011834: 7100013f    	cmp	w9, #0x0
100011838: 540000cd    	b.le	0x100011850 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x1d0>
10001183c: d65f03c0    	ret
100011840: 2a1f03fe    	mov	w30, wzr
100011844: aa0203e0    	mov	x0, x2
100011848: 17ffffa2    	b	0x1000116d0 <_Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x50>
10001184c: 14029631    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011850: 1405e93c    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011854: cccccccc    	<unknown>
100011858: cccccccc    	<unknown>
10001185c: cccccccc    	<unknown>

# svm: _Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB
0000000100011860 <_Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB>:
100011860: d10103e9    	sub	x9, sp, #0x40
100011864: f940078a    	ldr	x10, [x28, #0x8]
100011868: eb0a013f    	cmp	x9, x10
10001186c: 540008c9    	b.ls	0x100011984 <_Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x124>
100011870: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011874: 9100013f    	mov	sp, x9
100011878: 7100043f    	cmp	w1, #0x1
10001187c: 5400080b    	b.lt	0x10001197c <_Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x11c>
100011880: b9002fe1    	str	w1, [sp, #0x2c]
100011884: 52873320    	mov	w0, #0x3999             ; =14745
100011888: 72a000a0    	movk	w0, #0x5, lsl #16
10001188c: 8b000f60    	add	x0, x27, x0, lsl #3
100011890: f9401803    	ldr	x3, [x0, #0x30]
100011894: f90013e3    	str	x3, [sp, #0x20]
100011898: 2a1f03e9    	mov	w9, wzr
10001189c: b9002be9    	str	w9, [sp, #0x28]
1000118a0: aa0203e0    	mov	x0, x2
1000118a4: aa0303fe    	mov	x30, x3
1000118a8: d63f03c0    	blr	x30
1000118ac: f94013fe    	ldr	x30, [sp, #0x20]
1000118b0: d63f03c0    	blr	x30
1000118b4: f94013fe    	ldr	x30, [sp, #0x20]
1000118b8: d63f03c0    	blr	x30
1000118bc: f94013fe    	ldr	x30, [sp, #0x20]
1000118c0: d63f03c0    	blr	x30
1000118c4: f94013fe    	ldr	x30, [sp, #0x20]
1000118c8: d63f03c0    	blr	x30
1000118cc: f94013fe    	ldr	x30, [sp, #0x20]
1000118d0: d63f03c0    	blr	x30
1000118d4: f94013fe    	ldr	x30, [sp, #0x20]
1000118d8: d63f03c0    	blr	x30
1000118dc: f94013fe    	ldr	x30, [sp, #0x20]
1000118e0: d63f03c0    	blr	x30
1000118e4: f94013fe    	ldr	x30, [sp, #0x20]
1000118e8: d63f03c0    	blr	x30
1000118ec: f94013fe    	ldr	x30, [sp, #0x20]
1000118f0: d63f03c0    	blr	x30
1000118f4: f94013fe    	ldr	x30, [sp, #0x20]
1000118f8: d63f03c0    	blr	x30
1000118fc: f94013fe    	ldr	x30, [sp, #0x20]
100011900: d63f03c0    	blr	x30
100011904: f94013fe    	ldr	x30, [sp, #0x20]
100011908: d63f03c0    	blr	x30
10001190c: f94013fe    	ldr	x30, [sp, #0x20]
100011910: d63f03c0    	blr	x30
100011914: f94013fe    	ldr	x30, [sp, #0x20]
100011918: d63f03c0    	blr	x30
10001191c: f94013fe    	ldr	x30, [sp, #0x20]
100011920: d63f03c0    	blr	x30
100011924: b9402bfd    	ldr	w29, [sp, #0x28]
100011928: 110043a1    	add	w1, w29, #0x10
10001192c: b9402fe2    	ldr	w2, [sp, #0x2c]
100011930: 6b02003f    	cmp	w1, w2
100011934: 5400018a    	b.ge	0x100011964 <_Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x104>
100011938: b9401389    	ldr	w9, [x28, #0x10]
10001193c: 340000a9    	cbz	w9, 0x100011950 <_Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0xf0>
100011940: b9002be1    	str	w1, [sp, #0x28]
100011944: f94013e3    	ldr	x3, [sp, #0x20]
100011948: aa0003e2    	mov	x2, x0
10001194c: 17ffffd5    	b	0x1000118a0 <_Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x40>
100011950: 9405e8fc    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011954: b9002be1    	str	w1, [sp, #0x28]
100011958: f94013e3    	ldr	x3, [sp, #0x20]
10001195c: aa0003e2    	mov	x2, x0
100011960: 17ffffd0    	b	0x1000118a0 <_Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x40>
100011964: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100011968: 910103ff    	add	sp, sp, #0x40
10001196c: b9401389    	ldr	w9, [x28, #0x10]
100011970: 7100013f    	cmp	w9, #0x0
100011974: 540000ad    	b.le	0x100011988 <_Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x128>
100011978: d65f03c0    	ret
10001197c: aa0203e0    	mov	x0, x2
100011980: 17fffff9    	b	0x100011964 <_Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x104>
100011984: 140295e3    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011988: 1405e8ee    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001198c: cccccccc    	<unknown>

# svm: _Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK
0000000100011990 <_Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK>:
100011990: d10103e9    	sub	x9, sp, #0x40
100011994: f940078a    	ldr	x10, [x28, #0x8]
100011998: eb0a013f    	cmp	x9, x10
10001199c: 540008c9    	b.ls	0x100011ab4 <_Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x124>
1000119a0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000119a4: 9100013f    	mov	sp, x9
1000119a8: 7100043f    	cmp	w1, #0x1
1000119ac: 5400080b    	b.lt	0x100011aac <_Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x11c>
1000119b0: b9002fe1    	str	w1, [sp, #0x2c]
1000119b4: 52873320    	mov	w0, #0x3999             ; =14745
1000119b8: 72a000a0    	movk	w0, #0x5, lsl #16
1000119bc: 8b000f60    	add	x0, x27, x0, lsl #3
1000119c0: f9401803    	ldr	x3, [x0, #0x30]
1000119c4: f90013e3    	str	x3, [sp, #0x20]
1000119c8: 2a1f03e9    	mov	w9, wzr
1000119cc: b9002be9    	str	w9, [sp, #0x28]
1000119d0: aa0203e0    	mov	x0, x2
1000119d4: aa0303fe    	mov	x30, x3
1000119d8: d63f03c0    	blr	x30
1000119dc: f94013fe    	ldr	x30, [sp, #0x20]
1000119e0: d63f03c0    	blr	x30
1000119e4: f94013fe    	ldr	x30, [sp, #0x20]
1000119e8: d63f03c0    	blr	x30
1000119ec: f94013fe    	ldr	x30, [sp, #0x20]
1000119f0: d63f03c0    	blr	x30
1000119f4: f94013fe    	ldr	x30, [sp, #0x20]
1000119f8: d63f03c0    	blr	x30
1000119fc: f94013fe    	ldr	x30, [sp, #0x20]
100011a00: d63f03c0    	blr	x30
100011a04: f94013fe    	ldr	x30, [sp, #0x20]
100011a08: d63f03c0    	blr	x30
100011a0c: f94013fe    	ldr	x30, [sp, #0x20]
100011a10: d63f03c0    	blr	x30
100011a14: f94013fe    	ldr	x30, [sp, #0x20]
100011a18: d63f03c0    	blr	x30
100011a1c: f94013fe    	ldr	x30, [sp, #0x20]
100011a20: d63f03c0    	blr	x30
100011a24: f94013fe    	ldr	x30, [sp, #0x20]
100011a28: d63f03c0    	blr	x30
100011a2c: f94013fe    	ldr	x30, [sp, #0x20]
100011a30: d63f03c0    	blr	x30
100011a34: f94013fe    	ldr	x30, [sp, #0x20]
100011a38: d63f03c0    	blr	x30
100011a3c: f94013fe    	ldr	x30, [sp, #0x20]
100011a40: d63f03c0    	blr	x30
100011a44: f94013fe    	ldr	x30, [sp, #0x20]
100011a48: d63f03c0    	blr	x30
100011a4c: f94013fe    	ldr	x30, [sp, #0x20]
100011a50: d63f03c0    	blr	x30
100011a54: b9402bfd    	ldr	w29, [sp, #0x28]
100011a58: 110043a1    	add	w1, w29, #0x10
100011a5c: b9402fe2    	ldr	w2, [sp, #0x2c]
100011a60: 6b02003f    	cmp	w1, w2
100011a64: 5400018a    	b.ge	0x100011a94 <_Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x104>
100011a68: b9401389    	ldr	w9, [x28, #0x10]
100011a6c: 340000a9    	cbz	w9, 0x100011a80 <_Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0xf0>
100011a70: b9002be1    	str	w1, [sp, #0x28]
100011a74: f94013e3    	ldr	x3, [sp, #0x20]
100011a78: aa0003e2    	mov	x2, x0
100011a7c: 17ffffd5    	b	0x1000119d0 <_Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x40>
100011a80: 9405e8b0    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011a84: b9002be1    	str	w1, [sp, #0x28]
100011a88: f94013e3    	ldr	x3, [sp, #0x20]
100011a8c: aa0003e2    	mov	x2, x0
100011a90: 17ffffd0    	b	0x1000119d0 <_Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x40>
100011a94: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100011a98: 910103ff    	add	sp, sp, #0x40
100011a9c: b9401389    	ldr	w9, [x28, #0x10]
100011aa0: 7100013f    	cmp	w9, #0x0
100011aa4: 540000ad    	b.le	0x100011ab8 <_Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x128>
100011aa8: d65f03c0    	ret
100011aac: aa0203e0    	mov	x0, x2
100011ab0: 17fffff9    	b	0x100011a94 <_Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x104>
100011ab4: 14029597    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011ab8: 1405e8a2    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011abc: cccccccc    	<unknown>

# svm: _Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6
0000000100011ac0 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6>:
100011ac0: d10143e9    	sub	x9, sp, #0x50
100011ac4: f940078a    	ldr	x10, [x28, #0x8]
100011ac8: eb0a013f    	cmp	x9, x10
100011acc: 54000e09    	b.ls	0x100011c8c <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x1cc>
100011ad0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011ad4: 9100013f    	mov	sp, x9
100011ad8: b90033e1    	str	w1, [sp, #0x30]
100011adc: f0002063    	adrp	x3, 0x100420000 <___data>
100011ae0: f9442863    	ldr	x3, [x3, #0x850]
100011ae4: f9000fe3    	str	x3, [sp, #0x18]
100011ae8: 7100043f    	cmp	w1, #0x1
100011aec: 320003e0    	orr	w0, wzr, #0x1
100011af0: 1a80d020    	csel	w0, w1, w0, le
100011af4: 7100041f    	cmp	w0, #0x1
100011af8: 54000c4b    	b.lt	0x100011c80 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x1c0>
100011afc: aa0203e0    	mov	x0, x2
100011b00: aa0303fe    	mov	x30, x3
100011b04: d63f03c0    	blr	x30
100011b08: 320003fe    	orr	w30, wzr, #0x1
100011b0c: b94033e1    	ldr	w1, [sp, #0x30]
100011b10: 52800082    	mov	w2, #0x4                ; =4
100011b14: 72b00002    	movk	w2, #0x8000, lsl #16
100011b18: 320103e3    	orr	w3, wzr, #0x80000000
100011b1c: 51001024    	sub	w4, w1, #0x4
100011b20: 6b02003f    	cmp	w1, w2
100011b24: 1a84b062    	csel	w2, w3, w4, lt
100011b28: b9002be2    	str	w2, [sp, #0x28]
100011b2c: 11000c43    	add	w3, w2, #0x3
100011b30: b90027e3    	str	w3, [sp, #0x24]
100011b34: f9400fe4    	ldr	x4, [sp, #0x18]
100011b38: 2a1e03e1    	mov	w1, w30
100011b3c: d503201f    	nop
100011b40: 6b02003f    	cmp	w1, w2
100011b44: 5400070a    	b.ge	0x100011c24 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x164>
100011b48: 4b01007e    	sub	w30, w3, w1
100011b4c: 53027fc6    	lsr	w6, w30, #2
100011b50: 121e77de    	and	w30, w30, #0xfffffffc
100011b54: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100011b58: 321203e6    	orr	w6, wzr, #0x4000
100011b5c: 1a8633c6    	csel	w6, w30, w6, lo
100011b60: 710004df    	cmp	w6, #0x1
100011b64: 5400058b    	b.lt	0x100011c14 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x154>
100011b68: b90037e1    	str	w1, [sp, #0x34]
100011b6c: b9003fe6    	str	w6, [sp, #0x3c]
100011b70: 2a1f03e2    	mov	w2, wzr
100011b74: 14000004    	b	0x100011b84 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0xc4>
100011b78: d503201f    	nop
100011b7c: d503201f    	nop
100011b80: f9400fe4    	ldr	x4, [sp, #0x18]
100011b84: 0b010048    	add	w8, w2, w1
100011b88: b90023e8    	str	w8, [sp, #0x20]
100011b8c: aa0403fe    	mov	x30, x4
100011b90: b9002fe2    	str	w2, [sp, #0x2c]
100011b94: d63f03c0    	blr	x30
100011b98: b94023e8    	ldr	w8, [sp, #0x20]
100011b9c: 11000501    	add	w1, w8, #0x1
100011ba0: f9400ffe    	ldr	x30, [sp, #0x18]
100011ba4: b9003be1    	str	w1, [sp, #0x38]
100011ba8: d63f03c0    	blr	x30
100011bac: b94023e8    	ldr	w8, [sp, #0x20]
100011bb0: 11000901    	add	w1, w8, #0x2
100011bb4: f9400ffe    	ldr	x30, [sp, #0x18]
100011bb8: b9003be1    	str	w1, [sp, #0x38]
100011bbc: d63f03c0    	blr	x30
100011bc0: b94023e8    	ldr	w8, [sp, #0x20]
100011bc4: 11000d01    	add	w1, w8, #0x3
100011bc8: f9400ffe    	ldr	x30, [sp, #0x18]
100011bcc: b90023e1    	str	w1, [sp, #0x20]
100011bd0: d63f03c0    	blr	x30
100011bd4: b9402fe2    	ldr	w2, [sp, #0x2c]
100011bd8: 11001042    	add	w2, w2, #0x4
100011bdc: b9403fe6    	ldr	w6, [sp, #0x3c]
100011be0: 6b06005f    	cmp	w2, w6
100011be4: b94037e1    	ldr	w1, [sp, #0x34]
100011be8: 54fffccb    	b.lt	0x100011b80 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0xc0>
100011bec: 0b010041    	add	w1, w2, w1
100011bf0: b9401389    	ldr	w9, [x28, #0x10]
100011bf4: 34000089    	cbz	w9, 0x100011c04 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x144>
100011bf8: 29448be3    	ldp	w3, w2, [sp, #0x24]
100011bfc: f9400fe4    	ldr	x4, [sp, #0x18]
100011c00: 17ffffd0    	b	0x100011b40 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x80>
100011c04: 9405e84f    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011c08: 29448be3    	ldp	w3, w2, [sp, #0x24]
100011c0c: f9400fe4    	ldr	x4, [sp, #0x18]
100011c10: 17ffffcc    	b	0x100011b40 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x80>
100011c14: b9401389    	ldr	w9, [x28, #0x10]
100011c18: 35fff949    	cbnz	w9, 0x100011b40 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x80>
100011c1c: 9405e849    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011c20: 17ffffc8    	b	0x100011b40 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x80>
100011c24: 2a0103e2    	mov	w2, w1
100011c28: aa0403e3    	mov	x3, x4
100011c2c: 1400000c    	b	0x100011c5c <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x19c>
100011c30: d503201f    	nop
100011c34: d503201f    	nop
100011c38: d503201f    	nop
100011c3c: d503201f    	nop
100011c40: b9002fe2    	str	w2, [sp, #0x2c]
100011c44: aa0303fe    	mov	x30, x3
100011c48: d63f03c0    	blr	x30
100011c4c: b9402fe2    	ldr	w2, [sp, #0x2c]
100011c50: 1100045d    	add	w29, w2, #0x1
100011c54: 2a1d03e2    	mov	w2, w29
100011c58: f9400fe3    	ldr	x3, [sp, #0x18]
100011c5c: b94033e1    	ldr	w1, [sp, #0x30]
100011c60: 6b01005f    	cmp	w2, w1
100011c64: 54fffeeb    	b.lt	0x100011c40 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x180>
100011c68: a9447bfd    	ldp	x29, x30, [sp, #0x40]
100011c6c: 910143ff    	add	sp, sp, #0x50
100011c70: b9401389    	ldr	w9, [x28, #0x10]
100011c74: 7100013f    	cmp	w9, #0x0
100011c78: 540000cd    	b.le	0x100011c90 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x1d0>
100011c7c: d65f03c0    	ret
100011c80: 2a1f03fe    	mov	w30, wzr
100011c84: aa0203e0    	mov	x0, x2
100011c88: 17ffffa2    	b	0x100011b10 <_Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x50>
100011c8c: 14029521    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011c90: 1405e82c    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011c94: cccccccc    	<unknown>
100011c98: cccccccc    	<unknown>
100011c9c: cccccccc    	<unknown>

# svm: _Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2
0000000100011ca0 <_Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2>:
100011ca0: d10103e9    	sub	x9, sp, #0x40
100011ca4: f940078a    	ldr	x10, [x28, #0x8]
100011ca8: eb0a013f    	cmp	x9, x10
100011cac: 54000689    	b.ls	0x100011d7c <_Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0xdc>
100011cb0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011cb4: 9100013f    	mov	sp, x9
100011cb8: b9002fe1    	str	w1, [sp, #0x2c]
100011cbc: 7100043f    	cmp	w1, #0x1
100011cc0: 5400022b    	b.lt	0x100011d04 <_Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x64>
100011cc4: 52873323    	mov	w3, #0x3999             ; =14745
100011cc8: 72a000a3    	movk	w3, #0x5, lsl #16
100011ccc: 8b030f63    	add	x3, x27, x3, lsl #3
100011cd0: f90013e3    	str	x3, [sp, #0x20]
100011cd4: f9401460    	ldr	x0, [x3, #0x28]
100011cd8: aa0203e1    	mov	x1, x2
100011cdc: 940000d9    	bl	0x100012040 <_Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA>
100011ce0: b9402fe1    	ldr	w1, [sp, #0x2c]
100011ce4: 7100443f    	cmp	w1, #0x11
100011ce8: 540001ca    	b.ge	0x100011d20 <_Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x80>
100011cec: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100011cf0: 910103ff    	add	sp, sp, #0x40
100011cf4: b9401389    	ldr	w9, [x28, #0x10]
100011cf8: 7100013f    	cmp	w9, #0x0
100011cfc: 5400042d    	b.le	0x100011d80 <_Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0xe0>
100011d00: d65f03c0    	ret
100011d04: aa0203e0    	mov	x0, x2
100011d08: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100011d0c: 910103ff    	add	sp, sp, #0x40
100011d10: b9401389    	ldr	w9, [x28, #0x10]
100011d14: 7100013f    	cmp	w9, #0x0
100011d18: 5400034d    	b.le	0x100011d80 <_Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0xe0>
100011d1c: d65f03c0    	ret
100011d20: 321c03e9    	orr	w9, wzr, #0x10
100011d24: b9002be9    	str	w9, [sp, #0x28]
100011d28: f94013e3    	ldr	x3, [sp, #0x20]
100011d2c: aa0003e1    	mov	x1, x0
100011d30: f9401460    	ldr	x0, [x3, #0x28]
100011d34: 940000c3    	bl	0x100012040 <_Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA>
100011d38: b9402be1    	ldr	w1, [sp, #0x28]
100011d3c: 11004022    	add	w2, w1, #0x10
100011d40: b9402fe3    	ldr	w3, [sp, #0x2c]
100011d44: 6b03005f    	cmp	w2, w3
100011d48: 54fffd2a    	b.ge	0x100011cec <_Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x4c>
100011d4c: b9401389    	ldr	w9, [x28, #0x10]
100011d50: 340000a9    	cbz	w9, 0x100011d64 <_Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0xc4>
100011d54: aa0003e1    	mov	x1, x0
100011d58: b9002be2    	str	w2, [sp, #0x28]
100011d5c: f94013e3    	ldr	x3, [sp, #0x20]
100011d60: 17fffff4    	b	0x100011d30 <_Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x90>
100011d64: f94013e4    	ldr	x4, [sp, #0x20]
100011d68: 9405e7f6    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011d6c: aa0003e1    	mov	x1, x0
100011d70: b9002be2    	str	w2, [sp, #0x28]
100011d74: aa0403e3    	mov	x3, x4
100011d78: 17ffffee    	b	0x100011d30 <_Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x90>
100011d7c: 140294e5    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011d80: 1405e7f0    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011d84: cccccccc    	<unknown>
100011d88: cccccccc    	<unknown>
100011d8c: cccccccc    	<unknown>

# svm: _Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10
0000000100011d90 <_Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10>:
100011d90: d10103e9    	sub	x9, sp, #0x40
100011d94: f940078a    	ldr	x10, [x28, #0x8]
100011d98: eb0a013f    	cmp	x9, x10
100011d9c: 54000649    	b.ls	0x100011e64 <_Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0xd4>
100011da0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011da4: 9100013f    	mov	sp, x9
100011da8: b9002fe1    	str	w1, [sp, #0x2c]
100011dac: 7100043f    	cmp	w1, #0x1
100011db0: 5400020b    	b.lt	0x100011df0 <_Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x60>
100011db4: f0002063    	adrp	x3, 0x100420000 <___data>
100011db8: f9420c63    	ldr	x3, [x3, #0x418]
100011dbc: f90013e3    	str	x3, [sp, #0x20]
100011dc0: aa0203e0    	mov	x0, x2
100011dc4: aa0303fe    	mov	x30, x3
100011dc8: d63f03c0    	blr	x30
100011dcc: b9402fe1    	ldr	w1, [sp, #0x2c]
100011dd0: 7101043f    	cmp	w1, #0x41
100011dd4: 540001ca    	b.ge	0x100011e0c <_Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x7c>
100011dd8: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100011ddc: 910103ff    	add	sp, sp, #0x40
100011de0: b9401389    	ldr	w9, [x28, #0x10]
100011de4: 7100013f    	cmp	w9, #0x0
100011de8: 5400040d    	b.le	0x100011e68 <_Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0xd8>
100011dec: d65f03c0    	ret
100011df0: aa0203e0    	mov	x0, x2
100011df4: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100011df8: 910103ff    	add	sp, sp, #0x40
100011dfc: b9401389    	ldr	w9, [x28, #0x10]
100011e00: 7100013f    	cmp	w9, #0x0
100011e04: 5400032d    	b.le	0x100011e68 <_Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0xd8>
100011e08: d65f03c0    	ret
100011e0c: 321a03e9    	orr	w9, wzr, #0x40
100011e10: b9002be9    	str	w9, [sp, #0x28]
100011e14: f94013e3    	ldr	x3, [sp, #0x20]
100011e18: d503201f    	nop
100011e1c: d503201f    	nop
100011e20: aa0303fe    	mov	x30, x3
100011e24: d63f03c0    	blr	x30
100011e28: b9402be1    	ldr	w1, [sp, #0x28]
100011e2c: 11010022    	add	w2, w1, #0x40
100011e30: b9402fe3    	ldr	w3, [sp, #0x2c]
100011e34: 6b03005f    	cmp	w2, w3
100011e38: 54fffd0a    	b.ge	0x100011dd8 <_Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x48>
100011e3c: b9401389    	ldr	w9, [x28, #0x10]
100011e40: 34000089    	cbz	w9, 0x100011e50 <_Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0xc0>
100011e44: b9002be2    	str	w2, [sp, #0x28]
100011e48: f94013e3    	ldr	x3, [sp, #0x20]
100011e4c: 17fffff5    	b	0x100011e20 <_Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x90>
100011e50: f94013e4    	ldr	x4, [sp, #0x20]
100011e54: 9405e7bb    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011e58: b9002be2    	str	w2, [sp, #0x28]
100011e5c: aa0403e3    	mov	x3, x4
100011e60: 17fffff0    	b	0x100011e20 <_Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x90>
100011e64: 140294ab    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011e68: 1405e7b6    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011e6c: cccccccc    	<unknown>

# svm: _Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL
0000000100011e70 <_Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL>:
100011e70: d10103e9    	sub	x9, sp, #0x40
100011e74: f940078a    	ldr	x10, [x28, #0x8]
100011e78: eb0a013f    	cmp	x9, x10
100011e7c: 54000649    	b.ls	0x100011f44 <_Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0xd4>
100011e80: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011e84: 9100013f    	mov	sp, x9
100011e88: b9002fe1    	str	w1, [sp, #0x2c]
100011e8c: 7100043f    	cmp	w1, #0x1
100011e90: 5400020b    	b.lt	0x100011ed0 <_Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x60>
100011e94: f0002063    	adrp	x3, 0x100420000 <___data>
100011e98: f9438063    	ldr	x3, [x3, #0x700]
100011e9c: f90013e3    	str	x3, [sp, #0x20]
100011ea0: aa0203e0    	mov	x0, x2
100011ea4: aa0303fe    	mov	x30, x3
100011ea8: d63f03c0    	blr	x30
100011eac: b9402fe1    	ldr	w1, [sp, #0x2c]
100011eb0: 7100143f    	cmp	w1, #0x5
100011eb4: 540001ca    	b.ge	0x100011eec <_Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x7c>
100011eb8: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100011ebc: 910103ff    	add	sp, sp, #0x40
100011ec0: b9401389    	ldr	w9, [x28, #0x10]
100011ec4: 7100013f    	cmp	w9, #0x0
100011ec8: 5400040d    	b.le	0x100011f48 <_Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0xd8>
100011ecc: d65f03c0    	ret
100011ed0: aa0203e0    	mov	x0, x2
100011ed4: a9437bfd    	ldp	x29, x30, [sp, #0x30]
100011ed8: 910103ff    	add	sp, sp, #0x40
100011edc: b9401389    	ldr	w9, [x28, #0x10]
100011ee0: 7100013f    	cmp	w9, #0x0
100011ee4: 5400032d    	b.le	0x100011f48 <_Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0xd8>
100011ee8: d65f03c0    	ret
100011eec: 321e03e9    	orr	w9, wzr, #0x4
100011ef0: b9002be9    	str	w9, [sp, #0x28]
100011ef4: f94013e3    	ldr	x3, [sp, #0x20]
100011ef8: d503201f    	nop
100011efc: d503201f    	nop
100011f00: aa0303fe    	mov	x30, x3
100011f04: d63f03c0    	blr	x30
100011f08: b9402be1    	ldr	w1, [sp, #0x28]
100011f0c: 11001022    	add	w2, w1, #0x4
100011f10: b9402fe3    	ldr	w3, [sp, #0x2c]
100011f14: 6b03005f    	cmp	w2, w3
100011f18: 54fffd0a    	b.ge	0x100011eb8 <_Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x48>
100011f1c: b9401389    	ldr	w9, [x28, #0x10]
100011f20: 34000089    	cbz	w9, 0x100011f30 <_Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0xc0>
100011f24: b9002be2    	str	w2, [sp, #0x28]
100011f28: f94013e3    	ldr	x3, [sp, #0x20]
100011f2c: 17fffff5    	b	0x100011f00 <_Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x90>
100011f30: f94013e4    	ldr	x4, [sp, #0x20]
100011f34: 9405e783    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011f38: b9002be2    	str	w2, [sp, #0x28]
100011f3c: aa0403e3    	mov	x3, x4
100011f40: 17fffff0    	b	0x100011f00 <_Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x90>
100011f44: 14029473    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011f48: 1405e77e    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011f4c: cccccccc    	<unknown>

# svm: _Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D
0000000100011f50 <_Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D>:
100011f50: d100c3e9    	sub	x9, sp, #0x30
100011f54: f940078a    	ldr	x10, [x28, #0x8]
100011f58: eb0a013f    	cmp	x9, x10
100011f5c: 54000589    	b.ls	0x10001200c <_Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0xbc>
100011f60: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011f64: 9100013f    	mov	sp, x9
100011f68: d28cc1a0    	mov	x0, #0x660d             ; =26125
100011f6c: f2a00320    	movk	x0, #0x19, lsl #16
100011f70: d29e6bfd    	mov	x29, #0xf35f            ; =62303
100011f74: f2a78ddd    	movk	x29, #0x3c6e, lsl #16
100011f78: 2a1f03fe    	mov	w30, wzr
100011f7c: d503201f    	nop
100011f80: 6b0103df    	cmp	w30, w1
100011f84: 5400036a    	b.ge	0x100011ff0 <_Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0xa0>
100011f88: 4b1e0023    	sub	w3, w1, w30
100011f8c: 7140047f    	cmp	w3, #0x1, lsl #12       ; =0x1000
100011f90: 321403e4    	orr	w4, wzr, #0x1000
100011f94: 1a849063    	csel	w3, w3, w4, ls
100011f98: 7100007f    	cmp	w3, #0x0
100011f9c: 2a1f03e4    	mov	w4, wzr
100011fa0: 1a84a064    	csel	w4, w3, w4, ge
100011fa4: 7100047f    	cmp	w3, #0x1
100011fa8: 5400014b    	b.lt	0x100011fd0 <_Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x80>
100011fac: 92800003    	mov	x3, #-0x1               ; =-1
100011fb0: 8b244063    	add	x3, x3, w4, uxtw
100011fb4: aa1f03e5    	mov	x5, xzr
100011fb8: d503201f    	nop
100011fbc: d503201f    	nop
100011fc0: 9b007442    	madd	x2, x2, x0, x29
100011fc4: 910004a5    	add	x5, x5, #0x1
100011fc8: eb05007f    	cmp	x3, x5
100011fcc: 54ffffaa    	b.ge	0x100011fc0 <_Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x70>
100011fd0: 0b0403c3    	add	w3, w30, w4
100011fd4: b9401389    	ldr	w9, [x28, #0x10]
100011fd8: 34000069    	cbz	w9, 0x100011fe4 <_Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x94>
100011fdc: 2a0303fe    	mov	w30, w3
100011fe0: 17ffffe8    	b	0x100011f80 <_Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x30>
100011fe4: 9405e757    	bl	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011fe8: 2a0303fe    	mov	w30, w3
100011fec: 17ffffe5    	b	0x100011f80 <_Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x30>
100011ff0: aa0203e0    	mov	x0, x2
100011ff4: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100011ff8: 9100c3ff    	add	sp, sp, #0x30
100011ffc: b9401389    	ldr	w9, [x28, #0x10]
100012000: 7100013f    	cmp	w9, #0x0
100012004: 5400006d    	b.le	0x100012010 <_Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0xc0>
100012008: d65f03c0    	ret
10001200c: 14029441    	b	0x1000b7110 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100012010: 1405e74c    	b	0x10018bd40 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100012014: cccccccc    	<unknown>
100012018: cccccccc    	<unknown>
10001201c: cccccccc    	<unknown>

# svm: _Floor_identity_RGfigO1fJmId7IgDqMOKbC
0000000100012020 <_Floor_identity_RGfigO1fJmId7IgDqMOKbC>:
100012020: d10083ff    	sub	sp, sp, #0x20
100012024: a9017bfd    	stp	x29, x30, [sp, #0x10]
100012028: d000207e    	adrp	x30, 0x100420000 <___data>
10001202c: f9433fde    	ldr	x30, [x30, #0x678]
100012030: d63f03c0    	blr	x30
100012034: a9417bfd    	ldp	x29, x30, [sp, #0x10]
100012038: 910083ff    	add	sp, sp, #0x20
10001203c: d65f03c0    	ret

# svm: _Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA
0000000100012040 <_Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA>:
100012040: d100c3ff    	sub	sp, sp, #0x30
100012044: a9027bfd    	stp	x29, x30, [sp, #0x20]
100012048: aa0003e2    	mov	x2, x0
10001204c: aa0103e0    	mov	x0, x1
100012050: aa0203fe    	mov	x30, x2
100012054: f9000fe2    	str	x2, [sp, #0x18]
100012058: d63f03c0    	blr	x30
10001205c: f9400ffe    	ldr	x30, [sp, #0x18]
100012060: d63f03c0    	blr	x30
100012064: f9400ffe    	ldr	x30, [sp, #0x18]
100012068: d63f03c0    	blr	x30
10001206c: f9400ffe    	ldr	x30, [sp, #0x18]
100012070: d63f03c0    	blr	x30
100012074: f9400ffe    	ldr	x30, [sp, #0x18]
100012078: d63f03c0    	blr	x30
10001207c: f9400ffe    	ldr	x30, [sp, #0x18]
100012080: d63f03c0    	blr	x30
100012084: f9400ffe    	ldr	x30, [sp, #0x18]
100012088: d63f03c0    	blr	x30
10001208c: f9400ffe    	ldr	x30, [sp, #0x18]
100012090: d63f03c0    	blr	x30
100012094: f9400ffe    	ldr	x30, [sp, #0x18]
100012098: d63f03c0    	blr	x30
10001209c: f9400ffe    	ldr	x30, [sp, #0x18]
1000120a0: d63f03c0    	blr	x30
1000120a4: f9400ffe    	ldr	x30, [sp, #0x18]
1000120a8: d63f03c0    	blr	x30
1000120ac: f9400ffe    	ldr	x30, [sp, #0x18]
1000120b0: d63f03c0    	blr	x30
1000120b4: f9400ffe    	ldr	x30, [sp, #0x18]
1000120b8: d63f03c0    	blr	x30
1000120bc: f9400ffe    	ldr	x30, [sp, #0x18]
1000120c0: d63f03c0    	blr	x30
1000120c4: f9400ffe    	ldr	x30, [sp, #0x18]
1000120c8: d63f03c0    	blr	x30
1000120cc: f9400ffe    	ldr	x30, [sp, #0x18]
1000120d0: d63f03c0    	blr	x30
1000120d4: a9427bfd    	ldp	x29, x30, [sp, #0x20]
1000120d8: 9100c3ff    	add	sp, sp, #0x30
1000120dc: d65f03c0    	ret

# svm: _Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1
00000001000120e0 <_Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1>:
1000120e0: d10103ff    	sub	sp, sp, #0x40
1000120e4: a9037bfd    	stp	x29, x30, [sp, #0x30]
1000120e8: f9000fe0    	str	x0, [sp, #0x18]
1000120ec: aa0003e2    	mov	x2, x0
1000120f0: 2a1f03e0    	mov	w0, wzr
1000120f4: 14000009    	b	0x100012118 <_Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x38>
1000120f8: d503201f    	nop
1000120fc: d503201f    	nop
100012100: aa0103e0    	mov	x0, x1
100012104: aa0203fe    	mov	x30, x2
100012108: d63f03c0    	blr	x30
10001210c: aa0003e1    	mov	x1, x0
100012110: f9400fe2    	ldr	x2, [sp, #0x18]
100012114: 320003e0    	orr	w0, wzr, #0x1
100012118: 34ffff40    	cbz	w0, 0x100012100 <_Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x20>
10001211c: f90013e1    	str	x1, [sp, #0x20]
100012120: 2a0003e1    	mov	w1, w0
100012124: f94013e0    	ldr	x0, [sp, #0x20]
100012128: 1400002e    	b	0x1000121e0 <_Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x100>
10001212c: d503201f    	nop
100012130: d503201f    	nop
100012134: d503201f    	nop
100012138: d503201f    	nop
10001213c: d503201f    	nop
100012140: aa0203fe    	mov	x30, x2
100012144: d63f03c0    	blr	x30
100012148: b9402be1    	ldr	w1, [sp, #0x28]
10001214c: 11000422    	add	w2, w1, #0x1
100012150: f9400ffe    	ldr	x30, [sp, #0x18]
100012154: b9002fe2    	str	w2, [sp, #0x2c]
100012158: d63f03c0    	blr	x30
10001215c: b9402be1    	ldr	w1, [sp, #0x28]
100012160: 11000822    	add	w2, w1, #0x2
100012164: f9400ffe    	ldr	x30, [sp, #0x18]
100012168: b9002fe2    	str	w2, [sp, #0x2c]
10001216c: d63f03c0    	blr	x30
100012170: b9402be1    	ldr	w1, [sp, #0x28]
100012174: 11000c22    	add	w2, w1, #0x3
100012178: f9400ffe    	ldr	x30, [sp, #0x18]
10001217c: b9002fe2    	str	w2, [sp, #0x2c]
100012180: d63f03c0    	blr	x30
100012184: b9402be1    	ldr	w1, [sp, #0x28]
100012188: 11001022    	add	w2, w1, #0x4
10001218c: f9400ffe    	ldr	x30, [sp, #0x18]
100012190: b9002fe2    	str	w2, [sp, #0x2c]
100012194: d63f03c0    	blr	x30
100012198: b9402be1    	ldr	w1, [sp, #0x28]
10001219c: 11001422    	add	w2, w1, #0x5
1000121a0: f9400ffe    	ldr	x30, [sp, #0x18]
1000121a4: b9002fe2    	str	w2, [sp, #0x2c]
1000121a8: d63f03c0    	blr	x30
1000121ac: b9402be1    	ldr	w1, [sp, #0x28]
1000121b0: 11001822    	add	w2, w1, #0x6
1000121b4: f9400ffe    	ldr	x30, [sp, #0x18]
1000121b8: b9002fe2    	str	w2, [sp, #0x2c]
1000121bc: d63f03c0    	blr	x30
1000121c0: b9402be1    	ldr	w1, [sp, #0x28]
1000121c4: 11001c22    	add	w2, w1, #0x7
1000121c8: f9400ffe    	ldr	x30, [sp, #0x18]
1000121cc: b9002fe2    	str	w2, [sp, #0x2c]
1000121d0: d63f03c0    	blr	x30
1000121d4: b9402be1    	ldr	w1, [sp, #0x28]
1000121d8: 11002021    	add	w1, w1, #0x8
1000121dc: f9400fe2    	ldr	x2, [sp, #0x18]
1000121e0: b9002be1    	str	w1, [sp, #0x28]
1000121e4: 7103e03f    	cmp	w1, #0xf8
1000121e8: 54fffac3    	b.lo	0x100012140 <_Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x60>
1000121ec: 1400000d    	b	0x100012220 <_Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x140>
1000121f0: d503201f    	nop
1000121f4: d503201f    	nop
1000121f8: d503201f    	nop
1000121fc: d503201f    	nop
100012200: b9002be1    	str	w1, [sp, #0x28]
100012204: aa0203fe    	mov	x30, x2
100012208: d63f03c0    	blr	x30
10001220c: b9402be1    	ldr	w1, [sp, #0x28]
100012210: 1100043d    	add	w29, w1, #0x1
100012214: 2a1d03e1    	mov	w1, w29
100012218: f9400fe2    	ldr	x2, [sp, #0x18]
10001221c: d503201f    	nop
100012220: 7104003f    	cmp	w1, #0x100
100012224: 54fffee3    	b.lo	0x100012200 <_Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x120>
100012228: a9437bfd    	ldp	x29, x30, [sp, #0x30]
10001222c: 910103ff    	add	sp, sp, #0x40
100012230: d65f03c0    	ret
100012234: cccccccc    	<unknown>
100012238: cccccccc    	<unknown>
10001223c: cccccccc    	<unknown>

# svm: _Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI
0000000100012240 <_Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI>:
100012240: d100c3ff    	sub	sp, sp, #0x30
100012244: a9027bfd    	stp	x29, x30, [sp, #0x20]
100012248: aa0003e2    	mov	x2, x0
10001224c: aa0103e0    	mov	x0, x1
100012250: aa0203fe    	mov	x30, x2
100012254: f9000fe2    	str	x2, [sp, #0x18]
100012258: d63f03c0    	blr	x30
10001225c: f9400ffe    	ldr	x30, [sp, #0x18]
100012260: d63f03c0    	blr	x30
100012264: f9400ffe    	ldr	x30, [sp, #0x18]
100012268: d63f03c0    	blr	x30
10001226c: f9400ffe    	ldr	x30, [sp, #0x18]
100012270: d63f03c0    	blr	x30
100012274: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100012278: 9100c3ff    	add	sp, sp, #0x30
10001227c: d65f03c0    	ret

# svm: _Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK
0000000100012280 <_Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK>:
100012280: d100c3ff    	sub	sp, sp, #0x30
100012284: a9027bfd    	stp	x29, x30, [sp, #0x20]
100012288: aa0003e2    	mov	x2, x0
10001228c: aa0103e0    	mov	x0, x1
100012290: aa0203fe    	mov	x30, x2
100012294: f9000fe2    	str	x2, [sp, #0x18]
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
100012314: f9400ffe    	ldr	x30, [sp, #0x18]
100012318: d63f03c0    	blr	x30
10001231c: f9400ffe    	ldr	x30, [sp, #0x18]
100012320: d63f03c0    	blr	x30
100012324: f9400ffe    	ldr	x30, [sp, #0x18]
100012328: d63f03c0    	blr	x30
10001232c: f9400ffe    	ldr	x30, [sp, #0x18]
100012330: d63f03c0    	blr	x30
100012334: f9400ffe    	ldr	x30, [sp, #0x18]
100012338: d63f03c0    	blr	x30
10001233c: f9400ffe    	ldr	x30, [sp, #0x18]
100012340: d63f03c0    	blr	x30
100012344: f9400ffe    	ldr	x30, [sp, #0x18]
100012348: d63f03c0    	blr	x30
10001234c: f9400ffe    	ldr	x30, [sp, #0x18]
100012350: d63f03c0    	blr	x30
100012354: f9400ffe    	ldr	x30, [sp, #0x18]
100012358: d63f03c0    	blr	x30
10001235c: f9400ffe    	ldr	x30, [sp, #0x18]
100012360: d63f03c0    	blr	x30
100012364: f9400ffe    	ldr	x30, [sp, #0x18]
100012368: d63f03c0    	blr	x30
10001236c: f9400ffe    	ldr	x30, [sp, #0x18]
100012370: d63f03c0    	blr	x30
100012374: f9400ffe    	ldr	x30, [sp, #0x18]
100012378: d63f03c0    	blr	x30
10001237c: f9400ffe    	ldr	x30, [sp, #0x18]
100012380: d63f03c0    	blr	x30
100012384: f9400ffe    	ldr	x30, [sp, #0x18]
100012388: d63f03c0    	blr	x30
10001238c: f9400ffe    	ldr	x30, [sp, #0x18]
100012390: d63f03c0    	blr	x30
100012394: f9400ffe    	ldr	x30, [sp, #0x18]
100012398: d63f03c0    	blr	x30
10001239c: f9400ffe    	ldr	x30, [sp, #0x18]
1000123a0: d63f03c0    	blr	x30
1000123a4: f9400ffe    	ldr	x30, [sp, #0x18]
1000123a8: d63f03c0    	blr	x30
1000123ac: f9400ffe    	ldr	x30, [sp, #0x18]
1000123b0: d63f03c0    	blr	x30
1000123b4: f9400ffe    	ldr	x30, [sp, #0x18]
1000123b8: d63f03c0    	blr	x30
1000123bc: f9400ffe    	ldr	x30, [sp, #0x18]
1000123c0: d63f03c0    	blr	x30
1000123c4: f9400ffe    	ldr	x30, [sp, #0x18]
1000123c8: d63f03c0    	blr	x30
1000123cc: f9400ffe    	ldr	x30, [sp, #0x18]
1000123d0: d63f03c0    	blr	x30
1000123d4: f9400ffe    	ldr	x30, [sp, #0x18]
1000123d8: d63f03c0    	blr	x30
1000123dc: f9400ffe    	ldr	x30, [sp, #0x18]
1000123e0: d63f03c0    	blr	x30
1000123e4: f9400ffe    	ldr	x30, [sp, #0x18]
1000123e8: d63f03c0    	blr	x30
1000123ec: f9400ffe    	ldr	x30, [sp, #0x18]
1000123f0: d63f03c0    	blr	x30
1000123f4: f9400ffe    	ldr	x30, [sp, #0x18]
1000123f8: d63f03c0    	blr	x30
1000123fc: f9400ffe    	ldr	x30, [sp, #0x18]
100012400: d63f03c0    	blr	x30
100012404: f9400ffe    	ldr	x30, [sp, #0x18]
100012408: d63f03c0    	blr	x30
10001240c: f9400ffe    	ldr	x30, [sp, #0x18]
100012410: d63f03c0    	blr	x30
100012414: f9400ffe    	ldr	x30, [sp, #0x18]
100012418: d63f03c0    	blr	x30
10001241c: f9400ffe    	ldr	x30, [sp, #0x18]
100012420: d63f03c0    	blr	x30
100012424: f9400ffe    	ldr	x30, [sp, #0x18]
100012428: d63f03c0    	blr	x30
10001242c: f9400ffe    	ldr	x30, [sp, #0x18]
100012430: d63f03c0    	blr	x30
100012434: f9400ffe    	ldr	x30, [sp, #0x18]
100012438: d63f03c0    	blr	x30
10001243c: f9400ffe    	ldr	x30, [sp, #0x18]
100012440: d63f03c0    	blr	x30
100012444: f9400ffe    	ldr	x30, [sp, #0x18]
100012448: d63f03c0    	blr	x30
10001244c: f9400ffe    	ldr	x30, [sp, #0x18]
100012450: d63f03c0    	blr	x30
100012454: f9400ffe    	ldr	x30, [sp, #0x18]
100012458: d63f03c0    	blr	x30
10001245c: f9400ffe    	ldr	x30, [sp, #0x18]
100012460: d63f03c0    	blr	x30
100012464: f9400ffe    	ldr	x30, [sp, #0x18]
100012468: d63f03c0    	blr	x30
10001246c: f9400ffe    	ldr	x30, [sp, #0x18]
100012470: d63f03c0    	blr	x30
100012474: f9400ffe    	ldr	x30, [sp, #0x18]
100012478: d63f03c0    	blr	x30
10001247c: f9400ffe    	ldr	x30, [sp, #0x18]
100012480: d63f03c0    	blr	x30
100012484: f9400ffe    	ldr	x30, [sp, #0x18]
100012488: d63f03c0    	blr	x30
10001248c: f9400ffe    	ldr	x30, [sp, #0x18]
100012490: d63f03c0    	blr	x30
100012494: a9427bfd    	ldp	x29, x30, [sp, #0x20]
100012498: 9100c3ff    	add	sp, sp, #0x30
10001249c: d65f03c0    	ret

# svm: _Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G
00000001000129b0 <_Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>:
1000129b0: d10083ff    	sub	sp, sp, #0x20
1000129b4: a9017bfd    	stp	x29, x30, [sp, #0x10]
1000129b8: d000207e    	adrp	x30, 0x100420000 <___data>
1000129bc: f94327de    	ldr	x30, [x30, #0x648]
1000129c0: d63f03c0    	blr	x30
1000129c4: a9417bfd    	ldp	x29, x30, [sp, #0x10]
1000129c8: 910083ff    	add	sp, sp, #0x20
1000129cc: d65f03c0    	ret

