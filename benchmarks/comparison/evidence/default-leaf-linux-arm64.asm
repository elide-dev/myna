# native: direct
00000000000111d4 <direct>:
   111d4: a9be7bfd     	stp	x29, x30, [sp, #-0x20]!
   111d8: f9000bf3     	str	x19, [sp, #0x10]
   111dc: 910003fd     	mov	x29, sp
   111e0: 2a0003f3     	mov	w19, w0
   111e4: 7100041f     	cmp	w0, #0x1
   111e8: aa0103e0     	mov	x0, x1
   111ec: 5400008b     	b.lt	0x111fc <direct+0x28>
   111f0: 9400009d     	bl	0x11464 <myna_bench_leaf>
   111f4: 71000673     	subs	w19, w19, #0x1
   111f8: 54ffffc1     	b.ne	0x111f0 <direct+0x1c>
   111fc: f9400bf3     	ldr	x19, [sp, #0x10]
   11200: a8c27bfd     	ldp	x29, x30, [sp], #0x20
   11204: d65f03c0     	ret

# native: indirect
0000000000011208 <indirect>:
   11208: a9be7bfd     	stp	x29, x30, [sp, #-0x20]!
   1120c: a9014ff4     	stp	x20, x19, [sp, #0x10]
   11210: 910003fd     	mov	x29, sp
   11214: 90000108     	adrp	x8, 0x31000 <strtol+0x31000>
   11218: 2a0003f3     	mov	w19, w0
   1121c: 7100041f     	cmp	w0, #0x1
   11220: f944ed14     	ldr	x20, [x8, #0x9d8]
   11224: aa0103e0     	mov	x0, x1
   11228: 5400008b     	b.lt	0x11238 <indirect+0x30>
   1122c: d63f0280     	blr	x20
   11230: 71000673     	subs	w19, w19, #0x1
   11234: 54ffffc1     	b.ne	0x1122c <indirect+0x24>
   11238: a9414ff4     	ldp	x20, x19, [sp, #0x10]
   1123c: a8c27bfd     	ldp	x29, x30, [sp], #0x20
   11240: d65f03c0     	ret

# native: identity_direct
0000000000011334 <identity_direct>:
   11334: a9be7bfd     	stp	x29, x30, [sp, #-0x20]!
   11338: f9000bf3     	str	x19, [sp, #0x10]
   1133c: 910003fd     	mov	x29, sp
   11340: 2a0003f3     	mov	w19, w0
   11344: 7100041f     	cmp	w0, #0x1
   11348: aa0103e0     	mov	x0, x1
   1134c: 5400008b     	b.lt	0x1135c <identity_direct+0x28>
   11350: 940000b5     	bl	0x11624 <myna_bench_identity>
   11354: 71000673     	subs	w19, w19, #0x1
   11358: 54ffffc1     	b.ne	0x11350 <identity_direct+0x1c>
   1135c: f9400bf3     	ldr	x19, [sp, #0x10]
   11360: a8c27bfd     	ldp	x29, x30, [sp], #0x20
   11364: d65f03c0     	ret

# native: identity_indirect
0000000000011368 <identity_indirect>:
   11368: a9be7bfd     	stp	x29, x30, [sp, #-0x20]!
   1136c: a9014ff4     	stp	x20, x19, [sp, #0x10]
   11370: 910003fd     	mov	x29, sp
   11374: 90000108     	adrp	x8, 0x31000 <strtol+0x31000>
   11378: 2a0003f3     	mov	w19, w0
   1137c: 7100041f     	cmp	w0, #0x1
   11380: f944f114     	ldr	x20, [x8, #0x9e0]
   11384: aa0103e0     	mov	x0, x1
   11388: 5400008b     	b.lt	0x11398 <identity_indirect+0x30>
   1138c: d63f0280     	blr	x20
   11390: 71000673     	subs	w19, w19, #0x1
   11394: 54ffffc1     	b.ne	0x1138c <identity_indirect+0x24>
   11398: a9414ff4     	ldp	x20, x19, [sp, #0x10]
   1139c: a8c27bfd     	ldp	x29, x30, [sp], #0x20
   113a0: d65f03c0     	ret

# native: identity_direct_unrolled4
00000000000113a4 <identity_direct_unrolled4>:
   113a4: a9be7bfd     	stp	x29, x30, [sp, #-0x20]!
   113a8: a9014ff4     	stp	x20, x19, [sp, #0x10]
   113ac: 910003fd     	mov	x29, sp
   113b0: 2a0003f3     	mov	w19, w0
   113b4: 7100041f     	cmp	w0, #0x1
   113b8: aa0103e0     	mov	x0, x1
   113bc: 5400012b     	b.lt	0x113e0 <identity_direct_unrolled4+0x3c>
   113c0: 2a1f03f4     	mov	w20, wzr
   113c4: 94000098     	bl	0x11624 <myna_bench_identity>
   113c8: 94000097     	bl	0x11624 <myna_bench_identity>
   113cc: 94000096     	bl	0x11624 <myna_bench_identity>
   113d0: 94000095     	bl	0x11624 <myna_bench_identity>
   113d4: 11001294     	add	w20, w20, #0x4
   113d8: 6b13029f     	cmp	w20, w19
   113dc: 54ffff4b     	b.lt	0x113c4 <identity_direct_unrolled4+0x20>
   113e0: a9414ff4     	ldp	x20, x19, [sp, #0x10]
   113e4: a8c27bfd     	ldp	x29, x30, [sp], #0x20
   113e8: d65f03c0     	ret

# native: identity_indirect_unrolled4
00000000000113ec <identity_indirect_unrolled4>:
   113ec: a9bd7bfd     	stp	x29, x30, [sp, #-0x30]!
   113f0: f9000bf5     	str	x21, [sp, #0x10]
   113f4: a9024ff4     	stp	x20, x19, [sp, #0x20]
   113f8: 910003fd     	mov	x29, sp
   113fc: 90000108     	adrp	x8, 0x31000 <strtol+0x31000>
   11400: 2a0003f3     	mov	w19, w0
   11404: 7100041f     	cmp	w0, #0x1
   11408: f944f114     	ldr	x20, [x8, #0x9e0]
   1140c: aa0103e0     	mov	x0, x1
   11410: 5400012b     	b.lt	0x11434 <identity_indirect_unrolled4+0x48>
   11414: 2a1f03f5     	mov	w21, wzr
   11418: d63f0280     	blr	x20
   1141c: d63f0280     	blr	x20
   11420: d63f0280     	blr	x20
   11424: d63f0280     	blr	x20
   11428: 110012b5     	add	w21, w21, #0x4
   1142c: 6b1302bf     	cmp	w21, w19
   11430: 54ffff4b     	b.lt	0x11418 <identity_indirect_unrolled4+0x2c>
   11434: a9424ff4     	ldp	x20, x19, [sp, #0x20]
   11438: f9400bf5     	ldr	x21, [sp, #0x10]
   1143c: a8c37bfd     	ldp	x29, x30, [sp], #0x30
   11440: d65f03c0     	ret

# native: myna_bench_identity
0000000000011624 <myna_bench_identity>:
   11624: d65f03c0     	ret

# native: myna_bench_identity_address
0000000000011628 <myna_bench_identity_address>:
   11628: d503201f     	nop
   1162c: 10ffffc0     	adr	x0, 0x11624 <myna_bench_identity>
   11630: d65f03c0     	ret

# svm: Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG
0000000000050250 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG>:
   50250: d10103e9     	sub	x9, sp, #0x40
   50254: f940078a     	ldr	x10, [x28, #0x8]
   50258: eb0a013f     	cmp	x9, x10
   5025c: 54000689     	b.ls	0x5032c <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0xdc>
   50260: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50264: 9100013f     	mov	sp, x9
   50268: b9002fe1     	str	w1, [sp, #0x2c]
   5026c: 7100043f     	cmp	w1, #0x1
   50270: 5400022b     	b.lt	0x502b4 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x64>
   50274: 529ed203     	mov	w3, #0xf690             // =63120
   50278: 72a000e3     	movk	w3, #0x7, lsl #16
   5027c: 8b030f63     	add	x3, x27, x3, lsl #3
   50280: f90013e3     	str	x3, [sp, #0x20]
   50284: f9402460     	ldr	x0, [x3, #0x48]
   50288: aa0203e1     	mov	x1, x2
   5028c: 94000795     	bl	0x520e0 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1>
   50290: b9402fe1     	ldr	w1, [sp, #0x2c]
   50294: 7104043f     	cmp	w1, #0x101
   50298: 540001ca     	b.ge	0x502d0 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x80>
   5029c: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   502a0: 910103ff     	add	sp, sp, #0x40
   502a4: b9401389     	ldr	w9, [x28, #0x10]
   502a8: 7100013f     	cmp	w9, #0x0
   502ac: 5400042d     	b.le	0x50330 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0xe0>
   502b0: d65f03c0     	ret
   502b4: aa0203e0     	mov	x0, x2
   502b8: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   502bc: 910103ff     	add	sp, sp, #0x40
   502c0: b9401389     	ldr	w9, [x28, #0x10]
   502c4: 7100013f     	cmp	w9, #0x0
   502c8: 5400034d     	b.le	0x50330 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0xe0>
   502cc: d65f03c0     	ret
   502d0: 321803e9     	orr	w9, wzr, #0x100
   502d4: b9002be9     	str	w9, [sp, #0x28]
   502d8: f94013e3     	ldr	x3, [sp, #0x20]
   502dc: aa0003e1     	mov	x1, x0
   502e0: f9402460     	ldr	x0, [x3, #0x48]
   502e4: 9400077f     	bl	0x520e0 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1>
   502e8: b9402be1     	ldr	w1, [sp, #0x28]
   502ec: 11040022     	add	w2, w1, #0x100
   502f0: b9402fe3     	ldr	w3, [sp, #0x2c]
   502f4: 6b03005f     	cmp	w2, w3
   502f8: 54fffd2a     	b.ge	0x5029c <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x4c>
   502fc: b9401389     	ldr	w9, [x28, #0x10]
   50300: 340000a9     	cbz	w9, 0x50314 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0xc4>
   50304: aa0003e1     	mov	x1, x0
   50308: b9002be2     	str	w2, [sp, #0x28]
   5030c: f94013e3     	ldr	x3, [sp, #0x20]
   50310: 17fffff4     	b	0x502e0 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x90>
   50314: f94013e4     	ldr	x4, [sp, #0x20]
   50318: 9406a19a     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5031c: aa0003e1     	mov	x1, x0
   50320: b9002be2     	str	w2, [sp, #0x28]
   50324: aa0403e3     	mov	x3, x4
   50328: 17ffffee     	b	0x502e0 <Floor$$L3d161c30_run_JuYeGUi3Q24PRu8DGinmYG+0x90>
   5032c: 1402c155     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50330: 1406a194     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50334: cccccccc     	<unknown>
   50338: cccccccc     	<unknown>
   5033c: cccccccc     	<unknown>

# svm: Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6
0000000000050340 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6>:
   50340: d10103e9     	sub	x9, sp, #0x40
   50344: f940078a     	ldr	x10, [x28, #0x8]
   50348: eb0a013f     	cmp	x9, x10
   5034c: 54000649     	b.ls	0x50414 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0xd4>
   50350: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50354: 9100013f     	mov	sp, x9
   50358: b9002fe1     	str	w1, [sp, #0x2c]
   5035c: 7100043f     	cmp	w1, #0x1
   50360: 5400020b     	b.lt	0x503a0 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x60>
   50364: 90004503     	adrp	x3, 0x8f0000 <.data>
   50368: f9406063     	ldr	x3, [x3, #0xc0]
   5036c: f90013e3     	str	x3, [sp, #0x20]
   50370: aa0203e0     	mov	x0, x2
   50374: aa0303fe     	mov	x30, x3
   50378: d63f03c0     	blr	x30
   5037c: b9402fe1     	ldr	w1, [sp, #0x2c]
   50380: 7100443f     	cmp	w1, #0x11
   50384: 540001ca     	b.ge	0x503bc <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x7c>
   50388: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   5038c: 910103ff     	add	sp, sp, #0x40
   50390: b9401389     	ldr	w9, [x28, #0x10]
   50394: 7100013f     	cmp	w9, #0x0
   50398: 5400040d     	b.le	0x50418 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0xd8>
   5039c: d65f03c0     	ret
   503a0: aa0203e0     	mov	x0, x2
   503a4: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   503a8: 910103ff     	add	sp, sp, #0x40
   503ac: b9401389     	ldr	w9, [x28, #0x10]
   503b0: 7100013f     	cmp	w9, #0x0
   503b4: 5400032d     	b.le	0x50418 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0xd8>
   503b8: d65f03c0     	ret
   503bc: 321c03e9     	orr	w9, wzr, #0x10
   503c0: b9002be9     	str	w9, [sp, #0x28]
   503c4: f94013e3     	ldr	x3, [sp, #0x20]
   503c8: d503201f     	nop
   503cc: d503201f     	nop
   503d0: aa0303fe     	mov	x30, x3
   503d4: d63f03c0     	blr	x30
   503d8: b9402be1     	ldr	w1, [sp, #0x28]
   503dc: 11004022     	add	w2, w1, #0x10
   503e0: b9402fe3     	ldr	w3, [sp, #0x2c]
   503e4: 6b03005f     	cmp	w2, w3
   503e8: 54fffd0a     	b.ge	0x50388 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x48>
   503ec: b9401389     	ldr	w9, [x28, #0x10]
   503f0: 34000089     	cbz	w9, 0x50400 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0xc0>
   503f4: b9002be2     	str	w2, [sp, #0x28]
   503f8: f94013e3     	ldr	x3, [sp, #0x20]
   503fc: 17fffff5     	b	0x503d0 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x90>
   50400: f94013e4     	ldr	x4, [sp, #0x20]
   50404: 9406a15f     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50408: b9002be2     	str	w2, [sp, #0x28]
   5040c: aa0403e3     	mov	x3, x4
   50410: 17fffff0     	b	0x503d0 <Floor$$L9094b578_run_UeshAlJdIEAWAzqbFRdcN6+0x90>
   50414: 1402c11b     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50418: 1406a15a     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5041c: cccccccc     	<unknown>

# svm: Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF
0000000000050420 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF>:
   50420: d10103e9     	sub	x9, sp, #0x40
   50424: f940078a     	ldr	x10, [x28, #0x8]
   50428: eb0a013f     	cmp	x9, x10
   5042c: 54000689     	b.ls	0x504fc <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0xdc>
   50430: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50434: 9100013f     	mov	sp, x9
   50438: b9002fe1     	str	w1, [sp, #0x2c]
   5043c: 7100043f     	cmp	w1, #0x1
   50440: 5400022b     	b.lt	0x50484 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x64>
   50444: 529ed203     	mov	w3, #0xf690             // =63120
   50448: 72a000e3     	movk	w3, #0x7, lsl #16
   5044c: 8b030f63     	add	x3, x27, x3, lsl #3
   50450: f90013e3     	str	x3, [sp, #0x20]
   50454: f9402460     	ldr	x0, [x3, #0x48]
   50458: aa0203e1     	mov	x1, x2
   5045c: 94000779     	bl	0x52240 <Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI>
   50460: b9402fe1     	ldr	w1, [sp, #0x2c]
   50464: 7100143f     	cmp	w1, #0x5
   50468: 540001ca     	b.ge	0x504a0 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x80>
   5046c: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50470: 910103ff     	add	sp, sp, #0x40
   50474: b9401389     	ldr	w9, [x28, #0x10]
   50478: 7100013f     	cmp	w9, #0x0
   5047c: 5400042d     	b.le	0x50500 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0xe0>
   50480: d65f03c0     	ret
   50484: aa0203e0     	mov	x0, x2
   50488: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   5048c: 910103ff     	add	sp, sp, #0x40
   50490: b9401389     	ldr	w9, [x28, #0x10]
   50494: 7100013f     	cmp	w9, #0x0
   50498: 5400034d     	b.le	0x50500 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0xe0>
   5049c: d65f03c0     	ret
   504a0: 321e03e9     	orr	w9, wzr, #0x4
   504a4: b9002be9     	str	w9, [sp, #0x28]
   504a8: f94013e3     	ldr	x3, [sp, #0x20]
   504ac: aa0003e1     	mov	x1, x0
   504b0: f9402460     	ldr	x0, [x3, #0x48]
   504b4: 94000763     	bl	0x52240 <Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI>
   504b8: b9402be1     	ldr	w1, [sp, #0x28]
   504bc: 11001022     	add	w2, w1, #0x4
   504c0: b9402fe3     	ldr	w3, [sp, #0x2c]
   504c4: 6b03005f     	cmp	w2, w3
   504c8: 54fffd2a     	b.ge	0x5046c <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x4c>
   504cc: b9401389     	ldr	w9, [x28, #0x10]
   504d0: 340000a9     	cbz	w9, 0x504e4 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0xc4>
   504d4: aa0003e1     	mov	x1, x0
   504d8: b9002be2     	str	w2, [sp, #0x28]
   504dc: f94013e3     	ldr	x3, [sp, #0x20]
   504e0: 17fffff4     	b	0x504b0 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x90>
   504e4: f94013e4     	ldr	x4, [sp, #0x20]
   504e8: 9406a126     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   504ec: aa0003e1     	mov	x1, x0
   504f0: b9002be2     	str	w2, [sp, #0x28]
   504f4: aa0403e3     	mov	x3, x4
   504f8: 17ffffee     	b	0x504b0 <Floor$$L1ee5b013_run_3XvS1gSGMi0OBIPJzNz6zF+0x90>
   504fc: 1402c0e1     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50500: 1406a120     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50504: cccccccc     	<unknown>
   50508: cccccccc     	<unknown>
   5050c: cccccccc     	<unknown>

# svm: Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE
0000000000050510 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE>:
   50510: d10143e9     	sub	x9, sp, #0x50
   50514: f940078a     	ldr	x10, [x28, #0x8]
   50518: eb0a013f     	cmp	x9, x10
   5051c: 54000bc9     	b.ls	0x50694 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x184>
   50520: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50524: 9100013f     	mov	sp, x9
   50528: b90033e1     	str	w1, [sp, #0x30]
   5052c: 7100043f     	cmp	w1, #0x1
   50530: 320003e0     	orr	w0, wzr, #0x1
   50534: 1a80d020     	csel	w0, w1, w0, le
   50538: 7100041f     	cmp	w0, #0x1
   5053c: 54000a6b     	b.lt	0x50688 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x178>
   50540: aa0203e0     	mov	x0, x2
   50544: 9400091b     	bl	0x529b0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   50548: 320003e2     	orr	w2, wzr, #0x1
   5054c: b94033e1     	ldr	w1, [sp, #0x30]
   50550: 52800083     	mov	w3, #0x4                // =4
   50554: 72b00003     	movk	w3, #0x8000, lsl #16
   50558: 320103e4     	orr	w4, wzr, #0x80000000
   5055c: 51001025     	sub	w5, w1, #0x4
   50560: 6b03003f     	cmp	w1, w3
   50564: 1a85b083     	csel	w3, w4, w5, lt
   50568: b9002be3     	str	w3, [sp, #0x28]
   5056c: 11000c64     	add	w4, w3, #0x3
   50570: b90027e4     	str	w4, [sp, #0x24]
   50574: 2a0203e1     	mov	w1, w2
   50578: d503201f     	nop
   5057c: d503201f     	nop
   50580: 6b03003f     	cmp	w1, w3
   50584: 5400062a     	b.ge	0x50648 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x138>
   50588: 4b010085     	sub	w5, w4, w1
   5058c: 53027ca6     	lsr	w6, w5, #2
   50590: 121e74a5     	and	w5, w5, #0xfffffffc
   50594: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   50598: 321203e6     	orr	w6, wzr, #0x4000
   5059c: 1a8630a5     	csel	w5, w5, w6, lo
   505a0: 710004bf     	cmp	w5, #0x1
   505a4: 540004ab     	b.lt	0x50638 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x128>
   505a8: b90037e1     	str	w1, [sp, #0x34]
   505ac: b9003fe5     	str	w5, [sp, #0x3c]
   505b0: 2a1f03e2     	mov	w2, wzr
   505b4: d503201f     	nop
   505b8: d503201f     	nop
   505bc: d503201f     	nop
   505c0: 0b010047     	add	w7, w2, w1
   505c4: b90023e7     	str	w7, [sp, #0x20]
   505c8: b9002fe2     	str	w2, [sp, #0x2c]
   505cc: 940008f9     	bl	0x529b0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   505d0: b94023e7     	ldr	w7, [sp, #0x20]
   505d4: 110004e1     	add	w1, w7, #0x1
   505d8: b9003be1     	str	w1, [sp, #0x38]
   505dc: 940008f5     	bl	0x529b0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   505e0: b94023e7     	ldr	w7, [sp, #0x20]
   505e4: 110008e1     	add	w1, w7, #0x2
   505e8: b9003be1     	str	w1, [sp, #0x38]
   505ec: 940008f1     	bl	0x529b0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   505f0: b94023e7     	ldr	w7, [sp, #0x20]
   505f4: 11000ce1     	add	w1, w7, #0x3
   505f8: b90023e1     	str	w1, [sp, #0x20]
   505fc: 940008ed     	bl	0x529b0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   50600: b9402fe2     	ldr	w2, [sp, #0x2c]
   50604: 11001042     	add	w2, w2, #0x4
   50608: b9403fe5     	ldr	w5, [sp, #0x3c]
   5060c: 6b05005f     	cmp	w2, w5
   50610: b94037e1     	ldr	w1, [sp, #0x34]
   50614: 54fffd6b     	b.lt	0x505c0 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0xb0>
   50618: 0b010041     	add	w1, w2, w1
   5061c: b9401389     	ldr	w9, [x28, #0x10]
   50620: 34000069     	cbz	w9, 0x5062c <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x11c>
   50624: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   50628: 17ffffd6     	b	0x50580 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x70>
   5062c: 9406a0d5     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50630: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   50634: 17ffffd3     	b	0x50580 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x70>
   50638: b9401389     	ldr	w9, [x28, #0x10]
   5063c: 35fffa29     	cbnz	w9, 0x50580 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x70>
   50640: 9406a0d0     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50644: 17ffffcf     	b	0x50580 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x70>
   50648: 2a0103e2     	mov	w2, w1
   5064c: 14000006     	b	0x50664 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x154>
   50650: b9002fe2     	str	w2, [sp, #0x2c]
   50654: 940008d7     	bl	0x529b0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   50658: b9402fe2     	ldr	w2, [sp, #0x2c]
   5065c: 1100045d     	add	w29, w2, #0x1
   50660: 2a1d03e2     	mov	w2, w29
   50664: b94033e1     	ldr	w1, [sp, #0x30]
   50668: 6b01005f     	cmp	w2, w1
   5066c: 54ffff2b     	b.lt	0x50650 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x140>
   50670: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   50674: 910143ff     	add	sp, sp, #0x50
   50678: b9401389     	ldr	w9, [x28, #0x10]
   5067c: 7100013f     	cmp	w9, #0x0
   50680: 540000cd     	b.le	0x50698 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x188>
   50684: d65f03c0     	ret
   50688: aa0203e0     	mov	x0, x2
   5068c: 2a1f03e2     	mov	w2, wzr
   50690: 17ffffb0     	b	0x50550 <Floor$$L906294e4_run_OZY2eX9pYLKkEkYqessxAE+0x40>
   50694: 1402c07b     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50698: 1406a0ba     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5069c: cccccccc     	<unknown>

# svm: Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669
00000000000506a0 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669>:
   506a0: d10103e9     	sub	x9, sp, #0x40
   506a4: f940078a     	ldr	x10, [x28, #0x8]
   506a8: eb0a013f     	cmp	x9, x10
   506ac: 54000809     	b.ls	0x507ac <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x10c>
   506b0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   506b4: 9100013f     	mov	sp, x9
   506b8: b9002fe1     	str	w1, [sp, #0x2c]
   506bc: 7100043f     	cmp	w1, #0x1
   506c0: 5400030b     	b.lt	0x50720 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x80>
   506c4: 529ed200     	mov	w0, #0xf690             // =63120
   506c8: 72a000e0     	movk	w0, #0x7, lsl #16
   506cc: 8b000f60     	add	x0, x27, x0, lsl #3
   506d0: f9402803     	ldr	x3, [x0, #0x50]
   506d4: f90013e3     	str	x3, [sp, #0x20]
   506d8: aa0203e0     	mov	x0, x2
   506dc: aa0303fe     	mov	x30, x3
   506e0: d63f03c0     	blr	x30
   506e4: f94013fe     	ldr	x30, [sp, #0x20]
   506e8: d63f03c0     	blr	x30
   506ec: f94013fe     	ldr	x30, [sp, #0x20]
   506f0: d63f03c0     	blr	x30
   506f4: f94013fe     	ldr	x30, [sp, #0x20]
   506f8: d63f03c0     	blr	x30
   506fc: b9402fe1     	ldr	w1, [sp, #0x2c]
   50700: 7100143f     	cmp	w1, #0x5
   50704: 540001ca     	b.ge	0x5073c <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x9c>
   50708: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   5070c: 910103ff     	add	sp, sp, #0x40
   50710: b9401389     	ldr	w9, [x28, #0x10]
   50714: 7100013f     	cmp	w9, #0x0
   50718: 540004cd     	b.le	0x507b0 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x110>
   5071c: d65f03c0     	ret
   50720: aa0203e0     	mov	x0, x2
   50724: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50728: 910103ff     	add	sp, sp, #0x40
   5072c: b9401389     	ldr	w9, [x28, #0x10]
   50730: 7100013f     	cmp	w9, #0x0
   50734: 540003ed     	b.le	0x507b0 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x110>
   50738: d65f03c0     	ret
   5073c: 321e03e9     	orr	w9, wzr, #0x4
   50740: b9002be9     	str	w9, [sp, #0x28]
   50744: f94013e3     	ldr	x3, [sp, #0x20]
   50748: d503201f     	nop
   5074c: d503201f     	nop
   50750: aa0303fe     	mov	x30, x3
   50754: d63f03c0     	blr	x30
   50758: f94013fe     	ldr	x30, [sp, #0x20]
   5075c: d63f03c0     	blr	x30
   50760: f94013fe     	ldr	x30, [sp, #0x20]
   50764: d63f03c0     	blr	x30
   50768: f94013fe     	ldr	x30, [sp, #0x20]
   5076c: d63f03c0     	blr	x30
   50770: b9402be1     	ldr	w1, [sp, #0x28]
   50774: 11001022     	add	w2, w1, #0x4
   50778: b9402fe3     	ldr	w3, [sp, #0x2c]
   5077c: 6b03005f     	cmp	w2, w3
   50780: 54fffc4a     	b.ge	0x50708 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0x68>
   50784: b9401389     	ldr	w9, [x28, #0x10]
   50788: 34000089     	cbz	w9, 0x50798 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0xf8>
   5078c: b9002be2     	str	w2, [sp, #0x28]
   50790: f94013e3     	ldr	x3, [sp, #0x20]
   50794: 17ffffef     	b	0x50750 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0xb0>
   50798: f94013e4     	ldr	x4, [sp, #0x20]
   5079c: 9406a079     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   507a0: b9002be2     	str	w2, [sp, #0x28]
   507a4: aa0403e3     	mov	x3, x4
   507a8: 17ffffea     	b	0x50750 <Floor$$L26994aad_run_sthG1RPPFq68efVXTl2669+0xb0>
   507ac: 1402c035     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   507b0: 1406a074     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   507b4: cccccccc     	<unknown>
   507b8: cccccccc     	<unknown>
   507bc: cccccccc     	<unknown>

# svm: Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4
00000000000507c0 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4>:
   507c0: d10143e9     	sub	x9, sp, #0x50
   507c4: f940078a     	ldr	x10, [x28, #0x8]
   507c8: eb0a013f     	cmp	x9, x10
   507cc: 54000f09     	b.ls	0x509ac <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1ec>
   507d0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   507d4: 9100013f     	mov	sp, x9
   507d8: b90033e1     	str	w1, [sp, #0x30]
   507dc: 529ed200     	mov	w0, #0xf690             // =63120
   507e0: 72a000e0     	movk	w0, #0x7, lsl #16
   507e4: 8b000f60     	add	x0, x27, x0, lsl #3
   507e8: f9402803     	ldr	x3, [x0, #0x50]
   507ec: f9000fe3     	str	x3, [sp, #0x18]
   507f0: 7100043f     	cmp	w1, #0x1
   507f4: 320003e0     	orr	w0, wzr, #0x1
   507f8: 1a80d020     	csel	w0, w1, w0, le
   507fc: 7100041f     	cmp	w0, #0x1
   50800: 54000d0b     	b.lt	0x509a0 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1e0>
   50804: aa0203e0     	mov	x0, x2
   50808: aa0303fe     	mov	x30, x3
   5080c: d63f03c0     	blr	x30
   50810: 320003fe     	orr	w30, wzr, #0x1
   50814: b94033e1     	ldr	w1, [sp, #0x30]
   50818: 52800082     	mov	w2, #0x4                // =4
   5081c: 72b00002     	movk	w2, #0x8000, lsl #16
   50820: 320103e3     	orr	w3, wzr, #0x80000000
   50824: 51001024     	sub	w4, w1, #0x4
   50828: 6b02003f     	cmp	w1, w2
   5082c: 1a84b062     	csel	w2, w3, w4, lt
   50830: b9002be2     	str	w2, [sp, #0x28]
   50834: 11000c43     	add	w3, w2, #0x3
   50838: b90027e3     	str	w3, [sp, #0x24]
   5083c: f9400fe4     	ldr	x4, [sp, #0x18]
   50840: 2a1e03e1     	mov	w1, w30
   50844: d503201f     	nop
   50848: d503201f     	nop
   5084c: d503201f     	nop
   50850: 6b02003f     	cmp	w1, w2
   50854: 5400078a     	b.ge	0x50944 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x184>
   50858: 4b01007e     	sub	w30, w3, w1
   5085c: 53027fc6     	lsr	w6, w30, #2
   50860: 121e77de     	and	w30, w30, #0xfffffffc
   50864: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   50868: 321203e6     	orr	w6, wzr, #0x4000
   5086c: 1a8633c6     	csel	w6, w30, w6, lo
   50870: 710004df     	cmp	w6, #0x1
   50874: 5400060b     	b.lt	0x50934 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x174>
   50878: b90037e1     	str	w1, [sp, #0x34]
   5087c: b9003fe6     	str	w6, [sp, #0x3c]
   50880: 2a1f03e2     	mov	w2, wzr
   50884: 14000008     	b	0x508a4 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0xe4>
   50888: d503201f     	nop
   5088c: d503201f     	nop
   50890: d503201f     	nop
   50894: d503201f     	nop
   50898: d503201f     	nop
   5089c: d503201f     	nop
   508a0: f9400fe4     	ldr	x4, [sp, #0x18]
   508a4: 0b010048     	add	w8, w2, w1
   508a8: b90023e8     	str	w8, [sp, #0x20]
   508ac: aa0403fe     	mov	x30, x4
   508b0: b9002fe2     	str	w2, [sp, #0x2c]
   508b4: d63f03c0     	blr	x30
   508b8: b94023e8     	ldr	w8, [sp, #0x20]
   508bc: 11000501     	add	w1, w8, #0x1
   508c0: f9400ffe     	ldr	x30, [sp, #0x18]
   508c4: b9003be1     	str	w1, [sp, #0x38]
   508c8: d63f03c0     	blr	x30
   508cc: b94023e8     	ldr	w8, [sp, #0x20]
   508d0: 11000901     	add	w1, w8, #0x2
   508d4: f9400ffe     	ldr	x30, [sp, #0x18]
   508d8: b9003be1     	str	w1, [sp, #0x38]
   508dc: d63f03c0     	blr	x30
   508e0: b94023e8     	ldr	w8, [sp, #0x20]
   508e4: 11000d01     	add	w1, w8, #0x3
   508e8: f9400ffe     	ldr	x30, [sp, #0x18]
   508ec: b90023e1     	str	w1, [sp, #0x20]
   508f0: d63f03c0     	blr	x30
   508f4: b9402fe2     	ldr	w2, [sp, #0x2c]
   508f8: 11001042     	add	w2, w2, #0x4
   508fc: b9403fe6     	ldr	w6, [sp, #0x3c]
   50900: 6b06005f     	cmp	w2, w6
   50904: b94037e1     	ldr	w1, [sp, #0x34]
   50908: 54fffccb     	b.lt	0x508a0 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0xe0>
   5090c: 0b010041     	add	w1, w2, w1
   50910: b9401389     	ldr	w9, [x28, #0x10]
   50914: 34000089     	cbz	w9, 0x50924 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x164>
   50918: 29448be3     	ldp	w3, w2, [sp, #0x24]
   5091c: f9400fe4     	ldr	x4, [sp, #0x18]
   50920: 17ffffcc     	b	0x50850 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x90>
   50924: 9406a017     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50928: 29448be3     	ldp	w3, w2, [sp, #0x24]
   5092c: f9400fe4     	ldr	x4, [sp, #0x18]
   50930: 17ffffc8     	b	0x50850 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x90>
   50934: b9401389     	ldr	w9, [x28, #0x10]
   50938: 35fff8c9     	cbnz	w9, 0x50850 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x90>
   5093c: 9406a011     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50940: 17ffffc4     	b	0x50850 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x90>
   50944: 2a0103e2     	mov	w2, w1
   50948: aa0403e3     	mov	x3, x4
   5094c: 1400000c     	b	0x5097c <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1bc>
   50950: d503201f     	nop
   50954: d503201f     	nop
   50958: d503201f     	nop
   5095c: d503201f     	nop
   50960: b9002fe2     	str	w2, [sp, #0x2c]
   50964: aa0303fe     	mov	x30, x3
   50968: d63f03c0     	blr	x30
   5096c: b9402fe2     	ldr	w2, [sp, #0x2c]
   50970: 1100045d     	add	w29, w2, #0x1
   50974: 2a1d03e2     	mov	w2, w29
   50978: f9400fe3     	ldr	x3, [sp, #0x18]
   5097c: b94033e1     	ldr	w1, [sp, #0x30]
   50980: 6b01005f     	cmp	w2, w1
   50984: 54fffeeb     	b.lt	0x50960 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1a0>
   50988: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   5098c: 910143ff     	add	sp, sp, #0x50
   50990: b9401389     	ldr	w9, [x28, #0x10]
   50994: 7100013f     	cmp	w9, #0x0
   50998: 540000cd     	b.le	0x509b0 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x1f0>
   5099c: d65f03c0     	ret
   509a0: 2a1f03fe     	mov	w30, wzr
   509a4: aa0203e0     	mov	x0, x2
   509a8: 17ffff9c     	b	0x50818 <Floor$$L6c62a309_run_KJOWDiJCQM6rWw9kkXmoh4+0x58>
   509ac: 1402bfb5     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   509b0: 14069ff4     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   509b4: cccccccc     	<unknown>
   509b8: cccccccc     	<unknown>
   509bc: cccccccc     	<unknown>

# svm: Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5
00000000000509c0 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5>:
   509c0: d10103e9     	sub	x9, sp, #0x40
   509c4: f940078a     	ldr	x10, [x28, #0x8]
   509c8: eb0a013f     	cmp	x9, x10
   509cc: 54000809     	b.ls	0x50acc <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x10c>
   509d0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   509d4: 9100013f     	mov	sp, x9
   509d8: b9002fe1     	str	w1, [sp, #0x2c]
   509dc: 7100043f     	cmp	w1, #0x1
   509e0: 5400030b     	b.lt	0x50a40 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x80>
   509e4: 529ed200     	mov	w0, #0xf690             // =63120
   509e8: 72a000e0     	movk	w0, #0x7, lsl #16
   509ec: 8b000f60     	add	x0, x27, x0, lsl #3
   509f0: f9402803     	ldr	x3, [x0, #0x50]
   509f4: f90013e3     	str	x3, [sp, #0x20]
   509f8: aa0203e0     	mov	x0, x2
   509fc: aa0303fe     	mov	x30, x3
   50a00: d63f03c0     	blr	x30
   50a04: f94013fe     	ldr	x30, [sp, #0x20]
   50a08: d63f03c0     	blr	x30
   50a0c: f94013fe     	ldr	x30, [sp, #0x20]
   50a10: d63f03c0     	blr	x30
   50a14: f94013fe     	ldr	x30, [sp, #0x20]
   50a18: d63f03c0     	blr	x30
   50a1c: b9402fe1     	ldr	w1, [sp, #0x2c]
   50a20: 7100143f     	cmp	w1, #0x5
   50a24: 540001ca     	b.ge	0x50a5c <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x9c>
   50a28: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50a2c: 910103ff     	add	sp, sp, #0x40
   50a30: b9401389     	ldr	w9, [x28, #0x10]
   50a34: 7100013f     	cmp	w9, #0x0
   50a38: 540004cd     	b.le	0x50ad0 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x110>
   50a3c: d65f03c0     	ret
   50a40: aa0203e0     	mov	x0, x2
   50a44: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50a48: 910103ff     	add	sp, sp, #0x40
   50a4c: b9401389     	ldr	w9, [x28, #0x10]
   50a50: 7100013f     	cmp	w9, #0x0
   50a54: 540003ed     	b.le	0x50ad0 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x110>
   50a58: d65f03c0     	ret
   50a5c: 321e03e9     	orr	w9, wzr, #0x4
   50a60: b9002be9     	str	w9, [sp, #0x28]
   50a64: f94013e3     	ldr	x3, [sp, #0x20]
   50a68: d503201f     	nop
   50a6c: d503201f     	nop
   50a70: aa0303fe     	mov	x30, x3
   50a74: d63f03c0     	blr	x30
   50a78: f94013fe     	ldr	x30, [sp, #0x20]
   50a7c: d63f03c0     	blr	x30
   50a80: f94013fe     	ldr	x30, [sp, #0x20]
   50a84: d63f03c0     	blr	x30
   50a88: f94013fe     	ldr	x30, [sp, #0x20]
   50a8c: d63f03c0     	blr	x30
   50a90: b9402be1     	ldr	w1, [sp, #0x28]
   50a94: 11001022     	add	w2, w1, #0x4
   50a98: b9402fe3     	ldr	w3, [sp, #0x2c]
   50a9c: 6b03005f     	cmp	w2, w3
   50aa0: 54fffc4a     	b.ge	0x50a28 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0x68>
   50aa4: b9401389     	ldr	w9, [x28, #0x10]
   50aa8: 34000089     	cbz	w9, 0x50ab8 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0xf8>
   50aac: b9002be2     	str	w2, [sp, #0x28]
   50ab0: f94013e3     	ldr	x3, [sp, #0x20]
   50ab4: 17ffffef     	b	0x50a70 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0xb0>
   50ab8: f94013e4     	ldr	x4, [sp, #0x20]
   50abc: 94069fb1     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50ac0: b9002be2     	str	w2, [sp, #0x28]
   50ac4: aa0403e3     	mov	x3, x4
   50ac8: 17ffffea     	b	0x50a70 <Floor$$L7b482530_run_E8Og6fELcv4WERRdzys2T5+0xb0>
   50acc: 1402bf6d     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50ad0: 14069fac     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50ad4: cccccccc     	<unknown>
   50ad8: cccccccc     	<unknown>
   50adc: cccccccc     	<unknown>

# svm: Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB
0000000000050ae0 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB>:
   50ae0: d10103e9     	sub	x9, sp, #0x40
   50ae4: f940078a     	ldr	x10, [x28, #0x8]
   50ae8: eb0a013f     	cmp	x9, x10
   50aec: 54000649     	b.ls	0x50bb4 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0xd4>
   50af0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50af4: 9100013f     	mov	sp, x9
   50af8: b9002fe1     	str	w1, [sp, #0x2c]
   50afc: 7100043f     	cmp	w1, #0x1
   50b00: 5400020b     	b.lt	0x50b40 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x60>
   50b04: 90004503     	adrp	x3, 0x8f0000 <.data>
   50b08: f941c063     	ldr	x3, [x3, #0x380]
   50b0c: f90013e3     	str	x3, [sp, #0x20]
   50b10: aa0203e0     	mov	x0, x2
   50b14: aa0303fe     	mov	x30, x3
   50b18: d63f03c0     	blr	x30
   50b1c: b9402fe1     	ldr	w1, [sp, #0x2c]
   50b20: 7104043f     	cmp	w1, #0x101
   50b24: 540001ca     	b.ge	0x50b5c <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x7c>
   50b28: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50b2c: 910103ff     	add	sp, sp, #0x40
   50b30: b9401389     	ldr	w9, [x28, #0x10]
   50b34: 7100013f     	cmp	w9, #0x0
   50b38: 5400040d     	b.le	0x50bb8 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0xd8>
   50b3c: d65f03c0     	ret
   50b40: aa0203e0     	mov	x0, x2
   50b44: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50b48: 910103ff     	add	sp, sp, #0x40
   50b4c: b9401389     	ldr	w9, [x28, #0x10]
   50b50: 7100013f     	cmp	w9, #0x0
   50b54: 5400032d     	b.le	0x50bb8 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0xd8>
   50b58: d65f03c0     	ret
   50b5c: 321803e9     	orr	w9, wzr, #0x100
   50b60: b9002be9     	str	w9, [sp, #0x28]
   50b64: f94013e3     	ldr	x3, [sp, #0x20]
   50b68: d503201f     	nop
   50b6c: d503201f     	nop
   50b70: aa0303fe     	mov	x30, x3
   50b74: d63f03c0     	blr	x30
   50b78: b9402be1     	ldr	w1, [sp, #0x28]
   50b7c: 11040022     	add	w2, w1, #0x100
   50b80: b9402fe3     	ldr	w3, [sp, #0x2c]
   50b84: 6b03005f     	cmp	w2, w3
   50b88: 54fffd0a     	b.ge	0x50b28 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x48>
   50b8c: b9401389     	ldr	w9, [x28, #0x10]
   50b90: 34000089     	cbz	w9, 0x50ba0 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0xc0>
   50b94: b9002be2     	str	w2, [sp, #0x28]
   50b98: f94013e3     	ldr	x3, [sp, #0x20]
   50b9c: 17fffff5     	b	0x50b70 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x90>
   50ba0: f94013e4     	ldr	x4, [sp, #0x20]
   50ba4: 94069f77     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50ba8: b9002be2     	str	w2, [sp, #0x28]
   50bac: aa0403e3     	mov	x3, x4
   50bb0: 17fffff0     	b	0x50b70 <Floor$$L92960097_run_58NQHUuvj9IokcCa5edcXB+0x90>
   50bb4: 1402bf33     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50bb8: 14069f72     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50bbc: cccccccc     	<unknown>

# svm: Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G
0000000000050bc0 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G>:
   50bc0: d10143e9     	sub	x9, sp, #0x50
   50bc4: f940078a     	ldr	x10, [x28, #0x8]
   50bc8: eb0a013f     	cmp	x9, x10
   50bcc: 54000bc9     	b.ls	0x50d44 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x184>
   50bd0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50bd4: 9100013f     	mov	sp, x9
   50bd8: b90033e1     	str	w1, [sp, #0x30]
   50bdc: 7100043f     	cmp	w1, #0x1
   50be0: 320003e0     	orr	w0, wzr, #0x1
   50be4: 1a80d020     	csel	w0, w1, w0, le
   50be8: 7100041f     	cmp	w0, #0x1
   50bec: 54000a6b     	b.lt	0x50d38 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x178>
   50bf0: aa0203e0     	mov	x0, x2
   50bf4: 9400050b     	bl	0x52020 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50bf8: 320003e2     	orr	w2, wzr, #0x1
   50bfc: b94033e1     	ldr	w1, [sp, #0x30]
   50c00: 52800083     	mov	w3, #0x4                // =4
   50c04: 72b00003     	movk	w3, #0x8000, lsl #16
   50c08: 320103e4     	orr	w4, wzr, #0x80000000
   50c0c: 51001025     	sub	w5, w1, #0x4
   50c10: 6b03003f     	cmp	w1, w3
   50c14: 1a85b083     	csel	w3, w4, w5, lt
   50c18: b9002be3     	str	w3, [sp, #0x28]
   50c1c: 11000c64     	add	w4, w3, #0x3
   50c20: b90027e4     	str	w4, [sp, #0x24]
   50c24: 2a0203e1     	mov	w1, w2
   50c28: d503201f     	nop
   50c2c: d503201f     	nop
   50c30: 6b03003f     	cmp	w1, w3
   50c34: 5400062a     	b.ge	0x50cf8 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x138>
   50c38: 4b010085     	sub	w5, w4, w1
   50c3c: 53027ca6     	lsr	w6, w5, #2
   50c40: 121e74a5     	and	w5, w5, #0xfffffffc
   50c44: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   50c48: 321203e6     	orr	w6, wzr, #0x4000
   50c4c: 1a8630a5     	csel	w5, w5, w6, lo
   50c50: 710004bf     	cmp	w5, #0x1
   50c54: 540004ab     	b.lt	0x50ce8 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x128>
   50c58: b90037e1     	str	w1, [sp, #0x34]
   50c5c: b9003fe5     	str	w5, [sp, #0x3c]
   50c60: 2a1f03e2     	mov	w2, wzr
   50c64: d503201f     	nop
   50c68: d503201f     	nop
   50c6c: d503201f     	nop
   50c70: 0b010047     	add	w7, w2, w1
   50c74: b90023e7     	str	w7, [sp, #0x20]
   50c78: b9002fe2     	str	w2, [sp, #0x2c]
   50c7c: 940004e9     	bl	0x52020 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50c80: b94023e7     	ldr	w7, [sp, #0x20]
   50c84: 110004e1     	add	w1, w7, #0x1
   50c88: b9003be1     	str	w1, [sp, #0x38]
   50c8c: 940004e5     	bl	0x52020 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50c90: b94023e7     	ldr	w7, [sp, #0x20]
   50c94: 110008e1     	add	w1, w7, #0x2
   50c98: b9003be1     	str	w1, [sp, #0x38]
   50c9c: 940004e1     	bl	0x52020 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50ca0: b94023e7     	ldr	w7, [sp, #0x20]
   50ca4: 11000ce1     	add	w1, w7, #0x3
   50ca8: b90023e1     	str	w1, [sp, #0x20]
   50cac: 940004dd     	bl	0x52020 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50cb0: b9402fe2     	ldr	w2, [sp, #0x2c]
   50cb4: 11001042     	add	w2, w2, #0x4
   50cb8: b9403fe5     	ldr	w5, [sp, #0x3c]
   50cbc: 6b05005f     	cmp	w2, w5
   50cc0: b94037e1     	ldr	w1, [sp, #0x34]
   50cc4: 54fffd6b     	b.lt	0x50c70 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0xb0>
   50cc8: 0b010041     	add	w1, w2, w1
   50ccc: b9401389     	ldr	w9, [x28, #0x10]
   50cd0: 34000069     	cbz	w9, 0x50cdc <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x11c>
   50cd4: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   50cd8: 17ffffd6     	b	0x50c30 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x70>
   50cdc: 94069f29     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50ce0: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   50ce4: 17ffffd3     	b	0x50c30 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x70>
   50ce8: b9401389     	ldr	w9, [x28, #0x10]
   50cec: 35fffa29     	cbnz	w9, 0x50c30 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x70>
   50cf0: 94069f24     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50cf4: 17ffffcf     	b	0x50c30 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x70>
   50cf8: 2a0103e2     	mov	w2, w1
   50cfc: 14000006     	b	0x50d14 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x154>
   50d00: b9002fe2     	str	w2, [sp, #0x2c]
   50d04: 940004c7     	bl	0x52020 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50d08: b9402fe2     	ldr	w2, [sp, #0x2c]
   50d0c: 1100045d     	add	w29, w2, #0x1
   50d10: 2a1d03e2     	mov	w2, w29
   50d14: b94033e1     	ldr	w1, [sp, #0x30]
   50d18: 6b01005f     	cmp	w2, w1
   50d1c: 54ffff2b     	b.lt	0x50d00 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x140>
   50d20: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   50d24: 910143ff     	add	sp, sp, #0x50
   50d28: b9401389     	ldr	w9, [x28, #0x10]
   50d2c: 7100013f     	cmp	w9, #0x0
   50d30: 540000cd     	b.le	0x50d48 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x188>
   50d34: d65f03c0     	ret
   50d38: aa0203e0     	mov	x0, x2
   50d3c: 2a1f03e2     	mov	w2, wzr
   50d40: 17ffffb0     	b	0x50c00 <Floor$$L9ced8ac4_run_vg8UCCRIOx68CbcymLt63G+0x40>
   50d44: 1402becf     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50d48: 14069f0e     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50d4c: cccccccc     	<unknown>

# svm: Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7
0000000000050d50 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7>:
   50d50: d10103e9     	sub	x9, sp, #0x40
   50d54: f940078a     	ldr	x10, [x28, #0x8]
   50d58: eb0a013f     	cmp	x9, x10
   50d5c: 54000689     	b.ls	0x50e2c <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0xdc>
   50d60: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50d64: 9100013f     	mov	sp, x9
   50d68: b9002fe1     	str	w1, [sp, #0x2c]
   50d6c: 7100043f     	cmp	w1, #0x1
   50d70: 5400022b     	b.lt	0x50db4 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x64>
   50d74: 529ed203     	mov	w3, #0xf690             // =63120
   50d78: 72a000e3     	movk	w3, #0x7, lsl #16
   50d7c: 8b030f63     	add	x3, x27, x3, lsl #3
   50d80: f90013e3     	str	x3, [sp, #0x20]
   50d84: f9402460     	ldr	x0, [x3, #0x48]
   50d88: aa0203e1     	mov	x1, x2
   50d8c: 9400053d     	bl	0x52280 <Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK>
   50d90: b9402fe1     	ldr	w1, [sp, #0x2c]
   50d94: 7101043f     	cmp	w1, #0x41
   50d98: 540001ca     	b.ge	0x50dd0 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x80>
   50d9c: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50da0: 910103ff     	add	sp, sp, #0x40
   50da4: b9401389     	ldr	w9, [x28, #0x10]
   50da8: 7100013f     	cmp	w9, #0x0
   50dac: 5400042d     	b.le	0x50e30 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0xe0>
   50db0: d65f03c0     	ret
   50db4: aa0203e0     	mov	x0, x2
   50db8: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50dbc: 910103ff     	add	sp, sp, #0x40
   50dc0: b9401389     	ldr	w9, [x28, #0x10]
   50dc4: 7100013f     	cmp	w9, #0x0
   50dc8: 5400034d     	b.le	0x50e30 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0xe0>
   50dcc: d65f03c0     	ret
   50dd0: 321a03e9     	orr	w9, wzr, #0x40
   50dd4: b9002be9     	str	w9, [sp, #0x28]
   50dd8: f94013e3     	ldr	x3, [sp, #0x20]
   50ddc: aa0003e1     	mov	x1, x0
   50de0: f9402460     	ldr	x0, [x3, #0x48]
   50de4: 94000527     	bl	0x52280 <Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK>
   50de8: b9402be1     	ldr	w1, [sp, #0x28]
   50dec: 11010022     	add	w2, w1, #0x40
   50df0: b9402fe3     	ldr	w3, [sp, #0x2c]
   50df4: 6b03005f     	cmp	w2, w3
   50df8: 54fffd2a     	b.ge	0x50d9c <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x4c>
   50dfc: b9401389     	ldr	w9, [x28, #0x10]
   50e00: 340000a9     	cbz	w9, 0x50e14 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0xc4>
   50e04: aa0003e1     	mov	x1, x0
   50e08: b9002be2     	str	w2, [sp, #0x28]
   50e0c: f94013e3     	ldr	x3, [sp, #0x20]
   50e10: 17fffff4     	b	0x50de0 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x90>
   50e14: f94013e4     	ldr	x4, [sp, #0x20]
   50e18: 94069eda     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50e1c: aa0003e1     	mov	x1, x0
   50e20: b9002be2     	str	w2, [sp, #0x28]
   50e24: aa0403e3     	mov	x3, x4
   50e28: 17ffffee     	b	0x50de0 <Floor$$Lb2d4d33d_run_o92IpIAwItHCdLzUnZzHE7+0x90>
   50e2c: 1402be95     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50e30: 14069ed4     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50e34: cccccccc     	<unknown>
   50e38: cccccccc     	<unknown>
   50e3c: cccccccc     	<unknown>

# svm: Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI
0000000000050e40 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI>:
   50e40: d10203e9     	sub	x9, sp, #0x80
   50e44: f940078a     	ldr	x10, [x28, #0x8]
   50e48: eb0a013f     	cmp	x9, x10
   50e4c: 54002849     	b.ls	0x51354 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x514>
   50e50: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50e54: 9100013f     	mov	sp, x9
   50e58: b9400801     	ldr	w1, [x0, #0x8]
   50e5c: b9004be1     	str	w1, [sp, #0x48]
   50e60: b940041e     	ldr	w30, [x0, #0x4]
   50e64: 2a0103e2     	mov	w2, w1
   50e68: 8b020f62     	add	x2, x27, x2, lsl #3
   50e6c: f90023e2     	str	x2, [sp, #0x40]
   50e70: 340025de     	cbz	w30, 0x51328 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4e8>
   50e74: 2a1e03fe     	mov	w30, w30
   50e78: 8b1e0f7e     	add	x30, x27, x30, lsl #3
   50e7c: b94007de     	ldr	w30, [x30, #0x4]
   50e80: 3400259e     	cbz	w30, 0x51330 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4f0>
   50e84: 2a1e03e0     	mov	w0, w30
   50e88: 8b000f60     	add	x0, x27, x0, lsl #3
   50e8c: d503201f     	nop
   50e90: 91004009     	add	x9, x0, #0x10
   50e94: 88dffd23     	ldar	w3, [x9]
   50e98: 340001e3     	cbz	w3, 0x50ed4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
   50e9c: 51000464     	sub	w4, w3, #0x1
   50ea0: 9100401e     	add	x30, x0, #0x10
   50ea4: 885f7fc5     	ldxr	w5, [x30]
   50ea8: 6b0300bf     	cmp	w5, w3
   50eac: 1a85008a     	csel	w10, w4, w5, eq
   50eb0: 8809ffca     	stlxr	w9, w10, [x30]
   50eb4: 35ffff89     	cbnz	w9, 0x50ea4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x64>
   50eb8: d5033bbf     	dmb	ish
   50ebc: 1a9f17fe     	cset	w30, eq
   50ec0: 35001b9e     	cbnz	w30, 0x51230 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3f0>
   50ec4: b9401389     	ldr	w9, [x28, #0x10]
   50ec8: 35fffe49     	cbnz	w9, 0x50e90 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x50>
   50ecc: 94069ead     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50ed0: 17fffff0     	b	0x50e90 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x50>
   50ed4: 529edc63     	mov	w3, #0xf6e3             // =63203
   50ed8: 72a000e3     	movk	w3, #0x7, lsl #16
   50edc: 8b030f63     	add	x3, x27, x3, lsl #3
   50ee0: f9000fe3     	str	x3, [sp, #0x18]
   50ee4: 529edd64     	mov	w4, #0xf6eb             // =63211
   50ee8: 72a000e4     	movk	w4, #0x7, lsl #16
   50eec: 8b040f64     	add	x4, x27, x4, lsl #3
   50ef0: f90013e4     	str	x4, [sp, #0x20]
   50ef4: 528022e5     	mov	w5, #0x117              // =279
   50ef8: 72a00025     	movk	w5, #0x1, lsl #16
   50efc: b90057e5     	str	w5, [sp, #0x54]
   50f00: 529e9526     	mov	w6, #0xf4a9             // =62633
   50f04: 72a00026     	movk	w6, #0x1, lsl #16
   50f08: 8b060f66     	add	x6, x27, x6, lsl #3
   50f0c: f9001fe6     	str	x6, [sp, #0x38]
   50f10: 52802667     	mov	w7, #0x133              // =307
   50f14: 72a00027     	movk	w7, #0x1, lsl #16
   50f18: b90053e7     	str	w7, [sp, #0x50]
   50f1c: 529eddc8     	mov	w8, #0xf6ee             // =63214
   50f20: 72a000e8     	movk	w8, #0x7, lsl #16
   50f24: b9004fe8     	str	w8, [sp, #0x4c]
   50f28: 529ede0b     	mov	w11, #0xf6f0            // =63216
   50f2c: 72a000eb     	movk	w11, #0x7, lsl #16
   50f30: 8b0b0f6b     	add	x11, x27, x11, lsl #3
   50f34: f9001beb     	str	x11, [sp, #0x30]
   50f38: 529ece0c     	mov	w12, #0xf670            // =63088
   50f3c: 72a000ec     	movk	w12, #0x7, lsl #16
   50f40: 8b0c0f6c     	add	x12, x27, x12, lsl #3
   50f44: f90017ec     	str	x12, [sp, #0x28]
   50f48: b27003ed     	orr	x13, xzr, #0x10000
   50f4c: f9002fed     	str	x13, [sp, #0x58]
   50f50: 321003ee     	orr	w14, wzr, #0x10000
   50f54: b9006bee     	str	w14, [sp, #0x68]
   50f58: 52850c0f     	mov	w15, #0x2860            // =10336
   50f5c: 72a0040f     	movk	w15, #0x20, lsl #16
   50f60: d2800110     	mov	x16, #0x8               // =8
   50f64: f2a00030     	movk	x16, #0x1, lsl #16
   50f68: 2a1f03f1     	mov	w17, wzr
   50f6c: d503201f     	nop
   50f70: 7108023f     	cmp	w17, #0x200
   50f74: 54001c22     	b.hs	0x512f8 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4b8>
   50f78: a9438381     	ldp	x1, x0, [x28, #0x38]
   50f7c: 8b100002     	add	x2, x0, x16
   50f80: eb02003f     	cmp	x1, x2
   50f84: 54001123     	b.lo	0x511a8 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x368>
   50f88: f9002382     	str	x2, [x28, #0x40]
   50f8c: b900000f     	str	w15, [x0]
   50f90: b900040e     	str	w14, [x0, #0x4]
   50f94: 91002001     	add	x1, x0, #0x8
   50f98: aa0d03e2     	mov	x2, x13
   50f9c: b4000162     	cbz	x2, 0x50fc8 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x188>
   50fa0: 361800a1     	tbz	w1, #0x3, 0x50fb4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x174>
   50fa4: d1002042     	sub	x2, x2, #0x8
   50fa8: f800843f     	str	xzr, [x1], #0x8
   50fac: 14000002     	b	0x50fb4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x174>
   50fb0: a8817c3f     	stp	xzr, xzr, [x1], #0x10
   50fb4: f1004042     	subs	x2, x2, #0x10
   50fb8: 54ffffca     	b.ge	0x50fb0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x170>
   50fbc: 91004042     	add	x2, x2, #0x10
   50fc0: 36180042     	tbz	w2, #0x3, 0x50fc8 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x188>
   50fc4: f800843f     	str	xzr, [x1], #0x8
   50fc8: d5033abf     	dmb	ishst
   50fcc: cb1b0000     	sub	x0, x0, x27
   50fd0: d343fc00     	lsr	x0, x0, #3
   50fd4: 91026189     	add	x9, x12, #0x98
   50fd8: 889ffd20     	stlr	w0, [x9]
   50fdc: b9400180     	ldr	w0, [x12]
   50fe0: 37080980     	tbnz	w0, #0x1, 0x51110 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x2d0>
   50fe4: 11000632     	add	w18, w17, #0x1
   50fe8: 72000e3f     	tst	w17, #0xf
   50fec: 54000060     	b.eq	0x50ff8 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1b8>
   50ff0: 2a1203f1     	mov	w17, w18
   50ff4: 17ffffdf     	b	0x50f70 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x130>
   50ff8: 91006169     	add	x9, x11, #0x18
   50ffc: 88dffd20     	ldar	w0, [x9]
   51000: 6b08001f     	cmp	w0, w8
   51004: 54000180     	b.eq	0x51034 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1f4>
   51008: 34000f20     	cbz	w0, 0x511ec <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3ac>
   5100c: 2a0003e0     	mov	w0, w0
   51010: 8b000f60     	add	x0, x27, x0, lsl #3
   51014: b9400001     	ldr	w1, [x0]
   51018: 53057c21     	lsr	w1, w1, #5
   5101c: 6b07003f     	cmp	w1, w7
   51020: 540017e1     	b.ne	0x5131c <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4dc>
   51024: 39401000     	ldrb	w0, [x0, #0x4]
   51028: 340000a0     	cbz	w0, 0x5103c <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1fc>
   5102c: 2a1203f1     	mov	w17, w18
   51030: 17ffffd0     	b	0x50f70 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x130>
   51034: aa0603e0     	mov	x0, x6
   51038: 17fffffb     	b	0x51024 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1e4>
   5103c: b9402460     	ldr	w0, [x3, #0x24]
   51040: 340000e0     	cbz	w0, 0x5105c <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x21c>
   51044: 2a0003e0     	mov	w0, w0
   51048: 8b000f60     	add	x0, x27, x0, lsl #3
   5104c: b9400000     	ldr	w0, [x0]
   51050: 53057c00     	lsr	w0, w0, #5
   51054: 6b05001f     	cmp	w0, w5
   51058: 54000740     	b.eq	0x51140 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x300>
   5105c: b9006ff2     	str	w18, [sp, #0x6c]
   51060: b90067f1     	str	w17, [sp, #0x64]
   51064: aa0303e0     	mov	x0, x3
   51068: aa0403e1     	mov	x1, x4
   5106c: 320003e2     	orr	w2, wzr, #0x1
   51070: 94020a30     	bl	0xd3930 <GCImpl_collectWithoutAllocating_3JkbhdqsFF0bYizLQcyXX9>
   51074: 340006a0     	cbz	w0, 0x51148 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x308>
   51078: f9400fe3     	ldr	x3, [sp, #0x18]
   5107c: b9402460     	ldr	w0, [x3, #0x24]
   51080: 340015e0     	cbz	w0, 0x5133c <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4fc>
   51084: 2a0003e0     	mov	w0, w0
   51088: 8b000f60     	add	x0, x27, x0, lsl #3
   5108c: b940001e     	ldr	w30, [x0]
   51090: 53057fc1     	lsr	w1, w30, #5
   51094: 2a0103e1     	mov	w1, w1
   51098: 8b010f61     	add	x1, x27, x1, lsl #3
   5109c: b9400821     	ldr	w1, [x1, #0x8]
   510a0: 128023e2     	mov	w2, #-0x120             // =-288
   510a4: 0b212041     	add	w1, w2, w1, uxth
   510a8: 7100303f     	cmp	w1, #0xc
   510ac: 540014c2     	b.hs	0x51344 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x504>
   510b0: 53057fde     	lsr	w30, w30, #5
   510b4: 2a1e03fe     	mov	w30, w30
   510b8: 8b1e0f7e     	add	x30, x27, x30, lsl #3
   510bc: f9404fde     	ldr	x30, [x30, #0x98]
   510c0: d503201f     	nop
   510c4: 103bef61     	adr	x1, 0xc8eb0 <CollectionPolicy_onMaximumHeapSizeExceeded_rghEcVm2KwLAMqoiYhXY02>
   510c8: cb1a0021     	sub	x1, x1, x26
   510cc: eb0103df     	cmp	x30, x1
   510d0: 54000bc0     	b.eq	0x51248 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x408>
   510d4: 8b1a03de     	add	x30, x30, x26
   510d8: d63f03c0     	blr	x30
   510dc: b9406ff1     	ldr	w17, [sp, #0x6c]
   510e0: d2800110     	mov	x16, #0x8               // =8
   510e4: f2a00030     	movk	x16, #0x1, lsl #16
   510e8: 52850c0f     	mov	w15, #0x2860            // =10336
   510ec: 72a0040f     	movk	w15, #0x20, lsl #16
   510f0: b9406bee     	ldr	w14, [sp, #0x68]
   510f4: f9402fed     	ldr	x13, [sp, #0x58]
   510f8: a942afec     	ldp	x12, x11, [sp, #0x28]
   510fc: 29499fe8     	ldp	w8, w7, [sp, #0x4c]
   51100: f9401fe6     	ldr	x6, [sp, #0x38]
   51104: b94057e5     	ldr	w5, [sp, #0x54]
   51108: a94193e3     	ldp	x3, x4, [sp, #0x18]
   5110c: 17ffff99     	b	0x50f70 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x130>
   51110: aa0c03e1     	mov	x1, x12
   51114: 37000340     	tbnz	w0, #0x0, 0x5117c <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x33c>
   51118: b26db3e0     	mov	x0, #-0x80000           // =-524288
   5111c: 8a000020     	and	x0, x1, x0
   51120: 91210002     	add	x2, x0, #0x840
   51124: cb020021     	sub	x1, x1, x2
   51128: b27d0be2     	orr	x2, xzr, #0x38
   5112c: 8b412441     	add	x1, x2, x1, lsr #9
   51130: 38e16802     	ldrsb	w2, [x0, x1]
   51134: 34fff582     	cbz	w2, 0x50fe4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1a4>
   51138: 3821681f     	strb	wzr, [x0, x1]
   5113c: 17ffffaa     	b	0x50fe4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1a4>
   51140: 2a1203f1     	mov	w17, w18
   51144: 17ffff8b     	b	0x50f70 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x130>
   51148: b9406ff1     	ldr	w17, [sp, #0x6c]
   5114c: d2800110     	mov	x16, #0x8               // =8
   51150: f2a00030     	movk	x16, #0x1, lsl #16
   51154: 52850c0f     	mov	w15, #0x2860            // =10336
   51158: 72a0040f     	movk	w15, #0x20, lsl #16
   5115c: b9406bee     	ldr	w14, [sp, #0x68]
   51160: f9402fed     	ldr	x13, [sp, #0x58]
   51164: a942afec     	ldp	x12, x11, [sp, #0x28]
   51168: 29499fe8     	ldp	w8, w7, [sp, #0x4c]
   5116c: f9401fe6     	ldr	x6, [sp, #0x38]
   51170: b94057e5     	ldr	w5, [sp, #0x54]
   51174: a94193e3     	ldp	x3, x4, [sp, #0x18]
   51178: 17ffff7e     	b	0x50f70 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x130>
   5117c: f85f8020     	ldur	x0, [x1, #-0x8]
   51180: aa0c03e2     	mov	x2, x12
   51184: cb000020     	sub	x0, x1, x0
   51188: 91026181     	add	x1, x12, #0x98
   5118c: cb020021     	sub	x1, x1, x2
   51190: b27c07e2     	orr	x2, xzr, #0x30
   51194: 8b412441     	add	x1, x2, x1, lsr #9
   51198: 38e16802     	ldrsb	w2, [x0, x1]
   5119c: 34fff242     	cbz	w2, 0x50fe4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1a4>
   511a0: 3821681f     	strb	wzr, [x0, x1]
   511a4: 17ffff90     	b	0x50fe4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x1a4>
   511a8: b90067f1     	str	w17, [sp, #0x64]
   511ac: 92407de0     	and	x0, x15, #0xffffffff
   511b0: b9406be1     	ldr	w1, [sp, #0x68]
   511b4: 940288a3     	bl	0xf3440 <GenScavengeAllocationSupport_slowNewArray_RH1677CN3I59XJMcBPW18F>
   511b8: b94067f1     	ldr	w17, [sp, #0x64]
   511bc: d2800110     	mov	x16, #0x8               // =8
   511c0: f2a00030     	movk	x16, #0x1, lsl #16
   511c4: 52850c0f     	mov	w15, #0x2860            // =10336
   511c8: 72a0040f     	movk	w15, #0x20, lsl #16
   511cc: b9406bee     	ldr	w14, [sp, #0x68]
   511d0: f9402fed     	ldr	x13, [sp, #0x58]
   511d4: a942afec     	ldp	x12, x11, [sp, #0x28]
   511d8: 29499fe8     	ldp	w8, w7, [sp, #0x4c]
   511dc: f9401fe6     	ldr	x6, [sp, #0x38]
   511e0: b94057e5     	ldr	w5, [sp, #0x54]
   511e4: a94193e3     	ldp	x3, x4, [sp, #0x18]
   511e8: 17ffff79     	b	0x50fcc <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x18c>
   511ec: b90067f1     	str	w17, [sp, #0x64]
   511f0: 940636c8     	bl	0x1ded10 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   511f4: b9404be1     	ldr	w1, [sp, #0x48]
   511f8: 34000a01     	cbz	w1, 0x51338 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4f8>
   511fc: f94023e2     	ldr	x2, [sp, #0x40]
   51200: cb1b0000     	sub	x0, x0, x27
   51204: d343fc00     	lsr	x0, x0, #3
   51208: 91001049     	add	x9, x2, #0x4
   5120c: 889ffd20     	stlr	w0, [x9]
   51210: b9400040     	ldr	w0, [x2]
   51214: 370801c0     	tbnz	w0, #0x1, 0x5124c <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x40c>
   51218: a9477bfd     	ldp	x29, x30, [sp, #0x70]
   5121c: 910203ff     	add	sp, sp, #0x80
   51220: b9401389     	ldr	w9, [x28, #0x10]
   51224: 7100013f     	cmp	w9, #0x0
   51228: 5400098d     	b.le	0x51358 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x518>
   5122c: d65f03c0     	ret
   51230: 7100047f     	cmp	w3, #0x1
   51234: 54ffe501     	b.ne	0x50ed4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
   51238: 91002009     	add	x9, x0, #0x8
   5123c: 88dffd3e     	ldar	w30, [x9]
   51240: 34ffe4be     	cbz	w30, 0x50ed4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
   51244: 1400000d     	b	0x51278 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x438>
   51248: 940310ee     	bl	0x115600 <OutOfMemoryUtil_heapSizeExceeded_tgNQlvtYRwAN6ucK4Y7ahE>
   5124c: b26db3fd     	mov	x29, #-0x80000          // =-524288
   51250: f94023e2     	ldr	x2, [sp, #0x40]
   51254: 8a1d005d     	and	x29, x2, x29
   51258: 912103be     	add	x30, x29, #0x840
   5125c: cb1e005e     	sub	x30, x2, x30
   51260: b27d0be0     	orr	x0, xzr, #0x38
   51264: 8b5e241e     	add	x30, x0, x30, lsr #9
   51268: 38fe6ba0     	ldrsb	w0, [x29, x30]
   5126c: 34fffd60     	cbz	w0, 0x51218 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3d8>
   51270: 383e6bbf     	strb	wzr, [x29, x30]
   51274: 17ffffe9     	b	0x51218 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3d8>
   51278: 2a1e03fe     	mov	w30, w30
   5127c: 8b1e0f7e     	add	x30, x27, x30, lsl #3
   51280: 910023c9     	add	x9, x30, #0x8
   51284: 88dffd3e     	ldar	w30, [x9]
   51288: 34ffe27e     	cbz	w30, 0x50ed4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
   5128c: 2a1e03e0     	mov	w0, w30
   51290: 8b000f60     	add	x0, x27, x0, lsl #3
   51294: 91004009     	add	x9, x0, #0x10
   51298: 88dffd3e     	ldar	w30, [x9]
   5129c: 34ffe1de     	cbz	w30, 0x50ed4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
   512a0: 91004009     	add	x9, x0, #0x10
   512a4: 88dffd23     	ldar	w3, [x9]
   512a8: 121f787e     	and	w30, w3, #0xfffffffe
   512ac: 91004004     	add	x4, x0, #0x10
   512b0: 885f7c85     	ldxr	w5, [x4]
   512b4: 6b0300bf     	cmp	w5, w3
   512b8: 1a8503ca     	csel	w10, w30, w5, eq
   512bc: 8809fc8a     	stlxr	w9, w10, [x4]
   512c0: 35ffff89     	cbnz	w9, 0x512b0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x470>
   512c4: d5033bbf     	dmb	ish
   512c8: 1a9f17fe     	cset	w30, eq
   512cc: 350000be     	cbnz	w30, 0x512e0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x4a0>
   512d0: b9401389     	ldr	w9, [x28, #0x10]
   512d4: 35fffe69     	cbnz	w9, 0x512a0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x460>
   512d8: 94069daa     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   512dc: 17fffff1     	b	0x512a0 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x460>
   512e0: b9400c00     	ldr	w0, [x0, #0xc]
   512e4: 34ffdf80     	cbz	w0, 0x50ed4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
   512e8: 2a0003e0     	mov	w0, w0
   512ec: 8b000f60     	add	x0, x27, x0, lsl #3
   512f0: 940696b0     	bl	0x1f6db0 <PlatformThreads_unpark_FJj4Yhv2Ja3jndtYAdmYl0>
   512f4: 17fffef8     	b	0x50ed4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x94>
   512f8: a9477bfd     	ldp	x29, x30, [sp, #0x70]
   512fc: 910203ff     	add	sp, sp, #0x80
   51300: b9401389     	ldr	w9, [x28, #0x10]
   51304: 7100013f     	cmp	w9, #0x0
   51308: 5400028d     	b.le	0x51358 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x518>
   5130c: d65f03c0     	ret
   51310: 17ffffb9     	b	0x511f4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
   51314: 17ffffb8     	b	0x511f4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
   51318: 17ffffb7     	b	0x511f4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
   5131c: b90067f1     	str	w17, [sp, #0x64]
   51320: 940634b4     	bl	0x1de5f0 <ImplicitExceptions_createOptClassCastException_Ewnh8dGuUvIm5VBYxiDX2K>
   51324: 17ffffb4     	b	0x511f4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
   51328: 9406367a     	bl	0x1ded10 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   5132c: 17ffffb2     	b	0x511f4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
   51330: 94063678     	bl	0x1ded10 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   51334: 17ffffb0     	b	0x511f4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
   51338: 940651e2     	bl	0x1e5ac0 <ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
   5133c: 94063675     	bl	0x1ded10 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   51340: 17ffffad     	b	0x511f4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
   51344: b94067f1     	ldr	w17, [sp, #0x64]
   51348: 9406358e     	bl	0x1de980 <ImplicitExceptions_createOptIncompatibleClassChangeError_ExDFiMUY3I5T00lGxxhlnL>
   5134c: 17ffffaa     	b	0x511f4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
   51350: 17ffffa9     	b	0x511f4 <Floor$$L78a8a022_run_bbe0oFsVNi6cAfruS8V5lI+0x3b4>
   51354: 1402bd4b     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51358: 14069d8a     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5135c: cccccccc     	<unknown>

# svm: Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI
0000000000051360 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI>:
   51360: d10143e9     	sub	x9, sp, #0x50
   51364: f940078a     	ldr	x10, [x28, #0x8]
   51368: eb0a013f     	cmp	x9, x10
   5136c: 54000f09     	b.ls	0x5154c <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1ec>
   51370: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51374: 9100013f     	mov	sp, x9
   51378: b90033e1     	str	w1, [sp, #0x30]
   5137c: 529ed200     	mov	w0, #0xf690             // =63120
   51380: 72a000e0     	movk	w0, #0x7, lsl #16
   51384: 8b000f60     	add	x0, x27, x0, lsl #3
   51388: f9402803     	ldr	x3, [x0, #0x50]
   5138c: f9000fe3     	str	x3, [sp, #0x18]
   51390: 7100043f     	cmp	w1, #0x1
   51394: 320003e0     	orr	w0, wzr, #0x1
   51398: 1a80d020     	csel	w0, w1, w0, le
   5139c: 7100041f     	cmp	w0, #0x1
   513a0: 54000d0b     	b.lt	0x51540 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1e0>
   513a4: aa0203e0     	mov	x0, x2
   513a8: aa0303fe     	mov	x30, x3
   513ac: d63f03c0     	blr	x30
   513b0: 320003fe     	orr	w30, wzr, #0x1
   513b4: b94033e1     	ldr	w1, [sp, #0x30]
   513b8: 52800082     	mov	w2, #0x4                // =4
   513bc: 72b00002     	movk	w2, #0x8000, lsl #16
   513c0: 320103e3     	orr	w3, wzr, #0x80000000
   513c4: 51001024     	sub	w4, w1, #0x4
   513c8: 6b02003f     	cmp	w1, w2
   513cc: 1a84b062     	csel	w2, w3, w4, lt
   513d0: b9002be2     	str	w2, [sp, #0x28]
   513d4: 11000c43     	add	w3, w2, #0x3
   513d8: b90027e3     	str	w3, [sp, #0x24]
   513dc: f9400fe4     	ldr	x4, [sp, #0x18]
   513e0: 2a1e03e1     	mov	w1, w30
   513e4: d503201f     	nop
   513e8: d503201f     	nop
   513ec: d503201f     	nop
   513f0: 6b02003f     	cmp	w1, w2
   513f4: 5400078a     	b.ge	0x514e4 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x184>
   513f8: 4b01007e     	sub	w30, w3, w1
   513fc: 53027fc6     	lsr	w6, w30, #2
   51400: 121e77de     	and	w30, w30, #0xfffffffc
   51404: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   51408: 321203e6     	orr	w6, wzr, #0x4000
   5140c: 1a8633c6     	csel	w6, w30, w6, lo
   51410: 710004df     	cmp	w6, #0x1
   51414: 5400060b     	b.lt	0x514d4 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x174>
   51418: b90037e1     	str	w1, [sp, #0x34]
   5141c: b9003fe6     	str	w6, [sp, #0x3c]
   51420: 2a1f03e2     	mov	w2, wzr
   51424: 14000008     	b	0x51444 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0xe4>
   51428: d503201f     	nop
   5142c: d503201f     	nop
   51430: d503201f     	nop
   51434: d503201f     	nop
   51438: d503201f     	nop
   5143c: d503201f     	nop
   51440: f9400fe4     	ldr	x4, [sp, #0x18]
   51444: 0b010048     	add	w8, w2, w1
   51448: b90023e8     	str	w8, [sp, #0x20]
   5144c: aa0403fe     	mov	x30, x4
   51450: b9002fe2     	str	w2, [sp, #0x2c]
   51454: d63f03c0     	blr	x30
   51458: b94023e8     	ldr	w8, [sp, #0x20]
   5145c: 11000501     	add	w1, w8, #0x1
   51460: f9400ffe     	ldr	x30, [sp, #0x18]
   51464: b9003be1     	str	w1, [sp, #0x38]
   51468: d63f03c0     	blr	x30
   5146c: b94023e8     	ldr	w8, [sp, #0x20]
   51470: 11000901     	add	w1, w8, #0x2
   51474: f9400ffe     	ldr	x30, [sp, #0x18]
   51478: b9003be1     	str	w1, [sp, #0x38]
   5147c: d63f03c0     	blr	x30
   51480: b94023e8     	ldr	w8, [sp, #0x20]
   51484: 11000d01     	add	w1, w8, #0x3
   51488: f9400ffe     	ldr	x30, [sp, #0x18]
   5148c: b90023e1     	str	w1, [sp, #0x20]
   51490: d63f03c0     	blr	x30
   51494: b9402fe2     	ldr	w2, [sp, #0x2c]
   51498: 11001042     	add	w2, w2, #0x4
   5149c: b9403fe6     	ldr	w6, [sp, #0x3c]
   514a0: 6b06005f     	cmp	w2, w6
   514a4: b94037e1     	ldr	w1, [sp, #0x34]
   514a8: 54fffccb     	b.lt	0x51440 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0xe0>
   514ac: 0b010041     	add	w1, w2, w1
   514b0: b9401389     	ldr	w9, [x28, #0x10]
   514b4: 34000089     	cbz	w9, 0x514c4 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x164>
   514b8: 29448be3     	ldp	w3, w2, [sp, #0x24]
   514bc: f9400fe4     	ldr	x4, [sp, #0x18]
   514c0: 17ffffcc     	b	0x513f0 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x90>
   514c4: 94069d2f     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   514c8: 29448be3     	ldp	w3, w2, [sp, #0x24]
   514cc: f9400fe4     	ldr	x4, [sp, #0x18]
   514d0: 17ffffc8     	b	0x513f0 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x90>
   514d4: b9401389     	ldr	w9, [x28, #0x10]
   514d8: 35fff8c9     	cbnz	w9, 0x513f0 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x90>
   514dc: 94069d29     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   514e0: 17ffffc4     	b	0x513f0 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x90>
   514e4: 2a0103e2     	mov	w2, w1
   514e8: aa0403e3     	mov	x3, x4
   514ec: 1400000c     	b	0x5151c <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1bc>
   514f0: d503201f     	nop
   514f4: d503201f     	nop
   514f8: d503201f     	nop
   514fc: d503201f     	nop
   51500: b9002fe2     	str	w2, [sp, #0x2c]
   51504: aa0303fe     	mov	x30, x3
   51508: d63f03c0     	blr	x30
   5150c: b9402fe2     	ldr	w2, [sp, #0x2c]
   51510: 1100045d     	add	w29, w2, #0x1
   51514: 2a1d03e2     	mov	w2, w29
   51518: f9400fe3     	ldr	x3, [sp, #0x18]
   5151c: b94033e1     	ldr	w1, [sp, #0x30]
   51520: 6b01005f     	cmp	w2, w1
   51524: 54fffeeb     	b.lt	0x51500 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1a0>
   51528: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   5152c: 910143ff     	add	sp, sp, #0x50
   51530: b9401389     	ldr	w9, [x28, #0x10]
   51534: 7100013f     	cmp	w9, #0x0
   51538: 540000cd     	b.le	0x51550 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x1f0>
   5153c: d65f03c0     	ret
   51540: 2a1f03fe     	mov	w30, wzr
   51544: aa0203e0     	mov	x0, x2
   51548: 17ffff9c     	b	0x513b8 <Floor$$L19f09f3c_run_Pnqjd0dOoyG9EFli4yq2gI+0x58>
   5154c: 1402bccd     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51550: 14069d0c     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51554: cccccccc     	<unknown>
   51558: cccccccc     	<unknown>
   5155c: cccccccc     	<unknown>

# svm: Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15
0000000000051560 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15>:
   51560: d101c3e9     	sub	x9, sp, #0x70
   51564: f940078a     	ldr	x10, [x28, #0x8]
   51568: eb0a013f     	cmp	x9, x10
   5156c: 54000869     	b.ls	0x51678 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x118>
   51570: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51574: 9100013f     	mov	sp, x9
   51578: b90037e1     	str	w1, [sp, #0x34]
   5157c: f00044e3     	adrp	x3, 0x8f0000 <.data>
   51580: f9446c63     	ldr	x3, [x3, #0x8d8]
   51584: f90017e3     	str	x3, [sp, #0x28]
   51588: aa0203e0     	mov	x0, x2
   5158c: 2a1f03e2     	mov	w2, wzr
   51590: 14000027     	b	0x5162c <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xcc>
   51594: d503201f     	nop
   51598: d503201f     	nop
   5159c: d503201f     	nop
   515a0: b90033e2     	str	w2, [sp, #0x30]
   515a4: 9100e3ed     	add	x13, sp, #0x38
   515a8: a900fdbf     	stp	xzr, xzr, [x13, #0x8]
   515ac: f9400f9e     	ldr	x30, [x28, #0x18]
   515b0: f9000dbe     	str	x30, [x13, #0x18]
   515b4: f9000f8d     	str	x13, [x28, #0x18]
   515b8: aa0303fe     	mov	x30, x3
   515bc: 10000029     	adr	x9, 0x515c0 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x60>
   515c0: f90005a9     	str	x9, [x13, #0x8]
   515c4: 910003e9     	mov	x9, sp
   515c8: f90009a9     	str	x9, [x13, #0x10]
   515cc: 320007e9     	orr	w9, wzr, #0x3
   515d0: 9100538a     	add	x10, x28, #0x14
   515d4: 889ffd49     	stlr	w9, [x10]
   515d8: d63f03c0     	blr	x30
   515dc: 320007e1     	orr	w1, wzr, #0x3
   515e0: 320003e2     	orr	w2, wzr, #0x1
   515e4: 91005383     	add	x3, x28, #0x14
   515e8: 885f7c64     	ldxr	w4, [x3]
   515ec: 6b01009f     	cmp	w4, w1
   515f0: 1a84004a     	csel	w10, w2, w4, eq
   515f4: 8809fc6a     	stlxr	w9, w10, [x3]
   515f8: 35ffff89     	cbnz	w9, 0x515e8 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x88>
   515fc: d5033bbf     	dmb	ish
   51600: 1a9f17e1     	cset	w1, eq
   51604: 340001a1     	cbz	w1, 0x51638 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xd8>
   51608: f9400f81     	ldr	x1, [x28, #0x18]
   5160c: f9400c21     	ldr	x1, [x1, #0x18]
   51610: f9000f81     	str	x1, [x28, #0x18]
   51614: b9402f81     	ldr	w1, [x28, #0x2c]
   51618: 350001c1     	cbnz	w1, 0x51650 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xf0>
   5161c: b94033e2     	ldr	w2, [sp, #0x30]
   51620: 11000442     	add	w2, w2, #0x1
   51624: f94017e3     	ldr	x3, [sp, #0x28]
   51628: b94037e1     	ldr	w1, [sp, #0x34]
   5162c: 6b01005f     	cmp	w2, w1
   51630: 54fffb8b     	b.lt	0x515a0 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x40>
   51634: 1400000b     	b	0x51660 <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x100>
   51638: f90013e0     	str	x0, [sp, #0x20]
   5163c: 320003e0     	orr	w0, wzr, #0x1
   51640: 320003e1     	orr	w1, wzr, #0x1
   51644: 94069f0b     	bl	0x1f9270 <SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
   51648: f94013e0     	ldr	x0, [sp, #0x20]
   5164c: 17fffff4     	b	0x5161c <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xbc>
   51650: f90013e0     	str	x0, [sp, #0x20]
   51654: 94069e9b     	bl	0x1f90c0 <SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
   51658: f94013e0     	ldr	x0, [sp, #0x20]
   5165c: 17fffff0     	b	0x5161c <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0xbc>
   51660: a9467bfd     	ldp	x29, x30, [sp, #0x60]
   51664: 9101c3ff     	add	sp, sp, #0x70
   51668: b9401389     	ldr	w9, [x28, #0x10]
   5166c: 7100013f     	cmp	w9, #0x0
   51670: 5400006d     	b.le	0x5167c <Floor$$Lf3aa6616_run_LwuK0mClf1JT64CYSZhR15+0x11c>
   51674: d65f03c0     	ret
   51678: 1402bc82     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   5167c: 14069cc1     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>

# svm: Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH
0000000000051680 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH>:
   51680: d10143e9     	sub	x9, sp, #0x50
   51684: f940078a     	ldr	x10, [x28, #0x8]
   51688: eb0a013f     	cmp	x9, x10
   5168c: 54000e09     	b.ls	0x5184c <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x1cc>
   51690: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51694: 9100013f     	mov	sp, x9
   51698: b90033e1     	str	w1, [sp, #0x30]
   5169c: f00044e3     	adrp	x3, 0x8f0000 <.data>
   516a0: f9431463     	ldr	x3, [x3, #0x628]
   516a4: f9000fe3     	str	x3, [sp, #0x18]
   516a8: 7100043f     	cmp	w1, #0x1
   516ac: 320003e0     	orr	w0, wzr, #0x1
   516b0: 1a80d020     	csel	w0, w1, w0, le
   516b4: 7100041f     	cmp	w0, #0x1
   516b8: 54000c4b     	b.lt	0x51840 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x1c0>
   516bc: aa0203e0     	mov	x0, x2
   516c0: aa0303fe     	mov	x30, x3
   516c4: d63f03c0     	blr	x30
   516c8: 320003fe     	orr	w30, wzr, #0x1
   516cc: b94033e1     	ldr	w1, [sp, #0x30]
   516d0: 52800082     	mov	w2, #0x4                // =4
   516d4: 72b00002     	movk	w2, #0x8000, lsl #16
   516d8: 320103e3     	orr	w3, wzr, #0x80000000
   516dc: 51001024     	sub	w4, w1, #0x4
   516e0: 6b02003f     	cmp	w1, w2
   516e4: 1a84b062     	csel	w2, w3, w4, lt
   516e8: b9002be2     	str	w2, [sp, #0x28]
   516ec: 11000c43     	add	w3, w2, #0x3
   516f0: b90027e3     	str	w3, [sp, #0x24]
   516f4: f9400fe4     	ldr	x4, [sp, #0x18]
   516f8: 2a1e03e1     	mov	w1, w30
   516fc: d503201f     	nop
   51700: 6b02003f     	cmp	w1, w2
   51704: 5400070a     	b.ge	0x517e4 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x164>
   51708: 4b01007e     	sub	w30, w3, w1
   5170c: 53027fc6     	lsr	w6, w30, #2
   51710: 121e77de     	and	w30, w30, #0xfffffffc
   51714: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   51718: 321203e6     	orr	w6, wzr, #0x4000
   5171c: 1a8633c6     	csel	w6, w30, w6, lo
   51720: 710004df     	cmp	w6, #0x1
   51724: 5400058b     	b.lt	0x517d4 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x154>
   51728: b90037e1     	str	w1, [sp, #0x34]
   5172c: b9003fe6     	str	w6, [sp, #0x3c]
   51730: 2a1f03e2     	mov	w2, wzr
   51734: 14000004     	b	0x51744 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0xc4>
   51738: d503201f     	nop
   5173c: d503201f     	nop
   51740: f9400fe4     	ldr	x4, [sp, #0x18]
   51744: 0b010048     	add	w8, w2, w1
   51748: b90023e8     	str	w8, [sp, #0x20]
   5174c: aa0403fe     	mov	x30, x4
   51750: b9002fe2     	str	w2, [sp, #0x2c]
   51754: d63f03c0     	blr	x30
   51758: b94023e8     	ldr	w8, [sp, #0x20]
   5175c: 11000501     	add	w1, w8, #0x1
   51760: f9400ffe     	ldr	x30, [sp, #0x18]
   51764: b9003be1     	str	w1, [sp, #0x38]
   51768: d63f03c0     	blr	x30
   5176c: b94023e8     	ldr	w8, [sp, #0x20]
   51770: 11000901     	add	w1, w8, #0x2
   51774: f9400ffe     	ldr	x30, [sp, #0x18]
   51778: b9003be1     	str	w1, [sp, #0x38]
   5177c: d63f03c0     	blr	x30
   51780: b94023e8     	ldr	w8, [sp, #0x20]
   51784: 11000d01     	add	w1, w8, #0x3
   51788: f9400ffe     	ldr	x30, [sp, #0x18]
   5178c: b90023e1     	str	w1, [sp, #0x20]
   51790: d63f03c0     	blr	x30
   51794: b9402fe2     	ldr	w2, [sp, #0x2c]
   51798: 11001042     	add	w2, w2, #0x4
   5179c: b9403fe6     	ldr	w6, [sp, #0x3c]
   517a0: 6b06005f     	cmp	w2, w6
   517a4: b94037e1     	ldr	w1, [sp, #0x34]
   517a8: 54fffccb     	b.lt	0x51740 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0xc0>
   517ac: 0b010041     	add	w1, w2, w1
   517b0: b9401389     	ldr	w9, [x28, #0x10]
   517b4: 34000089     	cbz	w9, 0x517c4 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x144>
   517b8: 29448be3     	ldp	w3, w2, [sp, #0x24]
   517bc: f9400fe4     	ldr	x4, [sp, #0x18]
   517c0: 17ffffd0     	b	0x51700 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x80>
   517c4: 94069c6f     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   517c8: 29448be3     	ldp	w3, w2, [sp, #0x24]
   517cc: f9400fe4     	ldr	x4, [sp, #0x18]
   517d0: 17ffffcc     	b	0x51700 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x80>
   517d4: b9401389     	ldr	w9, [x28, #0x10]
   517d8: 35fff949     	cbnz	w9, 0x51700 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x80>
   517dc: 94069c69     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   517e0: 17ffffc8     	b	0x51700 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x80>
   517e4: 2a0103e2     	mov	w2, w1
   517e8: aa0403e3     	mov	x3, x4
   517ec: 1400000c     	b	0x5181c <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x19c>
   517f0: d503201f     	nop
   517f4: d503201f     	nop
   517f8: d503201f     	nop
   517fc: d503201f     	nop
   51800: b9002fe2     	str	w2, [sp, #0x2c]
   51804: aa0303fe     	mov	x30, x3
   51808: d63f03c0     	blr	x30
   5180c: b9402fe2     	ldr	w2, [sp, #0x2c]
   51810: 1100045d     	add	w29, w2, #0x1
   51814: 2a1d03e2     	mov	w2, w29
   51818: f9400fe3     	ldr	x3, [sp, #0x18]
   5181c: b94033e1     	ldr	w1, [sp, #0x30]
   51820: 6b01005f     	cmp	w2, w1
   51824: 54fffeeb     	b.lt	0x51800 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x180>
   51828: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   5182c: 910143ff     	add	sp, sp, #0x50
   51830: b9401389     	ldr	w9, [x28, #0x10]
   51834: 7100013f     	cmp	w9, #0x0
   51838: 540000cd     	b.le	0x51850 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x1d0>
   5183c: d65f03c0     	ret
   51840: 2a1f03fe     	mov	w30, wzr
   51844: aa0203e0     	mov	x0, x2
   51848: 17ffffa2     	b	0x516d0 <Floor$$L32d8fcd4_run_tFtMC63tse0rFNl45HhnrH+0x50>
   5184c: 1402bc0d     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51850: 14069c4c     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51854: cccccccc     	<unknown>
   51858: cccccccc     	<unknown>
   5185c: cccccccc     	<unknown>

# svm: Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB
0000000000051860 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB>:
   51860: d10103e9     	sub	x9, sp, #0x40
   51864: f940078a     	ldr	x10, [x28, #0x8]
   51868: eb0a013f     	cmp	x9, x10
   5186c: 540008c9     	b.ls	0x51984 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x124>
   51870: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51874: 9100013f     	mov	sp, x9
   51878: 7100043f     	cmp	w1, #0x1
   5187c: 5400080b     	b.lt	0x5197c <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x11c>
   51880: b9002fe1     	str	w1, [sp, #0x2c]
   51884: 529ed200     	mov	w0, #0xf690             // =63120
   51888: 72a000e0     	movk	w0, #0x7, lsl #16
   5188c: 8b000f60     	add	x0, x27, x0, lsl #3
   51890: f9402803     	ldr	x3, [x0, #0x50]
   51894: f90013e3     	str	x3, [sp, #0x20]
   51898: 2a1f03e9     	mov	w9, wzr
   5189c: b9002be9     	str	w9, [sp, #0x28]
   518a0: aa0203e0     	mov	x0, x2
   518a4: aa0303fe     	mov	x30, x3
   518a8: d63f03c0     	blr	x30
   518ac: f94013fe     	ldr	x30, [sp, #0x20]
   518b0: d63f03c0     	blr	x30
   518b4: f94013fe     	ldr	x30, [sp, #0x20]
   518b8: d63f03c0     	blr	x30
   518bc: f94013fe     	ldr	x30, [sp, #0x20]
   518c0: d63f03c0     	blr	x30
   518c4: f94013fe     	ldr	x30, [sp, #0x20]
   518c8: d63f03c0     	blr	x30
   518cc: f94013fe     	ldr	x30, [sp, #0x20]
   518d0: d63f03c0     	blr	x30
   518d4: f94013fe     	ldr	x30, [sp, #0x20]
   518d8: d63f03c0     	blr	x30
   518dc: f94013fe     	ldr	x30, [sp, #0x20]
   518e0: d63f03c0     	blr	x30
   518e4: f94013fe     	ldr	x30, [sp, #0x20]
   518e8: d63f03c0     	blr	x30
   518ec: f94013fe     	ldr	x30, [sp, #0x20]
   518f0: d63f03c0     	blr	x30
   518f4: f94013fe     	ldr	x30, [sp, #0x20]
   518f8: d63f03c0     	blr	x30
   518fc: f94013fe     	ldr	x30, [sp, #0x20]
   51900: d63f03c0     	blr	x30
   51904: f94013fe     	ldr	x30, [sp, #0x20]
   51908: d63f03c0     	blr	x30
   5190c: f94013fe     	ldr	x30, [sp, #0x20]
   51910: d63f03c0     	blr	x30
   51914: f94013fe     	ldr	x30, [sp, #0x20]
   51918: d63f03c0     	blr	x30
   5191c: f94013fe     	ldr	x30, [sp, #0x20]
   51920: d63f03c0     	blr	x30
   51924: b9402bfd     	ldr	w29, [sp, #0x28]
   51928: 110043a1     	add	w1, w29, #0x10
   5192c: b9402fe2     	ldr	w2, [sp, #0x2c]
   51930: 6b02003f     	cmp	w1, w2
   51934: 5400018a     	b.ge	0x51964 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x104>
   51938: b9401389     	ldr	w9, [x28, #0x10]
   5193c: 340000a9     	cbz	w9, 0x51950 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0xf0>
   51940: b9002be1     	str	w1, [sp, #0x28]
   51944: f94013e3     	ldr	x3, [sp, #0x20]
   51948: aa0003e2     	mov	x2, x0
   5194c: 17ffffd5     	b	0x518a0 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x40>
   51950: 94069c0c     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51954: b9002be1     	str	w1, [sp, #0x28]
   51958: f94013e3     	ldr	x3, [sp, #0x20]
   5195c: aa0003e2     	mov	x2, x0
   51960: 17ffffd0     	b	0x518a0 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x40>
   51964: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   51968: 910103ff     	add	sp, sp, #0x40
   5196c: b9401389     	ldr	w9, [x28, #0x10]
   51970: 7100013f     	cmp	w9, #0x0
   51974: 540000ad     	b.le	0x51988 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x128>
   51978: d65f03c0     	ret
   5197c: aa0203e0     	mov	x0, x2
   51980: 17fffff9     	b	0x51964 <Floor$$L02f0279f_run_1BowCVAkR87smhvJR0albB+0x104>
   51984: 1402bbbf     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51988: 14069bfe     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5198c: cccccccc     	<unknown>

# svm: Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK
0000000000051990 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK>:
   51990: d10103e9     	sub	x9, sp, #0x40
   51994: f940078a     	ldr	x10, [x28, #0x8]
   51998: eb0a013f     	cmp	x9, x10
   5199c: 540008c9     	b.ls	0x51ab4 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x124>
   519a0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   519a4: 9100013f     	mov	sp, x9
   519a8: 7100043f     	cmp	w1, #0x1
   519ac: 5400080b     	b.lt	0x51aac <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x11c>
   519b0: b9002fe1     	str	w1, [sp, #0x2c]
   519b4: 529ed200     	mov	w0, #0xf690             // =63120
   519b8: 72a000e0     	movk	w0, #0x7, lsl #16
   519bc: 8b000f60     	add	x0, x27, x0, lsl #3
   519c0: f9402803     	ldr	x3, [x0, #0x50]
   519c4: f90013e3     	str	x3, [sp, #0x20]
   519c8: 2a1f03e9     	mov	w9, wzr
   519cc: b9002be9     	str	w9, [sp, #0x28]
   519d0: aa0203e0     	mov	x0, x2
   519d4: aa0303fe     	mov	x30, x3
   519d8: d63f03c0     	blr	x30
   519dc: f94013fe     	ldr	x30, [sp, #0x20]
   519e0: d63f03c0     	blr	x30
   519e4: f94013fe     	ldr	x30, [sp, #0x20]
   519e8: d63f03c0     	blr	x30
   519ec: f94013fe     	ldr	x30, [sp, #0x20]
   519f0: d63f03c0     	blr	x30
   519f4: f94013fe     	ldr	x30, [sp, #0x20]
   519f8: d63f03c0     	blr	x30
   519fc: f94013fe     	ldr	x30, [sp, #0x20]
   51a00: d63f03c0     	blr	x30
   51a04: f94013fe     	ldr	x30, [sp, #0x20]
   51a08: d63f03c0     	blr	x30
   51a0c: f94013fe     	ldr	x30, [sp, #0x20]
   51a10: d63f03c0     	blr	x30
   51a14: f94013fe     	ldr	x30, [sp, #0x20]
   51a18: d63f03c0     	blr	x30
   51a1c: f94013fe     	ldr	x30, [sp, #0x20]
   51a20: d63f03c0     	blr	x30
   51a24: f94013fe     	ldr	x30, [sp, #0x20]
   51a28: d63f03c0     	blr	x30
   51a2c: f94013fe     	ldr	x30, [sp, #0x20]
   51a30: d63f03c0     	blr	x30
   51a34: f94013fe     	ldr	x30, [sp, #0x20]
   51a38: d63f03c0     	blr	x30
   51a3c: f94013fe     	ldr	x30, [sp, #0x20]
   51a40: d63f03c0     	blr	x30
   51a44: f94013fe     	ldr	x30, [sp, #0x20]
   51a48: d63f03c0     	blr	x30
   51a4c: f94013fe     	ldr	x30, [sp, #0x20]
   51a50: d63f03c0     	blr	x30
   51a54: b9402bfd     	ldr	w29, [sp, #0x28]
   51a58: 110043a1     	add	w1, w29, #0x10
   51a5c: b9402fe2     	ldr	w2, [sp, #0x2c]
   51a60: 6b02003f     	cmp	w1, w2
   51a64: 5400018a     	b.ge	0x51a94 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x104>
   51a68: b9401389     	ldr	w9, [x28, #0x10]
   51a6c: 340000a9     	cbz	w9, 0x51a80 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0xf0>
   51a70: b9002be1     	str	w1, [sp, #0x28]
   51a74: f94013e3     	ldr	x3, [sp, #0x20]
   51a78: aa0003e2     	mov	x2, x0
   51a7c: 17ffffd5     	b	0x519d0 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x40>
   51a80: 94069bc0     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51a84: b9002be1     	str	w1, [sp, #0x28]
   51a88: f94013e3     	ldr	x3, [sp, #0x20]
   51a8c: aa0003e2     	mov	x2, x0
   51a90: 17ffffd0     	b	0x519d0 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x40>
   51a94: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   51a98: 910103ff     	add	sp, sp, #0x40
   51a9c: b9401389     	ldr	w9, [x28, #0x10]
   51aa0: 7100013f     	cmp	w9, #0x0
   51aa4: 540000ad     	b.le	0x51ab8 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x128>
   51aa8: d65f03c0     	ret
   51aac: aa0203e0     	mov	x0, x2
   51ab0: 17fffff9     	b	0x51a94 <Floor$$Lf4e8b095_run_paqoqlY3rVDS05dotmOwtK+0x104>
   51ab4: 1402bb73     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51ab8: 14069bb2     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51abc: cccccccc     	<unknown>

# svm: Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6
0000000000051ac0 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6>:
   51ac0: d10143e9     	sub	x9, sp, #0x50
   51ac4: f940078a     	ldr	x10, [x28, #0x8]
   51ac8: eb0a013f     	cmp	x9, x10
   51acc: 54000e09     	b.ls	0x51c8c <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x1cc>
   51ad0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51ad4: 9100013f     	mov	sp, x9
   51ad8: b90033e1     	str	w1, [sp, #0x30]
   51adc: f00044e3     	adrp	x3, 0x8f0000 <.data>
   51ae0: f943ec63     	ldr	x3, [x3, #0x7d8]
   51ae4: f9000fe3     	str	x3, [sp, #0x18]
   51ae8: 7100043f     	cmp	w1, #0x1
   51aec: 320003e0     	orr	w0, wzr, #0x1
   51af0: 1a80d020     	csel	w0, w1, w0, le
   51af4: 7100041f     	cmp	w0, #0x1
   51af8: 54000c4b     	b.lt	0x51c80 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x1c0>
   51afc: aa0203e0     	mov	x0, x2
   51b00: aa0303fe     	mov	x30, x3
   51b04: d63f03c0     	blr	x30
   51b08: 320003fe     	orr	w30, wzr, #0x1
   51b0c: b94033e1     	ldr	w1, [sp, #0x30]
   51b10: 52800082     	mov	w2, #0x4                // =4
   51b14: 72b00002     	movk	w2, #0x8000, lsl #16
   51b18: 320103e3     	orr	w3, wzr, #0x80000000
   51b1c: 51001024     	sub	w4, w1, #0x4
   51b20: 6b02003f     	cmp	w1, w2
   51b24: 1a84b062     	csel	w2, w3, w4, lt
   51b28: b9002be2     	str	w2, [sp, #0x28]
   51b2c: 11000c43     	add	w3, w2, #0x3
   51b30: b90027e3     	str	w3, [sp, #0x24]
   51b34: f9400fe4     	ldr	x4, [sp, #0x18]
   51b38: 2a1e03e1     	mov	w1, w30
   51b3c: d503201f     	nop
   51b40: 6b02003f     	cmp	w1, w2
   51b44: 5400070a     	b.ge	0x51c24 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x164>
   51b48: 4b01007e     	sub	w30, w3, w1
   51b4c: 53027fc6     	lsr	w6, w30, #2
   51b50: 121e77de     	and	w30, w30, #0xfffffffc
   51b54: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   51b58: 321203e6     	orr	w6, wzr, #0x4000
   51b5c: 1a8633c6     	csel	w6, w30, w6, lo
   51b60: 710004df     	cmp	w6, #0x1
   51b64: 5400058b     	b.lt	0x51c14 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x154>
   51b68: b90037e1     	str	w1, [sp, #0x34]
   51b6c: b9003fe6     	str	w6, [sp, #0x3c]
   51b70: 2a1f03e2     	mov	w2, wzr
   51b74: 14000004     	b	0x51b84 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0xc4>
   51b78: d503201f     	nop
   51b7c: d503201f     	nop
   51b80: f9400fe4     	ldr	x4, [sp, #0x18]
   51b84: 0b010048     	add	w8, w2, w1
   51b88: b90023e8     	str	w8, [sp, #0x20]
   51b8c: aa0403fe     	mov	x30, x4
   51b90: b9002fe2     	str	w2, [sp, #0x2c]
   51b94: d63f03c0     	blr	x30
   51b98: b94023e8     	ldr	w8, [sp, #0x20]
   51b9c: 11000501     	add	w1, w8, #0x1
   51ba0: f9400ffe     	ldr	x30, [sp, #0x18]
   51ba4: b9003be1     	str	w1, [sp, #0x38]
   51ba8: d63f03c0     	blr	x30
   51bac: b94023e8     	ldr	w8, [sp, #0x20]
   51bb0: 11000901     	add	w1, w8, #0x2
   51bb4: f9400ffe     	ldr	x30, [sp, #0x18]
   51bb8: b9003be1     	str	w1, [sp, #0x38]
   51bbc: d63f03c0     	blr	x30
   51bc0: b94023e8     	ldr	w8, [sp, #0x20]
   51bc4: 11000d01     	add	w1, w8, #0x3
   51bc8: f9400ffe     	ldr	x30, [sp, #0x18]
   51bcc: b90023e1     	str	w1, [sp, #0x20]
   51bd0: d63f03c0     	blr	x30
   51bd4: b9402fe2     	ldr	w2, [sp, #0x2c]
   51bd8: 11001042     	add	w2, w2, #0x4
   51bdc: b9403fe6     	ldr	w6, [sp, #0x3c]
   51be0: 6b06005f     	cmp	w2, w6
   51be4: b94037e1     	ldr	w1, [sp, #0x34]
   51be8: 54fffccb     	b.lt	0x51b80 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0xc0>
   51bec: 0b010041     	add	w1, w2, w1
   51bf0: b9401389     	ldr	w9, [x28, #0x10]
   51bf4: 34000089     	cbz	w9, 0x51c04 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x144>
   51bf8: 29448be3     	ldp	w3, w2, [sp, #0x24]
   51bfc: f9400fe4     	ldr	x4, [sp, #0x18]
   51c00: 17ffffd0     	b	0x51b40 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x80>
   51c04: 94069b5f     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51c08: 29448be3     	ldp	w3, w2, [sp, #0x24]
   51c0c: f9400fe4     	ldr	x4, [sp, #0x18]
   51c10: 17ffffcc     	b	0x51b40 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x80>
   51c14: b9401389     	ldr	w9, [x28, #0x10]
   51c18: 35fff949     	cbnz	w9, 0x51b40 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x80>
   51c1c: 94069b59     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51c20: 17ffffc8     	b	0x51b40 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x80>
   51c24: 2a0103e2     	mov	w2, w1
   51c28: aa0403e3     	mov	x3, x4
   51c2c: 1400000c     	b	0x51c5c <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x19c>
   51c30: d503201f     	nop
   51c34: d503201f     	nop
   51c38: d503201f     	nop
   51c3c: d503201f     	nop
   51c40: b9002fe2     	str	w2, [sp, #0x2c]
   51c44: aa0303fe     	mov	x30, x3
   51c48: d63f03c0     	blr	x30
   51c4c: b9402fe2     	ldr	w2, [sp, #0x2c]
   51c50: 1100045d     	add	w29, w2, #0x1
   51c54: 2a1d03e2     	mov	w2, w29
   51c58: f9400fe3     	ldr	x3, [sp, #0x18]
   51c5c: b94033e1     	ldr	w1, [sp, #0x30]
   51c60: 6b01005f     	cmp	w2, w1
   51c64: 54fffeeb     	b.lt	0x51c40 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x180>
   51c68: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   51c6c: 910143ff     	add	sp, sp, #0x50
   51c70: b9401389     	ldr	w9, [x28, #0x10]
   51c74: 7100013f     	cmp	w9, #0x0
   51c78: 540000cd     	b.le	0x51c90 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x1d0>
   51c7c: d65f03c0     	ret
   51c80: 2a1f03fe     	mov	w30, wzr
   51c84: aa0203e0     	mov	x0, x2
   51c88: 17ffffa2     	b	0x51b10 <Floor$$L38a94f92_run_AdD6BRDGzy7EOMAFyxUli6+0x50>
   51c8c: 1402bafd     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51c90: 14069b3c     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51c94: cccccccc     	<unknown>
   51c98: cccccccc     	<unknown>
   51c9c: cccccccc     	<unknown>

# svm: Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2
0000000000051ca0 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2>:
   51ca0: d10103e9     	sub	x9, sp, #0x40
   51ca4: f940078a     	ldr	x10, [x28, #0x8]
   51ca8: eb0a013f     	cmp	x9, x10
   51cac: 54000689     	b.ls	0x51d7c <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0xdc>
   51cb0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51cb4: 9100013f     	mov	sp, x9
   51cb8: b9002fe1     	str	w1, [sp, #0x2c]
   51cbc: 7100043f     	cmp	w1, #0x1
   51cc0: 5400022b     	b.lt	0x51d04 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x64>
   51cc4: 529ed203     	mov	w3, #0xf690             // =63120
   51cc8: 72a000e3     	movk	w3, #0x7, lsl #16
   51ccc: 8b030f63     	add	x3, x27, x3, lsl #3
   51cd0: f90013e3     	str	x3, [sp, #0x20]
   51cd4: f9402460     	ldr	x0, [x3, #0x48]
   51cd8: aa0203e1     	mov	x1, x2
   51cdc: 940000d9     	bl	0x52040 <Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA>
   51ce0: b9402fe1     	ldr	w1, [sp, #0x2c]
   51ce4: 7100443f     	cmp	w1, #0x11
   51ce8: 540001ca     	b.ge	0x51d20 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x80>
   51cec: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   51cf0: 910103ff     	add	sp, sp, #0x40
   51cf4: b9401389     	ldr	w9, [x28, #0x10]
   51cf8: 7100013f     	cmp	w9, #0x0
   51cfc: 5400042d     	b.le	0x51d80 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0xe0>
   51d00: d65f03c0     	ret
   51d04: aa0203e0     	mov	x0, x2
   51d08: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   51d0c: 910103ff     	add	sp, sp, #0x40
   51d10: b9401389     	ldr	w9, [x28, #0x10]
   51d14: 7100013f     	cmp	w9, #0x0
   51d18: 5400034d     	b.le	0x51d80 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0xe0>
   51d1c: d65f03c0     	ret
   51d20: 321c03e9     	orr	w9, wzr, #0x10
   51d24: b9002be9     	str	w9, [sp, #0x28]
   51d28: f94013e3     	ldr	x3, [sp, #0x20]
   51d2c: aa0003e1     	mov	x1, x0
   51d30: f9402460     	ldr	x0, [x3, #0x48]
   51d34: 940000c3     	bl	0x52040 <Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA>
   51d38: b9402be1     	ldr	w1, [sp, #0x28]
   51d3c: 11004022     	add	w2, w1, #0x10
   51d40: b9402fe3     	ldr	w3, [sp, #0x2c]
   51d44: 6b03005f     	cmp	w2, w3
   51d48: 54fffd2a     	b.ge	0x51cec <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x4c>
   51d4c: b9401389     	ldr	w9, [x28, #0x10]
   51d50: 340000a9     	cbz	w9, 0x51d64 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0xc4>
   51d54: aa0003e1     	mov	x1, x0
   51d58: b9002be2     	str	w2, [sp, #0x28]
   51d5c: f94013e3     	ldr	x3, [sp, #0x20]
   51d60: 17fffff4     	b	0x51d30 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x90>
   51d64: f94013e4     	ldr	x4, [sp, #0x20]
   51d68: 94069b06     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51d6c: aa0003e1     	mov	x1, x0
   51d70: b9002be2     	str	w2, [sp, #0x28]
   51d74: aa0403e3     	mov	x3, x4
   51d78: 17ffffee     	b	0x51d30 <Floor$$L7842a2eb_run_20AKGFyYt4EYcppUqZq0a2+0x90>
   51d7c: 1402bac1     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51d80: 14069b00     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51d84: cccccccc     	<unknown>
   51d88: cccccccc     	<unknown>
   51d8c: cccccccc     	<unknown>

# svm: Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10
0000000000051d90 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10>:
   51d90: d10103e9     	sub	x9, sp, #0x40
   51d94: f940078a     	ldr	x10, [x28, #0x8]
   51d98: eb0a013f     	cmp	x9, x10
   51d9c: 54000649     	b.ls	0x51e64 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0xd4>
   51da0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51da4: 9100013f     	mov	sp, x9
   51da8: b9002fe1     	str	w1, [sp, #0x2c]
   51dac: 7100043f     	cmp	w1, #0x1
   51db0: 5400020b     	b.lt	0x51df0 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x60>
   51db4: f00044e3     	adrp	x3, 0x8f0000 <.data>
   51db8: f9402463     	ldr	x3, [x3, #0x48]
   51dbc: f90013e3     	str	x3, [sp, #0x20]
   51dc0: aa0203e0     	mov	x0, x2
   51dc4: aa0303fe     	mov	x30, x3
   51dc8: d63f03c0     	blr	x30
   51dcc: b9402fe1     	ldr	w1, [sp, #0x2c]
   51dd0: 7101043f     	cmp	w1, #0x41
   51dd4: 540001ca     	b.ge	0x51e0c <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x7c>
   51dd8: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   51ddc: 910103ff     	add	sp, sp, #0x40
   51de0: b9401389     	ldr	w9, [x28, #0x10]
   51de4: 7100013f     	cmp	w9, #0x0
   51de8: 5400040d     	b.le	0x51e68 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0xd8>
   51dec: d65f03c0     	ret
   51df0: aa0203e0     	mov	x0, x2
   51df4: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   51df8: 910103ff     	add	sp, sp, #0x40
   51dfc: b9401389     	ldr	w9, [x28, #0x10]
   51e00: 7100013f     	cmp	w9, #0x0
   51e04: 5400032d     	b.le	0x51e68 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0xd8>
   51e08: d65f03c0     	ret
   51e0c: 321a03e9     	orr	w9, wzr, #0x40
   51e10: b9002be9     	str	w9, [sp, #0x28]
   51e14: f94013e3     	ldr	x3, [sp, #0x20]
   51e18: d503201f     	nop
   51e1c: d503201f     	nop
   51e20: aa0303fe     	mov	x30, x3
   51e24: d63f03c0     	blr	x30
   51e28: b9402be1     	ldr	w1, [sp, #0x28]
   51e2c: 11010022     	add	w2, w1, #0x40
   51e30: b9402fe3     	ldr	w3, [sp, #0x2c]
   51e34: 6b03005f     	cmp	w2, w3
   51e38: 54fffd0a     	b.ge	0x51dd8 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x48>
   51e3c: b9401389     	ldr	w9, [x28, #0x10]
   51e40: 34000089     	cbz	w9, 0x51e50 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0xc0>
   51e44: b9002be2     	str	w2, [sp, #0x28]
   51e48: f94013e3     	ldr	x3, [sp, #0x20]
   51e4c: 17fffff5     	b	0x51e20 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x90>
   51e50: f94013e4     	ldr	x4, [sp, #0x20]
   51e54: 94069acb     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51e58: b9002be2     	str	w2, [sp, #0x28]
   51e5c: aa0403e3     	mov	x3, x4
   51e60: 17fffff0     	b	0x51e20 <Floor$$Lb79431a0_run_FaQJriQJPQ9ZWaH1xGlV10+0x90>
   51e64: 1402ba87     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51e68: 14069ac6     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51e6c: cccccccc     	<unknown>

# svm: Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL
0000000000051e70 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL>:
   51e70: d10103e9     	sub	x9, sp, #0x40
   51e74: f940078a     	ldr	x10, [x28, #0x8]
   51e78: eb0a013f     	cmp	x9, x10
   51e7c: 54000649     	b.ls	0x51f44 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0xd4>
   51e80: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51e84: 9100013f     	mov	sp, x9
   51e88: b9002fe1     	str	w1, [sp, #0x2c]
   51e8c: 7100043f     	cmp	w1, #0x1
   51e90: 5400020b     	b.lt	0x51ed0 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x60>
   51e94: f00044e3     	adrp	x3, 0x8f0000 <.data>
   51e98: f943f863     	ldr	x3, [x3, #0x7f0]
   51e9c: f90013e3     	str	x3, [sp, #0x20]
   51ea0: aa0203e0     	mov	x0, x2
   51ea4: aa0303fe     	mov	x30, x3
   51ea8: d63f03c0     	blr	x30
   51eac: b9402fe1     	ldr	w1, [sp, #0x2c]
   51eb0: 7100143f     	cmp	w1, #0x5
   51eb4: 540001ca     	b.ge	0x51eec <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x7c>
   51eb8: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   51ebc: 910103ff     	add	sp, sp, #0x40
   51ec0: b9401389     	ldr	w9, [x28, #0x10]
   51ec4: 7100013f     	cmp	w9, #0x0
   51ec8: 5400040d     	b.le	0x51f48 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0xd8>
   51ecc: d65f03c0     	ret
   51ed0: aa0203e0     	mov	x0, x2
   51ed4: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   51ed8: 910103ff     	add	sp, sp, #0x40
   51edc: b9401389     	ldr	w9, [x28, #0x10]
   51ee0: 7100013f     	cmp	w9, #0x0
   51ee4: 5400032d     	b.le	0x51f48 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0xd8>
   51ee8: d65f03c0     	ret
   51eec: 321e03e9     	orr	w9, wzr, #0x4
   51ef0: b9002be9     	str	w9, [sp, #0x28]
   51ef4: f94013e3     	ldr	x3, [sp, #0x20]
   51ef8: d503201f     	nop
   51efc: d503201f     	nop
   51f00: aa0303fe     	mov	x30, x3
   51f04: d63f03c0     	blr	x30
   51f08: b9402be1     	ldr	w1, [sp, #0x28]
   51f0c: 11001022     	add	w2, w1, #0x4
   51f10: b9402fe3     	ldr	w3, [sp, #0x2c]
   51f14: 6b03005f     	cmp	w2, w3
   51f18: 54fffd0a     	b.ge	0x51eb8 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x48>
   51f1c: b9401389     	ldr	w9, [x28, #0x10]
   51f20: 34000089     	cbz	w9, 0x51f30 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0xc0>
   51f24: b9002be2     	str	w2, [sp, #0x28]
   51f28: f94013e3     	ldr	x3, [sp, #0x20]
   51f2c: 17fffff5     	b	0x51f00 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x90>
   51f30: f94013e4     	ldr	x4, [sp, #0x20]
   51f34: 94069a93     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51f38: b9002be2     	str	w2, [sp, #0x28]
   51f3c: aa0403e3     	mov	x3, x4
   51f40: 17fffff0     	b	0x51f00 <Floor$$Lbf255646_run_HnWLhWlZva2JScv3WxJYnL+0x90>
   51f44: 1402ba4f     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51f48: 14069a8e     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51f4c: cccccccc     	<unknown>

# svm: Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D
0000000000051f50 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D>:
   51f50: d100c3e9     	sub	x9, sp, #0x30
   51f54: f940078a     	ldr	x10, [x28, #0x8]
   51f58: eb0a013f     	cmp	x9, x10
   51f5c: 54000589     	b.ls	0x5200c <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0xbc>
   51f60: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51f64: 9100013f     	mov	sp, x9
   51f68: d28cc1a0     	mov	x0, #0x660d             // =26125
   51f6c: f2a00320     	movk	x0, #0x19, lsl #16
   51f70: d29e6bfd     	mov	x29, #0xf35f            // =62303
   51f74: f2a78ddd     	movk	x29, #0x3c6e, lsl #16
   51f78: 2a1f03fe     	mov	w30, wzr
   51f7c: d503201f     	nop
   51f80: 6b0103df     	cmp	w30, w1
   51f84: 5400036a     	b.ge	0x51ff0 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0xa0>
   51f88: 4b1e0023     	sub	w3, w1, w30
   51f8c: 7140047f     	cmp	w3, #0x1, lsl #12       // =0x1000
   51f90: 321403e4     	orr	w4, wzr, #0x1000
   51f94: 1a849063     	csel	w3, w3, w4, ls
   51f98: 7100007f     	cmp	w3, #0x0
   51f9c: 2a1f03e4     	mov	w4, wzr
   51fa0: 1a84a064     	csel	w4, w3, w4, ge
   51fa4: 7100047f     	cmp	w3, #0x1
   51fa8: 5400014b     	b.lt	0x51fd0 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x80>
   51fac: 92800003     	mov	x3, #-0x1               // =-1
   51fb0: 8b244063     	add	x3, x3, w4, uxtw
   51fb4: aa1f03e5     	mov	x5, xzr
   51fb8: d503201f     	nop
   51fbc: d503201f     	nop
   51fc0: 9b007442     	madd	x2, x2, x0, x29
   51fc4: 910004a5     	add	x5, x5, #0x1
   51fc8: eb05007f     	cmp	x3, x5
   51fcc: 54ffffaa     	b.ge	0x51fc0 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x70>
   51fd0: 0b0403c3     	add	w3, w30, w4
   51fd4: b9401389     	ldr	w9, [x28, #0x10]
   51fd8: 34000069     	cbz	w9, 0x51fe4 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x94>
   51fdc: 2a0303fe     	mov	w30, w3
   51fe0: 17ffffe8     	b	0x51f80 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x30>
   51fe4: 94069a67     	bl	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51fe8: 2a0303fe     	mov	w30, w3
   51fec: 17ffffe5     	b	0x51f80 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0x30>
   51ff0: aa0203e0     	mov	x0, x2
   51ff4: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   51ff8: 9100c3ff     	add	sp, sp, #0x30
   51ffc: b9401389     	ldr	w9, [x28, #0x10]
   52000: 7100013f     	cmp	w9, #0x0
   52004: 5400006d     	b.le	0x52010 <Floor$$Lc02859e8_run_gxC7QWhRSSFYthoffC7e0D+0xc0>
   52008: d65f03c0     	ret
   5200c: 1402ba1d     	b	0x100880 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   52010: 14069a5c     	b	0x1f8980 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   52014: cccccccc     	<unknown>
   52018: cccccccc     	<unknown>
   5201c: cccccccc     	<unknown>

# svm: Floor_identity_RGfigO1fJmId7IgDqMOKbC
0000000000052020 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>:
   52020: d10083ff     	sub	sp, sp, #0x20
   52024: a9017bfd     	stp	x29, x30, [sp, #0x10]
   52028: d00044fe     	adrp	x30, 0x8f0000 <.data>
   5202c: f94497de     	ldr	x30, [x30, #0x928]
   52030: d63f03c0     	blr	x30
   52034: a9417bfd     	ldp	x29, x30, [sp, #0x10]
   52038: 910083ff     	add	sp, sp, #0x20
   5203c: d65f03c0     	ret

# svm: Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA
0000000000052040 <Floor_javaBatch16_8LIcqghlXl80Dft9xk6yUA>:
   52040: d100c3ff     	sub	sp, sp, #0x30
   52044: a9027bfd     	stp	x29, x30, [sp, #0x20]
   52048: aa0003e2     	mov	x2, x0
   5204c: aa0103e0     	mov	x0, x1
   52050: aa0203fe     	mov	x30, x2
   52054: f9000fe2     	str	x2, [sp, #0x18]
   52058: d63f03c0     	blr	x30
   5205c: f9400ffe     	ldr	x30, [sp, #0x18]
   52060: d63f03c0     	blr	x30
   52064: f9400ffe     	ldr	x30, [sp, #0x18]
   52068: d63f03c0     	blr	x30
   5206c: f9400ffe     	ldr	x30, [sp, #0x18]
   52070: d63f03c0     	blr	x30
   52074: f9400ffe     	ldr	x30, [sp, #0x18]
   52078: d63f03c0     	blr	x30
   5207c: f9400ffe     	ldr	x30, [sp, #0x18]
   52080: d63f03c0     	blr	x30
   52084: f9400ffe     	ldr	x30, [sp, #0x18]
   52088: d63f03c0     	blr	x30
   5208c: f9400ffe     	ldr	x30, [sp, #0x18]
   52090: d63f03c0     	blr	x30
   52094: f9400ffe     	ldr	x30, [sp, #0x18]
   52098: d63f03c0     	blr	x30
   5209c: f9400ffe     	ldr	x30, [sp, #0x18]
   520a0: d63f03c0     	blr	x30
   520a4: f9400ffe     	ldr	x30, [sp, #0x18]
   520a8: d63f03c0     	blr	x30
   520ac: f9400ffe     	ldr	x30, [sp, #0x18]
   520b0: d63f03c0     	blr	x30
   520b4: f9400ffe     	ldr	x30, [sp, #0x18]
   520b8: d63f03c0     	blr	x30
   520bc: f9400ffe     	ldr	x30, [sp, #0x18]
   520c0: d63f03c0     	blr	x30
   520c4: f9400ffe     	ldr	x30, [sp, #0x18]
   520c8: d63f03c0     	blr	x30
   520cc: f9400ffe     	ldr	x30, [sp, #0x18]
   520d0: d63f03c0     	blr	x30
   520d4: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   520d8: 9100c3ff     	add	sp, sp, #0x30
   520dc: d65f03c0     	ret

# svm: Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1
00000000000520e0 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1>:
   520e0: d10103ff     	sub	sp, sp, #0x40
   520e4: a9037bfd     	stp	x29, x30, [sp, #0x30]
   520e8: f9000fe0     	str	x0, [sp, #0x18]
   520ec: aa0003e2     	mov	x2, x0
   520f0: 2a1f03e0     	mov	w0, wzr
   520f4: 14000009     	b	0x52118 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x38>
   520f8: d503201f     	nop
   520fc: d503201f     	nop
   52100: aa0103e0     	mov	x0, x1
   52104: aa0203fe     	mov	x30, x2
   52108: d63f03c0     	blr	x30
   5210c: aa0003e1     	mov	x1, x0
   52110: f9400fe2     	ldr	x2, [sp, #0x18]
   52114: 320003e0     	orr	w0, wzr, #0x1
   52118: 34ffff40     	cbz	w0, 0x52100 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x20>
   5211c: f90013e1     	str	x1, [sp, #0x20]
   52120: 2a0003e1     	mov	w1, w0
   52124: f94013e0     	ldr	x0, [sp, #0x20]
   52128: 1400002e     	b	0x521e0 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x100>
   5212c: d503201f     	nop
   52130: d503201f     	nop
   52134: d503201f     	nop
   52138: d503201f     	nop
   5213c: d503201f     	nop
   52140: aa0203fe     	mov	x30, x2
   52144: d63f03c0     	blr	x30
   52148: b9402be1     	ldr	w1, [sp, #0x28]
   5214c: 11000422     	add	w2, w1, #0x1
   52150: f9400ffe     	ldr	x30, [sp, #0x18]
   52154: b9002fe2     	str	w2, [sp, #0x2c]
   52158: d63f03c0     	blr	x30
   5215c: b9402be1     	ldr	w1, [sp, #0x28]
   52160: 11000822     	add	w2, w1, #0x2
   52164: f9400ffe     	ldr	x30, [sp, #0x18]
   52168: b9002fe2     	str	w2, [sp, #0x2c]
   5216c: d63f03c0     	blr	x30
   52170: b9402be1     	ldr	w1, [sp, #0x28]
   52174: 11000c22     	add	w2, w1, #0x3
   52178: f9400ffe     	ldr	x30, [sp, #0x18]
   5217c: b9002fe2     	str	w2, [sp, #0x2c]
   52180: d63f03c0     	blr	x30
   52184: b9402be1     	ldr	w1, [sp, #0x28]
   52188: 11001022     	add	w2, w1, #0x4
   5218c: f9400ffe     	ldr	x30, [sp, #0x18]
   52190: b9002fe2     	str	w2, [sp, #0x2c]
   52194: d63f03c0     	blr	x30
   52198: b9402be1     	ldr	w1, [sp, #0x28]
   5219c: 11001422     	add	w2, w1, #0x5
   521a0: f9400ffe     	ldr	x30, [sp, #0x18]
   521a4: b9002fe2     	str	w2, [sp, #0x2c]
   521a8: d63f03c0     	blr	x30
   521ac: b9402be1     	ldr	w1, [sp, #0x28]
   521b0: 11001822     	add	w2, w1, #0x6
   521b4: f9400ffe     	ldr	x30, [sp, #0x18]
   521b8: b9002fe2     	str	w2, [sp, #0x2c]
   521bc: d63f03c0     	blr	x30
   521c0: b9402be1     	ldr	w1, [sp, #0x28]
   521c4: 11001c22     	add	w2, w1, #0x7
   521c8: f9400ffe     	ldr	x30, [sp, #0x18]
   521cc: b9002fe2     	str	w2, [sp, #0x2c]
   521d0: d63f03c0     	blr	x30
   521d4: b9402be1     	ldr	w1, [sp, #0x28]
   521d8: 11002021     	add	w1, w1, #0x8
   521dc: f9400fe2     	ldr	x2, [sp, #0x18]
   521e0: b9002be1     	str	w1, [sp, #0x28]
   521e4: 7103e03f     	cmp	w1, #0xf8
   521e8: 54fffac3     	b.lo	0x52140 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x60>
   521ec: 1400000d     	b	0x52220 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x140>
   521f0: d503201f     	nop
   521f4: d503201f     	nop
   521f8: d503201f     	nop
   521fc: d503201f     	nop
   52200: b9002be1     	str	w1, [sp, #0x28]
   52204: aa0203fe     	mov	x30, x2
   52208: d63f03c0     	blr	x30
   5220c: b9402be1     	ldr	w1, [sp, #0x28]
   52210: 1100043d     	add	w29, w1, #0x1
   52214: 2a1d03e1     	mov	w1, w29
   52218: f9400fe2     	ldr	x2, [sp, #0x18]
   5221c: d503201f     	nop
   52220: 7104003f     	cmp	w1, #0x100
   52224: 54fffee3     	b.lo	0x52200 <Floor_javaBatch256_sBLMR8Ztqr8TxvWCU9C5E1+0x120>
   52228: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   5222c: 910103ff     	add	sp, sp, #0x40
   52230: d65f03c0     	ret
   52234: cccccccc     	<unknown>
   52238: cccccccc     	<unknown>
   5223c: cccccccc     	<unknown>

# svm: Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI
0000000000052240 <Floor_javaBatch4_wasvmklIr2I1bT69W7y2vI>:
   52240: d100c3ff     	sub	sp, sp, #0x30
   52244: a9027bfd     	stp	x29, x30, [sp, #0x20]
   52248: aa0003e2     	mov	x2, x0
   5224c: aa0103e0     	mov	x0, x1
   52250: aa0203fe     	mov	x30, x2
   52254: f9000fe2     	str	x2, [sp, #0x18]
   52258: d63f03c0     	blr	x30
   5225c: f9400ffe     	ldr	x30, [sp, #0x18]
   52260: d63f03c0     	blr	x30
   52264: f9400ffe     	ldr	x30, [sp, #0x18]
   52268: d63f03c0     	blr	x30
   5226c: f9400ffe     	ldr	x30, [sp, #0x18]
   52270: d63f03c0     	blr	x30
   52274: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   52278: 9100c3ff     	add	sp, sp, #0x30
   5227c: d65f03c0     	ret

# svm: Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK
0000000000052280 <Floor_javaBatch64_K1Yw1tcLJ64AWpNsVkdWbK>:
   52280: d100c3ff     	sub	sp, sp, #0x30
   52284: a9027bfd     	stp	x29, x30, [sp, #0x20]
   52288: aa0003e2     	mov	x2, x0
   5228c: aa0103e0     	mov	x0, x1
   52290: aa0203fe     	mov	x30, x2
   52294: f9000fe2     	str	x2, [sp, #0x18]
   52298: d63f03c0     	blr	x30
   5229c: f9400ffe     	ldr	x30, [sp, #0x18]
   522a0: d63f03c0     	blr	x30
   522a4: f9400ffe     	ldr	x30, [sp, #0x18]
   522a8: d63f03c0     	blr	x30
   522ac: f9400ffe     	ldr	x30, [sp, #0x18]
   522b0: d63f03c0     	blr	x30
   522b4: f9400ffe     	ldr	x30, [sp, #0x18]
   522b8: d63f03c0     	blr	x30
   522bc: f9400ffe     	ldr	x30, [sp, #0x18]
   522c0: d63f03c0     	blr	x30
   522c4: f9400ffe     	ldr	x30, [sp, #0x18]
   522c8: d63f03c0     	blr	x30
   522cc: f9400ffe     	ldr	x30, [sp, #0x18]
   522d0: d63f03c0     	blr	x30
   522d4: f9400ffe     	ldr	x30, [sp, #0x18]
   522d8: d63f03c0     	blr	x30
   522dc: f9400ffe     	ldr	x30, [sp, #0x18]
   522e0: d63f03c0     	blr	x30
   522e4: f9400ffe     	ldr	x30, [sp, #0x18]
   522e8: d63f03c0     	blr	x30
   522ec: f9400ffe     	ldr	x30, [sp, #0x18]
   522f0: d63f03c0     	blr	x30
   522f4: f9400ffe     	ldr	x30, [sp, #0x18]
   522f8: d63f03c0     	blr	x30
   522fc: f9400ffe     	ldr	x30, [sp, #0x18]
   52300: d63f03c0     	blr	x30
   52304: f9400ffe     	ldr	x30, [sp, #0x18]
   52308: d63f03c0     	blr	x30
   5230c: f9400ffe     	ldr	x30, [sp, #0x18]
   52310: d63f03c0     	blr	x30
   52314: f9400ffe     	ldr	x30, [sp, #0x18]
   52318: d63f03c0     	blr	x30
   5231c: f9400ffe     	ldr	x30, [sp, #0x18]
   52320: d63f03c0     	blr	x30
   52324: f9400ffe     	ldr	x30, [sp, #0x18]
   52328: d63f03c0     	blr	x30
   5232c: f9400ffe     	ldr	x30, [sp, #0x18]
   52330: d63f03c0     	blr	x30
   52334: f9400ffe     	ldr	x30, [sp, #0x18]
   52338: d63f03c0     	blr	x30
   5233c: f9400ffe     	ldr	x30, [sp, #0x18]
   52340: d63f03c0     	blr	x30
   52344: f9400ffe     	ldr	x30, [sp, #0x18]
   52348: d63f03c0     	blr	x30
   5234c: f9400ffe     	ldr	x30, [sp, #0x18]
   52350: d63f03c0     	blr	x30
   52354: f9400ffe     	ldr	x30, [sp, #0x18]
   52358: d63f03c0     	blr	x30
   5235c: f9400ffe     	ldr	x30, [sp, #0x18]
   52360: d63f03c0     	blr	x30
   52364: f9400ffe     	ldr	x30, [sp, #0x18]
   52368: d63f03c0     	blr	x30
   5236c: f9400ffe     	ldr	x30, [sp, #0x18]
   52370: d63f03c0     	blr	x30
   52374: f9400ffe     	ldr	x30, [sp, #0x18]
   52378: d63f03c0     	blr	x30
   5237c: f9400ffe     	ldr	x30, [sp, #0x18]
   52380: d63f03c0     	blr	x30
   52384: f9400ffe     	ldr	x30, [sp, #0x18]
   52388: d63f03c0     	blr	x30
   5238c: f9400ffe     	ldr	x30, [sp, #0x18]
   52390: d63f03c0     	blr	x30
   52394: f9400ffe     	ldr	x30, [sp, #0x18]
   52398: d63f03c0     	blr	x30
   5239c: f9400ffe     	ldr	x30, [sp, #0x18]
   523a0: d63f03c0     	blr	x30
   523a4: f9400ffe     	ldr	x30, [sp, #0x18]
   523a8: d63f03c0     	blr	x30
   523ac: f9400ffe     	ldr	x30, [sp, #0x18]
   523b0: d63f03c0     	blr	x30
   523b4: f9400ffe     	ldr	x30, [sp, #0x18]
   523b8: d63f03c0     	blr	x30
   523bc: f9400ffe     	ldr	x30, [sp, #0x18]
   523c0: d63f03c0     	blr	x30
   523c4: f9400ffe     	ldr	x30, [sp, #0x18]
   523c8: d63f03c0     	blr	x30
   523cc: f9400ffe     	ldr	x30, [sp, #0x18]
   523d0: d63f03c0     	blr	x30
   523d4: f9400ffe     	ldr	x30, [sp, #0x18]
   523d8: d63f03c0     	blr	x30
   523dc: f9400ffe     	ldr	x30, [sp, #0x18]
   523e0: d63f03c0     	blr	x30
   523e4: f9400ffe     	ldr	x30, [sp, #0x18]
   523e8: d63f03c0     	blr	x30
   523ec: f9400ffe     	ldr	x30, [sp, #0x18]
   523f0: d63f03c0     	blr	x30
   523f4: f9400ffe     	ldr	x30, [sp, #0x18]
   523f8: d63f03c0     	blr	x30
   523fc: f9400ffe     	ldr	x30, [sp, #0x18]
   52400: d63f03c0     	blr	x30
   52404: f9400ffe     	ldr	x30, [sp, #0x18]
   52408: d63f03c0     	blr	x30
   5240c: f9400ffe     	ldr	x30, [sp, #0x18]
   52410: d63f03c0     	blr	x30
   52414: f9400ffe     	ldr	x30, [sp, #0x18]
   52418: d63f03c0     	blr	x30
   5241c: f9400ffe     	ldr	x30, [sp, #0x18]
   52420: d63f03c0     	blr	x30
   52424: f9400ffe     	ldr	x30, [sp, #0x18]
   52428: d63f03c0     	blr	x30
   5242c: f9400ffe     	ldr	x30, [sp, #0x18]
   52430: d63f03c0     	blr	x30
   52434: f9400ffe     	ldr	x30, [sp, #0x18]
   52438: d63f03c0     	blr	x30
   5243c: f9400ffe     	ldr	x30, [sp, #0x18]
   52440: d63f03c0     	blr	x30
   52444: f9400ffe     	ldr	x30, [sp, #0x18]
   52448: d63f03c0     	blr	x30
   5244c: f9400ffe     	ldr	x30, [sp, #0x18]
   52450: d63f03c0     	blr	x30
   52454: f9400ffe     	ldr	x30, [sp, #0x18]
   52458: d63f03c0     	blr	x30
   5245c: f9400ffe     	ldr	x30, [sp, #0x18]
   52460: d63f03c0     	blr	x30
   52464: f9400ffe     	ldr	x30, [sp, #0x18]
   52468: d63f03c0     	blr	x30
   5246c: f9400ffe     	ldr	x30, [sp, #0x18]
   52470: d63f03c0     	blr	x30
   52474: f9400ffe     	ldr	x30, [sp, #0x18]
   52478: d63f03c0     	blr	x30
   5247c: f9400ffe     	ldr	x30, [sp, #0x18]
   52480: d63f03c0     	blr	x30
   52484: f9400ffe     	ldr	x30, [sp, #0x18]
   52488: d63f03c0     	blr	x30
   5248c: f9400ffe     	ldr	x30, [sp, #0x18]
   52490: d63f03c0     	blr	x30
   52494: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   52498: 9100c3ff     	add	sp, sp, #0x30
   5249c: d65f03c0     	ret

# svm: Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G
00000000000529b0 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>:
   529b0: d10083ff     	sub	sp, sp, #0x20
   529b4: a9017bfd     	stp	x29, x30, [sp, #0x10]
   529b8: d00044fe     	adrp	x30, 0x8f0000 <.data>
   529bc: f941ffde     	ldr	x30, [x30, #0x3f8]
   529c0: d63f03c0     	blr	x30
   529c4: a9417bfd     	ldp	x29, x30, [sp, #0x10]
   529c8: 910083ff     	add	sp, sp, #0x20
   529cc: d65f03c0     	ret

