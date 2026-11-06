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

# svm: Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F
00000000000502d0 <Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F>:
   502d0: d10103e9     	sub	x9, sp, #0x40
   502d4: f940078a     	ldr	x10, [x28, #0x8]
   502d8: eb0a013f     	cmp	x9, x10
   502dc: 540008c9     	b.ls	0x503f4 <Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x124>
   502e0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   502e4: 9100013f     	mov	sp, x9
   502e8: 7100043f     	cmp	w1, #0x1
   502ec: 5400080b     	b.lt	0x503ec <Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x11c>
   502f0: b9002fe1     	str	w1, [sp, #0x2c]
   502f4: 529ea3c0     	mov	w0, #0xf51e             // =62750
   502f8: 72a000e0     	movk	w0, #0x7, lsl #16
   502fc: 8b000f60     	add	x0, x27, x0, lsl #3
   50300: f9402403     	ldr	x3, [x0, #0x48]
   50304: f90013e3     	str	x3, [sp, #0x20]
   50308: 2a1f03e9     	mov	w9, wzr
   5030c: b9002be9     	str	w9, [sp, #0x28]
   50310: aa0203e0     	mov	x0, x2
   50314: aa0303fe     	mov	x30, x3
   50318: d63f03c0     	blr	x30
   5031c: f94013fe     	ldr	x30, [sp, #0x20]
   50320: d63f03c0     	blr	x30
   50324: f94013fe     	ldr	x30, [sp, #0x20]
   50328: d63f03c0     	blr	x30
   5032c: f94013fe     	ldr	x30, [sp, #0x20]
   50330: d63f03c0     	blr	x30
   50334: f94013fe     	ldr	x30, [sp, #0x20]
   50338: d63f03c0     	blr	x30
   5033c: f94013fe     	ldr	x30, [sp, #0x20]
   50340: d63f03c0     	blr	x30
   50344: f94013fe     	ldr	x30, [sp, #0x20]
   50348: d63f03c0     	blr	x30
   5034c: f94013fe     	ldr	x30, [sp, #0x20]
   50350: d63f03c0     	blr	x30
   50354: f94013fe     	ldr	x30, [sp, #0x20]
   50358: d63f03c0     	blr	x30
   5035c: f94013fe     	ldr	x30, [sp, #0x20]
   50360: d63f03c0     	blr	x30
   50364: f94013fe     	ldr	x30, [sp, #0x20]
   50368: d63f03c0     	blr	x30
   5036c: f94013fe     	ldr	x30, [sp, #0x20]
   50370: d63f03c0     	blr	x30
   50374: f94013fe     	ldr	x30, [sp, #0x20]
   50378: d63f03c0     	blr	x30
   5037c: f94013fe     	ldr	x30, [sp, #0x20]
   50380: d63f03c0     	blr	x30
   50384: f94013fe     	ldr	x30, [sp, #0x20]
   50388: d63f03c0     	blr	x30
   5038c: f94013fe     	ldr	x30, [sp, #0x20]
   50390: d63f03c0     	blr	x30
   50394: b9402bfd     	ldr	w29, [sp, #0x28]
   50398: 110043a1     	add	w1, w29, #0x10
   5039c: b9402fe2     	ldr	w2, [sp, #0x2c]
   503a0: 6b02003f     	cmp	w1, w2
   503a4: 5400018a     	b.ge	0x503d4 <Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x104>
   503a8: b9401389     	ldr	w9, [x28, #0x10]
   503ac: 340000a9     	cbz	w9, 0x503c0 <Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0xf0>
   503b0: b9002be1     	str	w1, [sp, #0x28]
   503b4: f94013e3     	ldr	x3, [sp, #0x20]
   503b8: aa0003e2     	mov	x2, x0
   503bc: 17ffffd5     	b	0x50310 <Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x40>
   503c0: 9406a304     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   503c4: b9002be1     	str	w1, [sp, #0x28]
   503c8: f94013e3     	ldr	x3, [sp, #0x20]
   503cc: aa0003e2     	mov	x2, x0
   503d0: 17ffffd0     	b	0x50310 <Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x40>
   503d4: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   503d8: 910103ff     	add	sp, sp, #0x40
   503dc: b9401389     	ldr	w9, [x28, #0x10]
   503e0: 7100013f     	cmp	w9, #0x0
   503e4: 540000ad     	b.le	0x503f8 <Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x128>
   503e8: d65f03c0     	ret
   503ec: aa0203e0     	mov	x0, x2
   503f0: 17fffff9     	b	0x503d4 <Floor$$L6da3e9eb_run_QuppMDsqob0AtmYZ0mPW0F+0x104>
   503f4: 1402c0af     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   503f8: 1406a2f6     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   503fc: cccccccc     	<unknown>

# svm: Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH
0000000000050400 <Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH>:
   50400: d10103e9     	sub	x9, sp, #0x40
   50404: f940078a     	ldr	x10, [x28, #0x8]
   50408: eb0a013f     	cmp	x9, x10
   5040c: 54000809     	b.ls	0x5050c <Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x10c>
   50410: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50414: 9100013f     	mov	sp, x9
   50418: b9002fe1     	str	w1, [sp, #0x2c]
   5041c: 7100043f     	cmp	w1, #0x1
   50420: 5400030b     	b.lt	0x50480 <Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x80>
   50424: 529ea3c0     	mov	w0, #0xf51e             // =62750
   50428: 72a000e0     	movk	w0, #0x7, lsl #16
   5042c: 8b000f60     	add	x0, x27, x0, lsl #3
   50430: f9402403     	ldr	x3, [x0, #0x48]
   50434: f90013e3     	str	x3, [sp, #0x20]
   50438: aa0203e0     	mov	x0, x2
   5043c: aa0303fe     	mov	x30, x3
   50440: d63f03c0     	blr	x30
   50444: f94013fe     	ldr	x30, [sp, #0x20]
   50448: d63f03c0     	blr	x30
   5044c: f94013fe     	ldr	x30, [sp, #0x20]
   50450: d63f03c0     	blr	x30
   50454: f94013fe     	ldr	x30, [sp, #0x20]
   50458: d63f03c0     	blr	x30
   5045c: b9402fe1     	ldr	w1, [sp, #0x2c]
   50460: 7100143f     	cmp	w1, #0x5
   50464: 540001ca     	b.ge	0x5049c <Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x9c>
   50468: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   5046c: 910103ff     	add	sp, sp, #0x40
   50470: b9401389     	ldr	w9, [x28, #0x10]
   50474: 7100013f     	cmp	w9, #0x0
   50478: 540004cd     	b.le	0x50510 <Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x110>
   5047c: d65f03c0     	ret
   50480: aa0203e0     	mov	x0, x2
   50484: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50488: 910103ff     	add	sp, sp, #0x40
   5048c: b9401389     	ldr	w9, [x28, #0x10]
   50490: 7100013f     	cmp	w9, #0x0
   50494: 540003ed     	b.le	0x50510 <Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x110>
   50498: d65f03c0     	ret
   5049c: 321e03e9     	orr	w9, wzr, #0x4
   504a0: b9002be9     	str	w9, [sp, #0x28]
   504a4: f94013e3     	ldr	x3, [sp, #0x20]
   504a8: d503201f     	nop
   504ac: d503201f     	nop
   504b0: aa0303fe     	mov	x30, x3
   504b4: d63f03c0     	blr	x30
   504b8: f94013fe     	ldr	x30, [sp, #0x20]
   504bc: d63f03c0     	blr	x30
   504c0: f94013fe     	ldr	x30, [sp, #0x20]
   504c4: d63f03c0     	blr	x30
   504c8: f94013fe     	ldr	x30, [sp, #0x20]
   504cc: d63f03c0     	blr	x30
   504d0: b9402be1     	ldr	w1, [sp, #0x28]
   504d4: 11001022     	add	w2, w1, #0x4
   504d8: b9402fe3     	ldr	w3, [sp, #0x2c]
   504dc: 6b03005f     	cmp	w2, w3
   504e0: 54fffc4a     	b.ge	0x50468 <Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0x68>
   504e4: b9401389     	ldr	w9, [x28, #0x10]
   504e8: 34000089     	cbz	w9, 0x504f8 <Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0xf8>
   504ec: b9002be2     	str	w2, [sp, #0x28]
   504f0: f94013e3     	ldr	x3, [sp, #0x20]
   504f4: 17ffffef     	b	0x504b0 <Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0xb0>
   504f8: f94013e4     	ldr	x4, [sp, #0x20]
   504fc: 9406a2b5     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50500: b9002be2     	str	w2, [sp, #0x28]
   50504: aa0403e3     	mov	x3, x4
   50508: 17ffffea     	b	0x504b0 <Floor$$L74e6baa5_run_YFUWe4LKllCznoyGXGLueH+0xb0>
   5050c: 1402c069     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50510: 1406a2b0     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50514: cccccccc     	<unknown>
   50518: cccccccc     	<unknown>
   5051c: cccccccc     	<unknown>

# svm: Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I
0000000000050520 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I>:
   50520: d10143e9     	sub	x9, sp, #0x50
   50524: f940078a     	ldr	x10, [x28, #0x8]
   50528: eb0a013f     	cmp	x9, x10
   5052c: 54000bc9     	b.ls	0x506a4 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x184>
   50530: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50534: 9100013f     	mov	sp, x9
   50538: b90033e1     	str	w1, [sp, #0x30]
   5053c: 7100043f     	cmp	w1, #0x1
   50540: 320003e0     	orr	w0, wzr, #0x1
   50544: 1a80d020     	csel	w0, w1, w0, le
   50548: 7100041f     	cmp	w0, #0x1
   5054c: 54000a6b     	b.lt	0x50698 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x178>
   50550: aa0203e0     	mov	x0, x2
   50554: 94001b2f     	bl	0x57210 <Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
   50558: 320003e2     	orr	w2, wzr, #0x1
   5055c: b94033e1     	ldr	w1, [sp, #0x30]
   50560: 52800083     	mov	w3, #0x4                // =4
   50564: 72b00003     	movk	w3, #0x8000, lsl #16
   50568: 320103e4     	orr	w4, wzr, #0x80000000
   5056c: 51001025     	sub	w5, w1, #0x4
   50570: 6b03003f     	cmp	w1, w3
   50574: 1a85b083     	csel	w3, w4, w5, lt
   50578: b9002be3     	str	w3, [sp, #0x28]
   5057c: 11000c64     	add	w4, w3, #0x3
   50580: b90027e4     	str	w4, [sp, #0x24]
   50584: 2a0203e1     	mov	w1, w2
   50588: d503201f     	nop
   5058c: d503201f     	nop
   50590: 6b03003f     	cmp	w1, w3
   50594: 5400062a     	b.ge	0x50658 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x138>
   50598: 4b010085     	sub	w5, w4, w1
   5059c: 53027ca6     	lsr	w6, w5, #2
   505a0: 121e74a5     	and	w5, w5, #0xfffffffc
   505a4: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   505a8: 321203e6     	orr	w6, wzr, #0x4000
   505ac: 1a8630a5     	csel	w5, w5, w6, lo
   505b0: 710004bf     	cmp	w5, #0x1
   505b4: 540004ab     	b.lt	0x50648 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x128>
   505b8: b90037e1     	str	w1, [sp, #0x34]
   505bc: b9003fe5     	str	w5, [sp, #0x3c]
   505c0: 2a1f03e2     	mov	w2, wzr
   505c4: d503201f     	nop
   505c8: d503201f     	nop
   505cc: d503201f     	nop
   505d0: 0b010047     	add	w7, w2, w1
   505d4: b90023e7     	str	w7, [sp, #0x20]
   505d8: b9002fe2     	str	w2, [sp, #0x2c]
   505dc: 94001b0d     	bl	0x57210 <Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
   505e0: b94023e7     	ldr	w7, [sp, #0x20]
   505e4: 110004e1     	add	w1, w7, #0x1
   505e8: b9003be1     	str	w1, [sp, #0x38]
   505ec: 94001b09     	bl	0x57210 <Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
   505f0: b94023e7     	ldr	w7, [sp, #0x20]
   505f4: 110008e1     	add	w1, w7, #0x2
   505f8: b9003be1     	str	w1, [sp, #0x38]
   505fc: 94001b05     	bl	0x57210 <Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
   50600: b94023e7     	ldr	w7, [sp, #0x20]
   50604: 11000ce1     	add	w1, w7, #0x3
   50608: b90023e1     	str	w1, [sp, #0x20]
   5060c: 94001b01     	bl	0x57210 <Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
   50610: b9402fe2     	ldr	w2, [sp, #0x2c]
   50614: 11001042     	add	w2, w2, #0x4
   50618: b9403fe5     	ldr	w5, [sp, #0x3c]
   5061c: 6b05005f     	cmp	w2, w5
   50620: b94037e1     	ldr	w1, [sp, #0x34]
   50624: 54fffd6b     	b.lt	0x505d0 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0xb0>
   50628: 0b010041     	add	w1, w2, w1
   5062c: b9401389     	ldr	w9, [x28, #0x10]
   50630: 34000069     	cbz	w9, 0x5063c <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x11c>
   50634: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   50638: 17ffffd6     	b	0x50590 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x70>
   5063c: 9406a265     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50640: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   50644: 17ffffd3     	b	0x50590 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x70>
   50648: b9401389     	ldr	w9, [x28, #0x10]
   5064c: 35fffa29     	cbnz	w9, 0x50590 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x70>
   50650: 9406a260     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50654: 17ffffcf     	b	0x50590 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x70>
   50658: 2a0103e2     	mov	w2, w1
   5065c: 14000006     	b	0x50674 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x154>
   50660: b9002fe2     	str	w2, [sp, #0x2c]
   50664: 94001aeb     	bl	0x57210 <Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>
   50668: b9402fe2     	ldr	w2, [sp, #0x2c]
   5066c: 1100045d     	add	w29, w2, #0x1
   50670: 2a1d03e2     	mov	w2, w29
   50674: b94033e1     	ldr	w1, [sp, #0x30]
   50678: 6b01005f     	cmp	w2, w1
   5067c: 54ffff2b     	b.lt	0x50660 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x140>
   50680: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   50684: 910143ff     	add	sp, sp, #0x50
   50688: b9401389     	ldr	w9, [x28, #0x10]
   5068c: 7100013f     	cmp	w9, #0x0
   50690: 540000cd     	b.le	0x506a8 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x188>
   50694: d65f03c0     	ret
   50698: aa0203e0     	mov	x0, x2
   5069c: 2a1f03e2     	mov	w2, wzr
   506a0: 17ffffb0     	b	0x50560 <Floor$$L95d43726_run_zGM0sRlKO6DI8sWvyMfZ4I+0x40>
   506a4: 1402c003     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   506a8: 1406a24a     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   506ac: cccccccc     	<unknown>

# svm: Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D
00000000000506b0 <Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D>:
   506b0: d10103e9     	sub	x9, sp, #0x40
   506b4: f940078a     	ldr	x10, [x28, #0x8]
   506b8: eb0a013f     	cmp	x9, x10
   506bc: 54000809     	b.ls	0x507bc <Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x10c>
   506c0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   506c4: 9100013f     	mov	sp, x9
   506c8: b9002fe1     	str	w1, [sp, #0x2c]
   506cc: 7100043f     	cmp	w1, #0x1
   506d0: 5400030b     	b.lt	0x50730 <Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x80>
   506d4: 529ea3c0     	mov	w0, #0xf51e             // =62750
   506d8: 72a000e0     	movk	w0, #0x7, lsl #16
   506dc: 8b000f60     	add	x0, x27, x0, lsl #3
   506e0: f9402403     	ldr	x3, [x0, #0x48]
   506e4: f90013e3     	str	x3, [sp, #0x20]
   506e8: aa0203e0     	mov	x0, x2
   506ec: aa0303fe     	mov	x30, x3
   506f0: d63f03c0     	blr	x30
   506f4: f94013fe     	ldr	x30, [sp, #0x20]
   506f8: d63f03c0     	blr	x30
   506fc: f94013fe     	ldr	x30, [sp, #0x20]
   50700: d63f03c0     	blr	x30
   50704: f94013fe     	ldr	x30, [sp, #0x20]
   50708: d63f03c0     	blr	x30
   5070c: b9402fe1     	ldr	w1, [sp, #0x2c]
   50710: 7100143f     	cmp	w1, #0x5
   50714: 540001ca     	b.ge	0x5074c <Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x9c>
   50718: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   5071c: 910103ff     	add	sp, sp, #0x40
   50720: b9401389     	ldr	w9, [x28, #0x10]
   50724: 7100013f     	cmp	w9, #0x0
   50728: 540004cd     	b.le	0x507c0 <Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x110>
   5072c: d65f03c0     	ret
   50730: aa0203e0     	mov	x0, x2
   50734: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50738: 910103ff     	add	sp, sp, #0x40
   5073c: b9401389     	ldr	w9, [x28, #0x10]
   50740: 7100013f     	cmp	w9, #0x0
   50744: 540003ed     	b.le	0x507c0 <Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x110>
   50748: d65f03c0     	ret
   5074c: 321e03e9     	orr	w9, wzr, #0x4
   50750: b9002be9     	str	w9, [sp, #0x28]
   50754: f94013e3     	ldr	x3, [sp, #0x20]
   50758: d503201f     	nop
   5075c: d503201f     	nop
   50760: aa0303fe     	mov	x30, x3
   50764: d63f03c0     	blr	x30
   50768: f94013fe     	ldr	x30, [sp, #0x20]
   5076c: d63f03c0     	blr	x30
   50770: f94013fe     	ldr	x30, [sp, #0x20]
   50774: d63f03c0     	blr	x30
   50778: f94013fe     	ldr	x30, [sp, #0x20]
   5077c: d63f03c0     	blr	x30
   50780: b9402be1     	ldr	w1, [sp, #0x28]
   50784: 11001022     	add	w2, w1, #0x4
   50788: b9402fe3     	ldr	w3, [sp, #0x2c]
   5078c: 6b03005f     	cmp	w2, w3
   50790: 54fffc4a     	b.ge	0x50718 <Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0x68>
   50794: b9401389     	ldr	w9, [x28, #0x10]
   50798: 34000089     	cbz	w9, 0x507a8 <Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0xf8>
   5079c: b9002be2     	str	w2, [sp, #0x28]
   507a0: f94013e3     	ldr	x3, [sp, #0x20]
   507a4: 17ffffef     	b	0x50760 <Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0xb0>
   507a8: f94013e4     	ldr	x4, [sp, #0x20]
   507ac: 9406a209     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   507b0: b9002be2     	str	w2, [sp, #0x28]
   507b4: aa0403e3     	mov	x3, x4
   507b8: 17ffffea     	b	0x50760 <Floor$$Lef419cf5_run_J7LgcnGz92C03MxtZTK86D+0xb0>
   507bc: 1402bfbd     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   507c0: 1406a204     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   507c4: cccccccc     	<unknown>
   507c8: cccccccc     	<unknown>
   507cc: cccccccc     	<unknown>

# svm: Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL
00000000000507d0 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL>:
   507d0: d10203e9     	sub	x9, sp, #0x80
   507d4: f940078a     	ldr	x10, [x28, #0x8]
   507d8: eb0a013f     	cmp	x9, x10
   507dc: 54002849     	b.ls	0x50ce4 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x514>
   507e0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   507e4: 9100013f     	mov	sp, x9
   507e8: b9400801     	ldr	w1, [x0, #0x8]
   507ec: b9004be1     	str	w1, [sp, #0x48]
   507f0: b940041e     	ldr	w30, [x0, #0x4]
   507f4: 2a0103e2     	mov	w2, w1
   507f8: 8b020f62     	add	x2, x27, x2, lsl #3
   507fc: f90023e2     	str	x2, [sp, #0x40]
   50800: 340025de     	cbz	w30, 0x50cb8 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4e8>
   50804: 2a1e03fe     	mov	w30, w30
   50808: 8b1e0f7e     	add	x30, x27, x30, lsl #3
   5080c: b94007de     	ldr	w30, [x30, #0x4]
   50810: 3400259e     	cbz	w30, 0x50cc0 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4f0>
   50814: 2a1e03e0     	mov	w0, w30
   50818: 8b000f60     	add	x0, x27, x0, lsl #3
   5081c: d503201f     	nop
   50820: 91004009     	add	x9, x0, #0x10
   50824: 88dffd23     	ldar	w3, [x9]
   50828: 340001e3     	cbz	w3, 0x50864 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
   5082c: 51000464     	sub	w4, w3, #0x1
   50830: 9100401e     	add	x30, x0, #0x10
   50834: 885f7fc5     	ldxr	w5, [x30]
   50838: 6b0300bf     	cmp	w5, w3
   5083c: 1a85008a     	csel	w10, w4, w5, eq
   50840: 8809ffca     	stlxr	w9, w10, [x30]
   50844: 35ffff89     	cbnz	w9, 0x50834 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x64>
   50848: d5033bbf     	dmb	ish
   5084c: 1a9f17fe     	cset	w30, eq
   50850: 35001b9e     	cbnz	w30, 0x50bc0 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3f0>
   50854: b9401389     	ldr	w9, [x28, #0x10]
   50858: 35fffe49     	cbnz	w9, 0x50820 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x50>
   5085c: 9406a1dd     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50860: 17fffff0     	b	0x50820 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x50>
   50864: 529eade3     	mov	w3, #0xf56f             // =62831
   50868: 72a000e3     	movk	w3, #0x7, lsl #16
   5086c: 8b030f63     	add	x3, x27, x3, lsl #3
   50870: f9000fe3     	str	x3, [sp, #0x18]
   50874: 529eaee4     	mov	w4, #0xf577             // =62839
   50878: 72a000e4     	movk	w4, #0x7, lsl #16
   5087c: 8b040f64     	add	x4, x27, x4, lsl #3
   50880: f90013e4     	str	x4, [sp, #0x20]
   50884: 528022e5     	mov	w5, #0x117              // =279
   50888: 72a00025     	movk	w5, #0x1, lsl #16
   5088c: b90057e5     	str	w5, [sp, #0x54]
   50890: 529e88e6     	mov	w6, #0xf447             // =62535
   50894: 72a00026     	movk	w6, #0x1, lsl #16
   50898: 8b060f66     	add	x6, x27, x6, lsl #3
   5089c: f9001fe6     	str	x6, [sp, #0x38]
   508a0: 52802667     	mov	w7, #0x133              // =307
   508a4: 72a00027     	movk	w7, #0x1, lsl #16
   508a8: b90053e7     	str	w7, [sp, #0x50]
   508ac: 529eaf48     	mov	w8, #0xf57a             // =62842
   508b0: 72a000e8     	movk	w8, #0x7, lsl #16
   508b4: b9004fe8     	str	w8, [sp, #0x4c]
   508b8: 529eaf8b     	mov	w11, #0xf57c            // =62844
   508bc: 72a000eb     	movk	w11, #0x7, lsl #16
   508c0: 8b0b0f6b     	add	x11, x27, x11, lsl #3
   508c4: f9001beb     	str	x11, [sp, #0x30]
   508c8: 529e9fcc     	mov	w12, #0xf4fe            // =62718
   508cc: 72a000ec     	movk	w12, #0x7, lsl #16
   508d0: 8b0c0f6c     	add	x12, x27, x12, lsl #3
   508d4: f90017ec     	str	x12, [sp, #0x28]
   508d8: b27003ed     	orr	x13, xzr, #0x10000
   508dc: f9002fed     	str	x13, [sp, #0x58]
   508e0: 321003ee     	orr	w14, wzr, #0x10000
   508e4: b9006bee     	str	w14, [sp, #0x68]
   508e8: 52850c0f     	mov	w15, #0x2860            // =10336
   508ec: 72a0040f     	movk	w15, #0x20, lsl #16
   508f0: d2800110     	mov	x16, #0x8               // =8
   508f4: f2a00030     	movk	x16, #0x1, lsl #16
   508f8: 2a1f03f1     	mov	w17, wzr
   508fc: d503201f     	nop
   50900: 7108023f     	cmp	w17, #0x200
   50904: 54001c22     	b.hs	0x50c88 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4b8>
   50908: a9438381     	ldp	x1, x0, [x28, #0x38]
   5090c: 8b100002     	add	x2, x0, x16
   50910: eb02003f     	cmp	x1, x2
   50914: 54001123     	b.lo	0x50b38 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x368>
   50918: f9002382     	str	x2, [x28, #0x40]
   5091c: b900000f     	str	w15, [x0]
   50920: b900040e     	str	w14, [x0, #0x4]
   50924: 91002001     	add	x1, x0, #0x8
   50928: aa0d03e2     	mov	x2, x13
   5092c: b4000162     	cbz	x2, 0x50958 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x188>
   50930: 361800a1     	tbz	w1, #0x3, 0x50944 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x174>
   50934: d1002042     	sub	x2, x2, #0x8
   50938: f800843f     	str	xzr, [x1], #0x8
   5093c: 14000002     	b	0x50944 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x174>
   50940: a8817c3f     	stp	xzr, xzr, [x1], #0x10
   50944: f1004042     	subs	x2, x2, #0x10
   50948: 54ffffca     	b.ge	0x50940 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x170>
   5094c: 91004042     	add	x2, x2, #0x10
   50950: 36180042     	tbz	w2, #0x3, 0x50958 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x188>
   50954: f800843f     	str	xzr, [x1], #0x8
   50958: d5033abf     	dmb	ishst
   5095c: cb1b0000     	sub	x0, x0, x27
   50960: d343fc00     	lsr	x0, x0, #3
   50964: 91027189     	add	x9, x12, #0x9c
   50968: 889ffd20     	stlr	w0, [x9]
   5096c: b9400180     	ldr	w0, [x12]
   50970: 37080980     	tbnz	w0, #0x1, 0x50aa0 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x2d0>
   50974: 11000632     	add	w18, w17, #0x1
   50978: 72000e3f     	tst	w17, #0xf
   5097c: 54000060     	b.eq	0x50988 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1b8>
   50980: 2a1203f1     	mov	w17, w18
   50984: 17ffffdf     	b	0x50900 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x130>
   50988: 91006169     	add	x9, x11, #0x18
   5098c: 88dffd20     	ldar	w0, [x9]
   50990: 6b08001f     	cmp	w0, w8
   50994: 54000180     	b.eq	0x509c4 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1f4>
   50998: 34000f20     	cbz	w0, 0x50b7c <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3ac>
   5099c: 2a0003e0     	mov	w0, w0
   509a0: 8b000f60     	add	x0, x27, x0, lsl #3
   509a4: b9400001     	ldr	w1, [x0]
   509a8: 53057c21     	lsr	w1, w1, #5
   509ac: 6b07003f     	cmp	w1, w7
   509b0: 540017e1     	b.ne	0x50cac <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4dc>
   509b4: 39401000     	ldrb	w0, [x0, #0x4]
   509b8: 340000a0     	cbz	w0, 0x509cc <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1fc>
   509bc: 2a1203f1     	mov	w17, w18
   509c0: 17ffffd0     	b	0x50900 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x130>
   509c4: aa0603e0     	mov	x0, x6
   509c8: 17fffffb     	b	0x509b4 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1e4>
   509cc: b9402460     	ldr	w0, [x3, #0x24]
   509d0: 340000e0     	cbz	w0, 0x509ec <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x21c>
   509d4: 2a0003e0     	mov	w0, w0
   509d8: 8b000f60     	add	x0, x27, x0, lsl #3
   509dc: b9400000     	ldr	w0, [x0]
   509e0: 53057c00     	lsr	w0, w0, #5
   509e4: 6b05001f     	cmp	w0, w5
   509e8: 54000740     	b.eq	0x50ad0 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x300>
   509ec: b9006ff2     	str	w18, [sp, #0x6c]
   509f0: b90067f1     	str	w17, [sp, #0x64]
   509f4: aa0303e0     	mov	x0, x3
   509f8: aa0403e1     	mov	x1, x4
   509fc: 320003e2     	orr	w2, wzr, #0x1
   50a00: 94020b64     	bl	0xd3790 <GCImpl_collectWithoutAllocating_3JkbhdqsFF0bYizLQcyXX9>
   50a04: 340006a0     	cbz	w0, 0x50ad8 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x308>
   50a08: f9400fe3     	ldr	x3, [sp, #0x18]
   50a0c: b9402460     	ldr	w0, [x3, #0x24]
   50a10: 340015e0     	cbz	w0, 0x50ccc <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4fc>
   50a14: 2a0003e0     	mov	w0, w0
   50a18: 8b000f60     	add	x0, x27, x0, lsl #3
   50a1c: b940001e     	ldr	w30, [x0]
   50a20: 53057fc1     	lsr	w1, w30, #5
   50a24: 2a0103e1     	mov	w1, w1
   50a28: 8b010f61     	add	x1, x27, x1, lsl #3
   50a2c: b9400821     	ldr	w1, [x1, #0x8]
   50a30: 12802362     	mov	w2, #-0x11c             // =-284
   50a34: 0b212041     	add	w1, w2, w1, uxth
   50a38: 7100303f     	cmp	w1, #0xc
   50a3c: 540014c2     	b.hs	0x50cd4 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x504>
   50a40: 53057fde     	lsr	w30, w30, #5
   50a44: 2a1e03fe     	mov	w30, w30
   50a48: 8b1e0f7e     	add	x30, x27, x30, lsl #3
   50a4c: f9404fde     	ldr	x30, [x30, #0x98]
   50a50: d503201f     	nop
   50a54: 103c15e1     	adr	x1, 0xc8d10 <CollectionPolicy_onMaximumHeapSizeExceeded_rghEcVm2KwLAMqoiYhXY02>
   50a58: cb1a0021     	sub	x1, x1, x26
   50a5c: eb0103df     	cmp	x30, x1
   50a60: 54000bc0     	b.eq	0x50bd8 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x408>
   50a64: 8b1a03de     	add	x30, x30, x26
   50a68: d63f03c0     	blr	x30
   50a6c: b9406ff1     	ldr	w17, [sp, #0x6c]
   50a70: d2800110     	mov	x16, #0x8               // =8
   50a74: f2a00030     	movk	x16, #0x1, lsl #16
   50a78: 52850c0f     	mov	w15, #0x2860            // =10336
   50a7c: 72a0040f     	movk	w15, #0x20, lsl #16
   50a80: b9406bee     	ldr	w14, [sp, #0x68]
   50a84: f9402fed     	ldr	x13, [sp, #0x58]
   50a88: a942afec     	ldp	x12, x11, [sp, #0x28]
   50a8c: 29499fe8     	ldp	w8, w7, [sp, #0x4c]
   50a90: f9401fe6     	ldr	x6, [sp, #0x38]
   50a94: b94057e5     	ldr	w5, [sp, #0x54]
   50a98: a94193e3     	ldp	x3, x4, [sp, #0x18]
   50a9c: 17ffff99     	b	0x50900 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x130>
   50aa0: aa0c03e1     	mov	x1, x12
   50aa4: 37000340     	tbnz	w0, #0x0, 0x50b0c <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x33c>
   50aa8: b26db3e0     	mov	x0, #-0x80000           // =-524288
   50aac: 8a000020     	and	x0, x1, x0
   50ab0: 91210002     	add	x2, x0, #0x840
   50ab4: cb020021     	sub	x1, x1, x2
   50ab8: b27d0be2     	orr	x2, xzr, #0x38
   50abc: 8b412441     	add	x1, x2, x1, lsr #9
   50ac0: 38e16802     	ldrsb	w2, [x0, x1]
   50ac4: 34fff582     	cbz	w2, 0x50974 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1a4>
   50ac8: 3821681f     	strb	wzr, [x0, x1]
   50acc: 17ffffaa     	b	0x50974 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1a4>
   50ad0: 2a1203f1     	mov	w17, w18
   50ad4: 17ffff8b     	b	0x50900 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x130>
   50ad8: b9406ff1     	ldr	w17, [sp, #0x6c]
   50adc: d2800110     	mov	x16, #0x8               // =8
   50ae0: f2a00030     	movk	x16, #0x1, lsl #16
   50ae4: 52850c0f     	mov	w15, #0x2860            // =10336
   50ae8: 72a0040f     	movk	w15, #0x20, lsl #16
   50aec: b9406bee     	ldr	w14, [sp, #0x68]
   50af0: f9402fed     	ldr	x13, [sp, #0x58]
   50af4: a942afec     	ldp	x12, x11, [sp, #0x28]
   50af8: 29499fe8     	ldp	w8, w7, [sp, #0x4c]
   50afc: f9401fe6     	ldr	x6, [sp, #0x38]
   50b00: b94057e5     	ldr	w5, [sp, #0x54]
   50b04: a94193e3     	ldp	x3, x4, [sp, #0x18]
   50b08: 17ffff7e     	b	0x50900 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x130>
   50b0c: f85f8020     	ldur	x0, [x1, #-0x8]
   50b10: aa0c03e2     	mov	x2, x12
   50b14: cb000020     	sub	x0, x1, x0
   50b18: 91027181     	add	x1, x12, #0x9c
   50b1c: cb020021     	sub	x1, x1, x2
   50b20: b27c07e2     	orr	x2, xzr, #0x30
   50b24: 8b412441     	add	x1, x2, x1, lsr #9
   50b28: 38e16802     	ldrsb	w2, [x0, x1]
   50b2c: 34fff242     	cbz	w2, 0x50974 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1a4>
   50b30: 3821681f     	strb	wzr, [x0, x1]
   50b34: 17ffff90     	b	0x50974 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x1a4>
   50b38: b90067f1     	str	w17, [sp, #0x64]
   50b3c: 92407de0     	and	x0, x15, #0xffffffff
   50b40: b9406be1     	ldr	w1, [sp, #0x68]
   50b44: 940289cb     	bl	0xf3270 <GenScavengeAllocationSupport_slowNewArray_RH1677CN3I59XJMcBPW18F>
   50b48: b94067f1     	ldr	w17, [sp, #0x64]
   50b4c: d2800110     	mov	x16, #0x8               // =8
   50b50: f2a00030     	movk	x16, #0x1, lsl #16
   50b54: 52850c0f     	mov	w15, #0x2860            // =10336
   50b58: 72a0040f     	movk	w15, #0x20, lsl #16
   50b5c: b9406bee     	ldr	w14, [sp, #0x68]
   50b60: f9402fed     	ldr	x13, [sp, #0x58]
   50b64: a942afec     	ldp	x12, x11, [sp, #0x28]
   50b68: 29499fe8     	ldp	w8, w7, [sp, #0x4c]
   50b6c: f9401fe6     	ldr	x6, [sp, #0x38]
   50b70: b94057e5     	ldr	w5, [sp, #0x54]
   50b74: a94193e3     	ldp	x3, x4, [sp, #0x18]
   50b78: 17ffff79     	b	0x5095c <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x18c>
   50b7c: b90067f1     	str	w17, [sp, #0x64]
   50b80: 940639f8     	bl	0x1df360 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   50b84: b9404be1     	ldr	w1, [sp, #0x48]
   50b88: 34000a01     	cbz	w1, 0x50cc8 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4f8>
   50b8c: f94023e2     	ldr	x2, [sp, #0x40]
   50b90: cb1b0000     	sub	x0, x0, x27
   50b94: d343fc00     	lsr	x0, x0, #3
   50b98: 91001049     	add	x9, x2, #0x4
   50b9c: 889ffd20     	stlr	w0, [x9]
   50ba0: b9400040     	ldr	w0, [x2]
   50ba4: 370801c0     	tbnz	w0, #0x1, 0x50bdc <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x40c>
   50ba8: a9477bfd     	ldp	x29, x30, [sp, #0x70]
   50bac: 910203ff     	add	sp, sp, #0x80
   50bb0: b9401389     	ldr	w9, [x28, #0x10]
   50bb4: 7100013f     	cmp	w9, #0x0
   50bb8: 5400098d     	b.le	0x50ce8 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x518>
   50bbc: d65f03c0     	ret
   50bc0: 7100047f     	cmp	w3, #0x1
   50bc4: 54ffe501     	b.ne	0x50864 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
   50bc8: 91002009     	add	x9, x0, #0x8
   50bcc: 88dffd3e     	ldar	w30, [x9]
   50bd0: 34ffe4be     	cbz	w30, 0x50864 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
   50bd4: 1400000d     	b	0x50c08 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x438>
   50bd8: 94031216     	bl	0x115430 <OutOfMemoryUtil_heapSizeExceeded_tgNQlvtYRwAN6ucK4Y7ahE>
   50bdc: b26db3fd     	mov	x29, #-0x80000          // =-524288
   50be0: f94023e2     	ldr	x2, [sp, #0x40]
   50be4: 8a1d005d     	and	x29, x2, x29
   50be8: 912103be     	add	x30, x29, #0x840
   50bec: cb1e005e     	sub	x30, x2, x30
   50bf0: b27d0be0     	orr	x0, xzr, #0x38
   50bf4: 8b5e241e     	add	x30, x0, x30, lsr #9
   50bf8: 38fe6ba0     	ldrsb	w0, [x29, x30]
   50bfc: 34fffd60     	cbz	w0, 0x50ba8 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3d8>
   50c00: 383e6bbf     	strb	wzr, [x29, x30]
   50c04: 17ffffe9     	b	0x50ba8 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3d8>
   50c08: 2a1e03fe     	mov	w30, w30
   50c0c: 8b1e0f7e     	add	x30, x27, x30, lsl #3
   50c10: 910023c9     	add	x9, x30, #0x8
   50c14: 88dffd3e     	ldar	w30, [x9]
   50c18: 34ffe27e     	cbz	w30, 0x50864 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
   50c1c: 2a1e03e0     	mov	w0, w30
   50c20: 8b000f60     	add	x0, x27, x0, lsl #3
   50c24: 91004009     	add	x9, x0, #0x10
   50c28: 88dffd3e     	ldar	w30, [x9]
   50c2c: 34ffe1de     	cbz	w30, 0x50864 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
   50c30: 91004009     	add	x9, x0, #0x10
   50c34: 88dffd23     	ldar	w3, [x9]
   50c38: 121f787e     	and	w30, w3, #0xfffffffe
   50c3c: 91004004     	add	x4, x0, #0x10
   50c40: 885f7c85     	ldxr	w5, [x4]
   50c44: 6b0300bf     	cmp	w5, w3
   50c48: 1a8503ca     	csel	w10, w30, w5, eq
   50c4c: 8809fc8a     	stlxr	w9, w10, [x4]
   50c50: 35ffff89     	cbnz	w9, 0x50c40 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x470>
   50c54: d5033bbf     	dmb	ish
   50c58: 1a9f17fe     	cset	w30, eq
   50c5c: 350000be     	cbnz	w30, 0x50c70 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x4a0>
   50c60: b9401389     	ldr	w9, [x28, #0x10]
   50c64: 35fffe69     	cbnz	w9, 0x50c30 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x460>
   50c68: 9406a0da     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50c6c: 17fffff1     	b	0x50c30 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x460>
   50c70: b9400c00     	ldr	w0, [x0, #0xc]
   50c74: 34ffdf80     	cbz	w0, 0x50864 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
   50c78: 2a0003e0     	mov	w0, w0
   50c7c: 8b000f60     	add	x0, x27, x0, lsl #3
   50c80: 940699e0     	bl	0x1f7400 <PlatformThreads_unpark_FJj4Yhv2Ja3jndtYAdmYl0>
   50c84: 17fffef8     	b	0x50864 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x94>
   50c88: a9477bfd     	ldp	x29, x30, [sp, #0x70]
   50c8c: 910203ff     	add	sp, sp, #0x80
   50c90: b9401389     	ldr	w9, [x28, #0x10]
   50c94: 7100013f     	cmp	w9, #0x0
   50c98: 5400028d     	b.le	0x50ce8 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x518>
   50c9c: d65f03c0     	ret
   50ca0: 17ffffb9     	b	0x50b84 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
   50ca4: 17ffffb8     	b	0x50b84 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
   50ca8: 17ffffb7     	b	0x50b84 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
   50cac: b90067f1     	str	w17, [sp, #0x64]
   50cb0: 940637e4     	bl	0x1dec40 <ImplicitExceptions_createOptClassCastException_Ewnh8dGuUvIm5VBYxiDX2K>
   50cb4: 17ffffb4     	b	0x50b84 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
   50cb8: 940639aa     	bl	0x1df360 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   50cbc: 17ffffb2     	b	0x50b84 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
   50cc0: 940639a8     	bl	0x1df360 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   50cc4: 17ffffb0     	b	0x50b84 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
   50cc8: 94065512     	bl	0x1e6110 <ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
   50ccc: 940639a5     	bl	0x1df360 <ImplicitExceptions_createOptNullPointerException_PShVv49ViuFEXSnudhxuAE>
   50cd0: 17ffffad     	b	0x50b84 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
   50cd4: b94067f1     	ldr	w17, [sp, #0x64]
   50cd8: 940638be     	bl	0x1defd0 <ImplicitExceptions_createOptIncompatibleClassChangeError_ExDFiMUY3I5T00lGxxhlnL>
   50cdc: 17ffffaa     	b	0x50b84 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
   50ce0: 17ffffa9     	b	0x50b84 <Floor$$L510bcefe_run_sY7rwcdEFY32dSqH5FoPQL+0x3b4>
   50ce4: 1402be73     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50ce8: 1406a0ba     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50cec: cccccccc     	<unknown>

# svm: Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC
0000000000050cf0 <Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC>:
   50cf0: d100c3e9     	sub	x9, sp, #0x30
   50cf4: f940078a     	ldr	x10, [x28, #0x8]
   50cf8: eb0a013f     	cmp	x9, x10
   50cfc: 54000549     	b.ls	0x50da4 <Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0xb4>
   50d00: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50d04: 9100013f     	mov	sp, x9
   50d08: b9001fe1     	str	w1, [sp, #0x1c]
   50d0c: 7100043f     	cmp	w1, #0x1
   50d10: 5400018b     	b.lt	0x50d40 <Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0x50>
   50d14: aa0203e0     	mov	x0, x2
   50d18: 97fffd4e     	bl	0x50250 <Batch_batch16_7L76EVQlXq5pOOlPBaERnB>
   50d1c: b9401fe1     	ldr	w1, [sp, #0x1c]
   50d20: 7100443f     	cmp	w1, #0x11
   50d24: 540001ca     	b.ge	0x50d5c <Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0x6c>
   50d28: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   50d2c: 9100c3ff     	add	sp, sp, #0x30
   50d30: b9401389     	ldr	w9, [x28, #0x10]
   50d34: 7100013f     	cmp	w9, #0x0
   50d38: 5400038d     	b.le	0x50da8 <Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0xb8>
   50d3c: d65f03c0     	ret
   50d40: aa0203e0     	mov	x0, x2
   50d44: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   50d48: 9100c3ff     	add	sp, sp, #0x30
   50d4c: b9401389     	ldr	w9, [x28, #0x10]
   50d50: 7100013f     	cmp	w9, #0x0
   50d54: 540002ad     	b.le	0x50da8 <Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0xb8>
   50d58: d65f03c0     	ret
   50d5c: 321c03e9     	orr	w9, wzr, #0x10
   50d60: b9001be9     	str	w9, [sp, #0x18]
   50d64: d503201f     	nop
   50d68: d503201f     	nop
   50d6c: d503201f     	nop
   50d70: 97fffd38     	bl	0x50250 <Batch_batch16_7L76EVQlXq5pOOlPBaERnB>
   50d74: b9401be1     	ldr	w1, [sp, #0x18]
   50d78: 11004022     	add	w2, w1, #0x10
   50d7c: b9401fe3     	ldr	w3, [sp, #0x1c]
   50d80: 6b03005f     	cmp	w2, w3
   50d84: 54fffd2a     	b.ge	0x50d28 <Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0x38>
   50d88: b9401389     	ldr	w9, [x28, #0x10]
   50d8c: 34000069     	cbz	w9, 0x50d98 <Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0xa8>
   50d90: b9001be2     	str	w2, [sp, #0x18]
   50d94: 17fffff7     	b	0x50d70 <Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0x80>
   50d98: 9406a08e     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50d9c: b9001be2     	str	w2, [sp, #0x18]
   50da0: 17fffff4     	b	0x50d70 <Floor$$L9dd63404_run_zeq0KfgBu5IumxxDL7K2JC+0x80>
   50da4: 1402be43     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50da8: 1406a08a     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50dac: cccccccc     	<unknown>

# svm: Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC
0000000000050db0 <Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC>:
   50db0: d10103e9     	sub	x9, sp, #0x40
   50db4: f940078a     	ldr	x10, [x28, #0x8]
   50db8: eb0a013f     	cmp	x9, x10
   50dbc: 540008c9     	b.ls	0x50ed4 <Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x124>
   50dc0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50dc4: 9100013f     	mov	sp, x9
   50dc8: 7100043f     	cmp	w1, #0x1
   50dcc: 5400080b     	b.lt	0x50ecc <Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x11c>
   50dd0: b9002fe1     	str	w1, [sp, #0x2c]
   50dd4: 529ea3c0     	mov	w0, #0xf51e             // =62750
   50dd8: 72a000e0     	movk	w0, #0x7, lsl #16
   50ddc: 8b000f60     	add	x0, x27, x0, lsl #3
   50de0: f9402403     	ldr	x3, [x0, #0x48]
   50de4: f90013e3     	str	x3, [sp, #0x20]
   50de8: 2a1f03e9     	mov	w9, wzr
   50dec: b9002be9     	str	w9, [sp, #0x28]
   50df0: aa0203e0     	mov	x0, x2
   50df4: aa0303fe     	mov	x30, x3
   50df8: d63f03c0     	blr	x30
   50dfc: f94013fe     	ldr	x30, [sp, #0x20]
   50e00: d63f03c0     	blr	x30
   50e04: f94013fe     	ldr	x30, [sp, #0x20]
   50e08: d63f03c0     	blr	x30
   50e0c: f94013fe     	ldr	x30, [sp, #0x20]
   50e10: d63f03c0     	blr	x30
   50e14: f94013fe     	ldr	x30, [sp, #0x20]
   50e18: d63f03c0     	blr	x30
   50e1c: f94013fe     	ldr	x30, [sp, #0x20]
   50e20: d63f03c0     	blr	x30
   50e24: f94013fe     	ldr	x30, [sp, #0x20]
   50e28: d63f03c0     	blr	x30
   50e2c: f94013fe     	ldr	x30, [sp, #0x20]
   50e30: d63f03c0     	blr	x30
   50e34: f94013fe     	ldr	x30, [sp, #0x20]
   50e38: d63f03c0     	blr	x30
   50e3c: f94013fe     	ldr	x30, [sp, #0x20]
   50e40: d63f03c0     	blr	x30
   50e44: f94013fe     	ldr	x30, [sp, #0x20]
   50e48: d63f03c0     	blr	x30
   50e4c: f94013fe     	ldr	x30, [sp, #0x20]
   50e50: d63f03c0     	blr	x30
   50e54: f94013fe     	ldr	x30, [sp, #0x20]
   50e58: d63f03c0     	blr	x30
   50e5c: f94013fe     	ldr	x30, [sp, #0x20]
   50e60: d63f03c0     	blr	x30
   50e64: f94013fe     	ldr	x30, [sp, #0x20]
   50e68: d63f03c0     	blr	x30
   50e6c: f94013fe     	ldr	x30, [sp, #0x20]
   50e70: d63f03c0     	blr	x30
   50e74: b9402bfd     	ldr	w29, [sp, #0x28]
   50e78: 110043a1     	add	w1, w29, #0x10
   50e7c: b9402fe2     	ldr	w2, [sp, #0x2c]
   50e80: 6b02003f     	cmp	w1, w2
   50e84: 5400018a     	b.ge	0x50eb4 <Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x104>
   50e88: b9401389     	ldr	w9, [x28, #0x10]
   50e8c: 340000a9     	cbz	w9, 0x50ea0 <Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0xf0>
   50e90: b9002be1     	str	w1, [sp, #0x28]
   50e94: f94013e3     	ldr	x3, [sp, #0x20]
   50e98: aa0003e2     	mov	x2, x0
   50e9c: 17ffffd5     	b	0x50df0 <Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x40>
   50ea0: 9406a04c     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50ea4: b9002be1     	str	w1, [sp, #0x28]
   50ea8: f94013e3     	ldr	x3, [sp, #0x20]
   50eac: aa0003e2     	mov	x2, x0
   50eb0: 17ffffd0     	b	0x50df0 <Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x40>
   50eb4: a9437bfd     	ldp	x29, x30, [sp, #0x30]
   50eb8: 910103ff     	add	sp, sp, #0x40
   50ebc: b9401389     	ldr	w9, [x28, #0x10]
   50ec0: 7100013f     	cmp	w9, #0x0
   50ec4: 540000ad     	b.le	0x50ed8 <Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x128>
   50ec8: d65f03c0     	ret
   50ecc: aa0203e0     	mov	x0, x2
   50ed0: 17fffff9     	b	0x50eb4 <Floor$$L30d5b9c5_run_xQDISKYGLb5pCu0Yk3FGYC+0x104>
   50ed4: 1402bdf7     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   50ed8: 1406a03e     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   50edc: cccccccc     	<unknown>

# svm: Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K
0000000000050ee0 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K>:
   50ee0: d10143e9     	sub	x9, sp, #0x50
   50ee4: f940078a     	ldr	x10, [x28, #0x8]
   50ee8: eb0a013f     	cmp	x9, x10
   50eec: 54000bc9     	b.ls	0x51064 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x184>
   50ef0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   50ef4: 9100013f     	mov	sp, x9
   50ef8: b90033e1     	str	w1, [sp, #0x30]
   50efc: 7100043f     	cmp	w1, #0x1
   50f00: 320003e0     	orr	w0, wzr, #0x1
   50f04: 1a80d020     	csel	w0, w1, w0, le
   50f08: 7100041f     	cmp	w0, #0x1
   50f0c: 54000a6b     	b.lt	0x51058 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x178>
   50f10: aa0203e0     	mov	x0, x2
   50f14: 940003eb     	bl	0x51ec0 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50f18: 320003e2     	orr	w2, wzr, #0x1
   50f1c: b94033e1     	ldr	w1, [sp, #0x30]
   50f20: 52800083     	mov	w3, #0x4                // =4
   50f24: 72b00003     	movk	w3, #0x8000, lsl #16
   50f28: 320103e4     	orr	w4, wzr, #0x80000000
   50f2c: 51001025     	sub	w5, w1, #0x4
   50f30: 6b03003f     	cmp	w1, w3
   50f34: 1a85b083     	csel	w3, w4, w5, lt
   50f38: b9002be3     	str	w3, [sp, #0x28]
   50f3c: 11000c64     	add	w4, w3, #0x3
   50f40: b90027e4     	str	w4, [sp, #0x24]
   50f44: 2a0203e1     	mov	w1, w2
   50f48: d503201f     	nop
   50f4c: d503201f     	nop
   50f50: 6b03003f     	cmp	w1, w3
   50f54: 5400062a     	b.ge	0x51018 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x138>
   50f58: 4b010085     	sub	w5, w4, w1
   50f5c: 53027ca6     	lsr	w6, w5, #2
   50f60: 121e74a5     	and	w5, w5, #0xfffffffc
   50f64: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   50f68: 321203e6     	orr	w6, wzr, #0x4000
   50f6c: 1a8630a5     	csel	w5, w5, w6, lo
   50f70: 710004bf     	cmp	w5, #0x1
   50f74: 540004ab     	b.lt	0x51008 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x128>
   50f78: b90037e1     	str	w1, [sp, #0x34]
   50f7c: b9003fe5     	str	w5, [sp, #0x3c]
   50f80: 2a1f03e2     	mov	w2, wzr
   50f84: d503201f     	nop
   50f88: d503201f     	nop
   50f8c: d503201f     	nop
   50f90: 0b010047     	add	w7, w2, w1
   50f94: b90023e7     	str	w7, [sp, #0x20]
   50f98: b9002fe2     	str	w2, [sp, #0x2c]
   50f9c: 940003c9     	bl	0x51ec0 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50fa0: b94023e7     	ldr	w7, [sp, #0x20]
   50fa4: 110004e1     	add	w1, w7, #0x1
   50fa8: b9003be1     	str	w1, [sp, #0x38]
   50fac: 940003c5     	bl	0x51ec0 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50fb0: b94023e7     	ldr	w7, [sp, #0x20]
   50fb4: 110008e1     	add	w1, w7, #0x2
   50fb8: b9003be1     	str	w1, [sp, #0x38]
   50fbc: 940003c1     	bl	0x51ec0 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50fc0: b94023e7     	ldr	w7, [sp, #0x20]
   50fc4: 11000ce1     	add	w1, w7, #0x3
   50fc8: b90023e1     	str	w1, [sp, #0x20]
   50fcc: 940003bd     	bl	0x51ec0 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   50fd0: b9402fe2     	ldr	w2, [sp, #0x2c]
   50fd4: 11001042     	add	w2, w2, #0x4
   50fd8: b9403fe5     	ldr	w5, [sp, #0x3c]
   50fdc: 6b05005f     	cmp	w2, w5
   50fe0: b94037e1     	ldr	w1, [sp, #0x34]
   50fe4: 54fffd6b     	b.lt	0x50f90 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0xb0>
   50fe8: 0b010041     	add	w1, w2, w1
   50fec: b9401389     	ldr	w9, [x28, #0x10]
   50ff0: 34000069     	cbz	w9, 0x50ffc <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x11c>
   50ff4: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   50ff8: 17ffffd6     	b	0x50f50 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x70>
   50ffc: 94069ff5     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51000: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   51004: 17ffffd3     	b	0x50f50 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x70>
   51008: b9401389     	ldr	w9, [x28, #0x10]
   5100c: 35fffa29     	cbnz	w9, 0x50f50 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x70>
   51010: 94069ff0     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51014: 17ffffcf     	b	0x50f50 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x70>
   51018: 2a0103e2     	mov	w2, w1
   5101c: 14000006     	b	0x51034 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x154>
   51020: b9002fe2     	str	w2, [sp, #0x2c]
   51024: 940003a7     	bl	0x51ec0 <Floor_identity_RGfigO1fJmId7IgDqMOKbC>
   51028: b9402fe2     	ldr	w2, [sp, #0x2c]
   5102c: 1100045d     	add	w29, w2, #0x1
   51030: 2a1d03e2     	mov	w2, w29
   51034: b94033e1     	ldr	w1, [sp, #0x30]
   51038: 6b01005f     	cmp	w2, w1
   5103c: 54ffff2b     	b.lt	0x51020 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x140>
   51040: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   51044: 910143ff     	add	sp, sp, #0x50
   51048: b9401389     	ldr	w9, [x28, #0x10]
   5104c: 7100013f     	cmp	w9, #0x0
   51050: 540000cd     	b.le	0x51068 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x188>
   51054: d65f03c0     	ret
   51058: aa0203e0     	mov	x0, x2
   5105c: 2a1f03e2     	mov	w2, wzr
   51060: 17ffffb0     	b	0x50f20 <Floor$$L449e57c0_run_Kd5L8bVzwGGhMIomqLBV4K+0x40>
   51064: 1402bd93     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51068: 14069fda     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5106c: cccccccc     	<unknown>

# svm: Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL
0000000000051070 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL>:
   51070: d10143e9     	sub	x9, sp, #0x50
   51074: f940078a     	ldr	x10, [x28, #0x8]
   51078: eb0a013f     	cmp	x9, x10
   5107c: 54000bc9     	b.ls	0x511f4 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x184>
   51080: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51084: 9100013f     	mov	sp, x9
   51088: b90033e1     	str	w1, [sp, #0x30]
   5108c: 7100043f     	cmp	w1, #0x1
   51090: 320003e0     	orr	w0, wzr, #0x1
   51094: 1a80d020     	csel	w0, w1, w0, le
   51098: 7100041f     	cmp	w0, #0x1
   5109c: 54000a6b     	b.lt	0x511e8 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x178>
   510a0: aa0203e0     	mov	x0, x2
   510a4: 940005e3     	bl	0x52830 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   510a8: 320003e2     	orr	w2, wzr, #0x1
   510ac: b94033e1     	ldr	w1, [sp, #0x30]
   510b0: 52800083     	mov	w3, #0x4                // =4
   510b4: 72b00003     	movk	w3, #0x8000, lsl #16
   510b8: 320103e4     	orr	w4, wzr, #0x80000000
   510bc: 51001025     	sub	w5, w1, #0x4
   510c0: 6b03003f     	cmp	w1, w3
   510c4: 1a85b083     	csel	w3, w4, w5, lt
   510c8: b9002be3     	str	w3, [sp, #0x28]
   510cc: 11000c64     	add	w4, w3, #0x3
   510d0: b90027e4     	str	w4, [sp, #0x24]
   510d4: 2a0203e1     	mov	w1, w2
   510d8: d503201f     	nop
   510dc: d503201f     	nop
   510e0: 6b03003f     	cmp	w1, w3
   510e4: 5400062a     	b.ge	0x511a8 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x138>
   510e8: 4b010085     	sub	w5, w4, w1
   510ec: 53027ca6     	lsr	w6, w5, #2
   510f0: 121e74a5     	and	w5, w5, #0xfffffffc
   510f4: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   510f8: 321203e6     	orr	w6, wzr, #0x4000
   510fc: 1a8630a5     	csel	w5, w5, w6, lo
   51100: 710004bf     	cmp	w5, #0x1
   51104: 540004ab     	b.lt	0x51198 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x128>
   51108: b90037e1     	str	w1, [sp, #0x34]
   5110c: b9003fe5     	str	w5, [sp, #0x3c]
   51110: 2a1f03e2     	mov	w2, wzr
   51114: d503201f     	nop
   51118: d503201f     	nop
   5111c: d503201f     	nop
   51120: 0b010047     	add	w7, w2, w1
   51124: b90023e7     	str	w7, [sp, #0x20]
   51128: b9002fe2     	str	w2, [sp, #0x2c]
   5112c: 940005c1     	bl	0x52830 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   51130: b94023e7     	ldr	w7, [sp, #0x20]
   51134: 110004e1     	add	w1, w7, #0x1
   51138: b9003be1     	str	w1, [sp, #0x38]
   5113c: 940005bd     	bl	0x52830 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   51140: b94023e7     	ldr	w7, [sp, #0x20]
   51144: 110008e1     	add	w1, w7, #0x2
   51148: b9003be1     	str	w1, [sp, #0x38]
   5114c: 940005b9     	bl	0x52830 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   51150: b94023e7     	ldr	w7, [sp, #0x20]
   51154: 11000ce1     	add	w1, w7, #0x3
   51158: b90023e1     	str	w1, [sp, #0x20]
   5115c: 940005b5     	bl	0x52830 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   51160: b9402fe2     	ldr	w2, [sp, #0x2c]
   51164: 11001042     	add	w2, w2, #0x4
   51168: b9403fe5     	ldr	w5, [sp, #0x3c]
   5116c: 6b05005f     	cmp	w2, w5
   51170: b94037e1     	ldr	w1, [sp, #0x34]
   51174: 54fffd6b     	b.lt	0x51120 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0xb0>
   51178: 0b010041     	add	w1, w2, w1
   5117c: b9401389     	ldr	w9, [x28, #0x10]
   51180: 34000069     	cbz	w9, 0x5118c <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x11c>
   51184: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   51188: 17ffffd6     	b	0x510e0 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x70>
   5118c: 94069f91     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51190: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   51194: 17ffffd3     	b	0x510e0 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x70>
   51198: b9401389     	ldr	w9, [x28, #0x10]
   5119c: 35fffa29     	cbnz	w9, 0x510e0 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x70>
   511a0: 94069f8c     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   511a4: 17ffffcf     	b	0x510e0 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x70>
   511a8: 2a0103e2     	mov	w2, w1
   511ac: 14000006     	b	0x511c4 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x154>
   511b0: b9002fe2     	str	w2, [sp, #0x2c]
   511b4: 9400059f     	bl	0x52830 <Floor_leaf_xl8tBddbMc9N8Nhe5l1Y8G>
   511b8: b9402fe2     	ldr	w2, [sp, #0x2c]
   511bc: 1100045d     	add	w29, w2, #0x1
   511c0: 2a1d03e2     	mov	w2, w29
   511c4: b94033e1     	ldr	w1, [sp, #0x30]
   511c8: 6b01005f     	cmp	w2, w1
   511cc: 54ffff2b     	b.lt	0x511b0 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x140>
   511d0: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   511d4: 910143ff     	add	sp, sp, #0x50
   511d8: b9401389     	ldr	w9, [x28, #0x10]
   511dc: 7100013f     	cmp	w9, #0x0
   511e0: 540000cd     	b.le	0x511f8 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x188>
   511e4: d65f03c0     	ret
   511e8: aa0203e0     	mov	x0, x2
   511ec: 2a1f03e2     	mov	w2, wzr
   511f0: 17ffffb0     	b	0x510b0 <Floor$$L61909a59_run_wfD2VY4UVQLvKHA63JxVLL+0x40>
   511f4: 1402bd2f     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   511f8: 14069f76     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   511fc: cccccccc     	<unknown>

# svm: Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60
0000000000051200 <Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60>:
   51200: d100c3e9     	sub	x9, sp, #0x30
   51204: f940078a     	ldr	x10, [x28, #0x8]
   51208: eb0a013f     	cmp	x9, x10
   5120c: 54000589     	b.ls	0x512bc <Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0xbc>
   51210: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51214: 9100013f     	mov	sp, x9
   51218: d28cc1a0     	mov	x0, #0x660d             // =26125
   5121c: f2a00320     	movk	x0, #0x19, lsl #16
   51220: d29e6bfd     	mov	x29, #0xf35f            // =62303
   51224: f2a78ddd     	movk	x29, #0x3c6e, lsl #16
   51228: 2a1f03fe     	mov	w30, wzr
   5122c: d503201f     	nop
   51230: 6b0103df     	cmp	w30, w1
   51234: 5400036a     	b.ge	0x512a0 <Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0xa0>
   51238: 4b1e0023     	sub	w3, w1, w30
   5123c: 7140047f     	cmp	w3, #0x1, lsl #12       // =0x1000
   51240: 321403e4     	orr	w4, wzr, #0x1000
   51244: 1a849063     	csel	w3, w3, w4, ls
   51248: 7100007f     	cmp	w3, #0x0
   5124c: 2a1f03e4     	mov	w4, wzr
   51250: 1a84a064     	csel	w4, w3, w4, ge
   51254: 7100047f     	cmp	w3, #0x1
   51258: 5400014b     	b.lt	0x51280 <Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0x80>
   5125c: 92800003     	mov	x3, #-0x1               // =-1
   51260: 8b244063     	add	x3, x3, w4, uxtw
   51264: aa1f03e5     	mov	x5, xzr
   51268: d503201f     	nop
   5126c: d503201f     	nop
   51270: 9b007442     	madd	x2, x2, x0, x29
   51274: 910004a5     	add	x5, x5, #0x1
   51278: eb05007f     	cmp	x3, x5
   5127c: 54ffffaa     	b.ge	0x51270 <Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0x70>
   51280: 0b0403c3     	add	w3, w30, w4
   51284: b9401389     	ldr	w9, [x28, #0x10]
   51288: 34000069     	cbz	w9, 0x51294 <Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0x94>
   5128c: 2a0303fe     	mov	w30, w3
   51290: 17ffffe8     	b	0x51230 <Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0x30>
   51294: 94069f4f     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51298: 2a0303fe     	mov	w30, w3
   5129c: 17ffffe5     	b	0x51230 <Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0x30>
   512a0: aa0203e0     	mov	x0, x2
   512a4: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   512a8: 9100c3ff     	add	sp, sp, #0x30
   512ac: b9401389     	ldr	w9, [x28, #0x10]
   512b0: 7100013f     	cmp	w9, #0x0
   512b4: 5400006d     	b.le	0x512c0 <Floor$$L000fc164_run_xtYbtwj8Sw9RCHmUYDsp60+0xc0>
   512b8: d65f03c0     	ret
   512bc: 1402bcfd     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   512c0: 14069f44     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   512c4: cccccccc     	<unknown>
   512c8: cccccccc     	<unknown>
   512cc: cccccccc     	<unknown>

# svm: Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737
00000000000512d0 <Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737>:
   512d0: d100c3e9     	sub	x9, sp, #0x30
   512d4: f940078a     	ldr	x10, [x28, #0x8]
   512d8: eb0a013f     	cmp	x9, x10
   512dc: 54000549     	b.ls	0x51384 <Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0xb4>
   512e0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   512e4: 9100013f     	mov	sp, x9
   512e8: b9001fe1     	str	w1, [sp, #0x1c]
   512ec: 7100043f     	cmp	w1, #0x1
   512f0: 5400018b     	b.lt	0x51320 <Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0x50>
   512f4: aa0203e0     	mov	x0, x2
   512f8: 94000382     	bl	0x52100 <Floor_javaBatch64_pgMJB9JHBI7okdXsdyhwU1>
   512fc: b9401fe1     	ldr	w1, [sp, #0x1c]
   51300: 7101043f     	cmp	w1, #0x41
   51304: 540001ca     	b.ge	0x5133c <Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0x6c>
   51308: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   5130c: 9100c3ff     	add	sp, sp, #0x30
   51310: b9401389     	ldr	w9, [x28, #0x10]
   51314: 7100013f     	cmp	w9, #0x0
   51318: 5400038d     	b.le	0x51388 <Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0xb8>
   5131c: d65f03c0     	ret
   51320: aa0203e0     	mov	x0, x2
   51324: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   51328: 9100c3ff     	add	sp, sp, #0x30
   5132c: b9401389     	ldr	w9, [x28, #0x10]
   51330: 7100013f     	cmp	w9, #0x0
   51334: 540002ad     	b.le	0x51388 <Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0xb8>
   51338: d65f03c0     	ret
   5133c: 321a03e9     	orr	w9, wzr, #0x40
   51340: b9001be9     	str	w9, [sp, #0x18]
   51344: d503201f     	nop
   51348: d503201f     	nop
   5134c: d503201f     	nop
   51350: 9400036c     	bl	0x52100 <Floor_javaBatch64_pgMJB9JHBI7okdXsdyhwU1>
   51354: b9401be1     	ldr	w1, [sp, #0x18]
   51358: 11010022     	add	w2, w1, #0x40
   5135c: b9401fe3     	ldr	w3, [sp, #0x1c]
   51360: 6b03005f     	cmp	w2, w3
   51364: 54fffd2a     	b.ge	0x51308 <Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0x38>
   51368: b9401389     	ldr	w9, [x28, #0x10]
   5136c: 34000069     	cbz	w9, 0x51378 <Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0xa8>
   51370: b9001be2     	str	w2, [sp, #0x18]
   51374: 17fffff7     	b	0x51350 <Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0x80>
   51378: 94069f16     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5137c: b9001be2     	str	w2, [sp, #0x18]
   51380: 17fffff4     	b	0x51350 <Floor$$La0fdad9e_run_SkCJLqgJj0FmJa5mFWu737+0x80>
   51384: 1402bccb     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51388: 14069f12     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5138c: cccccccc     	<unknown>

# svm: Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I
0000000000051390 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I>:
   51390: d10143e9     	sub	x9, sp, #0x50
   51394: f940078a     	ldr	x10, [x28, #0x8]
   51398: eb0a013f     	cmp	x9, x10
   5139c: 54000f09     	b.ls	0x5157c <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x1ec>
   513a0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   513a4: 9100013f     	mov	sp, x9
   513a8: b90033e1     	str	w1, [sp, #0x30]
   513ac: 529ea3c0     	mov	w0, #0xf51e             // =62750
   513b0: 72a000e0     	movk	w0, #0x7, lsl #16
   513b4: 8b000f60     	add	x0, x27, x0, lsl #3
   513b8: f9402403     	ldr	x3, [x0, #0x48]
   513bc: f9000fe3     	str	x3, [sp, #0x18]
   513c0: 7100043f     	cmp	w1, #0x1
   513c4: 320003e0     	orr	w0, wzr, #0x1
   513c8: 1a80d020     	csel	w0, w1, w0, le
   513cc: 7100041f     	cmp	w0, #0x1
   513d0: 54000d0b     	b.lt	0x51570 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x1e0>
   513d4: aa0203e0     	mov	x0, x2
   513d8: aa0303fe     	mov	x30, x3
   513dc: d63f03c0     	blr	x30
   513e0: 320003fe     	orr	w30, wzr, #0x1
   513e4: b94033e1     	ldr	w1, [sp, #0x30]
   513e8: 52800082     	mov	w2, #0x4                // =4
   513ec: 72b00002     	movk	w2, #0x8000, lsl #16
   513f0: 320103e3     	orr	w3, wzr, #0x80000000
   513f4: 51001024     	sub	w4, w1, #0x4
   513f8: 6b02003f     	cmp	w1, w2
   513fc: 1a84b062     	csel	w2, w3, w4, lt
   51400: b9002be2     	str	w2, [sp, #0x28]
   51404: 11000c43     	add	w3, w2, #0x3
   51408: b90027e3     	str	w3, [sp, #0x24]
   5140c: f9400fe4     	ldr	x4, [sp, #0x18]
   51410: 2a1e03e1     	mov	w1, w30
   51414: d503201f     	nop
   51418: d503201f     	nop
   5141c: d503201f     	nop
   51420: 6b02003f     	cmp	w1, w2
   51424: 5400078a     	b.ge	0x51514 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x184>
   51428: 4b01007e     	sub	w30, w3, w1
   5142c: 53027fc6     	lsr	w6, w30, #2
   51430: 121e77de     	and	w30, w30, #0xfffffffc
   51434: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   51438: 321203e6     	orr	w6, wzr, #0x4000
   5143c: 1a8633c6     	csel	w6, w30, w6, lo
   51440: 710004df     	cmp	w6, #0x1
   51444: 5400060b     	b.lt	0x51504 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x174>
   51448: b90037e1     	str	w1, [sp, #0x34]
   5144c: b9003fe6     	str	w6, [sp, #0x3c]
   51450: 2a1f03e2     	mov	w2, wzr
   51454: 14000008     	b	0x51474 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0xe4>
   51458: d503201f     	nop
   5145c: d503201f     	nop
   51460: d503201f     	nop
   51464: d503201f     	nop
   51468: d503201f     	nop
   5146c: d503201f     	nop
   51470: f9400fe4     	ldr	x4, [sp, #0x18]
   51474: 0b010048     	add	w8, w2, w1
   51478: b90023e8     	str	w8, [sp, #0x20]
   5147c: aa0403fe     	mov	x30, x4
   51480: b9002fe2     	str	w2, [sp, #0x2c]
   51484: d63f03c0     	blr	x30
   51488: b94023e8     	ldr	w8, [sp, #0x20]
   5148c: 11000501     	add	w1, w8, #0x1
   51490: f9400ffe     	ldr	x30, [sp, #0x18]
   51494: b9003be1     	str	w1, [sp, #0x38]
   51498: d63f03c0     	blr	x30
   5149c: b94023e8     	ldr	w8, [sp, #0x20]
   514a0: 11000901     	add	w1, w8, #0x2
   514a4: f9400ffe     	ldr	x30, [sp, #0x18]
   514a8: b9003be1     	str	w1, [sp, #0x38]
   514ac: d63f03c0     	blr	x30
   514b0: b94023e8     	ldr	w8, [sp, #0x20]
   514b4: 11000d01     	add	w1, w8, #0x3
   514b8: f9400ffe     	ldr	x30, [sp, #0x18]
   514bc: b90023e1     	str	w1, [sp, #0x20]
   514c0: d63f03c0     	blr	x30
   514c4: b9402fe2     	ldr	w2, [sp, #0x2c]
   514c8: 11001042     	add	w2, w2, #0x4
   514cc: b9403fe6     	ldr	w6, [sp, #0x3c]
   514d0: 6b06005f     	cmp	w2, w6
   514d4: b94037e1     	ldr	w1, [sp, #0x34]
   514d8: 54fffccb     	b.lt	0x51470 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0xe0>
   514dc: 0b010041     	add	w1, w2, w1
   514e0: b9401389     	ldr	w9, [x28, #0x10]
   514e4: 34000089     	cbz	w9, 0x514f4 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x164>
   514e8: 29448be3     	ldp	w3, w2, [sp, #0x24]
   514ec: f9400fe4     	ldr	x4, [sp, #0x18]
   514f0: 17ffffcc     	b	0x51420 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x90>
   514f4: 94069eb7     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   514f8: 29448be3     	ldp	w3, w2, [sp, #0x24]
   514fc: f9400fe4     	ldr	x4, [sp, #0x18]
   51500: 17ffffc8     	b	0x51420 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x90>
   51504: b9401389     	ldr	w9, [x28, #0x10]
   51508: 35fff8c9     	cbnz	w9, 0x51420 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x90>
   5150c: 94069eb1     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51510: 17ffffc4     	b	0x51420 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x90>
   51514: 2a0103e2     	mov	w2, w1
   51518: aa0403e3     	mov	x3, x4
   5151c: 1400000c     	b	0x5154c <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x1bc>
   51520: d503201f     	nop
   51524: d503201f     	nop
   51528: d503201f     	nop
   5152c: d503201f     	nop
   51530: b9002fe2     	str	w2, [sp, #0x2c]
   51534: aa0303fe     	mov	x30, x3
   51538: d63f03c0     	blr	x30
   5153c: b9402fe2     	ldr	w2, [sp, #0x2c]
   51540: 1100045d     	add	w29, w2, #0x1
   51544: 2a1d03e2     	mov	w2, w29
   51548: f9400fe3     	ldr	x3, [sp, #0x18]
   5154c: b94033e1     	ldr	w1, [sp, #0x30]
   51550: 6b01005f     	cmp	w2, w1
   51554: 54fffeeb     	b.lt	0x51530 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x1a0>
   51558: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   5155c: 910143ff     	add	sp, sp, #0x50
   51560: b9401389     	ldr	w9, [x28, #0x10]
   51564: 7100013f     	cmp	w9, #0x0
   51568: 540000cd     	b.le	0x51580 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x1f0>
   5156c: d65f03c0     	ret
   51570: 2a1f03fe     	mov	w30, wzr
   51574: aa0203e0     	mov	x0, x2
   51578: 17ffff9c     	b	0x513e8 <Floor$$La67625fa_run_8WOcKLMPn3CJJrJIdGpg1I+0x58>
   5157c: 1402bc4d     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51580: 14069e94     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51584: cccccccc     	<unknown>
   51588: cccccccc     	<unknown>
   5158c: cccccccc     	<unknown>

# svm: Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4
0000000000051590 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4>:
   51590: d10143e9     	sub	x9, sp, #0x50
   51594: f940078a     	ldr	x10, [x28, #0x8]
   51598: eb0a013f     	cmp	x9, x10
   5159c: 54000bc9     	b.ls	0x51714 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x184>
   515a0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   515a4: 9100013f     	mov	sp, x9
   515a8: b90033e1     	str	w1, [sp, #0x30]
   515ac: 7100043f     	cmp	w1, #0x1
   515b0: 320003e0     	orr	w0, wzr, #0x1
   515b4: 1a80d020     	csel	w0, w1, w0, le
   515b8: 7100041f     	cmp	w0, #0x1
   515bc: 54000a6b     	b.lt	0x51708 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x178>
   515c0: aa0203e0     	mov	x0, x2
   515c4: 9400171b     	bl	0x57230 <Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
   515c8: 320003e2     	orr	w2, wzr, #0x1
   515cc: b94033e1     	ldr	w1, [sp, #0x30]
   515d0: 52800083     	mov	w3, #0x4                // =4
   515d4: 72b00003     	movk	w3, #0x8000, lsl #16
   515d8: 320103e4     	orr	w4, wzr, #0x80000000
   515dc: 51001025     	sub	w5, w1, #0x4
   515e0: 6b03003f     	cmp	w1, w3
   515e4: 1a85b083     	csel	w3, w4, w5, lt
   515e8: b9002be3     	str	w3, [sp, #0x28]
   515ec: 11000c64     	add	w4, w3, #0x3
   515f0: b90027e4     	str	w4, [sp, #0x24]
   515f4: 2a0203e1     	mov	w1, w2
   515f8: d503201f     	nop
   515fc: d503201f     	nop
   51600: 6b03003f     	cmp	w1, w3
   51604: 5400062a     	b.ge	0x516c8 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x138>
   51608: 4b010085     	sub	w5, w4, w1
   5160c: 53027ca6     	lsr	w6, w5, #2
   51610: 121e74a5     	and	w5, w5, #0xfffffffc
   51614: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   51618: 321203e6     	orr	w6, wzr, #0x4000
   5161c: 1a8630a5     	csel	w5, w5, w6, lo
   51620: 710004bf     	cmp	w5, #0x1
   51624: 540004ab     	b.lt	0x516b8 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x128>
   51628: b90037e1     	str	w1, [sp, #0x34]
   5162c: b9003fe5     	str	w5, [sp, #0x3c]
   51630: 2a1f03e2     	mov	w2, wzr
   51634: d503201f     	nop
   51638: d503201f     	nop
   5163c: d503201f     	nop
   51640: 0b010047     	add	w7, w2, w1
   51644: b90023e7     	str	w7, [sp, #0x20]
   51648: b9002fe2     	str	w2, [sp, #0x2c]
   5164c: 940016f9     	bl	0x57230 <Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
   51650: b94023e7     	ldr	w7, [sp, #0x20]
   51654: 110004e1     	add	w1, w7, #0x1
   51658: b9003be1     	str	w1, [sp, #0x38]
   5165c: 940016f5     	bl	0x57230 <Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
   51660: b94023e7     	ldr	w7, [sp, #0x20]
   51664: 110008e1     	add	w1, w7, #0x2
   51668: b9003be1     	str	w1, [sp, #0x38]
   5166c: 940016f1     	bl	0x57230 <Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
   51670: b94023e7     	ldr	w7, [sp, #0x20]
   51674: 11000ce1     	add	w1, w7, #0x3
   51678: b90023e1     	str	w1, [sp, #0x20]
   5167c: 940016ed     	bl	0x57230 <Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
   51680: b9402fe2     	ldr	w2, [sp, #0x2c]
   51684: 11001042     	add	w2, w2, #0x4
   51688: b9403fe5     	ldr	w5, [sp, #0x3c]
   5168c: 6b05005f     	cmp	w2, w5
   51690: b94037e1     	ldr	w1, [sp, #0x34]
   51694: 54fffd6b     	b.lt	0x51640 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0xb0>
   51698: 0b010041     	add	w1, w2, w1
   5169c: b9401389     	ldr	w9, [x28, #0x10]
   516a0: 34000069     	cbz	w9, 0x516ac <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x11c>
   516a4: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   516a8: 17ffffd6     	b	0x51600 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x70>
   516ac: 94069e49     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   516b0: 29448fe4     	ldp	w4, w3, [sp, #0x24]
   516b4: 17ffffd3     	b	0x51600 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x70>
   516b8: b9401389     	ldr	w9, [x28, #0x10]
   516bc: 35fffa29     	cbnz	w9, 0x51600 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x70>
   516c0: 94069e44     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   516c4: 17ffffcf     	b	0x51600 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x70>
   516c8: 2a0103e2     	mov	w2, w1
   516cc: 14000006     	b	0x516e4 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x154>
   516d0: b9002fe2     	str	w2, [sp, #0x2c]
   516d4: 940016d7     	bl	0x57230 <Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>
   516d8: b9402fe2     	ldr	w2, [sp, #0x2c]
   516dc: 1100045d     	add	w29, w2, #0x1
   516e0: 2a1d03e2     	mov	w2, w29
   516e4: b94033e1     	ldr	w1, [sp, #0x30]
   516e8: 6b01005f     	cmp	w2, w1
   516ec: 54ffff2b     	b.lt	0x516d0 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x140>
   516f0: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   516f4: 910143ff     	add	sp, sp, #0x50
   516f8: b9401389     	ldr	w9, [x28, #0x10]
   516fc: 7100013f     	cmp	w9, #0x0
   51700: 540000cd     	b.le	0x51718 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x188>
   51704: d65f03c0     	ret
   51708: aa0203e0     	mov	x0, x2
   5170c: 2a1f03e2     	mov	w2, wzr
   51710: 17ffffb0     	b	0x515d0 <Floor$$L3d90f1ff_run_8ODbOjRoKQEmTlHclWlJv4+0x40>
   51714: 1402bbe7     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51718: 14069e2e     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5171c: cccccccc     	<unknown>

# svm: Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K
0000000000051720 <Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K>:
   51720: d100c3e9     	sub	x9, sp, #0x30
   51724: f940078a     	ldr	x10, [x28, #0x8]
   51728: eb0a013f     	cmp	x9, x10
   5172c: 54000549     	b.ls	0x517d4 <Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0xb4>
   51730: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51734: 9100013f     	mov	sp, x9
   51738: b9001fe1     	str	w1, [sp, #0x1c]
   5173c: 7100043f     	cmp	w1, #0x1
   51740: 5400018b     	b.lt	0x51770 <Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0x50>
   51744: aa0203e0     	mov	x0, x2
   51748: 97fffaca     	bl	0x50270 <Batch_batch256_DscONwqsjcJ5uLVvo6oz1L>
   5174c: b9401fe1     	ldr	w1, [sp, #0x1c]
   51750: 7104043f     	cmp	w1, #0x101
   51754: 540001ca     	b.ge	0x5178c <Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0x6c>
   51758: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   5175c: 9100c3ff     	add	sp, sp, #0x30
   51760: b9401389     	ldr	w9, [x28, #0x10]
   51764: 7100013f     	cmp	w9, #0x0
   51768: 5400038d     	b.le	0x517d8 <Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0xb8>
   5176c: d65f03c0     	ret
   51770: aa0203e0     	mov	x0, x2
   51774: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   51778: 9100c3ff     	add	sp, sp, #0x30
   5177c: b9401389     	ldr	w9, [x28, #0x10]
   51780: 7100013f     	cmp	w9, #0x0
   51784: 540002ad     	b.le	0x517d8 <Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0xb8>
   51788: d65f03c0     	ret
   5178c: 321803e9     	orr	w9, wzr, #0x100
   51790: b9001be9     	str	w9, [sp, #0x18]
   51794: d503201f     	nop
   51798: d503201f     	nop
   5179c: d503201f     	nop
   517a0: 97fffab4     	bl	0x50270 <Batch_batch256_DscONwqsjcJ5uLVvo6oz1L>
   517a4: b9401be1     	ldr	w1, [sp, #0x18]
   517a8: 11040022     	add	w2, w1, #0x100
   517ac: b9401fe3     	ldr	w3, [sp, #0x1c]
   517b0: 6b03005f     	cmp	w2, w3
   517b4: 54fffd2a     	b.ge	0x51758 <Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0x38>
   517b8: b9401389     	ldr	w9, [x28, #0x10]
   517bc: 34000069     	cbz	w9, 0x517c8 <Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0xa8>
   517c0: b9001be2     	str	w2, [sp, #0x18]
   517c4: 17fffff7     	b	0x517a0 <Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0x80>
   517c8: 94069e02     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   517cc: b9001be2     	str	w2, [sp, #0x18]
   517d0: 17fffff4     	b	0x517a0 <Floor$$L96d13f15_run_heZ2UOVKfQIpyPY9MSXq0K+0x80>
   517d4: 1402bbb7     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   517d8: 14069dfe     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   517dc: cccccccc     	<unknown>

# svm: Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9
00000000000517e0 <Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9>:
   517e0: d100c3e9     	sub	x9, sp, #0x30
   517e4: f940078a     	ldr	x10, [x28, #0x8]
   517e8: eb0a013f     	cmp	x9, x10
   517ec: 54000549     	b.ls	0x51894 <Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0xb4>
   517f0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   517f4: 9100013f     	mov	sp, x9
   517f8: b9001fe1     	str	w1, [sp, #0x1c]
   517fc: 7100043f     	cmp	w1, #0x1
   51800: 5400018b     	b.lt	0x51830 <Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0x50>
   51804: aa0203e0     	mov	x0, x2
   51808: 940001de     	bl	0x51f80 <Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0>
   5180c: b9401fe1     	ldr	w1, [sp, #0x1c]
   51810: 7104043f     	cmp	w1, #0x101
   51814: 540001ca     	b.ge	0x5184c <Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0x6c>
   51818: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   5181c: 9100c3ff     	add	sp, sp, #0x30
   51820: b9401389     	ldr	w9, [x28, #0x10]
   51824: 7100013f     	cmp	w9, #0x0
   51828: 5400038d     	b.le	0x51898 <Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0xb8>
   5182c: d65f03c0     	ret
   51830: aa0203e0     	mov	x0, x2
   51834: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   51838: 9100c3ff     	add	sp, sp, #0x30
   5183c: b9401389     	ldr	w9, [x28, #0x10]
   51840: 7100013f     	cmp	w9, #0x0
   51844: 540002ad     	b.le	0x51898 <Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0xb8>
   51848: d65f03c0     	ret
   5184c: 321803e9     	orr	w9, wzr, #0x100
   51850: b9001be9     	str	w9, [sp, #0x18]
   51854: d503201f     	nop
   51858: d503201f     	nop
   5185c: d503201f     	nop
   51860: 940001c8     	bl	0x51f80 <Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0>
   51864: b9401be1     	ldr	w1, [sp, #0x18]
   51868: 11040022     	add	w2, w1, #0x100
   5186c: b9401fe3     	ldr	w3, [sp, #0x1c]
   51870: 6b03005f     	cmp	w2, w3
   51874: 54fffd2a     	b.ge	0x51818 <Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0x38>
   51878: b9401389     	ldr	w9, [x28, #0x10]
   5187c: 34000069     	cbz	w9, 0x51888 <Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0xa8>
   51880: b9001be2     	str	w2, [sp, #0x18]
   51884: 17fffff7     	b	0x51860 <Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0x80>
   51888: 94069dd2     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5188c: b9001be2     	str	w2, [sp, #0x18]
   51890: 17fffff4     	b	0x51860 <Floor$$Lcfad789b_run_3DONXrQrVp8R2X5T2KCkt9+0x80>
   51894: 1402bb87     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51898: 14069dce     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5189c: cccccccc     	<unknown>

# svm: Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK
00000000000518a0 <Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK>:
   518a0: d100c3e9     	sub	x9, sp, #0x30
   518a4: f940078a     	ldr	x10, [x28, #0x8]
   518a8: eb0a013f     	cmp	x9, x10
   518ac: 54000549     	b.ls	0x51954 <Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0xb4>
   518b0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   518b4: 9100013f     	mov	sp, x9
   518b8: b9001fe1     	str	w1, [sp, #0x1c]
   518bc: 7100043f     	cmp	w1, #0x1
   518c0: 5400018b     	b.lt	0x518f0 <Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0x50>
   518c4: aa0203e0     	mov	x0, x2
   518c8: 97fffa72     	bl	0x50290 <Batch_batch4_38OjNnaTu60qxbWBQgs1KG>
   518cc: b9401fe1     	ldr	w1, [sp, #0x1c]
   518d0: 7100143f     	cmp	w1, #0x5
   518d4: 540001ca     	b.ge	0x5190c <Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0x6c>
   518d8: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   518dc: 9100c3ff     	add	sp, sp, #0x30
   518e0: b9401389     	ldr	w9, [x28, #0x10]
   518e4: 7100013f     	cmp	w9, #0x0
   518e8: 5400038d     	b.le	0x51958 <Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0xb8>
   518ec: d65f03c0     	ret
   518f0: aa0203e0     	mov	x0, x2
   518f4: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   518f8: 9100c3ff     	add	sp, sp, #0x30
   518fc: b9401389     	ldr	w9, [x28, #0x10]
   51900: 7100013f     	cmp	w9, #0x0
   51904: 540002ad     	b.le	0x51958 <Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0xb8>
   51908: d65f03c0     	ret
   5190c: 321e03e9     	orr	w9, wzr, #0x4
   51910: b9001be9     	str	w9, [sp, #0x18]
   51914: d503201f     	nop
   51918: d503201f     	nop
   5191c: d503201f     	nop
   51920: 97fffa5c     	bl	0x50290 <Batch_batch4_38OjNnaTu60qxbWBQgs1KG>
   51924: b9401be1     	ldr	w1, [sp, #0x18]
   51928: 11001022     	add	w2, w1, #0x4
   5192c: b9401fe3     	ldr	w3, [sp, #0x1c]
   51930: 6b03005f     	cmp	w2, w3
   51934: 54fffd2a     	b.ge	0x518d8 <Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0x38>
   51938: b9401389     	ldr	w9, [x28, #0x10]
   5193c: 34000069     	cbz	w9, 0x51948 <Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0xa8>
   51940: b9001be2     	str	w2, [sp, #0x18]
   51944: 17fffff7     	b	0x51920 <Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0x80>
   51948: 94069da2     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5194c: b9001be2     	str	w2, [sp, #0x18]
   51950: 17fffff4     	b	0x51920 <Floor$$L68668255_run_9kZK4HqEYPKMAgU2FCFFgK+0x80>
   51954: 1402bb57     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51958: 14069d9e     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   5195c: cccccccc     	<unknown>

# svm: Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB
0000000000051960 <Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB>:
   51960: d100c3e9     	sub	x9, sp, #0x30
   51964: f940078a     	ldr	x10, [x28, #0x8]
   51968: eb0a013f     	cmp	x9, x10
   5196c: 54000549     	b.ls	0x51a14 <Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0xb4>
   51970: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51974: 9100013f     	mov	sp, x9
   51978: b9001fe1     	str	w1, [sp, #0x1c]
   5197c: 7100043f     	cmp	w1, #0x1
   51980: 5400018b     	b.lt	0x519b0 <Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0x50>
   51984: aa0203e0     	mov	x0, x2
   51988: 97fffa4a     	bl	0x502b0 <Batch_batch64_xRVK4bfrby1czdEDQHBCYB>
   5198c: b9401fe1     	ldr	w1, [sp, #0x1c]
   51990: 7101043f     	cmp	w1, #0x41
   51994: 540001ca     	b.ge	0x519cc <Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0x6c>
   51998: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   5199c: 9100c3ff     	add	sp, sp, #0x30
   519a0: b9401389     	ldr	w9, [x28, #0x10]
   519a4: 7100013f     	cmp	w9, #0x0
   519a8: 5400038d     	b.le	0x51a18 <Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0xb8>
   519ac: d65f03c0     	ret
   519b0: aa0203e0     	mov	x0, x2
   519b4: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   519b8: 9100c3ff     	add	sp, sp, #0x30
   519bc: b9401389     	ldr	w9, [x28, #0x10]
   519c0: 7100013f     	cmp	w9, #0x0
   519c4: 540002ad     	b.le	0x51a18 <Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0xb8>
   519c8: d65f03c0     	ret
   519cc: 321a03e9     	orr	w9, wzr, #0x40
   519d0: b9001be9     	str	w9, [sp, #0x18]
   519d4: d503201f     	nop
   519d8: d503201f     	nop
   519dc: d503201f     	nop
   519e0: 97fffa34     	bl	0x502b0 <Batch_batch64_xRVK4bfrby1czdEDQHBCYB>
   519e4: b9401be1     	ldr	w1, [sp, #0x18]
   519e8: 11010022     	add	w2, w1, #0x40
   519ec: b9401fe3     	ldr	w3, [sp, #0x1c]
   519f0: 6b03005f     	cmp	w2, w3
   519f4: 54fffd2a     	b.ge	0x51998 <Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0x38>
   519f8: b9401389     	ldr	w9, [x28, #0x10]
   519fc: 34000069     	cbz	w9, 0x51a08 <Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0xa8>
   51a00: b9001be2     	str	w2, [sp, #0x18]
   51a04: 17fffff7     	b	0x519e0 <Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0x80>
   51a08: 94069d72     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51a0c: b9001be2     	str	w2, [sp, #0x18]
   51a10: 17fffff4     	b	0x519e0 <Floor$$L94443504_run_6yvMnpYPA0DjfERyVQDVNB+0x80>
   51a14: 1402bb27     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51a18: 14069d6e     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51a1c: cccccccc     	<unknown>

# svm: Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3
0000000000051a20 <Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3>:
   51a20: d100c3e9     	sub	x9, sp, #0x30
   51a24: f940078a     	ldr	x10, [x28, #0x8]
   51a28: eb0a013f     	cmp	x9, x10
   51a2c: 54000549     	b.ls	0x51ad4 <Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0xb4>
   51a30: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51a34: 9100013f     	mov	sp, x9
   51a38: b9001fe1     	str	w1, [sp, #0x1c]
   51a3c: 7100043f     	cmp	w1, #0x1
   51a40: 5400018b     	b.lt	0x51a70 <Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0x50>
   51a44: aa0203e0     	mov	x0, x2
   51a48: 9400019e     	bl	0x520c0 <Floor_javaBatch4_f1KINYClC5BmmtfwLbXF1F>
   51a4c: b9401fe1     	ldr	w1, [sp, #0x1c]
   51a50: 7100143f     	cmp	w1, #0x5
   51a54: 540001ca     	b.ge	0x51a8c <Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0x6c>
   51a58: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   51a5c: 9100c3ff     	add	sp, sp, #0x30
   51a60: b9401389     	ldr	w9, [x28, #0x10]
   51a64: 7100013f     	cmp	w9, #0x0
   51a68: 5400038d     	b.le	0x51ad8 <Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0xb8>
   51a6c: d65f03c0     	ret
   51a70: aa0203e0     	mov	x0, x2
   51a74: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   51a78: 9100c3ff     	add	sp, sp, #0x30
   51a7c: b9401389     	ldr	w9, [x28, #0x10]
   51a80: 7100013f     	cmp	w9, #0x0
   51a84: 540002ad     	b.le	0x51ad8 <Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0xb8>
   51a88: d65f03c0     	ret
   51a8c: 321e03e9     	orr	w9, wzr, #0x4
   51a90: b9001be9     	str	w9, [sp, #0x18]
   51a94: d503201f     	nop
   51a98: d503201f     	nop
   51a9c: d503201f     	nop
   51aa0: 94000188     	bl	0x520c0 <Floor_javaBatch4_f1KINYClC5BmmtfwLbXF1F>
   51aa4: b9401be1     	ldr	w1, [sp, #0x18]
   51aa8: 11001022     	add	w2, w1, #0x4
   51aac: b9401fe3     	ldr	w3, [sp, #0x1c]
   51ab0: 6b03005f     	cmp	w2, w3
   51ab4: 54fffd2a     	b.ge	0x51a58 <Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0x38>
   51ab8: b9401389     	ldr	w9, [x28, #0x10]
   51abc: 34000069     	cbz	w9, 0x51ac8 <Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0xa8>
   51ac0: b9001be2     	str	w2, [sp, #0x18]
   51ac4: 17fffff7     	b	0x51aa0 <Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0x80>
   51ac8: 94069d42     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51acc: b9001be2     	str	w2, [sp, #0x18]
   51ad0: 17fffff4     	b	0x51aa0 <Floor$$Lb172ff21_run_7YPUX1Q7TU4vgVsYZ4vAR3+0x80>
   51ad4: 1402baf7     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51ad8: 14069d3e     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51adc: cccccccc     	<unknown>

# svm: Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH
0000000000051ae0 <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH>:
   51ae0: d101c3e9     	sub	x9, sp, #0x70
   51ae4: f940078a     	ldr	x10, [x28, #0x8]
   51ae8: eb0a013f     	cmp	x9, x10
   51aec: 54000869     	b.ls	0x51bf8 <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x118>
   51af0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51af4: 9100013f     	mov	sp, x9
   51af8: b90037e1     	str	w1, [sp, #0x34]
   51afc: f00044e3     	adrp	x3, 0x8f0000 <.data>
   51b00: f9453863     	ldr	x3, [x3, #0xa70]
   51b04: f90017e3     	str	x3, [sp, #0x28]
   51b08: aa0203e0     	mov	x0, x2
   51b0c: 2a1f03e2     	mov	w2, wzr
   51b10: 14000027     	b	0x51bac <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0xcc>
   51b14: d503201f     	nop
   51b18: d503201f     	nop
   51b1c: d503201f     	nop
   51b20: b90033e2     	str	w2, [sp, #0x30]
   51b24: 9100e3ed     	add	x13, sp, #0x38
   51b28: a900fdbf     	stp	xzr, xzr, [x13, #0x8]
   51b2c: f9400f9e     	ldr	x30, [x28, #0x18]
   51b30: f9000dbe     	str	x30, [x13, #0x18]
   51b34: f9000f8d     	str	x13, [x28, #0x18]
   51b38: aa0303fe     	mov	x30, x3
   51b3c: 10000029     	adr	x9, 0x51b40 <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x60>
   51b40: f90005a9     	str	x9, [x13, #0x8]
   51b44: 910003e9     	mov	x9, sp
   51b48: f90009a9     	str	x9, [x13, #0x10]
   51b4c: 320007e9     	orr	w9, wzr, #0x3
   51b50: 9100538a     	add	x10, x28, #0x14
   51b54: 889ffd49     	stlr	w9, [x10]
   51b58: d63f03c0     	blr	x30
   51b5c: 320007e1     	orr	w1, wzr, #0x3
   51b60: 320003e2     	orr	w2, wzr, #0x1
   51b64: 91005383     	add	x3, x28, #0x14
   51b68: 885f7c64     	ldxr	w4, [x3]
   51b6c: 6b01009f     	cmp	w4, w1
   51b70: 1a84004a     	csel	w10, w2, w4, eq
   51b74: 8809fc6a     	stlxr	w9, w10, [x3]
   51b78: 35ffff89     	cbnz	w9, 0x51b68 <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x88>
   51b7c: d5033bbf     	dmb	ish
   51b80: 1a9f17e1     	cset	w1, eq
   51b84: 340001a1     	cbz	w1, 0x51bb8 <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0xd8>
   51b88: f9400f81     	ldr	x1, [x28, #0x18]
   51b8c: f9400c21     	ldr	x1, [x1, #0x18]
   51b90: f9000f81     	str	x1, [x28, #0x18]
   51b94: b9402f81     	ldr	w1, [x28, #0x2c]
   51b98: 350001c1     	cbnz	w1, 0x51bd0 <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0xf0>
   51b9c: b94033e2     	ldr	w2, [sp, #0x30]
   51ba0: 11000442     	add	w2, w2, #0x1
   51ba4: f94017e3     	ldr	x3, [sp, #0x28]
   51ba8: b94037e1     	ldr	w1, [sp, #0x34]
   51bac: 6b01005f     	cmp	w2, w1
   51bb0: 54fffb8b     	b.lt	0x51b20 <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x40>
   51bb4: 1400000b     	b	0x51be0 <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x100>
   51bb8: f90013e0     	str	x0, [sp, #0x20]
   51bbc: 320003e0     	orr	w0, wzr, #0x1
   51bc0: 320003e1     	orr	w1, wzr, #0x1
   51bc4: 94069f3f     	bl	0x1f98c0 <SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
   51bc8: f94013e0     	ldr	x0, [sp, #0x20]
   51bcc: 17fffff4     	b	0x51b9c <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0xbc>
   51bd0: f90013e0     	str	x0, [sp, #0x20]
   51bd4: 94069ecf     	bl	0x1f9710 <SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
   51bd8: f94013e0     	ldr	x0, [sp, #0x20]
   51bdc: 17fffff0     	b	0x51b9c <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0xbc>
   51be0: a9467bfd     	ldp	x29, x30, [sp, #0x60]
   51be4: 9101c3ff     	add	sp, sp, #0x70
   51be8: b9401389     	ldr	w9, [x28, #0x10]
   51bec: 7100013f     	cmp	w9, #0x0
   51bf0: 5400006d     	b.le	0x51bfc <Floor$$L53758d58_run_YwKBjRYGCg1OknlOimQnBH+0x11c>
   51bf4: d65f03c0     	ret
   51bf8: 1402baae     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51bfc: 14069cf5     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>

# svm: Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB
0000000000051c00 <Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB>:
   51c00: d100c3e9     	sub	x9, sp, #0x30
   51c04: f940078a     	ldr	x10, [x28, #0x8]
   51c08: eb0a013f     	cmp	x9, x10
   51c0c: 54000549     	b.ls	0x51cb4 <Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0xb4>
   51c10: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51c14: 9100013f     	mov	sp, x9
   51c18: b9001fe1     	str	w1, [sp, #0x1c]
   51c1c: 7100043f     	cmp	w1, #0x1
   51c20: 5400018b     	b.lt	0x51c50 <Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0x50>
   51c24: aa0203e0     	mov	x0, x2
   51c28: 940000ae     	bl	0x51ee0 <Floor_javaBatch16_y3MnyKV1Au2DVE8WunT2mC>
   51c2c: b9401fe1     	ldr	w1, [sp, #0x1c]
   51c30: 7100443f     	cmp	w1, #0x11
   51c34: 540001ca     	b.ge	0x51c6c <Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0x6c>
   51c38: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   51c3c: 9100c3ff     	add	sp, sp, #0x30
   51c40: b9401389     	ldr	w9, [x28, #0x10]
   51c44: 7100013f     	cmp	w9, #0x0
   51c48: 5400038d     	b.le	0x51cb8 <Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0xb8>
   51c4c: d65f03c0     	ret
   51c50: aa0203e0     	mov	x0, x2
   51c54: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   51c58: 9100c3ff     	add	sp, sp, #0x30
   51c5c: b9401389     	ldr	w9, [x28, #0x10]
   51c60: 7100013f     	cmp	w9, #0x0
   51c64: 540002ad     	b.le	0x51cb8 <Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0xb8>
   51c68: d65f03c0     	ret
   51c6c: 321c03e9     	orr	w9, wzr, #0x10
   51c70: b9001be9     	str	w9, [sp, #0x18]
   51c74: d503201f     	nop
   51c78: d503201f     	nop
   51c7c: d503201f     	nop
   51c80: 94000098     	bl	0x51ee0 <Floor_javaBatch16_y3MnyKV1Au2DVE8WunT2mC>
   51c84: b9401be1     	ldr	w1, [sp, #0x18]
   51c88: 11004022     	add	w2, w1, #0x10
   51c8c: b9401fe3     	ldr	w3, [sp, #0x1c]
   51c90: 6b03005f     	cmp	w2, w3
   51c94: 54fffd2a     	b.ge	0x51c38 <Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0x38>
   51c98: b9401389     	ldr	w9, [x28, #0x10]
   51c9c: 34000069     	cbz	w9, 0x51ca8 <Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0xa8>
   51ca0: b9001be2     	str	w2, [sp, #0x18]
   51ca4: 17fffff7     	b	0x51c80 <Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0x80>
   51ca8: 94069cca     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51cac: b9001be2     	str	w2, [sp, #0x18]
   51cb0: 17fffff4     	b	0x51c80 <Floor$$L42bf9579_run_nEQtvOCj5JKmKJEFOrpGfB+0x80>
   51cb4: 1402ba7f     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51cb8: 14069cc6     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51cbc: cccccccc     	<unknown>

# svm: Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L
0000000000051cc0 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L>:
   51cc0: d10143e9     	sub	x9, sp, #0x50
   51cc4: f940078a     	ldr	x10, [x28, #0x8]
   51cc8: eb0a013f     	cmp	x9, x10
   51ccc: 54000f09     	b.ls	0x51eac <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x1ec>
   51cd0: a93f7bfd     	stp	x29, x30, [sp, #-0x10]
   51cd4: 9100013f     	mov	sp, x9
   51cd8: b90033e1     	str	w1, [sp, #0x30]
   51cdc: 529ea3c0     	mov	w0, #0xf51e             // =62750
   51ce0: 72a000e0     	movk	w0, #0x7, lsl #16
   51ce4: 8b000f60     	add	x0, x27, x0, lsl #3
   51ce8: f9402403     	ldr	x3, [x0, #0x48]
   51cec: f9000fe3     	str	x3, [sp, #0x18]
   51cf0: 7100043f     	cmp	w1, #0x1
   51cf4: 320003e0     	orr	w0, wzr, #0x1
   51cf8: 1a80d020     	csel	w0, w1, w0, le
   51cfc: 7100041f     	cmp	w0, #0x1
   51d00: 54000d0b     	b.lt	0x51ea0 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x1e0>
   51d04: aa0203e0     	mov	x0, x2
   51d08: aa0303fe     	mov	x30, x3
   51d0c: d63f03c0     	blr	x30
   51d10: 320003fe     	orr	w30, wzr, #0x1
   51d14: b94033e1     	ldr	w1, [sp, #0x30]
   51d18: 52800082     	mov	w2, #0x4                // =4
   51d1c: 72b00002     	movk	w2, #0x8000, lsl #16
   51d20: 320103e3     	orr	w3, wzr, #0x80000000
   51d24: 51001024     	sub	w4, w1, #0x4
   51d28: 6b02003f     	cmp	w1, w2
   51d2c: 1a84b062     	csel	w2, w3, w4, lt
   51d30: b9002be2     	str	w2, [sp, #0x28]
   51d34: 11000c43     	add	w3, w2, #0x3
   51d38: b90027e3     	str	w3, [sp, #0x24]
   51d3c: f9400fe4     	ldr	x4, [sp, #0x18]
   51d40: 2a1e03e1     	mov	w1, w30
   51d44: d503201f     	nop
   51d48: d503201f     	nop
   51d4c: d503201f     	nop
   51d50: 6b02003f     	cmp	w1, w2
   51d54: 5400078a     	b.ge	0x51e44 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x184>
   51d58: 4b01007e     	sub	w30, w3, w1
   51d5c: 53027fc6     	lsr	w6, w30, #2
   51d60: 121e77de     	and	w30, w30, #0xfffffffc
   51d64: 714004df     	cmp	w6, #0x1, lsl #12       // =0x1000
   51d68: 321203e6     	orr	w6, wzr, #0x4000
   51d6c: 1a8633c6     	csel	w6, w30, w6, lo
   51d70: 710004df     	cmp	w6, #0x1
   51d74: 5400060b     	b.lt	0x51e34 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x174>
   51d78: b90037e1     	str	w1, [sp, #0x34]
   51d7c: b9003fe6     	str	w6, [sp, #0x3c]
   51d80: 2a1f03e2     	mov	w2, wzr
   51d84: 14000008     	b	0x51da4 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0xe4>
   51d88: d503201f     	nop
   51d8c: d503201f     	nop
   51d90: d503201f     	nop
   51d94: d503201f     	nop
   51d98: d503201f     	nop
   51d9c: d503201f     	nop
   51da0: f9400fe4     	ldr	x4, [sp, #0x18]
   51da4: 0b010048     	add	w8, w2, w1
   51da8: b90023e8     	str	w8, [sp, #0x20]
   51dac: aa0403fe     	mov	x30, x4
   51db0: b9002fe2     	str	w2, [sp, #0x2c]
   51db4: d63f03c0     	blr	x30
   51db8: b94023e8     	ldr	w8, [sp, #0x20]
   51dbc: 11000501     	add	w1, w8, #0x1
   51dc0: f9400ffe     	ldr	x30, [sp, #0x18]
   51dc4: b9003be1     	str	w1, [sp, #0x38]
   51dc8: d63f03c0     	blr	x30
   51dcc: b94023e8     	ldr	w8, [sp, #0x20]
   51dd0: 11000901     	add	w1, w8, #0x2
   51dd4: f9400ffe     	ldr	x30, [sp, #0x18]
   51dd8: b9003be1     	str	w1, [sp, #0x38]
   51ddc: d63f03c0     	blr	x30
   51de0: b94023e8     	ldr	w8, [sp, #0x20]
   51de4: 11000d01     	add	w1, w8, #0x3
   51de8: f9400ffe     	ldr	x30, [sp, #0x18]
   51dec: b90023e1     	str	w1, [sp, #0x20]
   51df0: d63f03c0     	blr	x30
   51df4: b9402fe2     	ldr	w2, [sp, #0x2c]
   51df8: 11001042     	add	w2, w2, #0x4
   51dfc: b9403fe6     	ldr	w6, [sp, #0x3c]
   51e00: 6b06005f     	cmp	w2, w6
   51e04: b94037e1     	ldr	w1, [sp, #0x34]
   51e08: 54fffccb     	b.lt	0x51da0 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0xe0>
   51e0c: 0b010041     	add	w1, w2, w1
   51e10: b9401389     	ldr	w9, [x28, #0x10]
   51e14: 34000089     	cbz	w9, 0x51e24 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x164>
   51e18: 29448be3     	ldp	w3, w2, [sp, #0x24]
   51e1c: f9400fe4     	ldr	x4, [sp, #0x18]
   51e20: 17ffffcc     	b	0x51d50 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x90>
   51e24: 94069c6b     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51e28: 29448be3     	ldp	w3, w2, [sp, #0x24]
   51e2c: f9400fe4     	ldr	x4, [sp, #0x18]
   51e30: 17ffffc8     	b	0x51d50 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x90>
   51e34: b9401389     	ldr	w9, [x28, #0x10]
   51e38: 35fff8c9     	cbnz	w9, 0x51d50 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x90>
   51e3c: 94069c65     	bl	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51e40: 17ffffc4     	b	0x51d50 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x90>
   51e44: 2a0103e2     	mov	w2, w1
   51e48: aa0403e3     	mov	x3, x4
   51e4c: 1400000c     	b	0x51e7c <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x1bc>
   51e50: d503201f     	nop
   51e54: d503201f     	nop
   51e58: d503201f     	nop
   51e5c: d503201f     	nop
   51e60: b9002fe2     	str	w2, [sp, #0x2c]
   51e64: aa0303fe     	mov	x30, x3
   51e68: d63f03c0     	blr	x30
   51e6c: b9402fe2     	ldr	w2, [sp, #0x2c]
   51e70: 1100045d     	add	w29, w2, #0x1
   51e74: 2a1d03e2     	mov	w2, w29
   51e78: f9400fe3     	ldr	x3, [sp, #0x18]
   51e7c: b94033e1     	ldr	w1, [sp, #0x30]
   51e80: 6b01005f     	cmp	w2, w1
   51e84: 54fffeeb     	b.lt	0x51e60 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x1a0>
   51e88: a9447bfd     	ldp	x29, x30, [sp, #0x40]
   51e8c: 910143ff     	add	sp, sp, #0x50
   51e90: b9401389     	ldr	w9, [x28, #0x10]
   51e94: 7100013f     	cmp	w9, #0x0
   51e98: 540000cd     	b.le	0x51eb0 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x1f0>
   51e9c: d65f03c0     	ret
   51ea0: 2a1f03fe     	mov	w30, wzr
   51ea4: aa0203e0     	mov	x0, x2
   51ea8: 17ffff9c     	b	0x51d18 <Floor$$L539ba646_run_3qHj8aDoIyKOlv2gjhQv9L+0x58>
   51eac: 1402ba01     	b	0x1006b0 <StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
   51eb0: 14069c48     	b	0x1f8fd0 <SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
   51eb4: cccccccc     	<unknown>
   51eb8: cccccccc     	<unknown>
   51ebc: cccccccc     	<unknown>

# svm: Floor_javaBatch16_y3MnyKV1Au2DVE8WunT2mC
0000000000051ee0 <Floor_javaBatch16_y3MnyKV1Au2DVE8WunT2mC>:
   51ee0: d100c3ff     	sub	sp, sp, #0x30
   51ee4: a9027bfd     	stp	x29, x30, [sp, #0x20]
   51ee8: f00044e1     	adrp	x1, 0x8f0000 <.data>
   51eec: f9405021     	ldr	x1, [x1, #0xa0]
   51ef0: aa0103fe     	mov	x30, x1
   51ef4: f9000fe1     	str	x1, [sp, #0x18]
   51ef8: d63f03c0     	blr	x30
   51efc: f9400ffe     	ldr	x30, [sp, #0x18]
   51f00: d63f03c0     	blr	x30
   51f04: f9400ffe     	ldr	x30, [sp, #0x18]
   51f08: d63f03c0     	blr	x30
   51f0c: f9400ffe     	ldr	x30, [sp, #0x18]
   51f10: d63f03c0     	blr	x30
   51f14: f9400ffe     	ldr	x30, [sp, #0x18]
   51f18: d63f03c0     	blr	x30
   51f1c: f9400ffe     	ldr	x30, [sp, #0x18]
   51f20: d63f03c0     	blr	x30
   51f24: f9400ffe     	ldr	x30, [sp, #0x18]
   51f28: d63f03c0     	blr	x30
   51f2c: f9400ffe     	ldr	x30, [sp, #0x18]
   51f30: d63f03c0     	blr	x30
   51f34: f9400ffe     	ldr	x30, [sp, #0x18]
   51f38: d63f03c0     	blr	x30
   51f3c: f9400ffe     	ldr	x30, [sp, #0x18]
   51f40: d63f03c0     	blr	x30
   51f44: f9400ffe     	ldr	x30, [sp, #0x18]
   51f48: d63f03c0     	blr	x30
   51f4c: f9400ffe     	ldr	x30, [sp, #0x18]
   51f50: d63f03c0     	blr	x30
   51f54: f9400ffe     	ldr	x30, [sp, #0x18]
   51f58: d63f03c0     	blr	x30
   51f5c: f9400ffe     	ldr	x30, [sp, #0x18]
   51f60: d63f03c0     	blr	x30
   51f64: f9400ffe     	ldr	x30, [sp, #0x18]
   51f68: d63f03c0     	blr	x30
   51f6c: f9400ffe     	ldr	x30, [sp, #0x18]
   51f70: d63f03c0     	blr	x30
   51f74: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   51f78: 9100c3ff     	add	sp, sp, #0x30
   51f7c: d65f03c0     	ret

# svm: Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0
0000000000051f80 <Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0>:
   51f80: d100c3ff     	sub	sp, sp, #0x30
   51f84: a9027bfd     	stp	x29, x30, [sp, #0x20]
   51f88: f00044e1     	adrp	x1, 0x8f0000 <.data>
   51f8c: f9405021     	ldr	x1, [x1, #0xa0]
   51f90: f9000be1     	str	x1, [sp, #0x10]
   51f94: 2a1f03e2     	mov	w2, wzr
   51f98: 14000006     	b	0x51fb0 <Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0x30>
   51f9c: d503201f     	nop
   51fa0: aa0103fe     	mov	x30, x1
   51fa4: d63f03c0     	blr	x30
   51fa8: 320003e2     	orr	w2, wzr, #0x1
   51fac: f9400be1     	ldr	x1, [sp, #0x10]
   51fb0: 34ffff82     	cbz	w2, 0x51fa0 <Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0x20>
   51fb4: 2a0203e1     	mov	w1, w2
   51fb8: 14000029     	b	0x5205c <Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0xdc>
   51fbc: d503201f     	nop
   51fc0: aa0203fe     	mov	x30, x2
   51fc4: d63f03c0     	blr	x30
   51fc8: b9401be1     	ldr	w1, [sp, #0x18]
   51fcc: 11000422     	add	w2, w1, #0x1
   51fd0: f9400bfe     	ldr	x30, [sp, #0x10]
   51fd4: b9001fe2     	str	w2, [sp, #0x1c]
   51fd8: d63f03c0     	blr	x30
   51fdc: b9401be1     	ldr	w1, [sp, #0x18]
   51fe0: 11000822     	add	w2, w1, #0x2
   51fe4: f9400bfe     	ldr	x30, [sp, #0x10]
   51fe8: b9001fe2     	str	w2, [sp, #0x1c]
   51fec: d63f03c0     	blr	x30
   51ff0: b9401be1     	ldr	w1, [sp, #0x18]
   51ff4: 11000c22     	add	w2, w1, #0x3
   51ff8: f9400bfe     	ldr	x30, [sp, #0x10]
   51ffc: b9001fe2     	str	w2, [sp, #0x1c]
   52000: d63f03c0     	blr	x30
   52004: b9401be1     	ldr	w1, [sp, #0x18]
   52008: 11001022     	add	w2, w1, #0x4
   5200c: f9400bfe     	ldr	x30, [sp, #0x10]
   52010: b9001fe2     	str	w2, [sp, #0x1c]
   52014: d63f03c0     	blr	x30
   52018: b9401be1     	ldr	w1, [sp, #0x18]
   5201c: 11001422     	add	w2, w1, #0x5
   52020: f9400bfe     	ldr	x30, [sp, #0x10]
   52024: b9001fe2     	str	w2, [sp, #0x1c]
   52028: d63f03c0     	blr	x30
   5202c: b9401be1     	ldr	w1, [sp, #0x18]
   52030: 11001822     	add	w2, w1, #0x6
   52034: f9400bfe     	ldr	x30, [sp, #0x10]
   52038: b9001fe2     	str	w2, [sp, #0x1c]
   5203c: d63f03c0     	blr	x30
   52040: b9401be1     	ldr	w1, [sp, #0x18]
   52044: 11001c22     	add	w2, w1, #0x7
   52048: f9400bfe     	ldr	x30, [sp, #0x10]
   5204c: b9001fe2     	str	w2, [sp, #0x1c]
   52050: d63f03c0     	blr	x30
   52054: b9401be1     	ldr	w1, [sp, #0x18]
   52058: 11002021     	add	w1, w1, #0x8
   5205c: f9400be2     	ldr	x2, [sp, #0x10]
   52060: b9001be1     	str	w1, [sp, #0x18]
   52064: 7103e03f     	cmp	w1, #0xf8
   52068: 54fffac3     	b.lo	0x51fc0 <Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0x40>
   5206c: 1400000d     	b	0x520a0 <Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0x120>
   52070: d503201f     	nop
   52074: d503201f     	nop
   52078: d503201f     	nop
   5207c: d503201f     	nop
   52080: b9001be1     	str	w1, [sp, #0x18]
   52084: aa0203fe     	mov	x30, x2
   52088: d63f03c0     	blr	x30
   5208c: b9401be1     	ldr	w1, [sp, #0x18]
   52090: 1100043d     	add	w29, w1, #0x1
   52094: 2a1d03e1     	mov	w1, w29
   52098: f9400be2     	ldr	x2, [sp, #0x10]
   5209c: d503201f     	nop
   520a0: 7104003f     	cmp	w1, #0x100
   520a4: 54fffee3     	b.lo	0x52080 <Floor_javaBatch256_mMJuhMu6RmCgK0YWiRzdP0+0x100>
   520a8: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   520ac: 9100c3ff     	add	sp, sp, #0x30
   520b0: d65f03c0     	ret
   520b4: cccccccc     	<unknown>
   520b8: cccccccc     	<unknown>
   520bc: cccccccc     	<unknown>

# svm: Floor_javaBatch4_f1KINYClC5BmmtfwLbXF1F
00000000000520c0 <Floor_javaBatch4_f1KINYClC5BmmtfwLbXF1F>:
   520c0: d100c3ff     	sub	sp, sp, #0x30
   520c4: a9027bfd     	stp	x29, x30, [sp, #0x20]
   520c8: d00044e1     	adrp	x1, 0x8f0000 <.data>
   520cc: f9405021     	ldr	x1, [x1, #0xa0]
   520d0: aa0103fe     	mov	x30, x1
   520d4: f9000fe1     	str	x1, [sp, #0x18]
   520d8: d63f03c0     	blr	x30
   520dc: f9400ffe     	ldr	x30, [sp, #0x18]
   520e0: d63f03c0     	blr	x30
   520e4: f9400ffe     	ldr	x30, [sp, #0x18]
   520e8: d63f03c0     	blr	x30
   520ec: f9400ffe     	ldr	x30, [sp, #0x18]
   520f0: d63f03c0     	blr	x30
   520f4: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   520f8: 9100c3ff     	add	sp, sp, #0x30
   520fc: d65f03c0     	ret

# svm: Floor_javaBatch64_pgMJB9JHBI7okdXsdyhwU1
0000000000052100 <Floor_javaBatch64_pgMJB9JHBI7okdXsdyhwU1>:
   52100: d100c3ff     	sub	sp, sp, #0x30
   52104: a9027bfd     	stp	x29, x30, [sp, #0x20]
   52108: d00044e1     	adrp	x1, 0x8f0000 <.data>
   5210c: f9405021     	ldr	x1, [x1, #0xa0]
   52110: aa0103fe     	mov	x30, x1
   52114: f9000fe1     	str	x1, [sp, #0x18]
   52118: d63f03c0     	blr	x30
   5211c: f9400ffe     	ldr	x30, [sp, #0x18]
   52120: d63f03c0     	blr	x30
   52124: f9400ffe     	ldr	x30, [sp, #0x18]
   52128: d63f03c0     	blr	x30
   5212c: f9400ffe     	ldr	x30, [sp, #0x18]
   52130: d63f03c0     	blr	x30
   52134: f9400ffe     	ldr	x30, [sp, #0x18]
   52138: d63f03c0     	blr	x30
   5213c: f9400ffe     	ldr	x30, [sp, #0x18]
   52140: d63f03c0     	blr	x30
   52144: f9400ffe     	ldr	x30, [sp, #0x18]
   52148: d63f03c0     	blr	x30
   5214c: f9400ffe     	ldr	x30, [sp, #0x18]
   52150: d63f03c0     	blr	x30
   52154: f9400ffe     	ldr	x30, [sp, #0x18]
   52158: d63f03c0     	blr	x30
   5215c: f9400ffe     	ldr	x30, [sp, #0x18]
   52160: d63f03c0     	blr	x30
   52164: f9400ffe     	ldr	x30, [sp, #0x18]
   52168: d63f03c0     	blr	x30
   5216c: f9400ffe     	ldr	x30, [sp, #0x18]
   52170: d63f03c0     	blr	x30
   52174: f9400ffe     	ldr	x30, [sp, #0x18]
   52178: d63f03c0     	blr	x30
   5217c: f9400ffe     	ldr	x30, [sp, #0x18]
   52180: d63f03c0     	blr	x30
   52184: f9400ffe     	ldr	x30, [sp, #0x18]
   52188: d63f03c0     	blr	x30
   5218c: f9400ffe     	ldr	x30, [sp, #0x18]
   52190: d63f03c0     	blr	x30
   52194: f9400ffe     	ldr	x30, [sp, #0x18]
   52198: d63f03c0     	blr	x30
   5219c: f9400ffe     	ldr	x30, [sp, #0x18]
   521a0: d63f03c0     	blr	x30
   521a4: f9400ffe     	ldr	x30, [sp, #0x18]
   521a8: d63f03c0     	blr	x30
   521ac: f9400ffe     	ldr	x30, [sp, #0x18]
   521b0: d63f03c0     	blr	x30
   521b4: f9400ffe     	ldr	x30, [sp, #0x18]
   521b8: d63f03c0     	blr	x30
   521bc: f9400ffe     	ldr	x30, [sp, #0x18]
   521c0: d63f03c0     	blr	x30
   521c4: f9400ffe     	ldr	x30, [sp, #0x18]
   521c8: d63f03c0     	blr	x30
   521cc: f9400ffe     	ldr	x30, [sp, #0x18]
   521d0: d63f03c0     	blr	x30
   521d4: f9400ffe     	ldr	x30, [sp, #0x18]
   521d8: d63f03c0     	blr	x30
   521dc: f9400ffe     	ldr	x30, [sp, #0x18]
   521e0: d63f03c0     	blr	x30
   521e4: f9400ffe     	ldr	x30, [sp, #0x18]
   521e8: d63f03c0     	blr	x30
   521ec: f9400ffe     	ldr	x30, [sp, #0x18]
   521f0: d63f03c0     	blr	x30
   521f4: f9400ffe     	ldr	x30, [sp, #0x18]
   521f8: d63f03c0     	blr	x30
   521fc: f9400ffe     	ldr	x30, [sp, #0x18]
   52200: d63f03c0     	blr	x30
   52204: f9400ffe     	ldr	x30, [sp, #0x18]
   52208: d63f03c0     	blr	x30
   5220c: f9400ffe     	ldr	x30, [sp, #0x18]
   52210: d63f03c0     	blr	x30
   52214: f9400ffe     	ldr	x30, [sp, #0x18]
   52218: d63f03c0     	blr	x30
   5221c: f9400ffe     	ldr	x30, [sp, #0x18]
   52220: d63f03c0     	blr	x30
   52224: f9400ffe     	ldr	x30, [sp, #0x18]
   52228: d63f03c0     	blr	x30
   5222c: f9400ffe     	ldr	x30, [sp, #0x18]
   52230: d63f03c0     	blr	x30
   52234: f9400ffe     	ldr	x30, [sp, #0x18]
   52238: d63f03c0     	blr	x30
   5223c: f9400ffe     	ldr	x30, [sp, #0x18]
   52240: d63f03c0     	blr	x30
   52244: f9400ffe     	ldr	x30, [sp, #0x18]
   52248: d63f03c0     	blr	x30
   5224c: f9400ffe     	ldr	x30, [sp, #0x18]
   52250: d63f03c0     	blr	x30
   52254: f9400ffe     	ldr	x30, [sp, #0x18]
   52258: d63f03c0     	blr	x30
   5225c: f9400ffe     	ldr	x30, [sp, #0x18]
   52260: d63f03c0     	blr	x30
   52264: f9400ffe     	ldr	x30, [sp, #0x18]
   52268: d63f03c0     	blr	x30
   5226c: f9400ffe     	ldr	x30, [sp, #0x18]
   52270: d63f03c0     	blr	x30
   52274: f9400ffe     	ldr	x30, [sp, #0x18]
   52278: d63f03c0     	blr	x30
   5227c: f9400ffe     	ldr	x30, [sp, #0x18]
   52280: d63f03c0     	blr	x30
   52284: f9400ffe     	ldr	x30, [sp, #0x18]
   52288: d63f03c0     	blr	x30
   5228c: f9400ffe     	ldr	x30, [sp, #0x18]
   52290: d63f03c0     	blr	x30
   52294: f9400ffe     	ldr	x30, [sp, #0x18]
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
   52314: a9427bfd     	ldp	x29, x30, [sp, #0x20]
   52318: 9100c3ff     	add	sp, sp, #0x30
   5231c: d65f03c0     	ret

# svm: Generated_leaf_A7zZco9JWUE1TvDDMvfDp7
0000000000057210 <Generated_leaf_A7zZco9JWUE1TvDDMvfDp7>:
   57210: d10083ff     	sub	sp, sp, #0x20
   57214: a9017bfd     	stp	x29, x30, [sp, #0x10]
   57218: b00044de     	adrp	x30, 0x8f0000 <.data>
   5721c: f94053de     	ldr	x30, [x30, #0xa0]
   57220: d63f03c0     	blr	x30
   57224: a9417bfd     	ldp	x29, x30, [sp, #0x10]
   57228: 910083ff     	add	sp, sp, #0x20
   5722c: d65f03c0     	ret

# svm: Identity_leaf_gOd6suzlEz5e8qvz4k8wcB
0000000000057230 <Identity_leaf_gOd6suzlEz5e8qvz4k8wcB>:
   57230: d10083ff     	sub	sp, sp, #0x20
   57234: a9017bfd     	stp	x29, x30, [sp, #0x10]
   57238: b00044de     	adrp	x30, 0x8f0000 <.data>
   5723c: f94127de     	ldr	x30, [x30, #0x248]
   57240: d63f03c0     	blr	x30
   57244: a9417bfd     	ldp	x29, x30, [sp, #0x10]
   57248: 910083ff     	add	sp, sp, #0x20
   5724c: d65f03c0     	ret

