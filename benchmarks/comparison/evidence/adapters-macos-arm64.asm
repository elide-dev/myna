; native/generated-normal
0000000100012990 <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE>:
100012990: d101c3e9    	sub	x9, sp, #0x70
100012994: f940078a    	ldr	x10, [x28, #0x8]
100012998: eb0a013f    	cmp	x9, x10
10001299c: 54000869    	b.ls	0x100012aa8 <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0x118>
1000129a0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000129a4: 9100013f    	mov	sp, x9
1000129a8: b90037e1    	str	w1, [sp, #0x34]
1000129ac: d0002063    	adrp	x3, 0x100420000 <___data>
1000129b0: f9407063    	ldr	x3, [x3, #0xe0]
1000129b4: f90017e3    	str	x3, [sp, #0x28]
1000129b8: aa0203e0    	mov	x0, x2
1000129bc: 2a1f03e2    	mov	w2, wzr
1000129c0: 14000027    	b	0x100012a5c <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0xcc>
1000129c4: d503201f    	nop
1000129c8: d503201f    	nop
1000129cc: d503201f    	nop
1000129d0: b90033e2    	str	w2, [sp, #0x30]
1000129d4: 9100e3ed    	add	x13, sp, #0x38
1000129d8: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
1000129dc: f9400f9e    	ldr	x30, [x28, #0x18]
1000129e0: f9000dbe    	str	x30, [x13, #0x18]
1000129e4: f9000f8d    	str	x13, [x28, #0x18]
1000129e8: aa0303fe    	mov	x30, x3
1000129ec: 10000029    	adr	x9, 0x1000129f0 <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0x60>
1000129f0: f90005a9    	str	x9, [x13, #0x8]
1000129f4: 910003e9    	mov	x9, sp
1000129f8: f90009a9    	str	x9, [x13, #0x10]
1000129fc: 320007e9    	orr	w9, wzr, #0x3
100012a00: 9100538a    	add	x10, x28, #0x14
100012a04: 889ffd49    	stlr	w9, [x10]
100012a08: d63f03c0    	blr	x30
100012a0c: 320007e1    	orr	w1, wzr, #0x3
100012a10: 320003e2    	orr	w2, wzr, #0x1
100012a14: 91005383    	add	x3, x28, #0x14
100012a18: 885f7c64    	ldxr	w4, [x3]
100012a1c: 6b01009f    	cmp	w4, w1
100012a20: 1a84004a    	csel	w10, w2, w4, eq
100012a24: 8809fc6a    	stlxr	w9, w10, [x3]
100012a28: 35ffff89    	cbnz	w9, 0x100012a18 <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0x88>
100012a2c: d5033bbf    	dmb	ish
100012a30: 1a9f17e1    	cset	w1, eq
100012a34: 340001a1    	cbz	w1, 0x100012a68 <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0xd8>
100012a38: f9400f81    	ldr	x1, [x28, #0x18]
100012a3c: f9400c21    	ldr	x1, [x1, #0x18]
100012a40: f9000f81    	str	x1, [x28, #0x18]
100012a44: b9402f81    	ldr	w1, [x28, #0x2c]
100012a48: 350001c1    	cbnz	w1, 0x100012a80 <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0xf0>
100012a4c: b94033e2    	ldr	w2, [sp, #0x30]
100012a50: 11000442    	add	w2, w2, #0x1
100012a54: f94017e3    	ldr	x3, [sp, #0x28]
100012a58: b94037e1    	ldr	w1, [sp, #0x34]
100012a5c: 6b01005f    	cmp	w2, w1
100012a60: 54fffb8b    	b.lt	0x1000129d0 <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0x40>
100012a64: 1400000b    	b	0x100012a90 <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0x100>
100012a68: f90013e0    	str	x0, [sp, #0x20]
100012a6c: 320003e0    	orr	w0, wzr, #0x1
100012a70: 320003e1    	orr	w1, wzr, #0x1
100012a74: 9405e07f    	bl	0x10018ac70 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
100012a78: f94013e0    	ldr	x0, [sp, #0x20]
100012a7c: 17fffff4    	b	0x100012a4c <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0xbc>
100012a80: f90013e0    	str	x0, [sp, #0x20]
100012a84: 9405e00f    	bl	0x10018aac0 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
100012a88: f94013e0    	ldr	x0, [sp, #0x20]
100012a8c: 17fffff0    	b	0x100012a4c <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0xbc>
100012a90: a9467bfd    	ldp	x29, x30, [sp, #0x60]
100012a94: 9101c3ff    	add	sp, sp, #0x70
100012a98: b9401389    	ldr	w9, [x28, #0x10]
100012a9c: 7100013f    	cmp	w9, #0x0
100012aa0: 5400006d    	b.le	0x100012aac <_Native$GeneratedNormal_run_O33nV67aluIGjOwBRn23QE+0x11c>
100012aa4: d65f03c0    	ret
100012aa8: 1402854a    	b	0x1000b3fd0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100012aac: 1405de35    	b	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>


; native/manual-normal
0000000100012f90 <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD>:
100012f90: d101c3e9    	sub	x9, sp, #0x70
100012f94: f940078a    	ldr	x10, [x28, #0x8]
100012f98: eb0a013f    	cmp	x9, x10
100012f9c: 54000869    	b.ls	0x1000130a8 <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0x118>
100012fa0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100012fa4: 9100013f    	mov	sp, x9
100012fa8: b90037e1    	str	w1, [sp, #0x34]
100012fac: d0002063    	adrp	x3, 0x100420000 <___data>
100012fb0: f9407063    	ldr	x3, [x3, #0xe0]
100012fb4: f90017e3    	str	x3, [sp, #0x28]
100012fb8: aa0203e0    	mov	x0, x2
100012fbc: 2a1f03e2    	mov	w2, wzr
100012fc0: 14000027    	b	0x10001305c <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0xcc>
100012fc4: d503201f    	nop
100012fc8: d503201f    	nop
100012fcc: d503201f    	nop
100012fd0: b90033e2    	str	w2, [sp, #0x30]
100012fd4: 9100e3ed    	add	x13, sp, #0x38
100012fd8: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
100012fdc: f9400f9e    	ldr	x30, [x28, #0x18]
100012fe0: f9000dbe    	str	x30, [x13, #0x18]
100012fe4: f9000f8d    	str	x13, [x28, #0x18]
100012fe8: aa0303fe    	mov	x30, x3
100012fec: 10000029    	adr	x9, 0x100012ff0 <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0x60>
100012ff0: f90005a9    	str	x9, [x13, #0x8]
100012ff4: 910003e9    	mov	x9, sp
100012ff8: f90009a9    	str	x9, [x13, #0x10]
100012ffc: 320007e9    	orr	w9, wzr, #0x3
100013000: 9100538a    	add	x10, x28, #0x14
100013004: 889ffd49    	stlr	w9, [x10]
100013008: d63f03c0    	blr	x30
10001300c: 320007e1    	orr	w1, wzr, #0x3
100013010: 320003e2    	orr	w2, wzr, #0x1
100013014: 91005383    	add	x3, x28, #0x14
100013018: 885f7c64    	ldxr	w4, [x3]
10001301c: 6b01009f    	cmp	w4, w1
100013020: 1a84004a    	csel	w10, w2, w4, eq
100013024: 8809fc6a    	stlxr	w9, w10, [x3]
100013028: 35ffff89    	cbnz	w9, 0x100013018 <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0x88>
10001302c: d5033bbf    	dmb	ish
100013030: 1a9f17e1    	cset	w1, eq
100013034: 340001a1    	cbz	w1, 0x100013068 <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0xd8>
100013038: f9400f81    	ldr	x1, [x28, #0x18]
10001303c: f9400c21    	ldr	x1, [x1, #0x18]
100013040: f9000f81    	str	x1, [x28, #0x18]
100013044: b9402f81    	ldr	w1, [x28, #0x2c]
100013048: 350001c1    	cbnz	w1, 0x100013080 <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0xf0>
10001304c: b94033e2    	ldr	w2, [sp, #0x30]
100013050: 11000442    	add	w2, w2, #0x1
100013054: f94017e3    	ldr	x3, [sp, #0x28]
100013058: b94037e1    	ldr	w1, [sp, #0x34]
10001305c: 6b01005f    	cmp	w2, w1
100013060: 54fffb8b    	b.lt	0x100012fd0 <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0x40>
100013064: 1400000b    	b	0x100013090 <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0x100>
100013068: f90013e0    	str	x0, [sp, #0x20]
10001306c: 320003e0    	orr	w0, wzr, #0x1
100013070: 320003e1    	orr	w1, wzr, #0x1
100013074: 9405deff    	bl	0x10018ac70 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
100013078: f94013e0    	ldr	x0, [sp, #0x20]
10001307c: 17fffff4    	b	0x10001304c <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0xbc>
100013080: f90013e0    	str	x0, [sp, #0x20]
100013084: 9405de8f    	bl	0x10018aac0 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
100013088: f94013e0    	ldr	x0, [sp, #0x20]
10001308c: 17fffff0    	b	0x10001304c <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0xbc>
100013090: a9467bfd    	ldp	x29, x30, [sp, #0x60]
100013094: 9101c3ff    	add	sp, sp, #0x70
100013098: b9401389    	ldr	w9, [x28, #0x10]
10001309c: 7100013f    	cmp	w9, #0x0
1000130a0: 5400006d    	b.le	0x1000130ac <_Native$ManualNormal_run_uHdgTewYDtEdK30bZv8sjD+0x11c>
1000130a4: d65f03c0    	ret
1000130a8: 140283ca    	b	0x1000b3fd0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000130ac: 1405dcb5    	b	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>


; native/generated-leaf
00000001000127b0 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA>:
1000127b0: d10143e9    	sub	x9, sp, #0x50
1000127b4: f940078a    	ldr	x10, [x28, #0x8]
1000127b8: eb0a013f    	cmp	x9, x10
1000127bc: 54000e09    	b.ls	0x10001297c <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x1cc>
1000127c0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000127c4: 9100013f    	mov	sp, x9
1000127c8: b90033e1    	str	w1, [sp, #0x30]
1000127cc: d0002063    	adrp	x3, 0x100420000 <___data>
1000127d0: f9441463    	ldr	x3, [x3, #0x828]
1000127d4: f9000fe3    	str	x3, [sp, #0x18]
1000127d8: 7100043f    	cmp	w1, #0x1
1000127dc: 320003e0    	orr	w0, wzr, #0x1
1000127e0: 1a80d020    	csel	w0, w1, w0, le
1000127e4: 7100041f    	cmp	w0, #0x1
1000127e8: 54000c4b    	b.lt	0x100012970 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x1c0>
1000127ec: aa0203e0    	mov	x0, x2
1000127f0: aa0303fe    	mov	x30, x3
1000127f4: d63f03c0    	blr	x30
1000127f8: 320003fe    	orr	w30, wzr, #0x1
1000127fc: b94033e1    	ldr	w1, [sp, #0x30]
100012800: 52800082    	mov	w2, #0x4                ; =4
100012804: 72b00002    	movk	w2, #0x8000, lsl #16
100012808: 320103e3    	orr	w3, wzr, #0x80000000
10001280c: 51001024    	sub	w4, w1, #0x4
100012810: 6b02003f    	cmp	w1, w2
100012814: 1a84b062    	csel	w2, w3, w4, lt
100012818: b9002be2    	str	w2, [sp, #0x28]
10001281c: 11000c43    	add	w3, w2, #0x3
100012820: b90027e3    	str	w3, [sp, #0x24]
100012824: f9400fe4    	ldr	x4, [sp, #0x18]
100012828: 2a1e03e1    	mov	w1, w30
10001282c: d503201f    	nop
100012830: 6b02003f    	cmp	w1, w2
100012834: 5400070a    	b.ge	0x100012914 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x164>
100012838: 4b01007e    	sub	w30, w3, w1
10001283c: 53027fc6    	lsr	w6, w30, #2
100012840: 121e77de    	and	w30, w30, #0xfffffffc
100012844: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100012848: 321203e6    	orr	w6, wzr, #0x4000
10001284c: 1a8633c6    	csel	w6, w30, w6, lo
100012850: 710004df    	cmp	w6, #0x1
100012854: 5400058b    	b.lt	0x100012904 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x154>
100012858: b90037e1    	str	w1, [sp, #0x34]
10001285c: b9003fe6    	str	w6, [sp, #0x3c]
100012860: 2a1f03e2    	mov	w2, wzr
100012864: 14000004    	b	0x100012874 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0xc4>
100012868: d503201f    	nop
10001286c: d503201f    	nop
100012870: f9400fe4    	ldr	x4, [sp, #0x18]
100012874: 0b010048    	add	w8, w2, w1
100012878: b90023e8    	str	w8, [sp, #0x20]
10001287c: aa0403fe    	mov	x30, x4
100012880: b9002fe2    	str	w2, [sp, #0x2c]
100012884: d63f03c0    	blr	x30
100012888: b94023e8    	ldr	w8, [sp, #0x20]
10001288c: 11000501    	add	w1, w8, #0x1
100012890: f9400ffe    	ldr	x30, [sp, #0x18]
100012894: b9003be1    	str	w1, [sp, #0x38]
100012898: d63f03c0    	blr	x30
10001289c: b94023e8    	ldr	w8, [sp, #0x20]
1000128a0: 11000901    	add	w1, w8, #0x2
1000128a4: f9400ffe    	ldr	x30, [sp, #0x18]
1000128a8: b9003be1    	str	w1, [sp, #0x38]
1000128ac: d63f03c0    	blr	x30
1000128b0: b94023e8    	ldr	w8, [sp, #0x20]
1000128b4: 11000d01    	add	w1, w8, #0x3
1000128b8: f9400ffe    	ldr	x30, [sp, #0x18]
1000128bc: b90023e1    	str	w1, [sp, #0x20]
1000128c0: d63f03c0    	blr	x30
1000128c4: b9402fe2    	ldr	w2, [sp, #0x2c]
1000128c8: 11001042    	add	w2, w2, #0x4
1000128cc: b9403fe6    	ldr	w6, [sp, #0x3c]
1000128d0: 6b06005f    	cmp	w2, w6
1000128d4: b94037e1    	ldr	w1, [sp, #0x34]
1000128d8: 54fffccb    	b.lt	0x100012870 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0xc0>
1000128dc: 0b010041    	add	w1, w2, w1
1000128e0: b9401389    	ldr	w9, [x28, #0x10]
1000128e4: 34000089    	cbz	w9, 0x1000128f4 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x144>
1000128e8: 29448be3    	ldp	w3, w2, [sp, #0x24]
1000128ec: f9400fe4    	ldr	x4, [sp, #0x18]
1000128f0: 17ffffd0    	b	0x100012830 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x80>
1000128f4: 9405dea3    	bl	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000128f8: 29448be3    	ldp	w3, w2, [sp, #0x24]
1000128fc: f9400fe4    	ldr	x4, [sp, #0x18]
100012900: 17ffffcc    	b	0x100012830 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x80>
100012904: b9401389    	ldr	w9, [x28, #0x10]
100012908: 35fff949    	cbnz	w9, 0x100012830 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x80>
10001290c: 9405de9d    	bl	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100012910: 17ffffc8    	b	0x100012830 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x80>
100012914: 2a0103e2    	mov	w2, w1
100012918: aa0403e3    	mov	x3, x4
10001291c: 1400000c    	b	0x10001294c <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x19c>
100012920: d503201f    	nop
100012924: d503201f    	nop
100012928: d503201f    	nop
10001292c: d503201f    	nop
100012930: b9002fe2    	str	w2, [sp, #0x2c]
100012934: aa0303fe    	mov	x30, x3
100012938: d63f03c0    	blr	x30
10001293c: b9402fe2    	ldr	w2, [sp, #0x2c]
100012940: 1100045d    	add	w29, w2, #0x1
100012944: 2a1d03e2    	mov	w2, w29
100012948: f9400fe3    	ldr	x3, [sp, #0x18]
10001294c: b94033e1    	ldr	w1, [sp, #0x30]
100012950: 6b01005f    	cmp	w2, w1
100012954: 54fffeeb    	b.lt	0x100012930 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x180>
100012958: a9447bfd    	ldp	x29, x30, [sp, #0x40]
10001295c: 910143ff    	add	sp, sp, #0x50
100012960: b9401389    	ldr	w9, [x28, #0x10]
100012964: 7100013f    	cmp	w9, #0x0
100012968: 540000cd    	b.le	0x100012980 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x1d0>
10001296c: d65f03c0    	ret
100012970: 2a1f03fe    	mov	w30, wzr
100012974: aa0203e0    	mov	x0, x2
100012978: 17ffffa2    	b	0x100012800 <_Native$GeneratedLeaf_run_EaXamf1cn4Kuyi2hC6OtWA+0x50>
10001297c: 14028595    	b	0x1000b3fd0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100012980: 1405de80    	b	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100012984: cccccccc    	<unknown>
100012988: cccccccc    	<unknown>
10001298c: cccccccc    	<unknown>


; native/manual-leaf
0000000100012e00 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8>:
100012e00: d10143e9    	sub	x9, sp, #0x50
100012e04: f940078a    	ldr	x10, [x28, #0x8]
100012e08: eb0a013f    	cmp	x9, x10
100012e0c: 54000bc9    	b.ls	0x100012f84 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x184>
100012e10: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100012e14: 9100013f    	mov	sp, x9
100012e18: b90033e1    	str	w1, [sp, #0x30]
100012e1c: 7100043f    	cmp	w1, #0x1
100012e20: 320003e0    	orr	w0, wzr, #0x1
100012e24: 1a80d020    	csel	w0, w1, w0, le
100012e28: 7100041f    	cmp	w0, #0x1
100012e2c: 54000a6b    	b.lt	0x100012f78 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x178>
100012e30: aa0203e0    	mov	x0, x2
100012e34: 94000173    	bl	0x100013400 <_Native_leaf_4jmZJhyx6ZCIpcsFKc9g8F>
100012e38: 320003e2    	orr	w2, wzr, #0x1
100012e3c: b94033e1    	ldr	w1, [sp, #0x30]
100012e40: 52800083    	mov	w3, #0x4                ; =4
100012e44: 72b00003    	movk	w3, #0x8000, lsl #16
100012e48: 320103e4    	orr	w4, wzr, #0x80000000
100012e4c: 51001025    	sub	w5, w1, #0x4
100012e50: 6b03003f    	cmp	w1, w3
100012e54: 1a85b083    	csel	w3, w4, w5, lt
100012e58: b9002be3    	str	w3, [sp, #0x28]
100012e5c: 11000c64    	add	w4, w3, #0x3
100012e60: b90027e4    	str	w4, [sp, #0x24]
100012e64: 2a0203e1    	mov	w1, w2
100012e68: d503201f    	nop
100012e6c: d503201f    	nop
100012e70: 6b03003f    	cmp	w1, w3
100012e74: 5400062a    	b.ge	0x100012f38 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x138>
100012e78: 4b010085    	sub	w5, w4, w1
100012e7c: 53027ca6    	lsr	w6, w5, #2
100012e80: 121e74a5    	and	w5, w5, #0xfffffffc
100012e84: 714004df    	cmp	w6, #0x1, lsl #12       ; =0x1000
100012e88: 321203e6    	orr	w6, wzr, #0x4000
100012e8c: 1a8630a5    	csel	w5, w5, w6, lo
100012e90: 710004bf    	cmp	w5, #0x1
100012e94: 540004ab    	b.lt	0x100012f28 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x128>
100012e98: b90037e1    	str	w1, [sp, #0x34]
100012e9c: b9003fe5    	str	w5, [sp, #0x3c]
100012ea0: 2a1f03e2    	mov	w2, wzr
100012ea4: d503201f    	nop
100012ea8: d503201f    	nop
100012eac: d503201f    	nop
100012eb0: 0b010047    	add	w7, w2, w1
100012eb4: b90023e7    	str	w7, [sp, #0x20]
100012eb8: b9002fe2    	str	w2, [sp, #0x2c]
100012ebc: 94000151    	bl	0x100013400 <_Native_leaf_4jmZJhyx6ZCIpcsFKc9g8F>
100012ec0: b94023e7    	ldr	w7, [sp, #0x20]
100012ec4: 110004e1    	add	w1, w7, #0x1
100012ec8: b9003be1    	str	w1, [sp, #0x38]
100012ecc: 9400014d    	bl	0x100013400 <_Native_leaf_4jmZJhyx6ZCIpcsFKc9g8F>
100012ed0: b94023e7    	ldr	w7, [sp, #0x20]
100012ed4: 110008e1    	add	w1, w7, #0x2
100012ed8: b9003be1    	str	w1, [sp, #0x38]
100012edc: 94000149    	bl	0x100013400 <_Native_leaf_4jmZJhyx6ZCIpcsFKc9g8F>
100012ee0: b94023e7    	ldr	w7, [sp, #0x20]
100012ee4: 11000ce1    	add	w1, w7, #0x3
100012ee8: b90023e1    	str	w1, [sp, #0x20]
100012eec: 94000145    	bl	0x100013400 <_Native_leaf_4jmZJhyx6ZCIpcsFKc9g8F>
100012ef0: b9402fe2    	ldr	w2, [sp, #0x2c]
100012ef4: 11001042    	add	w2, w2, #0x4
100012ef8: b9403fe5    	ldr	w5, [sp, #0x3c]
100012efc: 6b05005f    	cmp	w2, w5
100012f00: b94037e1    	ldr	w1, [sp, #0x34]
100012f04: 54fffd6b    	b.lt	0x100012eb0 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0xb0>
100012f08: 0b010041    	add	w1, w2, w1
100012f0c: b9401389    	ldr	w9, [x28, #0x10]
100012f10: 34000069    	cbz	w9, 0x100012f1c <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x11c>
100012f14: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100012f18: 17ffffd6    	b	0x100012e70 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x70>
100012f1c: 9405dd19    	bl	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100012f20: 29448fe4    	ldp	w4, w3, [sp, #0x24]
100012f24: 17ffffd3    	b	0x100012e70 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x70>
100012f28: b9401389    	ldr	w9, [x28, #0x10]
100012f2c: 35fffa29    	cbnz	w9, 0x100012e70 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x70>
100012f30: 9405dd14    	bl	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100012f34: 17ffffcf    	b	0x100012e70 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x70>
100012f38: 2a0103e2    	mov	w2, w1
100012f3c: 14000006    	b	0x100012f54 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x154>
100012f40: b9002fe2    	str	w2, [sp, #0x2c]
100012f44: 9400012f    	bl	0x100013400 <_Native_leaf_4jmZJhyx6ZCIpcsFKc9g8F>
100012f48: b9402fe2    	ldr	w2, [sp, #0x2c]
100012f4c: 1100045d    	add	w29, w2, #0x1
100012f50: 2a1d03e2    	mov	w2, w29
100012f54: b94033e1    	ldr	w1, [sp, #0x30]
100012f58: 6b01005f    	cmp	w2, w1
100012f5c: 54ffff2b    	b.lt	0x100012f40 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x140>
100012f60: a9447bfd    	ldp	x29, x30, [sp, #0x40]
100012f64: 910143ff    	add	sp, sp, #0x50
100012f68: b9401389    	ldr	w9, [x28, #0x10]
100012f6c: 7100013f    	cmp	w9, #0x0
100012f70: 540000cd    	b.le	0x100012f88 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x188>
100012f74: d65f03c0    	ret
100012f78: aa0203e0    	mov	x0, x2
100012f7c: 2a1f03e2    	mov	w2, wzr
100012f80: 17ffffb0    	b	0x100012e40 <_Native$ManualLeaf_run_XqGHjJ0fwuH3x7KZ4Ti7e8+0x40>
100012f84: 14028413    	b	0x1000b3fd0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100012f88: 1405dcfe    	b	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100012f8c: cccccccc    	<unknown>


; native/manual-leaf-stub
0000000100013400 <_Native_leaf_4jmZJhyx6ZCIpcsFKc9g8F>:
100013400: d10083ff    	sub	sp, sp, #0x20
100013404: a9017bfd    	stp	x29, x30, [sp, #0x10]
100013408: b000207e    	adrp	x30, 0x100420000 <___data>
10001340c: f941bbde    	ldr	x30, [x30, #0x370]
100013410: d63f03c0    	blr	x30
100013414: a9417bfd    	ldp	x29, x30, [sp, #0x10]
100013418: 910083ff    	add	sp, sp, #0x20
10001341c: d65f03c0    	ret


; native/generated-static-jni
0000000100012ab0 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD>:
100012ab0: d10143e9    	sub	x9, sp, #0x50
100012ab4: f940078a    	ldr	x10, [x28, #0x8]
100012ab8: eb0a013f    	cmp	x9, x10
100012abc: 540019c9    	b.ls	0x100012df4 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x344>
100012ac0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100012ac4: 9100013f    	mov	sp, x9
100012ac8: 7100043f    	cmp	w1, #0x1
100012acc: 540018cb    	b.lt	0x100012de4 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x334>
100012ad0: b9003fe1    	str	w1, [sp, #0x3c]
100012ad4: 528495a3    	mov	w3, #0x24ad             ; =9389
100012ad8: 72a000a3    	movk	w3, #0x5, lsl #16
100012adc: 8b030f63    	add	x3, x27, x3, lsl #3
100012ae0: f90013e3    	str	x3, [sp, #0x20]
100012ae4: 91008469    	add	x9, x3, #0x21
100012ae8: 08dffd20    	ldarb	w0, [x9]
100012aec: 528022e4    	mov	w4, #0x117              ; =279
100012af0: 72a00024    	movk	w4, #0x1, lsl #16
100012af4: 8b040f64    	add	x4, x27, x4, lsl #3
100012af8: f90017e4    	str	x4, [sp, #0x28]
100012afc: 35001560    	cbnz	w0, 0x100012da8 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x2f8>
100012b00: aa0203e0    	mov	x0, x2
100012b04: 97fff5eb    	bl	0x1000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>
100012b08: b9403fe1    	ldr	w1, [sp, #0x3c]
100012b0c: 51002022    	sub	w2, w1, #0x8
100012b10: 7100085f    	cmp	w2, #0x2
100012b14: 540016cb    	b.lt	0x100012dec <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x33c>
100012b18: b9003be2    	str	w2, [sp, #0x38]
100012b1c: 320003e9    	orr	w9, wzr, #0x1
100012b20: b90033e9    	str	w9, [sp, #0x30]
100012b24: f94013e3    	ldr	x3, [sp, #0x20]
100012b28: aa0003e1    	mov	x1, x0
100012b2c: 14000051    	b	0x100012c70 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x1c0>
100012b30: aa0103e0    	mov	x0, x1
100012b34: 97fff5df    	bl	0x1000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>
100012b38: aa0003e1    	mov	x1, x0
100012b3c: f94013e3    	ldr	x3, [sp, #0x20]
100012b40: 91008469    	add	x9, x3, #0x21
100012b44: 08dffd20    	ldarb	w0, [x9]
100012b48: b94033e2    	ldr	w2, [sp, #0x30]
100012b4c: 11000444    	add	w4, w2, #0x1
100012b50: 35000c00    	cbnz	w0, 0x100012cd0 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x220>
100012b54: b90037e4    	str	w4, [sp, #0x34]
100012b58: aa0103e0    	mov	x0, x1
100012b5c: 97fff5d5    	bl	0x1000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>
100012b60: aa0003e1    	mov	x1, x0
100012b64: f94013e3    	ldr	x3, [sp, #0x20]
100012b68: 91008469    	add	x9, x3, #0x21
100012b6c: 08dffd20    	ldarb	w0, [x9]
100012b70: b94033e2    	ldr	w2, [sp, #0x30]
100012b74: 11000844    	add	w4, w2, #0x2
100012b78: 350008e0    	cbnz	w0, 0x100012c94 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x1e4>
100012b7c: b90037e4    	str	w4, [sp, #0x34]
100012b80: aa0103e0    	mov	x0, x1
100012b84: 97fff5cb    	bl	0x1000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>
100012b88: aa0003e1    	mov	x1, x0
100012b8c: f94013e3    	ldr	x3, [sp, #0x20]
100012b90: 91008469    	add	x9, x3, #0x21
100012b94: 08dffd20    	ldarb	w0, [x9]
100012b98: b94033e2    	ldr	w2, [sp, #0x30]
100012b9c: 11000c44    	add	w4, w2, #0x3
100012ba0: 35000840    	cbnz	w0, 0x100012ca8 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x1f8>
100012ba4: b90037e4    	str	w4, [sp, #0x34]
100012ba8: aa0103e0    	mov	x0, x1
100012bac: 97fff5c1    	bl	0x1000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>
100012bb0: aa0003e1    	mov	x1, x0
100012bb4: f94013e3    	ldr	x3, [sp, #0x20]
100012bb8: 91008469    	add	x9, x3, #0x21
100012bbc: 08dffd20    	ldarb	w0, [x9]
100012bc0: b94033e2    	ldr	w2, [sp, #0x30]
100012bc4: 11001044    	add	w4, w2, #0x4
100012bc8: 35000a20    	cbnz	w0, 0x100012d0c <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x25c>
100012bcc: b90037e4    	str	w4, [sp, #0x34]
100012bd0: aa0103e0    	mov	x0, x1
100012bd4: 97fff5b7    	bl	0x1000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>
100012bd8: aa0003e1    	mov	x1, x0
100012bdc: f94013e3    	ldr	x3, [sp, #0x20]
100012be0: 91008469    	add	x9, x3, #0x21
100012be4: 08dffd20    	ldarb	w0, [x9]
100012be8: b94033e2    	ldr	w2, [sp, #0x30]
100012bec: 11001444    	add	w4, w2, #0x5
100012bf0: 35000840    	cbnz	w0, 0x100012cf8 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x248>
100012bf4: b90037e4    	str	w4, [sp, #0x34]
100012bf8: aa0103e0    	mov	x0, x1
100012bfc: 97fff5ad    	bl	0x1000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>
100012c00: aa0003e1    	mov	x1, x0
100012c04: f94013e3    	ldr	x3, [sp, #0x20]
100012c08: 91008469    	add	x9, x3, #0x21
100012c0c: 08dffd20    	ldarb	w0, [x9]
100012c10: b94033e2    	ldr	w2, [sp, #0x30]
100012c14: 11001844    	add	w4, w2, #0x6
100012c18: 35000660    	cbnz	w0, 0x100012ce4 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x234>
100012c1c: b90037e4    	str	w4, [sp, #0x34]
100012c20: aa0103e0    	mov	x0, x1
100012c24: 97fff5a3    	bl	0x1000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>
100012c28: aa0003e1    	mov	x1, x0
100012c2c: f94013e3    	ldr	x3, [sp, #0x20]
100012c30: 91008469    	add	x9, x3, #0x21
100012c34: 08dffd20    	ldarb	w0, [x9]
100012c38: b94033e2    	ldr	w2, [sp, #0x30]
100012c3c: 11001c44    	add	w4, w2, #0x7
100012c40: 350003e0    	cbnz	w0, 0x100012cbc <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x20c>
100012c44: b90037e4    	str	w4, [sp, #0x34]
100012c48: aa0103e0    	mov	x0, x1
100012c4c: 97fff599    	bl	0x1000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>
100012c50: b94033e2    	ldr	w2, [sp, #0x30]
100012c54: 11002042    	add	w2, w2, #0x8
100012c58: b9403bfd    	ldr	w29, [sp, #0x38]
100012c5c: 6b1d005f    	cmp	w2, w29
100012c60: 5400060a    	b.ge	0x100012d20 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x270>
100012c64: b90033e2    	str	w2, [sp, #0x30]
100012c68: aa0003e1    	mov	x1, x0
100012c6c: f94013e3    	ldr	x3, [sp, #0x20]
100012c70: 91008469    	add	x9, x3, #0x21
100012c74: 08dffd20    	ldarb	w0, [x9]
100012c78: 34fff5c0    	cbz	w0, 0x100012b30 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x80>
100012c7c: d503201f    	nop
100012c80: b94033e2    	ldr	w2, [sp, #0x30]
100012c84: b9403bfd    	ldr	w29, [sp, #0x38]
100012c88: f94017e0    	ldr	x0, [sp, #0x28]
100012c8c: 9400c06d    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012c90: 17ffffa8    	b	0x100012b30 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x80>
100012c94: b90037e4    	str	w4, [sp, #0x34]
100012c98: b9403bfd    	ldr	w29, [sp, #0x38]
100012c9c: f94017e0    	ldr	x0, [sp, #0x28]
100012ca0: 9400c068    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012ca4: 17ffffb7    	b	0x100012b80 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0xd0>
100012ca8: b90037e4    	str	w4, [sp, #0x34]
100012cac: b9403bfd    	ldr	w29, [sp, #0x38]
100012cb0: f94017e0    	ldr	x0, [sp, #0x28]
100012cb4: 9400c063    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012cb8: 17ffffbc    	b	0x100012ba8 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0xf8>
100012cbc: b90037e4    	str	w4, [sp, #0x34]
100012cc0: b9403bfd    	ldr	w29, [sp, #0x38]
100012cc4: f94017e0    	ldr	x0, [sp, #0x28]
100012cc8: 9400c05e    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012ccc: 17ffffdf    	b	0x100012c48 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x198>
100012cd0: b90037e4    	str	w4, [sp, #0x34]
100012cd4: b9403bfd    	ldr	w29, [sp, #0x38]
100012cd8: f94017e0    	ldr	x0, [sp, #0x28]
100012cdc: 9400c059    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012ce0: 17ffff9e    	b	0x100012b58 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0xa8>
100012ce4: b90037e4    	str	w4, [sp, #0x34]
100012ce8: b9403bfd    	ldr	w29, [sp, #0x38]
100012cec: f94017e0    	ldr	x0, [sp, #0x28]
100012cf0: 9400c054    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012cf4: 17ffffcb    	b	0x100012c20 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x170>
100012cf8: b90037e4    	str	w4, [sp, #0x34]
100012cfc: b9403bfd    	ldr	w29, [sp, #0x38]
100012d00: f94017e0    	ldr	x0, [sp, #0x28]
100012d04: 9400c04f    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012d08: 17ffffbc    	b	0x100012bf8 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x148>
100012d0c: b90037e4    	str	w4, [sp, #0x34]
100012d10: b9403bfd    	ldr	w29, [sp, #0x38]
100012d14: f94017e0    	ldr	x0, [sp, #0x28]
100012d18: 9400c04a    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012d1c: 17ffffad    	b	0x100012bd0 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x120>
100012d20: b9403fe1    	ldr	w1, [sp, #0x3c]
100012d24: 6b01005f    	cmp	w2, w1
100012d28: 540000eb    	b.lt	0x100012d44 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x294>
100012d2c: a9447bfd    	ldp	x29, x30, [sp, #0x40]
100012d30: 910143ff    	add	sp, sp, #0x50
100012d34: b9401389    	ldr	w9, [x28, #0x10]
100012d38: 7100013f    	cmp	w9, #0x0
100012d3c: 540005ed    	b.le	0x100012df8 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x348>
100012d40: d65f03c0    	ret
100012d44: b90033e2    	str	w2, [sp, #0x30]
100012d48: f94013e3    	ldr	x3, [sp, #0x20]
100012d4c: aa0003e1    	mov	x1, x0
100012d50: 14000012    	b	0x100012d98 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x2e8>
100012d54: d503201f    	nop
100012d58: d503201f    	nop
100012d5c: d503201f    	nop
100012d60: d503201f    	nop
100012d64: d503201f    	nop
100012d68: d503201f    	nop
100012d6c: d503201f    	nop
100012d70: aa0103e0    	mov	x0, x1
100012d74: 97fff54f    	bl	0x1000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>
100012d78: b94033e2    	ldr	w2, [sp, #0x30]
100012d7c: 1100045e    	add	w30, w2, #0x1
100012d80: b9403fe3    	ldr	w3, [sp, #0x3c]
100012d84: 6b0303df    	cmp	w30, w3
100012d88: 54fffd2a    	b.ge	0x100012d2c <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x27c>
100012d8c: b90033fe    	str	w30, [sp, #0x30]
100012d90: aa0003e1    	mov	x1, x0
100012d94: f94013e3    	ldr	x3, [sp, #0x20]
100012d98: 91008469    	add	x9, x3, #0x21
100012d9c: 08dffd20    	ldarb	w0, [x9]
100012da0: 34fffe80    	cbz	w0, 0x100012d70 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x2c0>
100012da4: 1400000b    	b	0x100012dd0 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x320>
100012da8: 2a0103e3    	mov	w3, w1
100012dac: f94017e0    	ldr	x0, [sp, #0x28]
100012db0: 9400c024    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012db4: 17ffff53    	b	0x100012b00 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x50>
100012db8: d503201f    	nop
100012dbc: d503201f    	nop
100012dc0: d503201f    	nop
100012dc4: d503201f    	nop
100012dc8: d503201f    	nop
100012dcc: d503201f    	nop
100012dd0: b94033e2    	ldr	w2, [sp, #0x30]
100012dd4: b9403fe3    	ldr	w3, [sp, #0x3c]
100012dd8: f94017e0    	ldr	x0, [sp, #0x28]
100012ddc: 9400c019    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012de0: 17ffffe4    	b	0x100012d70 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x2c0>
100012de4: aa0203e0    	mov	x0, x2
100012de8: 17ffffd1    	b	0x100012d2c <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x27c>
100012dec: 320003e2    	orr	w2, wzr, #0x1
100012df0: 17ffffcd    	b	0x100012d24 <_Native$GeneratedStaticJni_run_nofEeE2VlqJtgwWOWS0PhD+0x274>
100012df4: 14028477    	b	0x1000b3fd0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100012df8: 1405dd62    	b	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100012dfc: cccccccc    	<unknown>


; native/manual-static-jni
00000001000130b0 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3>:
1000130b0: d10143e9    	sub	x9, sp, #0x50
1000130b4: f940078a    	ldr	x10, [x28, #0x8]
1000130b8: eb0a013f    	cmp	x9, x10
1000130bc: 540019c9    	b.ls	0x1000133f4 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x344>
1000130c0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000130c4: 9100013f    	mov	sp, x9
1000130c8: 7100043f    	cmp	w1, #0x1
1000130cc: 540018cb    	b.lt	0x1000133e4 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x334>
1000130d0: b9003fe1    	str	w1, [sp, #0x3c]
1000130d4: 52849663    	mov	w3, #0x24b3             ; =9395
1000130d8: 72a000a3    	movk	w3, #0x5, lsl #16
1000130dc: 8b030f63    	add	x3, x27, x3, lsl #3
1000130e0: f90013e3    	str	x3, [sp, #0x20]
1000130e4: 91008469    	add	x9, x3, #0x21
1000130e8: 08dffd20    	ldarb	w0, [x9]
1000130ec: 52802424    	mov	w4, #0x121              ; =289
1000130f0: 72a00024    	movk	w4, #0x1, lsl #16
1000130f4: 8b040f64    	add	x4, x27, x4, lsl #3
1000130f8: f90017e4    	str	x4, [sp, #0x28]
1000130fc: 35001560    	cbnz	w0, 0x1000133a8 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x2f8>
100013100: aa0203e0    	mov	x0, x2
100013104: 97fff563    	bl	0x100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>
100013108: b9403fe1    	ldr	w1, [sp, #0x3c]
10001310c: 51002022    	sub	w2, w1, #0x8
100013110: 7100085f    	cmp	w2, #0x2
100013114: 540016cb    	b.lt	0x1000133ec <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x33c>
100013118: b9003be2    	str	w2, [sp, #0x38]
10001311c: 320003e9    	orr	w9, wzr, #0x1
100013120: b90033e9    	str	w9, [sp, #0x30]
100013124: f94013e3    	ldr	x3, [sp, #0x20]
100013128: aa0003e1    	mov	x1, x0
10001312c: 14000051    	b	0x100013270 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x1c0>
100013130: aa0103e0    	mov	x0, x1
100013134: 97fff557    	bl	0x100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>
100013138: aa0003e1    	mov	x1, x0
10001313c: f94013e3    	ldr	x3, [sp, #0x20]
100013140: 91008469    	add	x9, x3, #0x21
100013144: 08dffd20    	ldarb	w0, [x9]
100013148: b94033e2    	ldr	w2, [sp, #0x30]
10001314c: 11000444    	add	w4, w2, #0x1
100013150: 35000c00    	cbnz	w0, 0x1000132d0 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x220>
100013154: b90037e4    	str	w4, [sp, #0x34]
100013158: aa0103e0    	mov	x0, x1
10001315c: 97fff54d    	bl	0x100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>
100013160: aa0003e1    	mov	x1, x0
100013164: f94013e3    	ldr	x3, [sp, #0x20]
100013168: 91008469    	add	x9, x3, #0x21
10001316c: 08dffd20    	ldarb	w0, [x9]
100013170: b94033e2    	ldr	w2, [sp, #0x30]
100013174: 11000844    	add	w4, w2, #0x2
100013178: 350008e0    	cbnz	w0, 0x100013294 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x1e4>
10001317c: b90037e4    	str	w4, [sp, #0x34]
100013180: aa0103e0    	mov	x0, x1
100013184: 97fff543    	bl	0x100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>
100013188: aa0003e1    	mov	x1, x0
10001318c: f94013e3    	ldr	x3, [sp, #0x20]
100013190: 91008469    	add	x9, x3, #0x21
100013194: 08dffd20    	ldarb	w0, [x9]
100013198: b94033e2    	ldr	w2, [sp, #0x30]
10001319c: 11000c44    	add	w4, w2, #0x3
1000131a0: 35000840    	cbnz	w0, 0x1000132a8 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x1f8>
1000131a4: b90037e4    	str	w4, [sp, #0x34]
1000131a8: aa0103e0    	mov	x0, x1
1000131ac: 97fff539    	bl	0x100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>
1000131b0: aa0003e1    	mov	x1, x0
1000131b4: f94013e3    	ldr	x3, [sp, #0x20]
1000131b8: 91008469    	add	x9, x3, #0x21
1000131bc: 08dffd20    	ldarb	w0, [x9]
1000131c0: b94033e2    	ldr	w2, [sp, #0x30]
1000131c4: 11001044    	add	w4, w2, #0x4
1000131c8: 35000a20    	cbnz	w0, 0x10001330c <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x25c>
1000131cc: b90037e4    	str	w4, [sp, #0x34]
1000131d0: aa0103e0    	mov	x0, x1
1000131d4: 97fff52f    	bl	0x100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>
1000131d8: aa0003e1    	mov	x1, x0
1000131dc: f94013e3    	ldr	x3, [sp, #0x20]
1000131e0: 91008469    	add	x9, x3, #0x21
1000131e4: 08dffd20    	ldarb	w0, [x9]
1000131e8: b94033e2    	ldr	w2, [sp, #0x30]
1000131ec: 11001444    	add	w4, w2, #0x5
1000131f0: 35000840    	cbnz	w0, 0x1000132f8 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x248>
1000131f4: b90037e4    	str	w4, [sp, #0x34]
1000131f8: aa0103e0    	mov	x0, x1
1000131fc: 97fff525    	bl	0x100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>
100013200: aa0003e1    	mov	x1, x0
100013204: f94013e3    	ldr	x3, [sp, #0x20]
100013208: 91008469    	add	x9, x3, #0x21
10001320c: 08dffd20    	ldarb	w0, [x9]
100013210: b94033e2    	ldr	w2, [sp, #0x30]
100013214: 11001844    	add	w4, w2, #0x6
100013218: 35000660    	cbnz	w0, 0x1000132e4 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x234>
10001321c: b90037e4    	str	w4, [sp, #0x34]
100013220: aa0103e0    	mov	x0, x1
100013224: 97fff51b    	bl	0x100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>
100013228: aa0003e1    	mov	x1, x0
10001322c: f94013e3    	ldr	x3, [sp, #0x20]
100013230: 91008469    	add	x9, x3, #0x21
100013234: 08dffd20    	ldarb	w0, [x9]
100013238: b94033e2    	ldr	w2, [sp, #0x30]
10001323c: 11001c44    	add	w4, w2, #0x7
100013240: 350003e0    	cbnz	w0, 0x1000132bc <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x20c>
100013244: b90037e4    	str	w4, [sp, #0x34]
100013248: aa0103e0    	mov	x0, x1
10001324c: 97fff511    	bl	0x100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>
100013250: b94033e2    	ldr	w2, [sp, #0x30]
100013254: 11002042    	add	w2, w2, #0x8
100013258: b9403bfd    	ldr	w29, [sp, #0x38]
10001325c: 6b1d005f    	cmp	w2, w29
100013260: 5400060a    	b.ge	0x100013320 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x270>
100013264: b90033e2    	str	w2, [sp, #0x30]
100013268: aa0003e1    	mov	x1, x0
10001326c: f94013e3    	ldr	x3, [sp, #0x20]
100013270: 91008469    	add	x9, x3, #0x21
100013274: 08dffd20    	ldarb	w0, [x9]
100013278: 34fff5c0    	cbz	w0, 0x100013130 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x80>
10001327c: d503201f    	nop
100013280: b94033e2    	ldr	w2, [sp, #0x30]
100013284: b9403bfd    	ldr	w29, [sp, #0x38]
100013288: f94017e0    	ldr	x0, [sp, #0x28]
10001328c: 9400beed    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100013290: 17ffffa8    	b	0x100013130 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x80>
100013294: b90037e4    	str	w4, [sp, #0x34]
100013298: b9403bfd    	ldr	w29, [sp, #0x38]
10001329c: f94017e0    	ldr	x0, [sp, #0x28]
1000132a0: 9400bee8    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
1000132a4: 17ffffb7    	b	0x100013180 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0xd0>
1000132a8: b90037e4    	str	w4, [sp, #0x34]
1000132ac: b9403bfd    	ldr	w29, [sp, #0x38]
1000132b0: f94017e0    	ldr	x0, [sp, #0x28]
1000132b4: 9400bee3    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
1000132b8: 17ffffbc    	b	0x1000131a8 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0xf8>
1000132bc: b90037e4    	str	w4, [sp, #0x34]
1000132c0: b9403bfd    	ldr	w29, [sp, #0x38]
1000132c4: f94017e0    	ldr	x0, [sp, #0x28]
1000132c8: 9400bede    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
1000132cc: 17ffffdf    	b	0x100013248 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x198>
1000132d0: b90037e4    	str	w4, [sp, #0x34]
1000132d4: b9403bfd    	ldr	w29, [sp, #0x38]
1000132d8: f94017e0    	ldr	x0, [sp, #0x28]
1000132dc: 9400bed9    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
1000132e0: 17ffff9e    	b	0x100013158 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0xa8>
1000132e4: b90037e4    	str	w4, [sp, #0x34]
1000132e8: b9403bfd    	ldr	w29, [sp, #0x38]
1000132ec: f94017e0    	ldr	x0, [sp, #0x28]
1000132f0: 9400bed4    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
1000132f4: 17ffffcb    	b	0x100013220 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x170>
1000132f8: b90037e4    	str	w4, [sp, #0x34]
1000132fc: b9403bfd    	ldr	w29, [sp, #0x38]
100013300: f94017e0    	ldr	x0, [sp, #0x28]
100013304: 9400becf    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100013308: 17ffffbc    	b	0x1000131f8 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x148>
10001330c: b90037e4    	str	w4, [sp, #0x34]
100013310: b9403bfd    	ldr	w29, [sp, #0x38]
100013314: f94017e0    	ldr	x0, [sp, #0x28]
100013318: 9400beca    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
10001331c: 17ffffad    	b	0x1000131d0 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x120>
100013320: b9403fe1    	ldr	w1, [sp, #0x3c]
100013324: 6b01005f    	cmp	w2, w1
100013328: 540000eb    	b.lt	0x100013344 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x294>
10001332c: a9447bfd    	ldp	x29, x30, [sp, #0x40]
100013330: 910143ff    	add	sp, sp, #0x50
100013334: b9401389    	ldr	w9, [x28, #0x10]
100013338: 7100013f    	cmp	w9, #0x0
10001333c: 540005ed    	b.le	0x1000133f8 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x348>
100013340: d65f03c0    	ret
100013344: b90033e2    	str	w2, [sp, #0x30]
100013348: f94013e3    	ldr	x3, [sp, #0x20]
10001334c: aa0003e1    	mov	x1, x0
100013350: 14000012    	b	0x100013398 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x2e8>
100013354: d503201f    	nop
100013358: d503201f    	nop
10001335c: d503201f    	nop
100013360: d503201f    	nop
100013364: d503201f    	nop
100013368: d503201f    	nop
10001336c: d503201f    	nop
100013370: aa0103e0    	mov	x0, x1
100013374: 97fff4c7    	bl	0x100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>
100013378: b94033e2    	ldr	w2, [sp, #0x30]
10001337c: 1100045e    	add	w30, w2, #0x1
100013380: b9403fe3    	ldr	w3, [sp, #0x3c]
100013384: 6b0303df    	cmp	w30, w3
100013388: 54fffd2a    	b.ge	0x10001332c <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x27c>
10001338c: b90033fe    	str	w30, [sp, #0x30]
100013390: aa0003e1    	mov	x1, x0
100013394: f94013e3    	ldr	x3, [sp, #0x20]
100013398: 91008469    	add	x9, x3, #0x21
10001339c: 08dffd20    	ldarb	w0, [x9]
1000133a0: 34fffe80    	cbz	w0, 0x100013370 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x2c0>
1000133a4: 1400000b    	b	0x1000133d0 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x320>
1000133a8: 2a0103e3    	mov	w3, w1
1000133ac: f94017e0    	ldr	x0, [sp, #0x28]
1000133b0: 9400bea4    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
1000133b4: 17ffff53    	b	0x100013100 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x50>
1000133b8: d503201f    	nop
1000133bc: d503201f    	nop
1000133c0: d503201f    	nop
1000133c4: d503201f    	nop
1000133c8: d503201f    	nop
1000133cc: d503201f    	nop
1000133d0: b94033e2    	ldr	w2, [sp, #0x30]
1000133d4: b9403fe3    	ldr	w3, [sp, #0x3c]
1000133d8: f94017e0    	ldr	x0, [sp, #0x28]
1000133dc: 9400be99    	bl	0x100042e40 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
1000133e0: 17ffffe4    	b	0x100013370 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x2c0>
1000133e4: aa0203e0    	mov	x0, x2
1000133e8: 17ffffd1    	b	0x10001332c <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x27c>
1000133ec: 320003e2    	orr	w2, wzr, #0x1
1000133f0: 17ffffcd    	b	0x100013324 <_Native$ManualStaticJni_run_FBtIPr4ojZ0h5doZEpWQb3+0x274>
1000133f4: 140282f7    	b	0x1000b3fd0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
1000133f8: 1405dbe2    	b	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
1000133fc: cccccccc    	<unknown>


; native/generated-jni-java-stub
00000001000102b0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5>:
1000102b0: d101c3e9    	sub	x9, sp, #0x70
1000102b4: f940078a    	ldr	x10, [x28, #0x8]
1000102b8: eb0a013f    	cmp	x9, x10
1000102bc: 54001b29    	b.ls	0x100010620 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x370>
1000102c0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000102c4: 9100013f    	mov	sp, x9
1000102c8: f9400381    	ldr	x1, [x28]
1000102cc: b4000e01    	cbz	x1, 0x10001048c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x1dc>
1000102d0: b940ef81    	ldr	w1, [x28, #0xec]
1000102d4: 34000ea1    	cbz	w1, 0x1000104a8 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x1f8>
1000102d8: 2a0103e1    	mov	w1, w1
1000102dc: 8b010f61    	add	x1, x27, x1, lsl #3
1000102e0: f90017e0    	str	x0, [sp, #0x28]
1000102e4: b9400c20    	ldr	w0, [x1, #0xc]
1000102e8: 31004000    	adds	w0, w0, #0x10
1000102ec: 54001986    	b.vs	0x10001061c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x36c>
1000102f0: b9400422    	ldr	w2, [x1, #0x4]
1000102f4: 340018c2    	cbz	w2, 0x10001060c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x35c>
1000102f8: f90013e1    	str	x1, [sp, #0x20]
1000102fc: 2a0203e2    	mov	w2, w2
100010300: 8b020f62    	add	x2, x27, x2, lsl #3
100010304: b9400442    	ldr	w2, [x2, #0x4]
100010308: 6b00005f    	cmp	w2, w0
10001030c: 54000d6b    	b.lt	0x1000104b8 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x208>
100010310: b9400820    	ldr	w0, [x1, #0x8]
100010314: 340017c0    	cbz	w0, 0x10001060c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x35c>
100010318: b9401022    	ldr	w2, [x1, #0x10]
10001031c: 2a0003e0    	mov	w0, w0
100010320: 8b000f60    	add	x0, x27, x0, lsl #3
100010324: b9400400    	ldr	w0, [x0, #0x4]
100010328: 6b00005f    	cmp	w2, w0
10001032c: 54000e40    	b.eq	0x1000104f4 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x244>
100010330: b9400820    	ldr	w0, [x1, #0x8]
100010334: 340016c0    	cbz	w0, 0x10001060c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x35c>
100010338: b9401022    	ldr	w2, [x1, #0x10]
10001033c: 2a0003e0    	mov	w0, w0
100010340: 8b000f60    	add	x0, x27, x0, lsl #3
100010344: b9400403    	ldr	w3, [x0, #0x4]
100010348: 6b03005f    	cmp	w2, w3
10001034c: 54001622    	b.hs	0x100010610 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x360>
100010350: b9400c23    	ldr	w3, [x1, #0xc]
100010354: b27d03e4    	orr	x4, xzr, #0x8
100010358: 8b020884    	add	x4, x4, x2, lsl #2
10001035c: b8246803    	str	w3, [x0, x4]
100010360: 11000442    	add	w2, w2, #0x1
100010364: b9001022    	str	w2, [x1, #0x10]
100010368: 528022e0    	mov	w0, #0x117              ; =279
10001036c: 72a00020    	movk	w0, #0x1, lsl #16
100010370: 8b000f60    	add	x0, x27, x0, lsl #3
100010374: aa0003e1    	mov	x1, x0
100010378: 5297e803    	mov	w3, #0xbf40             ; =48960
10001037c: 72a00023    	movk	w3, #0x1, lsl #16
100010380: 8b030f63    	add	x3, x27, x3, lsl #3
100010384: b9403464    	ldr	w4, [x3, #0x34]
100010388: 2a0403e4    	mov	w4, w4
10001038c: 8b040f64    	add	x4, x27, x4, lsl #3
100010390: eb1b009f    	cmp	x4, x27
100010394: 9a8403e4    	csel	x4, xzr, x4, eq
100010398: eb04003f    	cmp	x1, x4
10001039c: 54000b42    	b.hs	0x100010504 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x254>
1000103a0: b940ef81    	ldr	w1, [x28, #0xec]
1000103a4: 34001301    	cbz	w1, 0x100010604 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x354>
1000103a8: 2a0103e1    	mov	w1, w1
1000103ac: 8b010f61    	add	x1, x27, x1, lsl #3
1000103b0: b9400423    	ldr	w3, [x1, #0x4]
1000103b4: 340012c3    	cbz	w3, 0x10001060c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x35c>
1000103b8: b9400c24    	ldr	w4, [x1, #0xc]
1000103bc: 2a0303e3    	mov	w3, w3
1000103c0: 8b030f63    	add	x3, x27, x3, lsl #3
1000103c4: b9400465    	ldr	w5, [x3, #0x4]
1000103c8: 6b05009f    	cmp	w4, w5
1000103cc: b90037e2    	str	w2, [sp, #0x34]
1000103d0: 54000b4b    	b.lt	0x100010538 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x288>
1000103d4: 9403675f    	bl	0x1000ea150 <_JNIObjectHandles_createLocalSlow0_NB9V8PUqTvBLinhdXS6cPD>
1000103d8: aa0003e1    	mov	x1, x0
1000103dc: 9100e3ed    	add	x13, sp, #0x38
1000103e0: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
1000103e4: f9400f80    	ldr	x0, [x28, #0x18]
1000103e8: f9000da0    	str	x0, [x13, #0x18]
1000103ec: f9000f8d    	str	x13, [x28, #0x18]
1000103f0: 9000209e    	adrp	x30, 0x100420000 <___data>
1000103f4: f9425bde    	ldr	x30, [x30, #0x4b0]
1000103f8: aa1c03e0    	mov	x0, x28
1000103fc: f94017e2    	ldr	x2, [sp, #0x28]
100010400: 10000029    	adr	x9, 0x100010404 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x154>
100010404: f90005a9    	str	x9, [x13, #0x8]
100010408: 910003e9    	mov	x9, sp
10001040c: f90009a9    	str	x9, [x13, #0x10]
100010410: 320007e9    	orr	w9, wzr, #0x3
100010414: 9100538a    	add	x10, x28, #0x14
100010418: 889ffd49    	stlr	w9, [x10]
10001041c: d63f03c0    	blr	x30
100010420: 320007e1    	orr	w1, wzr, #0x3
100010424: 320003e2    	orr	w2, wzr, #0x1
100010428: 91005383    	add	x3, x28, #0x14
10001042c: 885f7c64    	ldxr	w4, [x3]
100010430: 6b01009f    	cmp	w4, w1
100010434: 1a84004a    	csel	w10, w2, w4, eq
100010438: 8809fc6a    	stlxr	w9, w10, [x3]
10001043c: 35ffff89    	cbnz	w9, 0x10001042c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x17c>
100010440: d5033bbf    	dmb	ish
100010444: 1a9f17e1    	cset	w1, eq
100010448: 34000ce1    	cbz	w1, 0x1000105e4 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x334>
10001044c: f9400f81    	ldr	x1, [x28, #0x18]
100010450: f9400c21    	ldr	x1, [x1, #0x18]
100010454: f9000f81    	str	x1, [x28, #0x18]
100010458: b9402f81    	ldr	w1, [x28, #0x2c]
10001045c: 35000ce1    	cbnz	w1, 0x1000105f8 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x348>
100010460: f90017e0    	str	x0, [sp, #0x28]
100010464: b94037e0    	ldr	w0, [sp, #0x34]
100010468: 940366ae    	bl	0x1000e9f20 <_JNIMethodSupport_nativeCallEpilogue_qd5OT3R9Y61aJqFSQx6Ix5>
10001046c: 94036729    	bl	0x1000ea110 <_JNIMethodSupport_rethrowPendingException_88jcaUwVPTB7xJeYe11hz7>
100010470: f94017e0    	ldr	x0, [sp, #0x28]
100010474: a9467bfd    	ldp	x29, x30, [sp, #0x60]
100010478: 9101c3ff    	add	sp, sp, #0x70
10001047c: b9401389    	ldr	w9, [x28, #0x10]
100010480: 7100013f    	cmp	w9, #0x0
100010484: 54000d0d    	b.le	0x100010624 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x374>
100010488: d65f03c0    	ret
10001048c: 5284dca1    	mov	w1, #0x26e5             ; =9957
100010490: 72a000a1    	movk	w1, #0x5, lsl #16
100010494: 8b010f61    	add	x1, x27, x1, lsl #3
100010498: f9401022    	ldr	x2, [x1, #0x20]
10001049c: b40006a2    	cbz	x2, 0x100010570 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x2c0>
1000104a0: f9000382    	str	x2, [x28]
1000104a4: 17ffff8b    	b	0x1000102d0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x20>
1000104a8: f90017e0    	str	x0, [sp, #0x28]
1000104ac: 94036795    	bl	0x1000ea300 <_JNIObjectHandles_createLocals_FkbjZiTnc6BikFhRQBvMlF>
1000104b0: aa0003e1    	mov	x1, x0
1000104b4: 17ffff8c    	b	0x1000102e4 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x34>
1000104b8: 321f03e3    	orr	w3, wzr, #0x2
1000104bc: 9b237c49    	smull	x9, w2, w3
1000104c0: 2a0903e2    	mov	w2, w9
1000104c4: eb29c13f    	cmp	x9, w9, sxtw
1000104c8: 320103e9    	orr	w9, wzr, #0x80000000
1000104cc: 1a9f1129    	csel	w9, w9, wzr, ne
1000104d0: 7100053f    	cmp	w9, #0x1
1000104d4: 54000a06    	b.vs	0x100010614 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x364>
1000104d8: 6b02001f    	cmp	w0, w2
1000104dc: 1a82a002    	csel	w2, w0, w2, ge
1000104e0: aa0103e0    	mov	x0, x1
1000104e4: 2a0203e1    	mov	w1, w2
1000104e8: 94069d4e    	bl	0x1001b7a20 <_ThreadLocalHandles_growCapacity_L2BNYcp1FSKj2L5gNje8MH>
1000104ec: f94013e1    	ldr	x1, [sp, #0x20]
1000104f0: 17ffff88    	b	0x100010310 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x60>
1000104f4: aa0103e0    	mov	x0, x1
1000104f8: 94069d76    	bl	0x1001b7ad0 <_ThreadLocalHandles_growFrameStack_EjnMPHGrQn2QLWuk4IFLDF>
1000104fc: f94013e1    	ldr	x1, [sp, #0x20]
100010500: 17ffff8c    	b	0x100010330 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x80>
100010504: b9403863    	ldr	w3, [x3, #0x38]
100010508: 2a0303e3    	mov	w3, w3
10001050c: 8b030f63    	add	x3, x27, x3, lsl #3
100010510: eb1b007f    	cmp	x3, x27
100010514: 9a8303e3    	csel	x3, xzr, x3, eq
100010518: eb01007f    	cmp	x3, x1
10001051c: 54fff423    	b.lo	0x1000103a0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0xf0>
100010520: cb1b0000    	sub	x0, x0, x27
100010524: b26003e1    	orr	x1, xzr, #0x100000000
100010528: 8b010000    	add	x0, x0, x1
10001052c: aa0003e1    	mov	x1, x0
100010530: b90037e2    	str	w2, [sp, #0x34]
100010534: 17ffffaa    	b	0x1000103dc <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x12c>
100010538: 6b05009f    	cmp	w4, w5
10001053c: 540006a2    	b.hs	0x100010610 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x360>
100010540: b27d03e2    	orr	x2, xzr, #0x8
100010544: 8b040842    	add	x2, x2, x4, lsl #2
100010548: 528022fe    	mov	w30, #0x117             ; =279
10001054c: 72a0003e    	movk	w30, #0x1, lsl #16
100010550: b822687e    	str	w30, [x3, x2]
100010554: b940007e    	ldr	w30, [x3]
100010558: 3708019e    	tbnz	w30, #0x1, 0x100010588 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x2d8>
10001055c: 11000482    	add	w2, w4, #0x1
100010560: b9000c22    	str	w2, [x1, #0xc]
100010564: 34fff384    	cbz	w4, 0x1000103d4 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x124>
100010568: aa0403e1    	mov	x1, x4
10001056c: 17ffff9c    	b	0x1000103dc <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x12c>
100010570: 5298c822    	mov	w2, #0xc641             ; =50753
100010574: 72a00082    	movk	w2, #0x4, lsl #16
100010578: 8b020f62    	add	x2, x27, x2, lsl #3
10001057c: 91002042    	add	x2, x2, #0x8
100010580: f9001022    	str	x2, [x1, #0x20]
100010584: 17ffffc7    	b	0x1000104a0 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x1f0>
100010588: aa0303ed    	mov	x13, x3
10001058c: 3700017e    	tbnz	w30, #0x0, 0x1000105b8 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x308>
100010590: b26db3e2    	mov	x2, #-0x80000           ; =-524288
100010594: 8a0201a2    	and	x2, x13, x2
100010598: 9121005e    	add	x30, x2, #0x840
10001059c: cb1e01be    	sub	x30, x13, x30
1000105a0: b27d0bed    	orr	x13, xzr, #0x38
1000105a4: 8b5e25be    	add	x30, x13, x30, lsr #9
1000105a8: 38fe684d    	ldrsb	w13, [x2, x30]
1000105ac: 34fffd8d    	cbz	w13, 0x10001055c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x2ac>
1000105b0: 383e685f    	strb	wzr, [x2, x30]
1000105b4: 17ffffea    	b	0x10001055c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x2ac>
1000105b8: f85f81be    	ldur	x30, [x13, #-0x8]
1000105bc: aa0303e5    	mov	x5, x3
1000105c0: cb1e01be    	sub	x30, x13, x30
1000105c4: 8b020062    	add	x2, x3, x2
1000105c8: cb050042    	sub	x2, x2, x5
1000105cc: b27c07ed    	orr	x13, xzr, #0x30
1000105d0: 8b4225a2    	add	x2, x13, x2, lsr #9
1000105d4: 38e26bcd    	ldrsb	w13, [x30, x2]
1000105d8: 34fffc2d    	cbz	w13, 0x10001055c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x2ac>
1000105dc: 38226bdf    	strb	wzr, [x30, x2]
1000105e0: 17ffffdf    	b	0x10001055c <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x2ac>
1000105e4: f90017e0    	str	x0, [sp, #0x28]
1000105e8: 320003e0    	orr	w0, wzr, #0x1
1000105ec: 320003e1    	orr	w1, wzr, #0x1
1000105f0: 9405e9a0    	bl	0x10018ac70 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
1000105f4: 17ffff9c    	b	0x100010464 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x1b4>
1000105f8: f90017e0    	str	x0, [sp, #0x28]
1000105fc: 9405e931    	bl	0x10018aac0 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
100010600: 17ffff99    	b	0x100010464 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x1b4>
100010604: b90037e2    	str	w2, [sp, #0x34]
100010608: 17ffff73    	b	0x1000103d4 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x124>
10001060c: 94059afd    	bl	0x100177200 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
100010610: 94059b9c    	bl	0x100177480 <_ImplicitExceptions_throwOptOutOfBoundsException_1u8LgzEFx5HG3Dm1iVN34B>
100010614: 32007be2    	orr	w2, wzr, #0x7fffffff
100010618: 17ffffb0    	b	0x1000104d8 <_GeneratedJNI_step_65N4HKhdLh1yVuebbVkuy5+0x228>
10001061c: 940594d5    	bl	0x100175970 <_ImplicitExceptions_throwNewIntegerOverflowException_z9qEBLHeFx7nfnCFFA4vx0>
100010620: 14028e6c    	b	0x1000b3fd0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010624: 1405e757    	b	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010628: cccccccc    	<unknown>
10001062c: cccccccc    	<unknown>


; native/manual-jni-java-stub
0000000100010690 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2>:
100010690: d101c3e9    	sub	x9, sp, #0x70
100010694: f940078a    	ldr	x10, [x28, #0x8]
100010698: eb0a013f    	cmp	x9, x10
10001069c: 54001b29    	b.ls	0x100010a00 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x370>
1000106a0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
1000106a4: 9100013f    	mov	sp, x9
1000106a8: f9400381    	ldr	x1, [x28]
1000106ac: b4000e01    	cbz	x1, 0x10001086c <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x1dc>
1000106b0: b940ef81    	ldr	w1, [x28, #0xec]
1000106b4: 34000ea1    	cbz	w1, 0x100010888 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x1f8>
1000106b8: 2a0103e1    	mov	w1, w1
1000106bc: 8b010f61    	add	x1, x27, x1, lsl #3
1000106c0: f90017e0    	str	x0, [sp, #0x28]
1000106c4: b9400c20    	ldr	w0, [x1, #0xc]
1000106c8: 31004000    	adds	w0, w0, #0x10
1000106cc: 54001986    	b.vs	0x1000109fc <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x36c>
1000106d0: b9400422    	ldr	w2, [x1, #0x4]
1000106d4: 340018c2    	cbz	w2, 0x1000109ec <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x35c>
1000106d8: f90013e1    	str	x1, [sp, #0x20]
1000106dc: 2a0203e2    	mov	w2, w2
1000106e0: 8b020f62    	add	x2, x27, x2, lsl #3
1000106e4: b9400442    	ldr	w2, [x2, #0x4]
1000106e8: 6b00005f    	cmp	w2, w0
1000106ec: 54000d6b    	b.lt	0x100010898 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x208>
1000106f0: b9400820    	ldr	w0, [x1, #0x8]
1000106f4: 340017c0    	cbz	w0, 0x1000109ec <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x35c>
1000106f8: b9401022    	ldr	w2, [x1, #0x10]
1000106fc: 2a0003e0    	mov	w0, w0
100010700: 8b000f60    	add	x0, x27, x0, lsl #3
100010704: b9400400    	ldr	w0, [x0, #0x4]
100010708: 6b00005f    	cmp	w2, w0
10001070c: 54000e40    	b.eq	0x1000108d4 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x244>
100010710: b9400820    	ldr	w0, [x1, #0x8]
100010714: 340016c0    	cbz	w0, 0x1000109ec <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x35c>
100010718: b9401022    	ldr	w2, [x1, #0x10]
10001071c: 2a0003e0    	mov	w0, w0
100010720: 8b000f60    	add	x0, x27, x0, lsl #3
100010724: b9400403    	ldr	w3, [x0, #0x4]
100010728: 6b03005f    	cmp	w2, w3
10001072c: 54001622    	b.hs	0x1000109f0 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x360>
100010730: b9400c23    	ldr	w3, [x1, #0xc]
100010734: b27d03e4    	orr	x4, xzr, #0x8
100010738: 8b020884    	add	x4, x4, x2, lsl #2
10001073c: b8246803    	str	w3, [x0, x4]
100010740: 11000442    	add	w2, w2, #0x1
100010744: b9001022    	str	w2, [x1, #0x10]
100010748: 52802420    	mov	w0, #0x121              ; =289
10001074c: 72a00020    	movk	w0, #0x1, lsl #16
100010750: 8b000f60    	add	x0, x27, x0, lsl #3
100010754: aa0003e1    	mov	x1, x0
100010758: 5297e803    	mov	w3, #0xbf40             ; =48960
10001075c: 72a00023    	movk	w3, #0x1, lsl #16
100010760: 8b030f63    	add	x3, x27, x3, lsl #3
100010764: b9403464    	ldr	w4, [x3, #0x34]
100010768: 2a0403e4    	mov	w4, w4
10001076c: 8b040f64    	add	x4, x27, x4, lsl #3
100010770: eb1b009f    	cmp	x4, x27
100010774: 9a8403e4    	csel	x4, xzr, x4, eq
100010778: eb04003f    	cmp	x1, x4
10001077c: 54000b42    	b.hs	0x1000108e4 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x254>
100010780: b940ef81    	ldr	w1, [x28, #0xec]
100010784: 34001301    	cbz	w1, 0x1000109e4 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x354>
100010788: 2a0103e1    	mov	w1, w1
10001078c: 8b010f61    	add	x1, x27, x1, lsl #3
100010790: b9400423    	ldr	w3, [x1, #0x4]
100010794: 340012c3    	cbz	w3, 0x1000109ec <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x35c>
100010798: b9400c24    	ldr	w4, [x1, #0xc]
10001079c: 2a0303e3    	mov	w3, w3
1000107a0: 8b030f63    	add	x3, x27, x3, lsl #3
1000107a4: b9400465    	ldr	w5, [x3, #0x4]
1000107a8: 6b05009f    	cmp	w4, w5
1000107ac: b90037e2    	str	w2, [sp, #0x34]
1000107b0: 54000b4b    	b.lt	0x100010918 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x288>
1000107b4: 94036667    	bl	0x1000ea150 <_JNIObjectHandles_createLocalSlow0_NB9V8PUqTvBLinhdXS6cPD>
1000107b8: aa0003e1    	mov	x1, x0
1000107bc: 9100e3ed    	add	x13, sp, #0x38
1000107c0: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
1000107c4: f9400f80    	ldr	x0, [x28, #0x18]
1000107c8: f9000da0    	str	x0, [x13, #0x18]
1000107cc: f9000f8d    	str	x13, [x28, #0x18]
1000107d0: 9000209e    	adrp	x30, 0x100420000 <___data>
1000107d4: f9408fde    	ldr	x30, [x30, #0x118]
1000107d8: aa1c03e0    	mov	x0, x28
1000107dc: f94017e2    	ldr	x2, [sp, #0x28]
1000107e0: 10000029    	adr	x9, 0x1000107e4 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x154>
1000107e4: f90005a9    	str	x9, [x13, #0x8]
1000107e8: 910003e9    	mov	x9, sp
1000107ec: f90009a9    	str	x9, [x13, #0x10]
1000107f0: 320007e9    	orr	w9, wzr, #0x3
1000107f4: 9100538a    	add	x10, x28, #0x14
1000107f8: 889ffd49    	stlr	w9, [x10]
1000107fc: d63f03c0    	blr	x30
100010800: 320007e1    	orr	w1, wzr, #0x3
100010804: 320003e2    	orr	w2, wzr, #0x1
100010808: 91005383    	add	x3, x28, #0x14
10001080c: 885f7c64    	ldxr	w4, [x3]
100010810: 6b01009f    	cmp	w4, w1
100010814: 1a84004a    	csel	w10, w2, w4, eq
100010818: 8809fc6a    	stlxr	w9, w10, [x3]
10001081c: 35ffff89    	cbnz	w9, 0x10001080c <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x17c>
100010820: d5033bbf    	dmb	ish
100010824: 1a9f17e1    	cset	w1, eq
100010828: 34000ce1    	cbz	w1, 0x1000109c4 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x334>
10001082c: f9400f81    	ldr	x1, [x28, #0x18]
100010830: f9400c21    	ldr	x1, [x1, #0x18]
100010834: f9000f81    	str	x1, [x28, #0x18]
100010838: b9402f81    	ldr	w1, [x28, #0x2c]
10001083c: 35000ce1    	cbnz	w1, 0x1000109d8 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x348>
100010840: f90017e0    	str	x0, [sp, #0x28]
100010844: b94037e0    	ldr	w0, [sp, #0x34]
100010848: 940365b6    	bl	0x1000e9f20 <_JNIMethodSupport_nativeCallEpilogue_qd5OT3R9Y61aJqFSQx6Ix5>
10001084c: 94036631    	bl	0x1000ea110 <_JNIMethodSupport_rethrowPendingException_88jcaUwVPTB7xJeYe11hz7>
100010850: f94017e0    	ldr	x0, [sp, #0x28]
100010854: a9467bfd    	ldp	x29, x30, [sp, #0x60]
100010858: 9101c3ff    	add	sp, sp, #0x70
10001085c: b9401389    	ldr	w9, [x28, #0x10]
100010860: 7100013f    	cmp	w9, #0x0
100010864: 54000d0d    	b.le	0x100010a04 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x374>
100010868: d65f03c0    	ret
10001086c: 5284dca1    	mov	w1, #0x26e5             ; =9957
100010870: 72a000a1    	movk	w1, #0x5, lsl #16
100010874: 8b010f61    	add	x1, x27, x1, lsl #3
100010878: f9401022    	ldr	x2, [x1, #0x20]
10001087c: b40006a2    	cbz	x2, 0x100010950 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x2c0>
100010880: f9000382    	str	x2, [x28]
100010884: 17ffff8b    	b	0x1000106b0 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x20>
100010888: f90017e0    	str	x0, [sp, #0x28]
10001088c: 9403669d    	bl	0x1000ea300 <_JNIObjectHandles_createLocals_FkbjZiTnc6BikFhRQBvMlF>
100010890: aa0003e1    	mov	x1, x0
100010894: 17ffff8c    	b	0x1000106c4 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x34>
100010898: 321f03e3    	orr	w3, wzr, #0x2
10001089c: 9b237c49    	smull	x9, w2, w3
1000108a0: 2a0903e2    	mov	w2, w9
1000108a4: eb29c13f    	cmp	x9, w9, sxtw
1000108a8: 320103e9    	orr	w9, wzr, #0x80000000
1000108ac: 1a9f1129    	csel	w9, w9, wzr, ne
1000108b0: 7100053f    	cmp	w9, #0x1
1000108b4: 54000a06    	b.vs	0x1000109f4 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x364>
1000108b8: 6b02001f    	cmp	w0, w2
1000108bc: 1a82a002    	csel	w2, w0, w2, ge
1000108c0: aa0103e0    	mov	x0, x1
1000108c4: 2a0203e1    	mov	w1, w2
1000108c8: 94069c56    	bl	0x1001b7a20 <_ThreadLocalHandles_growCapacity_L2BNYcp1FSKj2L5gNje8MH>
1000108cc: f94013e1    	ldr	x1, [sp, #0x20]
1000108d0: 17ffff88    	b	0x1000106f0 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x60>
1000108d4: aa0103e0    	mov	x0, x1
1000108d8: 94069c7e    	bl	0x1001b7ad0 <_ThreadLocalHandles_growFrameStack_EjnMPHGrQn2QLWuk4IFLDF>
1000108dc: f94013e1    	ldr	x1, [sp, #0x20]
1000108e0: 17ffff8c    	b	0x100010710 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x80>
1000108e4: b9403863    	ldr	w3, [x3, #0x38]
1000108e8: 2a0303e3    	mov	w3, w3
1000108ec: 8b030f63    	add	x3, x27, x3, lsl #3
1000108f0: eb1b007f    	cmp	x3, x27
1000108f4: 9a8303e3    	csel	x3, xzr, x3, eq
1000108f8: eb01007f    	cmp	x3, x1
1000108fc: 54fff423    	b.lo	0x100010780 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0xf0>
100010900: cb1b0000    	sub	x0, x0, x27
100010904: b26003e1    	orr	x1, xzr, #0x100000000
100010908: 8b010000    	add	x0, x0, x1
10001090c: aa0003e1    	mov	x1, x0
100010910: b90037e2    	str	w2, [sp, #0x34]
100010914: 17ffffaa    	b	0x1000107bc <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x12c>
100010918: 6b05009f    	cmp	w4, w5
10001091c: 540006a2    	b.hs	0x1000109f0 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x360>
100010920: b27d03e2    	orr	x2, xzr, #0x8
100010924: 8b040842    	add	x2, x2, x4, lsl #2
100010928: 5280243e    	mov	w30, #0x121             ; =289
10001092c: 72a0003e    	movk	w30, #0x1, lsl #16
100010930: b822687e    	str	w30, [x3, x2]
100010934: b940007e    	ldr	w30, [x3]
100010938: 3708019e    	tbnz	w30, #0x1, 0x100010968 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x2d8>
10001093c: 11000482    	add	w2, w4, #0x1
100010940: b9000c22    	str	w2, [x1, #0xc]
100010944: 34fff384    	cbz	w4, 0x1000107b4 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x124>
100010948: aa0403e1    	mov	x1, x4
10001094c: 17ffff9c    	b	0x1000107bc <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x12c>
100010950: 5298c822    	mov	w2, #0xc641             ; =50753
100010954: 72a00082    	movk	w2, #0x4, lsl #16
100010958: 8b020f62    	add	x2, x27, x2, lsl #3
10001095c: 91002042    	add	x2, x2, #0x8
100010960: f9001022    	str	x2, [x1, #0x20]
100010964: 17ffffc7    	b	0x100010880 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x1f0>
100010968: aa0303ed    	mov	x13, x3
10001096c: 3700017e    	tbnz	w30, #0x0, 0x100010998 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x308>
100010970: b26db3e2    	mov	x2, #-0x80000           ; =-524288
100010974: 8a0201a2    	and	x2, x13, x2
100010978: 9121005e    	add	x30, x2, #0x840
10001097c: cb1e01be    	sub	x30, x13, x30
100010980: b27d0bed    	orr	x13, xzr, #0x38
100010984: 8b5e25be    	add	x30, x13, x30, lsr #9
100010988: 38fe684d    	ldrsb	w13, [x2, x30]
10001098c: 34fffd8d    	cbz	w13, 0x10001093c <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x2ac>
100010990: 383e685f    	strb	wzr, [x2, x30]
100010994: 17ffffea    	b	0x10001093c <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x2ac>
100010998: f85f81be    	ldur	x30, [x13, #-0x8]
10001099c: aa0303e5    	mov	x5, x3
1000109a0: cb1e01be    	sub	x30, x13, x30
1000109a4: 8b020062    	add	x2, x3, x2
1000109a8: cb050042    	sub	x2, x2, x5
1000109ac: b27c07ed    	orr	x13, xzr, #0x30
1000109b0: 8b4225a2    	add	x2, x13, x2, lsr #9
1000109b4: 38e26bcd    	ldrsb	w13, [x30, x2]
1000109b8: 34fffc2d    	cbz	w13, 0x10001093c <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x2ac>
1000109bc: 38226bdf    	strb	wzr, [x30, x2]
1000109c0: 17ffffdf    	b	0x10001093c <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x2ac>
1000109c4: f90017e0    	str	x0, [sp, #0x28]
1000109c8: 320003e0    	orr	w0, wzr, #0x1
1000109cc: 320003e1    	orr	w1, wzr, #0x1
1000109d0: 9405e8a8    	bl	0x10018ac70 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
1000109d4: 17ffff9c    	b	0x100010844 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x1b4>
1000109d8: f90017e0    	str	x0, [sp, #0x28]
1000109dc: 9405e839    	bl	0x10018aac0 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
1000109e0: 17ffff99    	b	0x100010844 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x1b4>
1000109e4: b90037e2    	str	w2, [sp, #0x34]
1000109e8: 17ffff73    	b	0x1000107b4 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x124>
1000109ec: 94059a05    	bl	0x100177200 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
1000109f0: 94059aa4    	bl	0x100177480 <_ImplicitExceptions_throwOptOutOfBoundsException_1u8LgzEFx5HG3Dm1iVN34B>
1000109f4: 32007be2    	orr	w2, wzr, #0x7fffffff
1000109f8: 17ffffb0    	b	0x1000108b8 <_ManualJNI_step_yd0RaFLlk2AZlVxajNzkV2+0x228>
1000109fc: 940593dd    	bl	0x100175970 <_ImplicitExceptions_throwNewIntegerOverflowException_z9qEBLHeFx7nfnCFFA4vx0>
100010a00: 14028d74    	b	0x1000b3fd0 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100010a04: 1405e65f    	b	0x10018a380 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100010a08: cccccccc    	<unknown>
100010a0c: cccccccc    	<unknown>


; native/generated-jni-c-wrapper
00000001003fe0b4 <_Java_bench_GeneratedJNI_step__J>:
1003fe0b4: a9be4ff4    	stp	x20, x19, [sp, #-0x20]!
1003fe0b8: a9017bfd    	stp	x29, x30, [sp, #0x10]
1003fe0bc: 910043fd    	add	x29, sp, #0x10
1003fe0c0: aa0203f3    	mov	x19, x2
1003fe0c4: f9400008    	ldr	x8, [x0]
1003fe0c8: f9439108    	ldr	x8, [x8, #0x720]
1003fe0cc: d63f0100    	blr	x8
1003fe0d0: 340000a0    	cbz	w0, 0x1003fe0e4 <_Java_bench_GeneratedJNI_step__J+0x30>
1003fe0d4: d2800000    	mov	x0, #0x0                ; =0
1003fe0d8: a9417bfd    	ldp	x29, x30, [sp, #0x10]
1003fe0dc: a8c24ff4    	ldp	x20, x19, [sp], #0x20
1003fe0e0: d65f03c0    	ret
1003fe0e4: aa1303e0    	mov	x0, x19
1003fe0e8: a9417bfd    	ldp	x29, x30, [sp, #0x10]
1003fe0ec: a8c24ff4    	ldp	x20, x19, [sp], #0x20
1003fe0f0: 17ffb619    	b	0x1003eb954 <_myna_bench_step>


; native/manual-jni-c-wrapper
00000001003fe0f4 <_Java_bench_ManualJNI_step>:
1003fe0f4: a9be4ff4    	stp	x20, x19, [sp, #-0x20]!
1003fe0f8: a9017bfd    	stp	x29, x30, [sp, #0x10]
1003fe0fc: 910043fd    	add	x29, sp, #0x10
1003fe100: aa0203f3    	mov	x19, x2
1003fe104: f9400008    	ldr	x8, [x0]
1003fe108: f9439108    	ldr	x8, [x8, #0x720]
1003fe10c: d63f0100    	blr	x8
1003fe110: 340000a0    	cbz	w0, 0x1003fe124 <_Java_bench_ManualJNI_step+0x30>
1003fe114: d2800000    	mov	x0, #0x0                ; =0
1003fe118: a9417bfd    	ldp	x29, x30, [sp, #0x10]
1003fe11c: a8c24ff4    	ldp	x20, x19, [sp], #0x20
1003fe120: d65f03c0    	ret
1003fe124: aa1303e0    	mov	x0, x19
1003fe128: a9417bfd    	ldp	x29, x30, [sp, #0x10]
1003fe12c: a8c24ff4    	ldp	x20, x19, [sp], #0x20
1003fe130: 17ffb609    	b	0x1003eb954 <_myna_bench_step>


; ffm-aot/generated-ffm
0000000100011b90 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ>:
100011b90: d102c3e9    	sub	x9, sp, #0xb0
100011b94: f940078a    	ldr	x10, [x28, #0x8]
100011b98: eb0a013f    	cmp	x9, x10
100011b9c: 54002dc9    	b.ls	0x100012154 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x5c4>
100011ba0: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011ba4: 9100013f    	mov	sp, x9
100011ba8: b9006fe1    	str	w1, [sp, #0x6c]
100011bac: 52847a40    	mov	w0, #0x23d2             ; =9170
100011bb0: 72a00120    	movk	w0, #0x9, lsl #16
100011bb4: 8b000f60    	add	x0, x27, x0, lsl #3
100011bb8: 91008409    	add	x9, x0, #0x21
100011bbc: 08dffd20    	ldarb	w0, [x9]
100011bc0: 35002c00    	cbnz	w0, 0x100012140 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x5b0>
100011bc4: 52806163    	mov	w3, #0x30b              ; =779
100011bc8: 72a00023    	movk	w3, #0x1, lsl #16
100011bcc: b9005be3    	str	w3, [sp, #0x58]
100011bd0: 528063c4    	mov	w4, #0x31e              ; =798
100011bd4: 72a00024    	movk	w4, #0x1, lsl #16
100011bd8: b90057e4    	str	w4, [sp, #0x54]
100011bdc: 32007be5    	orr	w5, wzr, #0x7fffffff
100011be0: 52806626    	mov	w6, #0x331              ; =817
100011be4: 72a00026    	movk	w6, #0x1, lsl #16
100011be8: b9004fe6    	str	w6, [sp, #0x4c]
100011bec: 5284e007    	mov	w7, #0x2700             ; =9984
100011bf0: 72a00127    	movk	w7, #0x9, lsl #16
100011bf4: 8b070f67    	add	x7, x27, x7, lsl #3
100011bf8: f90017e7    	str	x7, [sp, #0x28]
100011bfc: 52847988    	mov	w8, #0x23cc             ; =9164
100011c00: 72a00128    	movk	w8, #0x9, lsl #16
100011c04: 8b080f68    	add	x8, x27, x8, lsl #3
100011c08: f9001fe8    	str	x8, [sp, #0x38]
100011c0c: 528478cb    	mov	w11, #0x23c6            ; =9158
100011c10: 72a0012b    	movk	w11, #0x9, lsl #16
100011c14: 8b0b0f6b    	add	x11, x27, x11, lsl #3
100011c18: f9001beb    	str	x11, [sp, #0x30]
100011c1c: 2a1f03ec    	mov	w12, wzr
100011c20: f90033e2    	str	x2, [sp, #0x60]
100011c24: d503201f    	nop
100011c28: d503201f    	nop
100011c2c: d503201f    	nop
100011c30: 6b01019f    	cmp	w12, w1
100011c34: 5400278a    	b.ge	0x100012124 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x594>
100011c38: b90073ec    	str	w12, [sp, #0x70]
100011c3c: 91008569    	add	x9, x11, #0x21
100011c40: 08dffd20    	ldarb	w0, [x9]
100011c44: 35001740    	cbnz	w0, 0x100011f2c <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x39c>
100011c48: 91008509    	add	x9, x8, #0x21
100011c4c: 08dffd20    	ldarb	w0, [x9]
100011c50: 35001820    	cbnz	w0, 0x100011f54 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x3c4>
100011c54: b940d8e0    	ldr	w0, [x7, #0xd8]
100011c58: 2a0003ed    	mov	w13, w0
100011c5c: 8b0d0f6d    	add	x13, x27, x13, lsl #3
100011c60: 340018e0    	cbz	w0, 0x100011f7c <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x3ec>
100011c64: f9400da0    	ldr	x0, [x13, #0x18]
100011c68: b4001c40    	cbz	x0, 0x100011ff0 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x460>
100011c6c: b94005a0    	ldr	w0, [x13, #0x4]
100011c70: 34001c60    	cbz	w0, 0x100011ffc <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x46c>
100011c74: 2a0003e0    	mov	w0, w0
100011c78: 8b000f60    	add	x0, x27, x0, lsl #3
100011c7c: f90023e0    	str	x0, [sp, #0x40]
100011c80: b940000e    	ldr	w14, [x0]
100011c84: b90077ee    	str	w14, [sp, #0x74]
100011c88: 53057dde    	lsr	w30, w14, #5
100011c8c: b90053fe    	str	w30, [sp, #0x50]
100011c90: 53057dcf    	lsr	w15, w14, #5
100011c94: b9005fef    	str	w15, [sp, #0x5c]
100011c98: 6b0601ff    	cmp	w15, w6
100011c9c: 54000a00    	b.eq	0x100011ddc <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x24c>
100011ca0: 6b0403df    	cmp	w30, w4
100011ca4: 54000bc0    	b.eq	0x100011e1c <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x28c>
100011ca8: 6b0303df    	cmp	w30, w3
100011cac: 54000bc0    	b.eq	0x100011e24 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x294>
100011cb0: f90013ed    	str	x13, [sp, #0x20]
100011cb4: 53057dd0    	lsr	w16, w14, #5
100011cb8: 2a1003f0    	mov	w16, w16
100011cbc: 8b100f70    	add	x16, x27, x16, lsl #3
100011cc0: f9403a10    	ldr	x16, [x16, #0x70]
100011cc4: 8b1a0210    	add	x16, x16, x26
100011cc8: aa0003f1    	mov	x17, x0
100011ccc: aa1003fe    	mov	x30, x16
100011cd0: d63f03c0    	blr	x30
100011cd4: f94023e0    	ldr	x0, [sp, #0x40]
100011cd8: f94013ed    	ldr	x13, [sp, #0x20]
100011cdc: 32007be5    	orr	w5, wzr, #0x7fffffff
100011ce0: f9400dbe    	ldr	x30, [x13, #0x18]
100011ce4: 9101e3ed    	add	x13, sp, #0x78
100011ce8: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
100011cec: f9400f81    	ldr	x1, [x28, #0x18]
100011cf0: f9000da1    	str	x1, [x13, #0x18]
100011cf4: f9000f8d    	str	x13, [x28, #0x18]
100011cf8: f94033e0    	ldr	x0, [sp, #0x60]
100011cfc: 10000029    	adr	x9, 0x100011d00 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x170>
100011d00: f90005a9    	str	x9, [x13, #0x8]
100011d04: 910003e9    	mov	x9, sp
100011d08: f90009a9    	str	x9, [x13, #0x10]
100011d0c: 320007e9    	orr	w9, wzr, #0x3
100011d10: 9100538a    	add	x10, x28, #0x14
100011d14: 889ffd49    	stlr	w9, [x10]
100011d18: d63f03c0    	blr	x30
100011d1c: 320007fe    	orr	w30, wzr, #0x3
100011d20: 320003e1    	orr	w1, wzr, #0x1
100011d24: 91005382    	add	x2, x28, #0x14
100011d28: 885f7c43    	ldxr	w3, [x2]
100011d2c: 6b1e007f    	cmp	w3, w30
100011d30: 1a83002a    	csel	w10, w1, w3, eq
100011d34: 8809fc4a    	stlxr	w9, w10, [x2]
100011d38: 35ffff89    	cbnz	w9, 0x100011d28 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x198>
100011d3c: d5033bbf    	dmb	ish
100011d40: 1a9f17fe    	cset	w30, eq
100011d44: 34000e1e    	cbz	w30, 0x100011f04 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x374>
100011d48: f9400f9e    	ldr	x30, [x28, #0x18]
100011d4c: f9400fde    	ldr	x30, [x30, #0x18]
100011d50: f9000f9e    	str	x30, [x28, #0x18]
100011d54: b9402f9e    	ldr	w30, [x28, #0x2c]
100011d58: 35000e3e    	cbnz	w30, 0x100011f1c <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x38c>
100011d5c: b9405fef    	ldr	w15, [sp, #0x5c]
100011d60: b94073ec    	ldr	w12, [sp, #0x70]
100011d64: b9404fe6    	ldr	w6, [sp, #0x4c]
100011d68: 11000581    	add	w1, w12, #0x1
100011d6c: 6b0601ff    	cmp	w15, w6
100011d70: 54000700    	b.eq	0x100011e50 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x2c0>
100011d74: b94053fe    	ldr	w30, [sp, #0x50]
100011d78: b9405be3    	ldr	w3, [sp, #0x58]
100011d7c: 6b0303df    	cmp	w30, w3
100011d80: 54000940    	b.eq	0x100011ea8 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x318>
100011d84: b94057e4    	ldr	w4, [sp, #0x54]
100011d88: 6b0401ff    	cmp	w15, w4
100011d8c: b90053e1    	str	w1, [sp, #0x50]
100011d90: f90013e0    	str	x0, [sp, #0x20]
100011d94: 540004c0    	b.eq	0x100011e2c <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x29c>
100011d98: b94077ee    	ldr	w14, [sp, #0x74]
100011d9c: 53057dde    	lsr	w30, w14, #5
100011da0: 2a1e03fe    	mov	w30, w30
100011da4: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100011da8: f94047de    	ldr	x30, [x30, #0x88]
100011dac: 8b1a03de    	add	x30, x30, x26
100011db0: f94023e0    	ldr	x0, [sp, #0x40]
100011db4: d63f03c0    	blr	x30
100011db8: b94053ec    	ldr	w12, [sp, #0x50]
100011dbc: f94013e9    	ldr	x9, [sp, #0x20]
100011dc0: f90033e9    	str	x9, [sp, #0x60]
100011dc4: a94323eb    	ldp	x11, x8, [sp, #0x30]
100011dc8: f94017e7    	ldr	x7, [sp, #0x28]
100011dcc: b9404fe6    	ldr	w6, [sp, #0x4c]
100011dd0: 294a8fe4    	ldp	w4, w3, [sp, #0x54]
100011dd4: b9406fe1    	ldr	w1, [sp, #0x6c]
100011dd8: 17ffff96    	b	0x100011c30 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0xa0>
100011ddc: f90013ed    	str	x13, [sp, #0x20]
100011de0: 2941780d    	ldp	w13, w30, [x0, #0x8]
100011de4: 3400070d    	cbz	w13, 0x100011ec4 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x334>
100011de8: b9402b81    	ldr	w1, [x28, #0x28]
100011dec: 6b0d003f    	cmp	w1, w13
100011df0: 54001601    	b.ne	0x1000120b0 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x520>
100011df4: 710003df    	cmp	w30, #0x0
100011df8: 5400164b    	b.lt	0x1000120c0 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x530>
100011dfc: 32007be5    	orr	w5, wzr, #0x7fffffff
100011e00: b940101e    	ldr	w30, [x0, #0x10]
100011e04: 6b0503df    	cmp	w30, w5
100011e08: 54000fc0    	b.eq	0x100012000 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x470>
100011e0c: 110007de    	add	w30, w30, #0x1
100011e10: b900101e    	str	w30, [x0, #0x10]
100011e14: f94013ed    	ldr	x13, [sp, #0x20]
100011e18: 17ffffb2    	b	0x100011ce0 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x150>
100011e1c: 32007be5    	orr	w5, wzr, #0x7fffffff
100011e20: 17ffffb0    	b	0x100011ce0 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x150>
100011e24: 32007be5    	orr	w5, wzr, #0x7fffffff
100011e28: 17ffffae    	b	0x100011ce0 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x150>
100011e2c: b94053ec    	ldr	w12, [sp, #0x50]
100011e30: f94013e9    	ldr	x9, [sp, #0x20]
100011e34: f90033e9    	str	x9, [sp, #0x60]
100011e38: a94323eb    	ldp	x11, x8, [sp, #0x30]
100011e3c: f94017e7    	ldr	x7, [sp, #0x28]
100011e40: b9404fe6    	ldr	w6, [sp, #0x4c]
100011e44: 294a8fe4    	ldp	w4, w3, [sp, #0x54]
100011e48: b9406fe1    	ldr	w1, [sp, #0x6c]
100011e4c: 17ffff79    	b	0x100011c30 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0xa0>
100011e50: b90053e1    	str	w1, [sp, #0x50]
100011e54: f90013e0    	str	x0, [sp, #0x20]
100011e58: f94023e0    	ldr	x0, [sp, #0x40]
100011e5c: b9402b81    	ldr	w1, [x28, #0x28]
100011e60: b9400802    	ldr	w2, [x0, #0x8]
100011e64: 6b02003f    	cmp	w1, w2
100011e68: 54000340    	b.eq	0x100011ed0 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x340>
100011e6c: 91005000    	add	x0, x0, #0x14
100011e70: 885f7c01    	ldxr	w1, [x0]
100011e74: 11000429    	add	w9, w1, #0x1
100011e78: 880afc09    	stlxr	w10, w9, [x0]
100011e7c: 35ffffaa    	cbnz	w10, 0x100011e70 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x2e0>
100011e80: d5033bbf    	dmb	ish
100011e84: b94053ec    	ldr	w12, [sp, #0x50]
100011e88: f94013e9    	ldr	x9, [sp, #0x20]
100011e8c: f90033e9    	str	x9, [sp, #0x60]
100011e90: a94323eb    	ldp	x11, x8, [sp, #0x30]
100011e94: f94017e7    	ldr	x7, [sp, #0x28]
100011e98: b9404fe6    	ldr	w6, [sp, #0x4c]
100011e9c: 294a8fe4    	ldp	w4, w3, [sp, #0x54]
100011ea0: b9406fe1    	ldr	w1, [sp, #0x6c]
100011ea4: 17ffff63    	b	0x100011c30 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0xa0>
100011ea8: 2a0103ec    	mov	w12, w1
100011eac: f90033e0    	str	x0, [sp, #0x60]
100011eb0: a94323eb    	ldp	x11, x8, [sp, #0x30]
100011eb4: f94017e7    	ldr	x7, [sp, #0x28]
100011eb8: b94057e4    	ldr	w4, [sp, #0x54]
100011ebc: b9406fe1    	ldr	w1, [sp, #0x6c]
100011ec0: 17ffff5c    	b	0x100011c30 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0xa0>
100011ec4: 710003df    	cmp	w30, #0x0
100011ec8: 54fff9aa    	b.ge	0x100011dfc <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x26c>
100011ecc: 1400006d    	b	0x100012080 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x4f0>
100011ed0: b94053e1    	ldr	w1, [sp, #0x50]
100011ed4: b9401002    	ldr	w2, [x0, #0x10]
100011ed8: 51000442    	sub	w2, w2, #0x1
100011edc: b9001002    	str	w2, [x0, #0x10]
100011ee0: 2a0103ec    	mov	w12, w1
100011ee4: f94013e9    	ldr	x9, [sp, #0x20]
100011ee8: f90033e9    	str	x9, [sp, #0x60]
100011eec: a94323eb    	ldp	x11, x8, [sp, #0x30]
100011ef0: f94017e7    	ldr	x7, [sp, #0x28]
100011ef4: b9404fe6    	ldr	w6, [sp, #0x4c]
100011ef8: 294a8fe4    	ldp	w4, w3, [sp, #0x54]
100011efc: b9406fe1    	ldr	w1, [sp, #0x6c]
100011f00: 17ffff4c    	b	0x100011c30 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0xa0>
100011f04: f90013e0    	str	x0, [sp, #0x20]
100011f08: 320003e0    	orr	w0, wzr, #0x1
100011f0c: 320003e1    	orr	w1, wzr, #0x1
100011f10: 9407bcd0    	bl	0x100201250 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
100011f14: f94013e0    	ldr	x0, [sp, #0x20]
100011f18: 17ffff91    	b	0x100011d5c <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x1cc>
100011f1c: f90013e0    	str	x0, [sp, #0x20]
100011f20: 9407bc60    	bl	0x1002010a0 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
100011f24: f94013e0    	ldr	x0, [sp, #0x20]
100011f28: 17ffff8d    	b	0x100011d5c <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x1cc>
100011f2c: 52802520    	mov	w0, #0x129              ; =297
100011f30: 72a00020    	movk	w0, #0x1, lsl #16
100011f34: 8b000f60    	add	x0, x27, x0, lsl #3
100011f38: 940106da    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100011f3c: b94073ec    	ldr	w12, [sp, #0x70]
100011f40: a94323eb    	ldp	x11, x8, [sp, #0x30]
100011f44: f94017e7    	ldr	x7, [sp, #0x28]
100011f48: b9404fe6    	ldr	w6, [sp, #0x4c]
100011f4c: 294a8fe4    	ldp	w4, w3, [sp, #0x54]
100011f50: 17ffff3e    	b	0x100011c48 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0xb8>
100011f54: 52802660    	mov	w0, #0x133              ; =307
100011f58: 72a00020    	movk	w0, #0x1, lsl #16
100011f5c: 8b000f60    	add	x0, x27, x0, lsl #3
100011f60: 940106d0    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100011f64: b94073ec    	ldr	w12, [sp, #0x70]
100011f68: a94323eb    	ldp	x11, x8, [sp, #0x30]
100011f6c: f94017e7    	ldr	x7, [sp, #0x28]
100011f70: b9404fe6    	ldr	w6, [sp, #0x4c]
100011f74: 294a8fe4    	ldp	w4, w3, [sp, #0x54]
100011f78: 17ffff37    	b	0x100011c54 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0xc4>
100011f7c: a9438381    	ldp	x1, x0, [x28, #0x38]
100011f80: 9100a002    	add	x2, x0, #0x28
100011f84: 528d1003    	mov	w3, #0x6880             ; =26752
100011f88: 72a00403    	movk	w3, #0x20, lsl #16
100011f8c: eb02003f    	cmp	x1, x2
100011f90: 54000b83    	b.lo	0x100012100 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x570>
100011f94: f9002382    	str	x2, [x28, #0x40]
100011f98: b9000003    	str	w3, [x0]
100011f9c: b900041f    	str	wzr, [x0, #0x4]
100011fa0: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
100011fa4: a901fc1f    	stp	xzr, xzr, [x0, #0x18]
100011fa8: f90013ed    	str	x13, [sp, #0x20]
100011fac: 52850061    	mov	w1, #0x2803             ; =10243
100011fb0: 72a00121    	movk	w1, #0x9, lsl #16
100011fb4: 52850022    	mov	w2, #0x2801             ; =10241
100011fb8: 72a00122    	movk	w2, #0x9, lsl #16
100011fbc: 29020801    	stp	w1, w2, [x0, #0x10]
100011fc0: cb1b0001    	sub	x1, x0, x27
100011fc4: d343fc21    	lsr	x1, x1, #3
100011fc8: b9000c01    	str	w1, [x0, #0xc]
100011fcc: d5033abf    	dmb	ishst
100011fd0: aa0003e2    	mov	x2, x0
100011fd4: 2a1f03e1    	mov	w1, wzr
100011fd8: f90023e2    	str	x2, [sp, #0x40]
100011fdc: 940c7715    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100011fe0: f94023e2    	ldr	x2, [sp, #0x40]
100011fe4: 9102c3e1    	add	x1, sp, #0xb0
100011fe8: aa0203e0    	mov	x0, x2
100011fec: 94074011    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
100011ff0: f90013ed    	str	x13, [sp, #0x20]
100011ff4: f94013e0    	ldr	x0, [sp, #0x20]
100011ff8: 94020266    	bl	0x100092990 <_SubstrateForeignUtil_throwSymbolIsNullException_eG3tVnjFmBKOZbMYv5OKpB>
100011ffc: 94076c39    	bl	0x1001ed0e0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
100012000: a9438381    	ldp	x1, x0, [x28, #0x38]
100012004: 91008002    	add	x2, x0, #0x20
100012008: 5287f003    	mov	w3, #0x3f80             ; =16256
10001200c: 72a00403    	movk	w3, #0x20, lsl #16
100012010: eb02003f    	cmp	x1, x2
100012014: 54000823    	b.lo	0x100012118 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x588>
100012018: f9002382    	str	x2, [x28, #0x40]
10001201c: b9000003    	str	w3, [x0]
100012020: b900041f    	str	wzr, [x0, #0x4]
100012024: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
100012028: f9000c1f    	str	xzr, [x0, #0x18]
10001202c: 52850061    	mov	w1, #0x2803             ; =10243
100012030: 72a00121    	movk	w1, #0x9, lsl #16
100012034: 52850022    	mov	w2, #0x2801             ; =10241
100012038: 72a00122    	movk	w2, #0x9, lsl #16
10001203c: 29020801    	stp	w1, w2, [x0, #0x10]
100012040: cb1b0001    	sub	x1, x0, x27
100012044: d343fc21    	lsr	x1, x1, #3
100012048: b9000c01    	str	w1, [x0, #0xc]
10001204c: d5033abf    	dmb	ishst
100012050: aa0003e2    	mov	x2, x0
100012054: 2a1f03e1    	mov	w1, wzr
100012058: f90017e2    	str	x2, [sp, #0x28]
10001205c: 940c76f5    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100012060: 52986a00    	mov	w0, #0xc350             ; =50000
100012064: 72a000e0    	movk	w0, #0x7, lsl #16
100012068: f94017e1    	ldr	x1, [sp, #0x28]
10001206c: b9000820    	str	w0, [x1, #0x8]
100012070: b9400020    	ldr	w0, [x1]
100012074: 370802e0    	tbnz	w0, #0x1, 0x1000120d0 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x540>
100012078: aa0103e2    	mov	x2, x1
10001207c: 17ffffda    	b	0x100011fe4 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x454>
100012080: 52851500    	mov	w0, #0x28a8             ; =10408
100012084: 72a00120    	movk	w0, #0x9, lsl #16
100012088: 8b000f60    	add	x0, x27, x0, lsl #3
10001208c: b940001e    	ldr	w30, [x0]
100012090: 53057fde    	lsr	w30, w30, #5
100012094: 2a1e03fe    	mov	w30, w30
100012098: 8b1e0f7e    	add	x30, x27, x30, lsl #3
10001209c: f9403fde    	ldr	x30, [x30, #0x78]
1000120a0: 8b1a03de    	add	x30, x30, x26
1000120a4: d63f03c0    	blr	x30
1000120a8: aa0003e2    	mov	x2, x0
1000120ac: 17ffffce    	b	0x100011fe4 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x454>
1000120b0: 52851540    	mov	w0, #0x28aa             ; =10410
1000120b4: 72a00120    	movk	w0, #0x9, lsl #16
1000120b8: 8b000f60    	add	x0, x27, x0, lsl #3
1000120bc: 17fffff4    	b	0x10001208c <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x4fc>
1000120c0: 52851500    	mov	w0, #0x28a8             ; =10408
1000120c4: 72a00120    	movk	w0, #0x9, lsl #16
1000120c8: 8b000f60    	add	x0, x27, x0, lsl #3
1000120cc: 17fffff0    	b	0x10001208c <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x4fc>
1000120d0: aa0103e0    	mov	x0, x1
1000120d4: b26db3e2    	mov	x2, #-0x80000           ; =-524288
1000120d8: 8a020002    	and	x2, x0, x2
1000120dc: 91210043    	add	x3, x2, #0x840
1000120e0: cb030000    	sub	x0, x0, x3
1000120e4: b27d0be3    	orr	x3, xzr, #0x38
1000120e8: 8b402460    	add	x0, x3, x0, lsr #9
1000120ec: 38e06843    	ldrsb	w3, [x2, x0]
1000120f0: 34000103    	cbz	w3, 0x100012110 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x580>
1000120f4: 3820685f    	strb	wzr, [x2, x0]
1000120f8: aa0103e2    	mov	x2, x1
1000120fc: 17ffffba    	b	0x100011fe4 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x454>
100012100: f90013ed    	str	x13, [sp, #0x20]
100012104: 92407c60    	and	x0, x3, #0xffffffff
100012108: 9402e626    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
10001210c: 17ffffa8    	b	0x100011fac <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x41c>
100012110: aa0103e2    	mov	x2, x1
100012114: 17ffffb4    	b	0x100011fe4 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x454>
100012118: 92407c60    	and	x0, x3, #0xffffffff
10001211c: 9402e621    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100012120: 17ffffc3    	b	0x10001202c <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x49c>
100012124: f94033e0    	ldr	x0, [sp, #0x60]
100012128: a94a7bfd    	ldp	x29, x30, [sp, #0xa0]
10001212c: 9102c3ff    	add	sp, sp, #0xb0
100012130: b9401389    	ldr	w9, [x28, #0x10]
100012134: 7100013f    	cmp	w9, #0x0
100012138: 5400010d    	b.le	0x100012158 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x5c8>
10001213c: d65f03c0    	ret
100012140: 52806020    	mov	w0, #0x301              ; =769
100012144: 72a00020    	movk	w0, #0x1, lsl #16
100012148: 8b000f60    	add	x0, x27, x0, lsl #3
10001214c: 94010655    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012150: 17fffe9d    	b	0x100011bc4 <_Jvm$GeneratedFfm_run_cp4hy7MCrhGq08R0QTjjBJ+0x34>
100012154: 14031b07    	b	0x1000d8d70 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100012158: 1407ba02    	b	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001215c: cccccccc    	<unknown>


; ffm-aot/manual-ffm
0000000100013c20 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5>:
100013c20: d10283e9    	sub	x9, sp, #0xa0
100013c24: f940078a    	ldr	x10, [x28, #0x8]
100013c28: eb0a013f    	cmp	x9, x10
100013c2c: 540028c9    	b.ls	0x100014144 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x524>
100013c30: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100013c34: 9100013f    	mov	sp, x9
100013c38: b9005fe1    	str	w1, [sp, #0x5c]
100013c3c: 52847a40    	mov	w0, #0x23d2             ; =9170
100013c40: 72a00120    	movk	w0, #0x9, lsl #16
100013c44: 8b000f60    	add	x0, x27, x0, lsl #3
100013c48: 91008409    	add	x9, x0, #0x21
100013c4c: 08dffd20    	ldarb	w0, [x9]
100013c50: 35002700    	cbnz	w0, 0x100014130 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x510>
100013c54: 52806163    	mov	w3, #0x30b              ; =779
100013c58: 72a00023    	movk	w3, #0x1, lsl #16
100013c5c: b9004be3    	str	w3, [sp, #0x48]
100013c60: 528063c4    	mov	w4, #0x31e              ; =798
100013c64: 72a00024    	movk	w4, #0x1, lsl #16
100013c68: b90047e4    	str	w4, [sp, #0x44]
100013c6c: 32007be5    	orr	w5, wzr, #0x7fffffff
100013c70: 52806626    	mov	w6, #0x331              ; =817
100013c74: 72a00026    	movk	w6, #0x1, lsl #16
100013c78: b90043e6    	str	w6, [sp, #0x40]
100013c7c: 5284e007    	mov	w7, #0x2700             ; =9984
100013c80: 72a00127    	movk	w7, #0x9, lsl #16
100013c84: 8b070f67    	add	x7, x27, x7, lsl #3
100013c88: f90017e7    	str	x7, [sp, #0x28]
100013c8c: 2a1f03e8    	mov	w8, wzr
100013c90: f9002be2    	str	x2, [sp, #0x50]
100013c94: d503201f    	nop
100013c98: d503201f    	nop
100013c9c: d503201f    	nop
100013ca0: 6b01011f    	cmp	w8, w1
100013ca4: 5400238a    	b.ge	0x100014114 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x4f4>
100013ca8: b90063e8    	str	w8, [sp, #0x60]
100013cac: b9409ce0    	ldr	w0, [x7, #0x9c]
100013cb0: 2a0003eb    	mov	w11, w0
100013cb4: 8b0b0f6b    	add	x11, x27, x11, lsl #3
100013cb8: 340015a0    	cbz	w0, 0x100013f6c <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x34c>
100013cbc: f9400d60    	ldr	x0, [x11, #0x18]
100013cc0: b4001900    	cbz	x0, 0x100013fe0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x3c0>
100013cc4: b9400560    	ldr	w0, [x11, #0x4]
100013cc8: 34001920    	cbz	w0, 0x100013fec <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x3cc>
100013ccc: 2a0003e0    	mov	w0, w0
100013cd0: 8b000f60    	add	x0, x27, x0, lsl #3
100013cd4: f9001be0    	str	x0, [sp, #0x30]
100013cd8: b940000c    	ldr	w12, [x0]
100013cdc: b90067ec    	str	w12, [sp, #0x64]
100013ce0: 53057d9e    	lsr	w30, w12, #5
100013ce4: b9003ffe    	str	w30, [sp, #0x3c]
100013ce8: 53057d8d    	lsr	w13, w12, #5
100013cec: b9004fed    	str	w13, [sp, #0x4c]
100013cf0: 6b0601bf    	cmp	w13, w6
100013cf4: 540009e0    	b.eq	0x100013e30 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x210>
100013cf8: 6b0403df    	cmp	w30, w4
100013cfc: 54000b80    	b.eq	0x100013e6c <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x24c>
100013d00: 6b0303df    	cmp	w30, w3
100013d04: 54000b80    	b.eq	0x100013e74 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x254>
100013d08: f90013eb    	str	x11, [sp, #0x20]
100013d0c: 53057d8e    	lsr	w14, w12, #5
100013d10: 2a0e03ee    	mov	w14, w14
100013d14: 8b0e0f6e    	add	x14, x27, x14, lsl #3
100013d18: f94039ce    	ldr	x14, [x14, #0x70]
100013d1c: 8b1a01ce    	add	x14, x14, x26
100013d20: aa0003ef    	mov	x15, x0
100013d24: aa0e03fe    	mov	x30, x14
100013d28: d63f03c0    	blr	x30
100013d2c: f9401be0    	ldr	x0, [sp, #0x30]
100013d30: f94013eb    	ldr	x11, [sp, #0x20]
100013d34: 32007be5    	orr	w5, wzr, #0x7fffffff
100013d38: f9400d7e    	ldr	x30, [x11, #0x18]
100013d3c: 9101a3ed    	add	x13, sp, #0x68
100013d40: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
100013d44: f9400f81    	ldr	x1, [x28, #0x18]
100013d48: f9000da1    	str	x1, [x13, #0x18]
100013d4c: f9000f8d    	str	x13, [x28, #0x18]
100013d50: f9402be0    	ldr	x0, [sp, #0x50]
100013d54: 10000029    	adr	x9, 0x100013d58 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x138>
100013d58: f90005a9    	str	x9, [x13, #0x8]
100013d5c: 910003e9    	mov	x9, sp
100013d60: f90009a9    	str	x9, [x13, #0x10]
100013d64: 320007e9    	orr	w9, wzr, #0x3
100013d68: 9100538a    	add	x10, x28, #0x14
100013d6c: 889ffd49    	stlr	w9, [x10]
100013d70: d63f03c0    	blr	x30
100013d74: 320007fe    	orr	w30, wzr, #0x3
100013d78: 320003e1    	orr	w1, wzr, #0x1
100013d7c: 91005382    	add	x2, x28, #0x14
100013d80: 885f7c43    	ldxr	w3, [x2]
100013d84: 6b1e007f    	cmp	w3, w30
100013d88: 1a83002a    	csel	w10, w1, w3, eq
100013d8c: 8809fc4a    	stlxr	w9, w10, [x2]
100013d90: 35ffff89    	cbnz	w9, 0x100013d80 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x160>
100013d94: d5033bbf    	dmb	ish
100013d98: 1a9f17fe    	cset	w30, eq
100013d9c: 34000d5e    	cbz	w30, 0x100013f44 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x324>
100013da0: f9400f9e    	ldr	x30, [x28, #0x18]
100013da4: f9400fde    	ldr	x30, [x30, #0x18]
100013da8: f9000f9e    	str	x30, [x28, #0x18]
100013dac: b9402f9e    	ldr	w30, [x28, #0x2c]
100013db0: 35000d7e    	cbnz	w30, 0x100013f5c <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x33c>
100013db4: b9404fed    	ldr	w13, [sp, #0x4c]
100013db8: b94063e8    	ldr	w8, [sp, #0x60]
100013dbc: b94043e6    	ldr	w6, [sp, #0x40]
100013dc0: 11000501    	add	w1, w8, #0x1
100013dc4: 6b0601bf    	cmp	w13, w6
100013dc8: 54000760    	b.eq	0x100013eb4 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x294>
100013dcc: b9403ffe    	ldr	w30, [sp, #0x3c]
100013dd0: b9404be3    	ldr	w3, [sp, #0x48]
100013dd4: 6b0303df    	cmp	w30, w3
100013dd8: 54000620    	b.eq	0x100013e9c <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x27c>
100013ddc: b94047e4    	ldr	w4, [sp, #0x44]
100013de0: 6b0401bf    	cmp	w13, w4
100013de4: b9003fe1    	str	w1, [sp, #0x3c]
100013de8: f90013e0    	str	x0, [sp, #0x20]
100013dec: 54000480    	b.eq	0x100013e7c <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x25c>
100013df0: b94067ec    	ldr	w12, [sp, #0x64]
100013df4: 53057d9e    	lsr	w30, w12, #5
100013df8: 2a1e03fe    	mov	w30, w30
100013dfc: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100013e00: f94047de    	ldr	x30, [x30, #0x88]
100013e04: 8b1a03de    	add	x30, x30, x26
100013e08: f9401be0    	ldr	x0, [sp, #0x30]
100013e0c: d63f03c0    	blr	x30
100013e10: b9403fe8    	ldr	w8, [sp, #0x3c]
100013e14: f94013e9    	ldr	x9, [sp, #0x20]
100013e18: f9002be9    	str	x9, [sp, #0x50]
100013e1c: f94017e7    	ldr	x7, [sp, #0x28]
100013e20: 294813e6    	ldp	w6, w4, [sp, #0x40]
100013e24: b9404be3    	ldr	w3, [sp, #0x48]
100013e28: b9405fe1    	ldr	w1, [sp, #0x5c]
100013e2c: 17ffff9d    	b	0x100013ca0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x80>
100013e30: f90013eb    	str	x11, [sp, #0x20]
100013e34: 2941780d    	ldp	w13, w30, [x0, #0x8]
100013e38: 3400068d    	cbz	w13, 0x100013f08 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x2e8>
100013e3c: b9402b81    	ldr	w1, [x28, #0x28]
100013e40: 6b0d003f    	cmp	w1, w13
100013e44: 54001161    	b.ne	0x100014070 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x450>
100013e48: 710003df    	cmp	w30, #0x0
100013e4c: 5400132b    	b.lt	0x1000140b0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x490>
100013e50: 32007be5    	orr	w5, wzr, #0x7fffffff
100013e54: b940101e    	ldr	w30, [x0, #0x10]
100013e58: 6b0503df    	cmp	w30, w5
100013e5c: 54000ca0    	b.eq	0x100013ff0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x3d0>
100013e60: 110007de    	add	w30, w30, #0x1
100013e64: b900101e    	str	w30, [x0, #0x10]
100013e68: 17ffffb4    	b	0x100013d38 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x118>
100013e6c: 32007be5    	orr	w5, wzr, #0x7fffffff
100013e70: 17ffffb2    	b	0x100013d38 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x118>
100013e74: 32007be5    	orr	w5, wzr, #0x7fffffff
100013e78: 17ffffb0    	b	0x100013d38 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x118>
100013e7c: b9403fe8    	ldr	w8, [sp, #0x3c]
100013e80: f94013e9    	ldr	x9, [sp, #0x20]
100013e84: f9002be9    	str	x9, [sp, #0x50]
100013e88: f94017e7    	ldr	x7, [sp, #0x28]
100013e8c: 294813e6    	ldp	w6, w4, [sp, #0x40]
100013e90: b9404be3    	ldr	w3, [sp, #0x48]
100013e94: b9405fe1    	ldr	w1, [sp, #0x5c]
100013e98: 17ffff82    	b	0x100013ca0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x80>
100013e9c: 2a0103e8    	mov	w8, w1
100013ea0: f9002be0    	str	x0, [sp, #0x50]
100013ea4: f94017e7    	ldr	x7, [sp, #0x28]
100013ea8: b94047e4    	ldr	w4, [sp, #0x44]
100013eac: b9405fe1    	ldr	w1, [sp, #0x5c]
100013eb0: 17ffff7c    	b	0x100013ca0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x80>
100013eb4: b9003fe1    	str	w1, [sp, #0x3c]
100013eb8: f90013e0    	str	x0, [sp, #0x20]
100013ebc: f9401be0    	ldr	x0, [sp, #0x30]
100013ec0: b9402b81    	ldr	w1, [x28, #0x28]
100013ec4: b9400802    	ldr	w2, [x0, #0x8]
100013ec8: 6b02003f    	cmp	w1, w2
100013ecc: 54000240    	b.eq	0x100013f14 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x2f4>
100013ed0: 91005000    	add	x0, x0, #0x14
100013ed4: 885f7c01    	ldxr	w1, [x0]
100013ed8: 11000429    	add	w9, w1, #0x1
100013edc: 880afc09    	stlxr	w10, w9, [x0]
100013ee0: 35ffffaa    	cbnz	w10, 0x100013ed4 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x2b4>
100013ee4: d5033bbf    	dmb	ish
100013ee8: b9403fe8    	ldr	w8, [sp, #0x3c]
100013eec: f94013e9    	ldr	x9, [sp, #0x20]
100013ef0: f9002be9    	str	x9, [sp, #0x50]
100013ef4: f94017e7    	ldr	x7, [sp, #0x28]
100013ef8: 294813e6    	ldp	w6, w4, [sp, #0x40]
100013efc: b9404be3    	ldr	w3, [sp, #0x48]
100013f00: b9405fe1    	ldr	w1, [sp, #0x5c]
100013f04: 17ffff67    	b	0x100013ca0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x80>
100013f08: 710003df    	cmp	w30, #0x0
100013f0c: 54fffa2a    	b.ge	0x100013e50 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x230>
100013f10: 14000064    	b	0x1000140a0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x480>
100013f14: b9403fe1    	ldr	w1, [sp, #0x3c]
100013f18: b9401002    	ldr	w2, [x0, #0x10]
100013f1c: 51000442    	sub	w2, w2, #0x1
100013f20: b9001002    	str	w2, [x0, #0x10]
100013f24: 2a0103e8    	mov	w8, w1
100013f28: f94013e9    	ldr	x9, [sp, #0x20]
100013f2c: f9002be9    	str	x9, [sp, #0x50]
100013f30: f94017e7    	ldr	x7, [sp, #0x28]
100013f34: 294813e6    	ldp	w6, w4, [sp, #0x40]
100013f38: b9404be3    	ldr	w3, [sp, #0x48]
100013f3c: b9405fe1    	ldr	w1, [sp, #0x5c]
100013f40: 17ffff58    	b	0x100013ca0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x80>
100013f44: f90013e0    	str	x0, [sp, #0x20]
100013f48: 320003e0    	orr	w0, wzr, #0x1
100013f4c: 320003e1    	orr	w1, wzr, #0x1
100013f50: 9407b4c0    	bl	0x100201250 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
100013f54: f94013e0    	ldr	x0, [sp, #0x20]
100013f58: 17ffff97    	b	0x100013db4 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x194>
100013f5c: f90013e0    	str	x0, [sp, #0x20]
100013f60: 9407b450    	bl	0x1002010a0 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
100013f64: f94013e0    	ldr	x0, [sp, #0x20]
100013f68: 17ffff93    	b	0x100013db4 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x194>
100013f6c: a9438381    	ldp	x1, x0, [x28, #0x38]
100013f70: 9100a002    	add	x2, x0, #0x28
100013f74: 528d1003    	mov	w3, #0x6880             ; =26752
100013f78: 72a00403    	movk	w3, #0x20, lsl #16
100013f7c: eb02003f    	cmp	x1, x2
100013f80: 54000b83    	b.lo	0x1000140f0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x4d0>
100013f84: f9002382    	str	x2, [x28, #0x40]
100013f88: b9000003    	str	w3, [x0]
100013f8c: b900041f    	str	wzr, [x0, #0x4]
100013f90: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
100013f94: a901fc1f    	stp	xzr, xzr, [x0, #0x18]
100013f98: f90013eb    	str	x11, [sp, #0x20]
100013f9c: 52850061    	mov	w1, #0x2803             ; =10243
100013fa0: 72a00121    	movk	w1, #0x9, lsl #16
100013fa4: 52850022    	mov	w2, #0x2801             ; =10241
100013fa8: 72a00122    	movk	w2, #0x9, lsl #16
100013fac: 29020801    	stp	w1, w2, [x0, #0x10]
100013fb0: cb1b0001    	sub	x1, x0, x27
100013fb4: d343fc21    	lsr	x1, x1, #3
100013fb8: b9000c01    	str	w1, [x0, #0xc]
100013fbc: d5033abf    	dmb	ishst
100013fc0: aa0003e2    	mov	x2, x0
100013fc4: 2a1f03e1    	mov	w1, wzr
100013fc8: f9001be2    	str	x2, [sp, #0x30]
100013fcc: 940c6f19    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100013fd0: f9401be2    	ldr	x2, [sp, #0x30]
100013fd4: 910283e1    	add	x1, sp, #0xa0
100013fd8: aa0203e0    	mov	x0, x2
100013fdc: 94073815    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
100013fe0: f90013eb    	str	x11, [sp, #0x20]
100013fe4: f94013e0    	ldr	x0, [sp, #0x20]
100013fe8: 9401fa6a    	bl	0x100092990 <_SubstrateForeignUtil_throwSymbolIsNullException_eG3tVnjFmBKOZbMYv5OKpB>
100013fec: 9407643d    	bl	0x1001ed0e0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
100013ff0: a9438381    	ldp	x1, x0, [x28, #0x38]
100013ff4: 91008002    	add	x2, x0, #0x20
100013ff8: 5287f003    	mov	w3, #0x3f80             ; =16256
100013ffc: 72a00403    	movk	w3, #0x20, lsl #16
100014000: eb02003f    	cmp	x1, x2
100014004: 54000823    	b.lo	0x100014108 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x4e8>
100014008: f9002382    	str	x2, [x28, #0x40]
10001400c: b9000003    	str	w3, [x0]
100014010: b900041f    	str	wzr, [x0, #0x4]
100014014: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
100014018: f9000c1f    	str	xzr, [x0, #0x18]
10001401c: 52850061    	mov	w1, #0x2803             ; =10243
100014020: 72a00121    	movk	w1, #0x9, lsl #16
100014024: 52850022    	mov	w2, #0x2801             ; =10241
100014028: 72a00122    	movk	w2, #0x9, lsl #16
10001402c: 29020801    	stp	w1, w2, [x0, #0x10]
100014030: cb1b0001    	sub	x1, x0, x27
100014034: d343fc21    	lsr	x1, x1, #3
100014038: b9000c01    	str	w1, [x0, #0xc]
10001403c: d5033abf    	dmb	ishst
100014040: aa0003e2    	mov	x2, x0
100014044: 2a1f03e1    	mov	w1, wzr
100014048: f90017e2    	str	x2, [sp, #0x28]
10001404c: 940c6ef9    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100014050: 52986a00    	mov	w0, #0xc350             ; =50000
100014054: 72a000e0    	movk	w0, #0x7, lsl #16
100014058: f94017e1    	ldr	x1, [sp, #0x28]
10001405c: b9000820    	str	w0, [x1, #0x8]
100014060: b9400020    	ldr	w0, [x1]
100014064: 370802e0    	tbnz	w0, #0x1, 0x1000140c0 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x4a0>
100014068: aa0103e2    	mov	x2, x1
10001406c: 17ffffda    	b	0x100013fd4 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x3b4>
100014070: 52851540    	mov	w0, #0x28aa             ; =10410
100014074: 72a00120    	movk	w0, #0x9, lsl #16
100014078: 8b000f60    	add	x0, x27, x0, lsl #3
10001407c: b940001e    	ldr	w30, [x0]
100014080: 53057fde    	lsr	w30, w30, #5
100014084: 2a1e03fe    	mov	w30, w30
100014088: 8b1e0f7e    	add	x30, x27, x30, lsl #3
10001408c: f9403fde    	ldr	x30, [x30, #0x78]
100014090: 8b1a03de    	add	x30, x30, x26
100014094: d63f03c0    	blr	x30
100014098: aa0003e2    	mov	x2, x0
10001409c: 17ffffce    	b	0x100013fd4 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x3b4>
1000140a0: 52851500    	mov	w0, #0x28a8             ; =10408
1000140a4: 72a00120    	movk	w0, #0x9, lsl #16
1000140a8: 8b000f60    	add	x0, x27, x0, lsl #3
1000140ac: 17fffff4    	b	0x10001407c <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x45c>
1000140b0: 52851500    	mov	w0, #0x28a8             ; =10408
1000140b4: 72a00120    	movk	w0, #0x9, lsl #16
1000140b8: 8b000f60    	add	x0, x27, x0, lsl #3
1000140bc: 17fffff0    	b	0x10001407c <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x45c>
1000140c0: aa0103e0    	mov	x0, x1
1000140c4: b26db3e2    	mov	x2, #-0x80000           ; =-524288
1000140c8: 8a020002    	and	x2, x0, x2
1000140cc: 91210043    	add	x3, x2, #0x840
1000140d0: cb030000    	sub	x0, x0, x3
1000140d4: b27d0be3    	orr	x3, xzr, #0x38
1000140d8: 8b402460    	add	x0, x3, x0, lsr #9
1000140dc: 38e06843    	ldrsb	w3, [x2, x0]
1000140e0: 34000103    	cbz	w3, 0x100014100 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x4e0>
1000140e4: 3820685f    	strb	wzr, [x2, x0]
1000140e8: aa0103e2    	mov	x2, x1
1000140ec: 17ffffba    	b	0x100013fd4 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x3b4>
1000140f0: f90013eb    	str	x11, [sp, #0x20]
1000140f4: 92407c60    	and	x0, x3, #0xffffffff
1000140f8: 9402de2a    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
1000140fc: 17ffffa8    	b	0x100013f9c <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x37c>
100014100: aa0103e2    	mov	x2, x1
100014104: 17ffffb4    	b	0x100013fd4 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x3b4>
100014108: 92407c60    	and	x0, x3, #0xffffffff
10001410c: 9402de25    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100014110: 17ffffc3    	b	0x10001401c <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x3fc>
100014114: f9402be0    	ldr	x0, [sp, #0x50]
100014118: a9497bfd    	ldp	x29, x30, [sp, #0x90]
10001411c: 910283ff    	add	sp, sp, #0xa0
100014120: b9401389    	ldr	w9, [x28, #0x10]
100014124: 7100013f    	cmp	w9, #0x0
100014128: 5400010d    	b.le	0x100014148 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x528>
10001412c: d65f03c0    	ret
100014130: 52806020    	mov	w0, #0x301              ; =769
100014134: 72a00020    	movk	w0, #0x1, lsl #16
100014138: 8b000f60    	add	x0, x27, x0, lsl #3
10001413c: 9400fe59    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100014140: 17fffec5    	b	0x100013c54 <_Jvm$ManualFfm_run_ZT7vbfWhYt5Fey6k2XuRT5+0x34>
100014144: 1403130b    	b	0x1000d8d70 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100014148: 1407b206    	b	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001414c: cccccccc    	<unknown>


; ffm-aot/generated-library-ffm
0000000100012160 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8>:
100012160: d10283e9    	sub	x9, sp, #0xa0
100012164: f940078a    	ldr	x10, [x28, #0x8]
100012168: eb0a013f    	cmp	x9, x10
10001216c: 54002d69    	b.ls	0x100012718 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x5b8>
100012170: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100012174: 9100013f    	mov	sp, x9
100012178: b9005fe1    	str	w1, [sp, #0x5c]
10001217c: 52847a40    	mov	w0, #0x23d2             ; =9170
100012180: 72a00120    	movk	w0, #0x9, lsl #16
100012184: 8b000f60    	add	x0, x27, x0, lsl #3
100012188: 91008409    	add	x9, x0, #0x21
10001218c: 08dffd20    	ldarb	w0, [x9]
100012190: 35002ba0    	cbnz	w0, 0x100012704 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x5a4>
100012194: 52806163    	mov	w3, #0x30b              ; =779
100012198: 72a00023    	movk	w3, #0x1, lsl #16
10001219c: b9004be3    	str	w3, [sp, #0x48]
1000121a0: 528063c4    	mov	w4, #0x31e              ; =798
1000121a4: 72a00024    	movk	w4, #0x1, lsl #16
1000121a8: b90043e4    	str	w4, [sp, #0x40]
1000121ac: 32007be5    	orr	w5, wzr, #0x7fffffff
1000121b0: 52806626    	mov	w6, #0x331              ; =817
1000121b4: 72a00026    	movk	w6, #0x1, lsl #16
1000121b8: b9003fe6    	str	w6, [sp, #0x3c]
1000121bc: 52802de7    	mov	w7, #0x16f              ; =367
1000121c0: 72a00027    	movk	w7, #0x1, lsl #16
1000121c4: b90047e7    	str	w7, [sp, #0x44]
1000121c8: 5284e008    	mov	w8, #0x2700             ; =9984
1000121cc: 72a00128    	movk	w8, #0x9, lsl #16
1000121d0: 8b080f68    	add	x8, x27, x8, lsl #3
1000121d4: f90017e8    	str	x8, [sp, #0x28]
1000121d8: 2a1f03eb    	mov	w11, wzr
1000121dc: f9002be2    	str	x2, [sp, #0x50]
1000121e0: 6b01017f    	cmp	w11, w1
1000121e4: 5400282a    	b.ge	0x1000126e8 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x588>
1000121e8: b940b900    	ldr	w0, [x8, #0xb8]
1000121ec: 34001840    	cbz	w0, 0x1000124f4 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x394>
1000121f0: 2a0003e0    	mov	w0, w0
1000121f4: 8b000f60    	add	x0, x27, x0, lsl #3
1000121f8: b9400800    	ldr	w0, [x0, #0x8]
1000121fc: 340017c0    	cbz	w0, 0x1000124f4 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x394>
100012200: b90063eb    	str	w11, [sp, #0x60]
100012204: 2a0003e0    	mov	w0, w0
100012208: 8b000f60    	add	x0, x27, x0, lsl #3
10001220c: b9400400    	ldr	w0, [x0, #0x4]
100012210: 34001740    	cbz	w0, 0x1000124f8 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x398>
100012214: 2a0003e0    	mov	w0, w0
100012218: 8b000f60    	add	x0, x27, x0, lsl #3
10001221c: b940001e    	ldr	w30, [x0]
100012220: 53057fde    	lsr	w30, w30, #5
100012224: 6b0703df    	cmp	w30, w7
100012228: 54001ac1    	b.ne	0x100012580 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x420>
10001222c: f9400c1e    	ldr	x30, [x0, #0x18]
100012230: b4001abe    	cbz	x30, 0x100012584 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x424>
100012234: b940041e    	ldr	w30, [x0, #0x4]
100012238: 340015fe    	cbz	w30, 0x1000124f4 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x394>
10001223c: 2a1e03fe    	mov	w30, w30
100012240: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100012244: f9001bfe    	str	x30, [sp, #0x30]
100012248: b94003cc    	ldr	w12, [x30]
10001224c: b90067ec    	str	w12, [sp, #0x64]
100012250: 53057d8d    	lsr	w13, w12, #5
100012254: b9004fed    	str	w13, [sp, #0x4c]
100012258: 53057d8e    	lsr	w14, w12, #5
10001225c: b9003bee    	str	w14, [sp, #0x38]
100012260: 6b0601df    	cmp	w14, w6
100012264: 54000a60    	b.eq	0x1000123b0 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x250>
100012268: 6b0401bf    	cmp	w13, w4
10001226c: 540009e0    	b.eq	0x1000123a8 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x248>
100012270: 6b0301bf    	cmp	w13, w3
100012274: 54000be0    	b.eq	0x1000123f0 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x290>
100012278: f90013e0    	str	x0, [sp, #0x20]
10001227c: 53057d8f    	lsr	w15, w12, #5
100012280: 2a0f03ef    	mov	w15, w15
100012284: 8b0f0f6f    	add	x15, x27, x15, lsl #3
100012288: f94039ef    	ldr	x15, [x15, #0x70]
10001228c: 8b1a01ef    	add	x15, x15, x26
100012290: aa1e03e0    	mov	x0, x30
100012294: aa0f03fe    	mov	x30, x15
100012298: d63f03c0    	blr	x30
10001229c: f9401bfe    	ldr	x30, [sp, #0x30]
1000122a0: f94013e0    	ldr	x0, [sp, #0x20]
1000122a4: 32007be5    	orr	w5, wzr, #0x7fffffff
1000122a8: f9400c0d    	ldr	x13, [x0, #0x18]
1000122ac: 9101a3e1    	add	x1, sp, #0x68
1000122b0: a900fc3f    	stp	xzr, xzr, [x1, #0x8]
1000122b4: f9400f80    	ldr	x0, [x28, #0x18]
1000122b8: f9000c20    	str	x0, [x1, #0x18]
1000122bc: f9000f81    	str	x1, [x28, #0x18]
1000122c0: f9402be0    	ldr	x0, [sp, #0x50]
1000122c4: aa0d03fe    	mov	x30, x13
1000122c8: aa0103ed    	mov	x13, x1
1000122cc: 10000029    	adr	x9, 0x1000122d0 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x170>
1000122d0: f90005a9    	str	x9, [x13, #0x8]
1000122d4: 910003e9    	mov	x9, sp
1000122d8: f90009a9    	str	x9, [x13, #0x10]
1000122dc: 320007e9    	orr	w9, wzr, #0x3
1000122e0: 9100538a    	add	x10, x28, #0x14
1000122e4: 889ffd49    	stlr	w9, [x10]
1000122e8: d63f03c0    	blr	x30
1000122ec: 320007fe    	orr	w30, wzr, #0x3
1000122f0: 320003e1    	orr	w1, wzr, #0x1
1000122f4: 91005382    	add	x2, x28, #0x14
1000122f8: 885f7c43    	ldxr	w3, [x2]
1000122fc: 6b1e007f    	cmp	w3, w30
100012300: 1a83002a    	csel	w10, w1, w3, eq
100012304: 8809fc4a    	stlxr	w9, w10, [x2]
100012308: 35ffff89    	cbnz	w9, 0x1000122f8 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x198>
10001230c: d5033bbf    	dmb	ish
100012310: 1a9f17fe    	cset	w30, eq
100012314: 34000dde    	cbz	w30, 0x1000124cc <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x36c>
100012318: f9400f9e    	ldr	x30, [x28, #0x18]
10001231c: f9400fde    	ldr	x30, [x30, #0x18]
100012320: f9000f9e    	str	x30, [x28, #0x18]
100012324: b9402f9e    	ldr	w30, [x28, #0x2c]
100012328: 35000dfe    	cbnz	w30, 0x1000124e4 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x384>
10001232c: b9403bee    	ldr	w14, [sp, #0x38]
100012330: b94063eb    	ldr	w11, [sp, #0x60]
100012334: b9403fe6    	ldr	w6, [sp, #0x3c]
100012338: 11000561    	add	w1, w11, #0x1
10001233c: 6b0601df    	cmp	w14, w6
100012340: 540006e0    	b.eq	0x10001241c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x2bc>
100012344: 294937e3    	ldp	w3, w13, [sp, #0x48]
100012348: 6b0301bf    	cmp	w13, w3
10001234c: 54000940    	b.eq	0x100012474 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x314>
100012350: b94043e4    	ldr	w4, [sp, #0x40]
100012354: 6b0401df    	cmp	w14, w4
100012358: b9003be1    	str	w1, [sp, #0x38]
10001235c: f90013e0    	str	x0, [sp, #0x20]
100012360: 540004c0    	b.eq	0x1000123f8 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x298>
100012364: b94067ec    	ldr	w12, [sp, #0x64]
100012368: 53057d9e    	lsr	w30, w12, #5
10001236c: 2a1e03fe    	mov	w30, w30
100012370: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100012374: f94047de    	ldr	x30, [x30, #0x88]
100012378: 8b1a03de    	add	x30, x30, x26
10001237c: f9401be0    	ldr	x0, [sp, #0x30]
100012380: d63f03c0    	blr	x30
100012384: b9403beb    	ldr	w11, [sp, #0x38]
100012388: f94013e9    	ldr	x9, [sp, #0x20]
10001238c: f9002be9    	str	x9, [sp, #0x50]
100012390: f94017e8    	ldr	x8, [sp, #0x28]
100012394: b94047e7    	ldr	w7, [sp, #0x44]
100012398: 294793e6    	ldp	w6, w4, [sp, #0x3c]
10001239c: b9404be3    	ldr	w3, [sp, #0x48]
1000123a0: b9405fe1    	ldr	w1, [sp, #0x5c]
1000123a4: 17ffff8f    	b	0x1000121e0 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x80>
1000123a8: 32007be5    	orr	w5, wzr, #0x7fffffff
1000123ac: 17ffffbf    	b	0x1000122a8 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x148>
1000123b0: f90013e0    	str	x0, [sp, #0x20]
1000123b4: 294103cd    	ldp	w13, w0, [x30, #0x8]
1000123b8: 340006ad    	cbz	w13, 0x10001248c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x32c>
1000123bc: b9402b81    	ldr	w1, [x28, #0x28]
1000123c0: 6b0d003f    	cmp	w1, w13
1000123c4: 54001241    	b.ne	0x10001260c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x4ac>
1000123c8: 7100001f    	cmp	w0, #0x0
1000123cc: 5400140b    	b.lt	0x10001264c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x4ec>
1000123d0: 32007be5    	orr	w5, wzr, #0x7fffffff
1000123d4: b94013c0    	ldr	w0, [x30, #0x10]
1000123d8: 6b05001f    	cmp	w0, w5
1000123dc: 54000d60    	b.eq	0x100012588 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x428>
1000123e0: 11000400    	add	w0, w0, #0x1
1000123e4: b90013c0    	str	w0, [x30, #0x10]
1000123e8: f94013e0    	ldr	x0, [sp, #0x20]
1000123ec: 17ffffaf    	b	0x1000122a8 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x148>
1000123f0: 32007be5    	orr	w5, wzr, #0x7fffffff
1000123f4: 17ffffad    	b	0x1000122a8 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x148>
1000123f8: b9403beb    	ldr	w11, [sp, #0x38]
1000123fc: f94013e9    	ldr	x9, [sp, #0x20]
100012400: f9002be9    	str	x9, [sp, #0x50]
100012404: f94017e8    	ldr	x8, [sp, #0x28]
100012408: b94047e7    	ldr	w7, [sp, #0x44]
10001240c: 294793e6    	ldp	w6, w4, [sp, #0x3c]
100012410: b9404be3    	ldr	w3, [sp, #0x48]
100012414: b9405fe1    	ldr	w1, [sp, #0x5c]
100012418: 17ffff72    	b	0x1000121e0 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x80>
10001241c: b9003be1    	str	w1, [sp, #0x38]
100012420: f90013e0    	str	x0, [sp, #0x20]
100012424: f9401bfe    	ldr	x30, [sp, #0x30]
100012428: b9402b80    	ldr	w0, [x28, #0x28]
10001242c: b9400bc1    	ldr	w1, [x30, #0x8]
100012430: 6b01001f    	cmp	w0, w1
100012434: 54000320    	b.eq	0x100012498 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x338>
100012438: 910053c0    	add	x0, x30, #0x14
10001243c: 885f7c01    	ldxr	w1, [x0]
100012440: 11000429    	add	w9, w1, #0x1
100012444: 880afc09    	stlxr	w10, w9, [x0]
100012448: 35ffffaa    	cbnz	w10, 0x10001243c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x2dc>
10001244c: d5033bbf    	dmb	ish
100012450: b9403beb    	ldr	w11, [sp, #0x38]
100012454: f94013e9    	ldr	x9, [sp, #0x20]
100012458: f9002be9    	str	x9, [sp, #0x50]
10001245c: f94017e8    	ldr	x8, [sp, #0x28]
100012460: b94047e7    	ldr	w7, [sp, #0x44]
100012464: 294793e6    	ldp	w6, w4, [sp, #0x3c]
100012468: b9404be3    	ldr	w3, [sp, #0x48]
10001246c: b9405fe1    	ldr	w1, [sp, #0x5c]
100012470: 17ffff5c    	b	0x1000121e0 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x80>
100012474: 2a0103eb    	mov	w11, w1
100012478: f9002be0    	str	x0, [sp, #0x50]
10001247c: f94017e8    	ldr	x8, [sp, #0x28]
100012480: 29481fe4    	ldp	w4, w7, [sp, #0x40]
100012484: b9405fe1    	ldr	w1, [sp, #0x5c]
100012488: 17ffff56    	b	0x1000121e0 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x80>
10001248c: 7100001f    	cmp	w0, #0x0
100012490: 54fffa0a    	b.ge	0x1000123d0 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x270>
100012494: 1400006a    	b	0x10001263c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x4dc>
100012498: b9403be1    	ldr	w1, [sp, #0x38]
10001249c: b94013c0    	ldr	w0, [x30, #0x10]
1000124a0: 51000400    	sub	w0, w0, #0x1
1000124a4: b90013c0    	str	w0, [x30, #0x10]
1000124a8: 2a0103eb    	mov	w11, w1
1000124ac: f94013e9    	ldr	x9, [sp, #0x20]
1000124b0: f9002be9    	str	x9, [sp, #0x50]
1000124b4: f94017e8    	ldr	x8, [sp, #0x28]
1000124b8: b94047e7    	ldr	w7, [sp, #0x44]
1000124bc: 294793e6    	ldp	w6, w4, [sp, #0x3c]
1000124c0: b9404be3    	ldr	w3, [sp, #0x48]
1000124c4: b9405fe1    	ldr	w1, [sp, #0x5c]
1000124c8: 17ffff46    	b	0x1000121e0 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x80>
1000124cc: f90013e0    	str	x0, [sp, #0x20]
1000124d0: 320003e0    	orr	w0, wzr, #0x1
1000124d4: 320003e1    	orr	w1, wzr, #0x1
1000124d8: 9407bb5e    	bl	0x100201250 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
1000124dc: f94013e0    	ldr	x0, [sp, #0x20]
1000124e0: 17ffff93    	b	0x10001232c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x1cc>
1000124e4: f90013e0    	str	x0, [sp, #0x20]
1000124e8: 9407baee    	bl	0x1002010a0 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
1000124ec: f94013e0    	ldr	x0, [sp, #0x20]
1000124f0: 17ffff8f    	b	0x10001232c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x1cc>
1000124f4: 94076afb    	bl	0x1001ed0e0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
1000124f8: a9438381    	ldp	x1, x0, [x28, #0x38]
1000124fc: 91008002    	add	x2, x0, #0x20
100012500: 52845c03    	mov	w3, #0x22e0             ; =8928
100012504: 72a00403    	movk	w3, #0x20, lsl #16
100012508: eb02003f    	cmp	x1, x2
10001250c: 54000dc3    	b.lo	0x1000126c4 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x564>
100012510: f9002382    	str	x2, [x28, #0x40]
100012514: b9000003    	str	w3, [x0]
100012518: b900041f    	str	wzr, [x0, #0x4]
10001251c: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
100012520: f9000c1f    	str	xzr, [x0, #0x18]
100012524: 52850061    	mov	w1, #0x2803             ; =10243
100012528: 72a00121    	movk	w1, #0x9, lsl #16
10001252c: 52850022    	mov	w2, #0x2801             ; =10241
100012530: 72a00122    	movk	w2, #0x9, lsl #16
100012534: 29020801    	stp	w1, w2, [x0, #0x10]
100012538: cb1b0001    	sub	x1, x0, x27
10001253c: d343fc21    	lsr	x1, x1, #3
100012540: b9000c01    	str	w1, [x0, #0xc]
100012544: d5033abf    	dmb	ishst
100012548: aa0003e2    	mov	x2, x0
10001254c: 2a1f03e1    	mov	w1, wzr
100012550: f9001be2    	str	x2, [sp, #0x30]
100012554: 940c75b7    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100012558: 5291d53e    	mov	w30, #0x8ea9            ; =36521
10001255c: 72a000de    	movk	w30, #0x6, lsl #16
100012560: f9401be0    	ldr	x0, [sp, #0x30]
100012564: b900081e    	str	w30, [x0, #0x8]
100012568: b940001e    	ldr	w30, [x0]
10001256c: 3708079e    	tbnz	w30, #0x1, 0x10001265c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x4fc>
100012570: aa0003e2    	mov	x2, x0
100012574: 910283e1    	add	x1, sp, #0xa0
100012578: aa0203e0    	mov	x0, x2
10001257c: 94073ead    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
100012580: 94076998    	bl	0x1001ecbe0 <_ImplicitExceptions_throwOptClassCastException_UXv9fg3pe9HQF6muVBaSh5>
100012584: 94020103    	bl	0x100092990 <_SubstrateForeignUtil_throwSymbolIsNullException_eG3tVnjFmBKOZbMYv5OKpB>
100012588: f94013e0    	ldr	x0, [sp, #0x20]
10001258c: a9438782    	ldp	x2, x1, [x28, #0x38]
100012590: 91008023    	add	x3, x1, #0x20
100012594: 5287f004    	mov	w4, #0x3f80             ; =16256
100012598: 72a00404    	movk	w4, #0x20, lsl #16
10001259c: eb03005f    	cmp	x2, x3
1000125a0: 540009c3    	b.lo	0x1000126d8 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x578>
1000125a4: f9002383    	str	x3, [x28, #0x40]
1000125a8: b9000024    	str	w4, [x1]
1000125ac: b900043f    	str	wzr, [x1, #0x4]
1000125b0: a900fc3f    	stp	xzr, xzr, [x1, #0x8]
1000125b4: f9000c3f    	str	xzr, [x1, #0x18]
1000125b8: 52850062    	mov	w2, #0x2803             ; =10243
1000125bc: 72a00122    	movk	w2, #0x9, lsl #16
1000125c0: 52850023    	mov	w3, #0x2801             ; =10241
1000125c4: 72a00123    	movk	w3, #0x9, lsl #16
1000125c8: 29020c22    	stp	w2, w3, [x1, #0x10]
1000125cc: cb1b0022    	sub	x2, x1, x27
1000125d0: d343fc42    	lsr	x2, x2, #3
1000125d4: b9000c22    	str	w2, [x1, #0xc]
1000125d8: d5033abf    	dmb	ishst
1000125dc: aa0103e0    	mov	x0, x1
1000125e0: f90017e1    	str	x1, [sp, #0x28]
1000125e4: 2a1f03e1    	mov	w1, wzr
1000125e8: 940c7592    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
1000125ec: 52986a00    	mov	w0, #0xc350             ; =50000
1000125f0: 72a000e0    	movk	w0, #0x7, lsl #16
1000125f4: f94017e1    	ldr	x1, [sp, #0x28]
1000125f8: b9000820    	str	w0, [x1, #0x8]
1000125fc: b9400020    	ldr	w0, [x1]
100012600: 370804a0    	tbnz	w0, #0x1, 0x100012694 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x534>
100012604: aa0103e2    	mov	x2, x1
100012608: 17ffffdb    	b	0x100012574 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x414>
10001260c: 52851540    	mov	w0, #0x28aa             ; =10410
100012610: 72a00120    	movk	w0, #0x9, lsl #16
100012614: 8b000f60    	add	x0, x27, x0, lsl #3
100012618: b940001e    	ldr	w30, [x0]
10001261c: 53057fde    	lsr	w30, w30, #5
100012620: 2a1e03fe    	mov	w30, w30
100012624: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100012628: f9403fde    	ldr	x30, [x30, #0x78]
10001262c: 8b1a03de    	add	x30, x30, x26
100012630: d63f03c0    	blr	x30
100012634: aa0003e2    	mov	x2, x0
100012638: 17ffffcf    	b	0x100012574 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x414>
10001263c: 52851500    	mov	w0, #0x28a8             ; =10408
100012640: 72a00120    	movk	w0, #0x9, lsl #16
100012644: 8b000f60    	add	x0, x27, x0, lsl #3
100012648: 17fffff4    	b	0x100012618 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x4b8>
10001264c: 52851500    	mov	w0, #0x28a8             ; =10408
100012650: 72a00120    	movk	w0, #0x9, lsl #16
100012654: 8b000f60    	add	x0, x27, x0, lsl #3
100012658: 17fffff0    	b	0x100012618 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x4b8>
10001265c: b26db3e1    	mov	x1, #-0x80000           ; =-524288
100012660: aa0003e2    	mov	x2, x0
100012664: 8a010041    	and	x1, x2, x1
100012668: 91210023    	add	x3, x1, #0x840
10001266c: cb030042    	sub	x2, x2, x3
100012670: b27d0be3    	orr	x3, xzr, #0x38
100012674: 8b422462    	add	x2, x3, x2, lsr #9
100012678: 38e26823    	ldrsb	w3, [x1, x2]
10001267c: 34000083    	cbz	w3, 0x10001268c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x52c>
100012680: 3822683f    	strb	wzr, [x1, x2]
100012684: aa0003e2    	mov	x2, x0
100012688: 17ffffbb    	b	0x100012574 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x414>
10001268c: aa0003e2    	mov	x2, x0
100012690: 17ffffb9    	b	0x100012574 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x414>
100012694: b26db3e0    	mov	x0, #-0x80000           ; =-524288
100012698: aa0103e2    	mov	x2, x1
10001269c: 8a000040    	and	x0, x2, x0
1000126a0: 91210003    	add	x3, x0, #0x840
1000126a4: cb030042    	sub	x2, x2, x3
1000126a8: b27d0be3    	orr	x3, xzr, #0x38
1000126ac: 8b422462    	add	x2, x3, x2, lsr #9
1000126b0: 38e26803    	ldrsb	w3, [x0, x2]
1000126b4: 340000e3    	cbz	w3, 0x1000126d0 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x570>
1000126b8: 3822681f    	strb	wzr, [x0, x2]
1000126bc: aa0103e2    	mov	x2, x1
1000126c0: 17ffffad    	b	0x100012574 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x414>
1000126c4: 92407c60    	and	x0, x3, #0xffffffff
1000126c8: 9402e4b6    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
1000126cc: 17ffff96    	b	0x100012524 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x3c4>
1000126d0: aa0103e2    	mov	x2, x1
1000126d4: 17ffffa8    	b	0x100012574 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x414>
1000126d8: 92407c80    	and	x0, x4, #0xffffffff
1000126dc: 9402e4b1    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
1000126e0: aa0003e1    	mov	x1, x0
1000126e4: 17ffffb5    	b	0x1000125b8 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x458>
1000126e8: f9402be0    	ldr	x0, [sp, #0x50]
1000126ec: a9497bfd    	ldp	x29, x30, [sp, #0x90]
1000126f0: 910283ff    	add	sp, sp, #0xa0
1000126f4: b9401389    	ldr	w9, [x28, #0x10]
1000126f8: 7100013f    	cmp	w9, #0x0
1000126fc: 5400010d    	b.le	0x10001271c <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x5bc>
100012700: d65f03c0    	ret
100012704: 52806020    	mov	w0, #0x301              ; =769
100012708: 72a00020    	movk	w0, #0x1, lsl #16
10001270c: 8b000f60    	add	x0, x27, x0, lsl #3
100012710: 940104e4    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012714: 17fffea0    	b	0x100012194 <_Jvm$GeneratedLibraryFfm_run_UW4rrXC4tmAOheOTpVcoA8+0x34>
100012718: 14031996    	b	0x1000d8d70 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
10001271c: 1407b891    	b	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>


; ffm-aot/manual-library-ffm
0000000100014150 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE>:
100014150: d10283e9    	sub	x9, sp, #0xa0
100014154: f940078a    	ldr	x10, [x28, #0x8]
100014158: eb0a013f    	cmp	x9, x10
10001415c: 540028c9    	b.ls	0x100014674 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x524>
100014160: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100014164: 9100013f    	mov	sp, x9
100014168: b9005fe1    	str	w1, [sp, #0x5c]
10001416c: 52847a40    	mov	w0, #0x23d2             ; =9170
100014170: 72a00120    	movk	w0, #0x9, lsl #16
100014174: 8b000f60    	add	x0, x27, x0, lsl #3
100014178: 91008409    	add	x9, x0, #0x21
10001417c: 08dffd20    	ldarb	w0, [x9]
100014180: 35002700    	cbnz	w0, 0x100014660 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x510>
100014184: 52806163    	mov	w3, #0x30b              ; =779
100014188: 72a00023    	movk	w3, #0x1, lsl #16
10001418c: b9004be3    	str	w3, [sp, #0x48]
100014190: 528063c4    	mov	w4, #0x31e              ; =798
100014194: 72a00024    	movk	w4, #0x1, lsl #16
100014198: b90047e4    	str	w4, [sp, #0x44]
10001419c: 32007be5    	orr	w5, wzr, #0x7fffffff
1000141a0: 52806626    	mov	w6, #0x331              ; =817
1000141a4: 72a00026    	movk	w6, #0x1, lsl #16
1000141a8: b90043e6    	str	w6, [sp, #0x40]
1000141ac: 5284e007    	mov	w7, #0x2700             ; =9984
1000141b0: 72a00127    	movk	w7, #0x9, lsl #16
1000141b4: 8b070f67    	add	x7, x27, x7, lsl #3
1000141b8: f90017e7    	str	x7, [sp, #0x28]
1000141bc: 2a1f03e8    	mov	w8, wzr
1000141c0: f9002be2    	str	x2, [sp, #0x50]
1000141c4: d503201f    	nop
1000141c8: d503201f    	nop
1000141cc: d503201f    	nop
1000141d0: 6b01011f    	cmp	w8, w1
1000141d4: 5400238a    	b.ge	0x100014644 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x4f4>
1000141d8: b90063e8    	str	w8, [sp, #0x60]
1000141dc: b940b4e0    	ldr	w0, [x7, #0xb4]
1000141e0: 2a0003eb    	mov	w11, w0
1000141e4: 8b0b0f6b    	add	x11, x27, x11, lsl #3
1000141e8: 340015a0    	cbz	w0, 0x10001449c <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x34c>
1000141ec: f9400d60    	ldr	x0, [x11, #0x18]
1000141f0: b4001900    	cbz	x0, 0x100014510 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x3c0>
1000141f4: b9400560    	ldr	w0, [x11, #0x4]
1000141f8: 34001920    	cbz	w0, 0x10001451c <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x3cc>
1000141fc: 2a0003e0    	mov	w0, w0
100014200: 8b000f60    	add	x0, x27, x0, lsl #3
100014204: f9001be0    	str	x0, [sp, #0x30]
100014208: b940000c    	ldr	w12, [x0]
10001420c: b90067ec    	str	w12, [sp, #0x64]
100014210: 53057d9e    	lsr	w30, w12, #5
100014214: b9003ffe    	str	w30, [sp, #0x3c]
100014218: 53057d8d    	lsr	w13, w12, #5
10001421c: b9004fed    	str	w13, [sp, #0x4c]
100014220: 6b0601bf    	cmp	w13, w6
100014224: 540009e0    	b.eq	0x100014360 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x210>
100014228: 6b0403df    	cmp	w30, w4
10001422c: 54000b80    	b.eq	0x10001439c <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x24c>
100014230: 6b0303df    	cmp	w30, w3
100014234: 54000b80    	b.eq	0x1000143a4 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x254>
100014238: f90013eb    	str	x11, [sp, #0x20]
10001423c: 53057d8e    	lsr	w14, w12, #5
100014240: 2a0e03ee    	mov	w14, w14
100014244: 8b0e0f6e    	add	x14, x27, x14, lsl #3
100014248: f94039ce    	ldr	x14, [x14, #0x70]
10001424c: 8b1a01ce    	add	x14, x14, x26
100014250: aa0003ef    	mov	x15, x0
100014254: aa0e03fe    	mov	x30, x14
100014258: d63f03c0    	blr	x30
10001425c: f9401be0    	ldr	x0, [sp, #0x30]
100014260: f94013eb    	ldr	x11, [sp, #0x20]
100014264: 32007be5    	orr	w5, wzr, #0x7fffffff
100014268: f9400d7e    	ldr	x30, [x11, #0x18]
10001426c: 9101a3ed    	add	x13, sp, #0x68
100014270: a900fdbf    	stp	xzr, xzr, [x13, #0x8]
100014274: f9400f81    	ldr	x1, [x28, #0x18]
100014278: f9000da1    	str	x1, [x13, #0x18]
10001427c: f9000f8d    	str	x13, [x28, #0x18]
100014280: f9402be0    	ldr	x0, [sp, #0x50]
100014284: 10000029    	adr	x9, 0x100014288 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x138>
100014288: f90005a9    	str	x9, [x13, #0x8]
10001428c: 910003e9    	mov	x9, sp
100014290: f90009a9    	str	x9, [x13, #0x10]
100014294: 320007e9    	orr	w9, wzr, #0x3
100014298: 9100538a    	add	x10, x28, #0x14
10001429c: 889ffd49    	stlr	w9, [x10]
1000142a0: d63f03c0    	blr	x30
1000142a4: 320007fe    	orr	w30, wzr, #0x3
1000142a8: 320003e1    	orr	w1, wzr, #0x1
1000142ac: 91005382    	add	x2, x28, #0x14
1000142b0: 885f7c43    	ldxr	w3, [x2]
1000142b4: 6b1e007f    	cmp	w3, w30
1000142b8: 1a83002a    	csel	w10, w1, w3, eq
1000142bc: 8809fc4a    	stlxr	w9, w10, [x2]
1000142c0: 35ffff89    	cbnz	w9, 0x1000142b0 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x160>
1000142c4: d5033bbf    	dmb	ish
1000142c8: 1a9f17fe    	cset	w30, eq
1000142cc: 34000d5e    	cbz	w30, 0x100014474 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x324>
1000142d0: f9400f9e    	ldr	x30, [x28, #0x18]
1000142d4: f9400fde    	ldr	x30, [x30, #0x18]
1000142d8: f9000f9e    	str	x30, [x28, #0x18]
1000142dc: b9402f9e    	ldr	w30, [x28, #0x2c]
1000142e0: 35000d7e    	cbnz	w30, 0x10001448c <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x33c>
1000142e4: b9404fed    	ldr	w13, [sp, #0x4c]
1000142e8: b94063e8    	ldr	w8, [sp, #0x60]
1000142ec: b94043e6    	ldr	w6, [sp, #0x40]
1000142f0: 11000501    	add	w1, w8, #0x1
1000142f4: 6b0601bf    	cmp	w13, w6
1000142f8: 54000760    	b.eq	0x1000143e4 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x294>
1000142fc: b9403ffe    	ldr	w30, [sp, #0x3c]
100014300: b9404be3    	ldr	w3, [sp, #0x48]
100014304: 6b0303df    	cmp	w30, w3
100014308: 54000620    	b.eq	0x1000143cc <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x27c>
10001430c: b94047e4    	ldr	w4, [sp, #0x44]
100014310: 6b0401bf    	cmp	w13, w4
100014314: b9003fe1    	str	w1, [sp, #0x3c]
100014318: f90013e0    	str	x0, [sp, #0x20]
10001431c: 54000480    	b.eq	0x1000143ac <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x25c>
100014320: b94067ec    	ldr	w12, [sp, #0x64]
100014324: 53057d9e    	lsr	w30, w12, #5
100014328: 2a1e03fe    	mov	w30, w30
10001432c: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100014330: f94047de    	ldr	x30, [x30, #0x88]
100014334: 8b1a03de    	add	x30, x30, x26
100014338: f9401be0    	ldr	x0, [sp, #0x30]
10001433c: d63f03c0    	blr	x30
100014340: b9403fe8    	ldr	w8, [sp, #0x3c]
100014344: f94013e9    	ldr	x9, [sp, #0x20]
100014348: f9002be9    	str	x9, [sp, #0x50]
10001434c: f94017e7    	ldr	x7, [sp, #0x28]
100014350: 294813e6    	ldp	w6, w4, [sp, #0x40]
100014354: b9404be3    	ldr	w3, [sp, #0x48]
100014358: b9405fe1    	ldr	w1, [sp, #0x5c]
10001435c: 17ffff9d    	b	0x1000141d0 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x80>
100014360: f90013eb    	str	x11, [sp, #0x20]
100014364: 2941780d    	ldp	w13, w30, [x0, #0x8]
100014368: 3400068d    	cbz	w13, 0x100014438 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x2e8>
10001436c: b9402b81    	ldr	w1, [x28, #0x28]
100014370: 6b0d003f    	cmp	w1, w13
100014374: 54001161    	b.ne	0x1000145a0 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x450>
100014378: 710003df    	cmp	w30, #0x0
10001437c: 5400132b    	b.lt	0x1000145e0 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x490>
100014380: 32007be5    	orr	w5, wzr, #0x7fffffff
100014384: b940101e    	ldr	w30, [x0, #0x10]
100014388: 6b0503df    	cmp	w30, w5
10001438c: 54000ca0    	b.eq	0x100014520 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x3d0>
100014390: 110007de    	add	w30, w30, #0x1
100014394: b900101e    	str	w30, [x0, #0x10]
100014398: 17ffffb4    	b	0x100014268 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x118>
10001439c: 32007be5    	orr	w5, wzr, #0x7fffffff
1000143a0: 17ffffb2    	b	0x100014268 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x118>
1000143a4: 32007be5    	orr	w5, wzr, #0x7fffffff
1000143a8: 17ffffb0    	b	0x100014268 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x118>
1000143ac: b9403fe8    	ldr	w8, [sp, #0x3c]
1000143b0: f94013e9    	ldr	x9, [sp, #0x20]
1000143b4: f9002be9    	str	x9, [sp, #0x50]
1000143b8: f94017e7    	ldr	x7, [sp, #0x28]
1000143bc: 294813e6    	ldp	w6, w4, [sp, #0x40]
1000143c0: b9404be3    	ldr	w3, [sp, #0x48]
1000143c4: b9405fe1    	ldr	w1, [sp, #0x5c]
1000143c8: 17ffff82    	b	0x1000141d0 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x80>
1000143cc: 2a0103e8    	mov	w8, w1
1000143d0: f9002be0    	str	x0, [sp, #0x50]
1000143d4: f94017e7    	ldr	x7, [sp, #0x28]
1000143d8: b94047e4    	ldr	w4, [sp, #0x44]
1000143dc: b9405fe1    	ldr	w1, [sp, #0x5c]
1000143e0: 17ffff7c    	b	0x1000141d0 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x80>
1000143e4: b9003fe1    	str	w1, [sp, #0x3c]
1000143e8: f90013e0    	str	x0, [sp, #0x20]
1000143ec: f9401be0    	ldr	x0, [sp, #0x30]
1000143f0: b9402b81    	ldr	w1, [x28, #0x28]
1000143f4: b9400802    	ldr	w2, [x0, #0x8]
1000143f8: 6b02003f    	cmp	w1, w2
1000143fc: 54000240    	b.eq	0x100014444 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x2f4>
100014400: 91005000    	add	x0, x0, #0x14
100014404: 885f7c01    	ldxr	w1, [x0]
100014408: 11000429    	add	w9, w1, #0x1
10001440c: 880afc09    	stlxr	w10, w9, [x0]
100014410: 35ffffaa    	cbnz	w10, 0x100014404 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x2b4>
100014414: d5033bbf    	dmb	ish
100014418: b9403fe8    	ldr	w8, [sp, #0x3c]
10001441c: f94013e9    	ldr	x9, [sp, #0x20]
100014420: f9002be9    	str	x9, [sp, #0x50]
100014424: f94017e7    	ldr	x7, [sp, #0x28]
100014428: 294813e6    	ldp	w6, w4, [sp, #0x40]
10001442c: b9404be3    	ldr	w3, [sp, #0x48]
100014430: b9405fe1    	ldr	w1, [sp, #0x5c]
100014434: 17ffff67    	b	0x1000141d0 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x80>
100014438: 710003df    	cmp	w30, #0x0
10001443c: 54fffa2a    	b.ge	0x100014380 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x230>
100014440: 14000064    	b	0x1000145d0 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x480>
100014444: b9403fe1    	ldr	w1, [sp, #0x3c]
100014448: b9401002    	ldr	w2, [x0, #0x10]
10001444c: 51000442    	sub	w2, w2, #0x1
100014450: b9001002    	str	w2, [x0, #0x10]
100014454: 2a0103e8    	mov	w8, w1
100014458: f94013e9    	ldr	x9, [sp, #0x20]
10001445c: f9002be9    	str	x9, [sp, #0x50]
100014460: f94017e7    	ldr	x7, [sp, #0x28]
100014464: 294813e6    	ldp	w6, w4, [sp, #0x40]
100014468: b9404be3    	ldr	w3, [sp, #0x48]
10001446c: b9405fe1    	ldr	w1, [sp, #0x5c]
100014470: 17ffff58    	b	0x1000141d0 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x80>
100014474: f90013e0    	str	x0, [sp, #0x20]
100014478: 320003e0    	orr	w0, wzr, #0x1
10001447c: 320003e1    	orr	w1, wzr, #0x1
100014480: 9407b374    	bl	0x100201250 <_SafepointSlowpath_enterSlowPathTransitionFromNativeToNewStatus_U31AxKqnQ8Jq14OFjiBN84>
100014484: f94013e0    	ldr	x0, [sp, #0x20]
100014488: 17ffff97    	b	0x1000142e4 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x194>
10001448c: f90013e0    	str	x0, [sp, #0x20]
100014490: 9407b304    	bl	0x1002010a0 <_SafepointSlowpath_enterSlowPathSafepointCheckRegularCCONV_G8eRCkU089De7ckv2Usgi8>
100014494: f94013e0    	ldr	x0, [sp, #0x20]
100014498: 17ffff93    	b	0x1000142e4 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x194>
10001449c: a9438381    	ldp	x1, x0, [x28, #0x38]
1000144a0: 9100a002    	add	x2, x0, #0x28
1000144a4: 528d1003    	mov	w3, #0x6880             ; =26752
1000144a8: 72a00403    	movk	w3, #0x20, lsl #16
1000144ac: eb02003f    	cmp	x1, x2
1000144b0: 54000b83    	b.lo	0x100014620 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x4d0>
1000144b4: f9002382    	str	x2, [x28, #0x40]
1000144b8: b9000003    	str	w3, [x0]
1000144bc: b900041f    	str	wzr, [x0, #0x4]
1000144c0: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
1000144c4: a901fc1f    	stp	xzr, xzr, [x0, #0x18]
1000144c8: f90013eb    	str	x11, [sp, #0x20]
1000144cc: 52850061    	mov	w1, #0x2803             ; =10243
1000144d0: 72a00121    	movk	w1, #0x9, lsl #16
1000144d4: 52850022    	mov	w2, #0x2801             ; =10241
1000144d8: 72a00122    	movk	w2, #0x9, lsl #16
1000144dc: 29020801    	stp	w1, w2, [x0, #0x10]
1000144e0: cb1b0001    	sub	x1, x0, x27
1000144e4: d343fc21    	lsr	x1, x1, #3
1000144e8: b9000c01    	str	w1, [x0, #0xc]
1000144ec: d5033abf    	dmb	ishst
1000144f0: aa0003e2    	mov	x2, x0
1000144f4: 2a1f03e1    	mov	w1, wzr
1000144f8: f9001be2    	str	x2, [sp, #0x30]
1000144fc: 940c6dcd    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100014500: f9401be2    	ldr	x2, [sp, #0x30]
100014504: 910283e1    	add	x1, sp, #0xa0
100014508: aa0203e0    	mov	x0, x2
10001450c: 940736c9    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
100014510: f90013eb    	str	x11, [sp, #0x20]
100014514: f94013e0    	ldr	x0, [sp, #0x20]
100014518: 9401f91e    	bl	0x100092990 <_SubstrateForeignUtil_throwSymbolIsNullException_eG3tVnjFmBKOZbMYv5OKpB>
10001451c: 940762f1    	bl	0x1001ed0e0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
100014520: a9438381    	ldp	x1, x0, [x28, #0x38]
100014524: 91008002    	add	x2, x0, #0x20
100014528: 5287f003    	mov	w3, #0x3f80             ; =16256
10001452c: 72a00403    	movk	w3, #0x20, lsl #16
100014530: eb02003f    	cmp	x1, x2
100014534: 54000823    	b.lo	0x100014638 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x4e8>
100014538: f9002382    	str	x2, [x28, #0x40]
10001453c: b9000003    	str	w3, [x0]
100014540: b900041f    	str	wzr, [x0, #0x4]
100014544: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
100014548: f9000c1f    	str	xzr, [x0, #0x18]
10001454c: 52850061    	mov	w1, #0x2803             ; =10243
100014550: 72a00121    	movk	w1, #0x9, lsl #16
100014554: 52850022    	mov	w2, #0x2801             ; =10241
100014558: 72a00122    	movk	w2, #0x9, lsl #16
10001455c: 29020801    	stp	w1, w2, [x0, #0x10]
100014560: cb1b0001    	sub	x1, x0, x27
100014564: d343fc21    	lsr	x1, x1, #3
100014568: b9000c01    	str	w1, [x0, #0xc]
10001456c: d5033abf    	dmb	ishst
100014570: aa0003e2    	mov	x2, x0
100014574: 2a1f03e1    	mov	w1, wzr
100014578: f90017e2    	str	x2, [sp, #0x28]
10001457c: 940c6dad    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100014580: 52986a00    	mov	w0, #0xc350             ; =50000
100014584: 72a000e0    	movk	w0, #0x7, lsl #16
100014588: f94017e1    	ldr	x1, [sp, #0x28]
10001458c: b9000820    	str	w0, [x1, #0x8]
100014590: b9400020    	ldr	w0, [x1]
100014594: 370802e0    	tbnz	w0, #0x1, 0x1000145f0 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x4a0>
100014598: aa0103e2    	mov	x2, x1
10001459c: 17ffffda    	b	0x100014504 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x3b4>
1000145a0: 52851540    	mov	w0, #0x28aa             ; =10410
1000145a4: 72a00120    	movk	w0, #0x9, lsl #16
1000145a8: 8b000f60    	add	x0, x27, x0, lsl #3
1000145ac: b940001e    	ldr	w30, [x0]
1000145b0: 53057fde    	lsr	w30, w30, #5
1000145b4: 2a1e03fe    	mov	w30, w30
1000145b8: 8b1e0f7e    	add	x30, x27, x30, lsl #3
1000145bc: f9403fde    	ldr	x30, [x30, #0x78]
1000145c0: 8b1a03de    	add	x30, x30, x26
1000145c4: d63f03c0    	blr	x30
1000145c8: aa0003e2    	mov	x2, x0
1000145cc: 17ffffce    	b	0x100014504 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x3b4>
1000145d0: 52851500    	mov	w0, #0x28a8             ; =10408
1000145d4: 72a00120    	movk	w0, #0x9, lsl #16
1000145d8: 8b000f60    	add	x0, x27, x0, lsl #3
1000145dc: 17fffff4    	b	0x1000145ac <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x45c>
1000145e0: 52851500    	mov	w0, #0x28a8             ; =10408
1000145e4: 72a00120    	movk	w0, #0x9, lsl #16
1000145e8: 8b000f60    	add	x0, x27, x0, lsl #3
1000145ec: 17fffff0    	b	0x1000145ac <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x45c>
1000145f0: aa0103e0    	mov	x0, x1
1000145f4: b26db3e2    	mov	x2, #-0x80000           ; =-524288
1000145f8: 8a020002    	and	x2, x0, x2
1000145fc: 91210043    	add	x3, x2, #0x840
100014600: cb030000    	sub	x0, x0, x3
100014604: b27d0be3    	orr	x3, xzr, #0x38
100014608: 8b402460    	add	x0, x3, x0, lsr #9
10001460c: 38e06843    	ldrsb	w3, [x2, x0]
100014610: 34000103    	cbz	w3, 0x100014630 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x4e0>
100014614: 3820685f    	strb	wzr, [x2, x0]
100014618: aa0103e2    	mov	x2, x1
10001461c: 17ffffba    	b	0x100014504 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x3b4>
100014620: f90013eb    	str	x11, [sp, #0x20]
100014624: 92407c60    	and	x0, x3, #0xffffffff
100014628: 9402dcde    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
10001462c: 17ffffa8    	b	0x1000144cc <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x37c>
100014630: aa0103e2    	mov	x2, x1
100014634: 17ffffb4    	b	0x100014504 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x3b4>
100014638: 92407c60    	and	x0, x3, #0xffffffff
10001463c: 9402dcd9    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100014640: 17ffffc3    	b	0x10001454c <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x3fc>
100014644: f9402be0    	ldr	x0, [sp, #0x50]
100014648: a9497bfd    	ldp	x29, x30, [sp, #0x90]
10001464c: 910283ff    	add	sp, sp, #0xa0
100014650: b9401389    	ldr	w9, [x28, #0x10]
100014654: 7100013f    	cmp	w9, #0x0
100014658: 5400010d    	b.le	0x100014678 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x528>
10001465c: d65f03c0    	ret
100014660: 52806020    	mov	w0, #0x301              ; =769
100014664: 72a00020    	movk	w0, #0x1, lsl #16
100014668: 8b000f60    	add	x0, x27, x0, lsl #3
10001466c: 9400fd0d    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100014670: 17fffec5    	b	0x100014184 <_Jvm$ManualLibraryFfm_run_XxF4eaxr0SHfMhCWpQVtdE+0x34>
100014674: 140311bf    	b	0x1000d8d70 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100014678: 1407b0ba    	b	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001467c: cccccccc    	<unknown>


; ffm-aot/generated-critical-ffm
0000000100011480 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA>:
100011480: d10343e9    	sub	x9, sp, #0xd0
100011484: f940078a    	ldr	x10, [x28, #0x8]
100011488: eb0a013f    	cmp	x9, x10
10001148c: 54003789    	b.ls	0x100011b7c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x6fc>
100011490: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100011494: 9100013f    	mov	sp, x9
100011498: b900bbe1    	str	w1, [sp, #0xb8]
10001149c: 52847a40    	mov	w0, #0x23d2             ; =9170
1000114a0: 72a00120    	movk	w0, #0x9, lsl #16
1000114a4: 8b000f60    	add	x0, x27, x0, lsl #3
1000114a8: 91008409    	add	x9, x0, #0x21
1000114ac: 08dffd20    	ldarb	w0, [x9]
1000114b0: 350035a0    	cbnz	w0, 0x100011b64 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x6e4>
1000114b4: 52804403    	mov	w3, #0x220              ; =544
1000114b8: 72a00023    	movk	w3, #0x1, lsl #16
1000114bc: 8b030f63    	add	x3, x27, x3, lsl #3
1000114c0: f90017e3    	str	x3, [sp, #0x28]
1000114c4: 52804604    	mov	w4, #0x230              ; =560
1000114c8: 72a00024    	movk	w4, #0x1, lsl #16
1000114cc: b90077e4    	str	w4, [sp, #0x74]
1000114d0: 52804745    	mov	w5, #0x23a              ; =570
1000114d4: 72a00025    	movk	w5, #0x1, lsl #16
1000114d8: 8b050f65    	add	x5, x27, x5, lsl #3
1000114dc: f9001be5    	str	x5, [sp, #0x30]
1000114e0: 52804a06    	mov	w6, #0x250              ; =592
1000114e4: 72a00026    	movk	w6, #0x1, lsl #16
1000114e8: b9007fe6    	str	w6, [sp, #0x7c]
1000114ec: 52804b47    	mov	w7, #0x25a              ; =602
1000114f0: 72a00027    	movk	w7, #0x1, lsl #16
1000114f4: 8b070f67    	add	x7, x27, x7, lsl #3
1000114f8: f9001fe7    	str	x7, [sp, #0x38]
1000114fc: 52804e08    	mov	w8, #0x270              ; =624
100011500: 72a00028    	movk	w8, #0x1, lsl #16
100011504: b90083e8    	str	w8, [sp, #0x80]
100011508: 52804f4b    	mov	w11, #0x27a             ; =634
10001150c: 72a0002b    	movk	w11, #0x1, lsl #16
100011510: 8b0b0f6b    	add	x11, x27, x11, lsl #3
100011514: f90023eb    	str	x11, [sp, #0x40]
100011518: 5280514c    	mov	w12, #0x28a             ; =650
10001151c: 72a0002c    	movk	w12, #0x1, lsl #16
100011520: b90087ec    	str	w12, [sp, #0x84]
100011524: 5280528d    	mov	w13, #0x294             ; =660
100011528: 72a0002d    	movk	w13, #0x1, lsl #16
10001152c: 8b0d0f6d    	add	x13, x27, x13, lsl #3
100011530: f90027ed    	str	x13, [sp, #0x48]
100011534: 5280554e    	mov	w14, #0x2aa             ; =682
100011538: 72a0002e    	movk	w14, #0x1, lsl #16
10001153c: b9008bee    	str	w14, [sp, #0x88]
100011540: 5280568f    	mov	w15, #0x2b4             ; =692
100011544: 72a0002f    	movk	w15, #0x1, lsl #16
100011548: 8b0f0f6f    	add	x15, x27, x15, lsl #3
10001154c: f90013ef    	str	x15, [sp, #0x20]
100011550: 52805950    	mov	w16, #0x2ca             ; =714
100011554: 72a00030    	movk	w16, #0x1, lsl #16
100011558: b9008ff0    	str	w16, [sp, #0x8c]
10001155c: 528b5011    	mov	w17, #0x5a80            ; =23168
100011560: 72a00411    	movk	w17, #0x20, lsl #16
100011564: 92407e33    	and	x19, x17, #0xffffffff
100011568: f90053f3    	str	x19, [sp, #0xa0]
10001156c: 528a5014    	mov	w20, #0x5280            ; =21120
100011570: 72a00414    	movk	w20, #0x20, lsl #16
100011574: 92407e95    	and	x21, x20, #0xffffffff
100011578: f9004ff5    	str	x21, [sp, #0x98]
10001157c: 52850456    	mov	w22, #0x2822            ; =10274
100011580: 72a00136    	movk	w22, #0x9, lsl #16
100011584: 8b160f76    	add	x22, x27, x22, lsl #3
100011588: f9002ff6    	str	x22, [sp, #0x58]
10001158c: 52851497    	mov	w23, #0x28a4            ; =10404
100011590: 72a00137    	movk	w23, #0x9, lsl #16
100011594: b9007bf7    	str	w23, [sp, #0x78]
100011598: 5284e018    	mov	w24, #0x2700            ; =9984
10001159c: 72a00138    	movk	w24, #0x9, lsl #16
1000115a0: 8b180f78    	add	x24, x27, x24, lsl #3
1000115a4: f9002bf8    	str	x24, [sp, #0x50]
1000115a8: aa0203e0    	mov	x0, x2
1000115ac: 2a1f03e2    	mov	w2, wzr
1000115b0: 6b01005f    	cmp	w2, w1
1000115b4: 54002c2a    	b.ge	0x100011b38 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x6b8>
1000115b8: 4b020039    	sub	w25, w1, w2
1000115bc: 7140073f    	cmp	w25, #0x1, lsl #12      ; =0x1000
1000115c0: 321403fd    	orr	w29, wzr, #0x1000
1000115c4: 1a9d9339    	csel	w25, w25, w29, ls
1000115c8: 7100073f    	cmp	w25, #0x1
1000115cc: 54001c6b    	b.lt	0x100011958 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x4d8>
1000115d0: b900bfe2    	str	w2, [sp, #0xbc]
1000115d4: b900b3f9    	str	w25, [sp, #0xb0]
1000115d8: 2a1f03e4    	mov	w4, wzr
1000115dc: aa0003e3    	mov	x3, x0
1000115e0: 14000066    	b	0x100011778 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x2f8>
1000115e4: d503201f    	nop
1000115e8: d503201f    	nop
1000115ec: d503201f    	nop
1000115f0: d503201f    	nop
1000115f4: d503201f    	nop
1000115f8: d503201f    	nop
1000115fc: d503201f    	nop
100011600: 2a0003e0    	mov	w0, w0
100011604: 8b000f60    	add	x0, x27, x0, lsl #3
100011608: b9400401    	ldr	w1, [x0, #0x4]
10001160c: 0b020085    	add	w5, w4, w2
100011610: 6b17003f    	cmp	w1, w23
100011614: 54001f41    	b.ne	0x1000119fc <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x57c>
100011618: b9400801    	ldr	w1, [x0, #0x8]
10001161c: 34001c41    	cbz	w1, 0x1000119a4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x524>
100011620: b900afe4    	str	w4, [sp, #0xac]
100011624: f90037e0    	str	x0, [sp, #0x68]
100011628: 91020061    	add	x1, x3, #0x80
10001162c: f104003f    	cmp	x1, #0x100
100011630: 54001002    	b.hs	0x100011830 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x3b0>
100011634: 11020061    	add	w1, w3, #0x80
100011638: 7104003f    	cmp	w1, #0x100
10001163c: 540029e2    	b.hs	0x100011b78 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x6f8>
100011640: b27d03e0    	orr	x0, xzr, #0x8
100011644: 8b214800    	add	x0, x0, w1, uxtw #2
100011648: b8606ac0    	ldr	w0, [x22, x0]
10001164c: 2a0003e0    	mov	w0, w0
100011650: 8b000f60    	add	x0, x27, x0, lsl #3
100011654: f90033e0    	str	x0, [sp, #0x60]
100011658: a9438782    	ldp	x2, x1, [x28, #0x38]
10001165c: 91004020    	add	x0, x1, #0x10
100011660: eb00005f    	cmp	x2, x0
100011664: 54001363    	b.lo	0x1000118d0 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x450>
100011668: f9002380    	str	x0, [x28, #0x40]
10001166c: b9000031    	str	w17, [x1]
100011670: 320003e0    	orr	w0, wzr, #0x1
100011674: b9000420    	str	w0, [x1, #0x4]
100011678: f900043f    	str	xzr, [x1, #0x8]
10001167c: f94033e0    	ldr	x0, [sp, #0x60]
100011680: b900b7e5    	str	w5, [sp, #0xb4]
100011684: f94037e2    	ldr	x2, [sp, #0x68]
100011688: f9004be3    	str	x3, [sp, #0x90]
10001168c: cb1b0000    	sub	x0, x0, x27
100011690: d343fc00    	lsr	x0, x0, #3
100011694: b9000820    	str	w0, [x1, #0x8]
100011698: d5033abf    	dmb	ishst
10001169c: aa0203e0    	mov	x0, x2
1000116a0: 940e10fc    	bl	0x100395a90 <_MethodHandle_invokeBasic_w1WLuXKv6oKBRkg53Rcaw3>
1000116a4: f94037e2    	ldr	x2, [sp, #0x68]
1000116a8: b9400441    	ldr	w1, [x2, #0x4]
1000116ac: 340017c1    	cbz	w1, 0x1000119a4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x524>
1000116b0: eb1b001f    	cmp	x0, x27
1000116b4: 54000d40    	b.eq	0x10001185c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x3dc>
1000116b8: b9400003    	ldr	w3, [x0]
1000116bc: 53057c63    	lsr	w3, w3, #5
1000116c0: 2a0303e3    	mov	w3, w3
1000116c4: 8b030f63    	add	x3, x27, x3, lsl #3
1000116c8: b9408ff0    	ldr	w16, [sp, #0x8c]
1000116cc: 2a0103e1    	mov	w1, w1
1000116d0: 8b010f61    	add	x1, x27, x1, lsl #3
1000116d4: b9400421    	ldr	w1, [x1, #0x4]
1000116d8: 6b10003f    	cmp	w1, w16
1000116dc: b9408bee    	ldr	w14, [sp, #0x88]
1000116e0: 54000520    	b.eq	0x100011784 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x304>
1000116e4: 6b0e003f    	cmp	w1, w14
1000116e8: 540006a0    	b.eq	0x1000117bc <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x33c>
1000116ec: b94087ec    	ldr	w12, [sp, #0x84]
1000116f0: 6b0c003f    	cmp	w1, w12
1000116f4: 54000820    	b.eq	0x1000117f8 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x378>
1000116f8: b94083e8    	ldr	w8, [sp, #0x80]
1000116fc: 6b08003f    	cmp	w1, w8
100011700: 54000b20    	b.eq	0x100011864 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x3e4>
100011704: b9407fe6    	ldr	w6, [sp, #0x7c]
100011708: 6b06003f    	cmp	w1, w6
10001170c: 54000c80    	b.eq	0x10001189c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x41c>
100011710: b94077e4    	ldr	w4, [sp, #0x74]
100011714: 6b04003f    	cmp	w1, w4
100011718: 54001481    	b.ne	0x1000119a8 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x528>
10001171c: eb1b001f    	cmp	x0, x27
100011720: 54001420    	b.eq	0x1000119a4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x524>
100011724: f94017e2    	ldr	x2, [sp, #0x28]
100011728: eb02007f    	cmp	x3, x2
10001172c: 54001801    	b.ne	0x100011a2c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x5ac>
100011730: 79400800    	ldrh	w0, [x0, #0x4]
100011734: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
100011738: f94013ef    	ldr	x15, [sp, #0x20]
10001173c: a94437eb    	ldp	x11, x13, [sp, #0x40]
100011740: a9431fe5    	ldp	x5, x7, [sp, #0x30]
100011744: 11000461    	add	w1, w3, #0x1
100011748: 6b19003f    	cmp	w1, w25
10001174c: 54000f0a    	b.ge	0x10001192c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x4ac>
100011750: aa0003e3    	mov	x3, x0
100011754: f9402bf8    	ldr	x24, [sp, #0x50]
100011758: b9407bf7    	ldr	w23, [sp, #0x78]
10001175c: f9402ff6    	ldr	x22, [sp, #0x58]
100011760: 528a5014    	mov	w20, #0x5280            ; =21120
100011764: 72a00414    	movk	w20, #0x20, lsl #16
100011768: 528b5011    	mov	w17, #0x5a80            ; =23168
10001176c: 72a00411    	movk	w17, #0x20, lsl #16
100011770: 2a0103e4    	mov	w4, w1
100011774: b940bfe2    	ldr	w2, [sp, #0xbc]
100011778: b940ab00    	ldr	w0, [x24, #0xa8]
10001177c: 35fff420    	cbnz	w0, 0x100011600 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x180>
100011780: 14000089    	b	0x1000119a4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x524>
100011784: 295033e8    	ldp	w8, w12, [sp, #0x80]
100011788: b9407fe6    	ldr	w6, [sp, #0x7c]
10001178c: b94077e4    	ldr	w4, [sp, #0x74]
100011790: f94017e2    	ldr	x2, [sp, #0x28]
100011794: eb1b001f    	cmp	x0, x27
100011798: 54001060    	b.eq	0x1000119a4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x524>
10001179c: f94013ef    	ldr	x15, [sp, #0x20]
1000117a0: eb0f007f    	cmp	x3, x15
1000117a4: 54001441    	b.ne	0x100011a2c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x5ac>
1000117a8: b9800400    	ldrsw	x0, [x0, #0x4]
1000117ac: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
1000117b0: a94437eb    	ldp	x11, x13, [sp, #0x40]
1000117b4: a9431fe5    	ldp	x5, x7, [sp, #0x30]
1000117b8: 17ffffe3    	b	0x100011744 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x2c4>
1000117bc: f94013ef    	ldr	x15, [sp, #0x20]
1000117c0: 295033e8    	ldp	w8, w12, [sp, #0x80]
1000117c4: b9407fe6    	ldr	w6, [sp, #0x7c]
1000117c8: b94077e4    	ldr	w4, [sp, #0x74]
1000117cc: f94017e2    	ldr	x2, [sp, #0x28]
1000117d0: eb1b001f    	cmp	x0, x27
1000117d4: 54000e80    	b.eq	0x1000119a4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x524>
1000117d8: f94027ed    	ldr	x13, [sp, #0x48]
1000117dc: eb0d007f    	cmp	x3, x13
1000117e0: 54001261    	b.ne	0x100011a2c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x5ac>
1000117e4: f9400400    	ldr	x0, [x0, #0x8]
1000117e8: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
1000117ec: a943afe7    	ldp	x7, x11, [sp, #0x38]
1000117f0: f9401be5    	ldr	x5, [sp, #0x30]
1000117f4: 17ffffd4    	b	0x100011744 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x2c4>
1000117f8: f94013ef    	ldr	x15, [sp, #0x20]
1000117fc: f94027ed    	ldr	x13, [sp, #0x48]
100011800: 294fa3e6    	ldp	w6, w8, [sp, #0x7c]
100011804: b94077e4    	ldr	w4, [sp, #0x74]
100011808: f94017e2    	ldr	x2, [sp, #0x28]
10001180c: eb1b001f    	cmp	x0, x27
100011810: 54000ca0    	b.eq	0x1000119a4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x524>
100011814: f94023eb    	ldr	x11, [sp, #0x40]
100011818: eb0b007f    	cmp	x3, x11
10001181c: 54001081    	b.ne	0x100011a2c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x5ac>
100011820: 39401000    	ldrb	w0, [x0, #0x4]
100011824: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
100011828: a9431fe5    	ldp	x5, x7, [sp, #0x30]
10001182c: 17ffffc6    	b	0x100011744 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x2c4>
100011830: a9438381    	ldp	x1, x0, [x28, #0x38]
100011834: 91004002    	add	x2, x0, #0x10
100011838: eb02003f    	cmp	x1, x2
10001183c: 540005a3    	b.lo	0x1000118f0 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x470>
100011840: f9002382    	str	x2, [x28, #0x40]
100011844: b9000014    	str	w20, [x0]
100011848: b900041f    	str	wzr, [x0, #0x4]
10001184c: f900041f    	str	xzr, [x0, #0x8]
100011850: f9000403    	str	x3, [x0, #0x8]
100011854: d5033abf    	dmb	ishst
100011858: 17ffff7f    	b	0x100011654 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x1d4>
10001185c: aa1b03e3    	mov	x3, x27
100011860: 17ffff9a    	b	0x1000116c8 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x248>
100011864: f94013ef    	ldr	x15, [sp, #0x20]
100011868: a94437eb    	ldp	x11, x13, [sp, #0x40]
10001186c: b9407fe6    	ldr	w6, [sp, #0x7c]
100011870: b94077e4    	ldr	w4, [sp, #0x74]
100011874: f94017e2    	ldr	x2, [sp, #0x28]
100011878: eb1b001f    	cmp	x0, x27
10001187c: 54000940    	b.eq	0x1000119a4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x524>
100011880: f9401fe7    	ldr	x7, [sp, #0x38]
100011884: eb07007f    	cmp	x3, x7
100011888: 54000d21    	b.ne	0x100011a2c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x5ac>
10001188c: 79800800    	ldrsh	x0, [x0, #0x4]
100011890: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
100011894: f9401be5    	ldr	x5, [sp, #0x30]
100011898: 17ffffab    	b	0x100011744 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x2c4>
10001189c: f94013ef    	ldr	x15, [sp, #0x20]
1000118a0: a94437eb    	ldp	x11, x13, [sp, #0x40]
1000118a4: f9401fe7    	ldr	x7, [sp, #0x38]
1000118a8: b94077e4    	ldr	w4, [sp, #0x74]
1000118ac: f94017e2    	ldr	x2, [sp, #0x28]
1000118b0: eb1b001f    	cmp	x0, x27
1000118b4: 54000780    	b.eq	0x1000119a4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x524>
1000118b8: f9401be5    	ldr	x5, [sp, #0x30]
1000118bc: eb05007f    	cmp	x3, x5
1000118c0: 54000b61    	b.ne	0x100011a2c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x5ac>
1000118c4: 39801000    	ldrsb	x0, [x0, #0x4]
1000118c8: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
1000118cc: 17ffff9e    	b	0x100011744 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x2c4>
1000118d0: b900b7e5    	str	w5, [sp, #0xb4]
1000118d4: f9004be3    	str	x3, [sp, #0x90]
1000118d8: f94053e0    	ldr	x0, [sp, #0xa0]
1000118dc: 320003e1    	orr	w1, wzr, #0x1
1000118e0: 9402e7cc    	bl	0x1000cb810 <_GenScavengeAllocationSupport_slowNewArray_RH1677CN3I59XJMcBPW18F>
1000118e4: aa0003e1    	mov	x1, x0
1000118e8: a9460be0    	ldp	x0, x2, [sp, #0x60]
1000118ec: 17ffff68    	b	0x10001168c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x20c>
1000118f0: b900b7e5    	str	w5, [sp, #0xb4]
1000118f4: f9004be3    	str	x3, [sp, #0x90]
1000118f8: f9404fe0    	ldr	x0, [sp, #0x98]
1000118fc: 9402e829    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100011900: b940b7e5    	ldr	w5, [sp, #0xb4]
100011904: b940afe4    	ldr	w4, [sp, #0xac]
100011908: f9404be3    	ldr	x3, [sp, #0x90]
10001190c: f9402bf8    	ldr	x24, [sp, #0x50]
100011910: b9407bf7    	ldr	w23, [sp, #0x78]
100011914: f9402ff6    	ldr	x22, [sp, #0x58]
100011918: 528a5014    	mov	w20, #0x5280            ; =21120
10001191c: 72a00414    	movk	w20, #0x20, lsl #16
100011920: 528b5011    	mov	w17, #0x5a80            ; =23168
100011924: 72a00411    	movk	w17, #0x20, lsl #16
100011928: 17ffffca    	b	0x100011850 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x3d0>
10001192c: b940bfe2    	ldr	w2, [sp, #0xbc]
100011930: 0b020022    	add	w2, w1, w2
100011934: f9402bf8    	ldr	x24, [sp, #0x50]
100011938: b9407bf7    	ldr	w23, [sp, #0x78]
10001193c: f9402ff6    	ldr	x22, [sp, #0x58]
100011940: 528a5014    	mov	w20, #0x5280            ; =21120
100011944: 72a00414    	movk	w20, #0x20, lsl #16
100011948: 528b5011    	mov	w17, #0x5a80            ; =23168
10001194c: 72a00411    	movk	w17, #0x20, lsl #16
100011950: b940bbe1    	ldr	w1, [sp, #0xb8]
100011954: 17ffff17    	b	0x1000115b0 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x130>
100011958: b9401389    	ldr	w9, [x28, #0x10]
10001195c: 34000129    	cbz	w9, 0x100011980 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x500>
100011960: f9402bf8    	ldr	x24, [sp, #0x50]
100011964: b9407bf7    	ldr	w23, [sp, #0x78]
100011968: f9402ff6    	ldr	x22, [sp, #0x58]
10001196c: 528a5014    	mov	w20, #0x5280            ; =21120
100011970: 72a00414    	movk	w20, #0x20, lsl #16
100011974: 528b5011    	mov	w17, #0x5a80            ; =23168
100011978: 72a00411    	movk	w17, #0x20, lsl #16
10001197c: 17ffff0d    	b	0x1000115b0 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x130>
100011980: 9407bbf8    	bl	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011984: f9402bf8    	ldr	x24, [sp, #0x50]
100011988: b9407bf7    	ldr	w23, [sp, #0x78]
10001198c: f9402ff6    	ldr	x22, [sp, #0x58]
100011990: 528a5014    	mov	w20, #0x5280            ; =21120
100011994: 72a00414    	movk	w20, #0x20, lsl #16
100011998: 528b5011    	mov	w17, #0x5a80            ; =23168
10001199c: 72a00411    	movk	w17, #0x20, lsl #16
1000119a0: 17ffff04    	b	0x1000115b0 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x130>
1000119a4: 94076dcf    	bl	0x1001ed0e0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
1000119a8: 52805c62    	mov	w2, #0x2e3              ; =739
1000119ac: 72a00022    	movk	w2, #0x1, lsl #16
1000119b0: 52952ea3    	mov	w3, #0xa975             ; =43381
1000119b4: 72a000e3    	movk	w3, #0x7, lsl #16
1000119b8: 8b030f63    	add	x3, x27, x3, lsl #3
1000119bc: 6b02003f    	cmp	w1, w2
1000119c0: 54000160    	b.eq	0x1000119ec <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x56c>
1000119c4: 52805da2    	mov	w2, #0x2ed              ; =749
1000119c8: 72a00022    	movk	w2, #0x1, lsl #16
1000119cc: 6b02003f    	cmp	w1, w2
1000119d0: 54000120    	b.eq	0x1000119f4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x574>
1000119d4: 52805ee2    	mov	w2, #0x2f7              ; =759
1000119d8: 72a00022    	movk	w2, #0x1, lsl #16
1000119dc: 6b02003f    	cmp	w1, w2
1000119e0: 54000281    	b.ne	0x100011a30 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x5b0>
1000119e4: aa0303e0    	mov	x0, x3
1000119e8: 94090866    	bl	0x100253b80 <_VMError_shouldNotReachHere_OJ2hHRdF16AAw9xjrSjhG4>
1000119ec: aa0303e0    	mov	x0, x3
1000119f0: 94090864    	bl	0x100253b80 <_VMError_shouldNotReachHere_OJ2hHRdF16AAw9xjrSjhG4>
1000119f4: aa0303e0    	mov	x0, x3
1000119f8: 94090862    	bl	0x100253b80 <_VMError_shouldNotReachHere_OJ2hHRdF16AAw9xjrSjhG4>
1000119fc: b900b7e5    	str	w5, [sp, #0xb4]
100011a00: 2a0103e1    	mov	w1, w1
100011a04: 8b010f61    	add	x1, x27, x1, lsl #3
100011a08: 52851482    	mov	w2, #0x28a4             ; =10404
100011a0c: 72a00122    	movk	w2, #0x9, lsl #16
100011a10: 8b020f62    	add	x2, x27, x2, lsl #3
100011a14: aa0103e0    	mov	x0, x1
100011a18: aa0203e1    	mov	x1, x2
100011a1c: 940d4681    	bl	0x100363420 <_Invokers_newWrongMethodTypeException_jNF0cwCrfG6sx7E5xixKa5>
100011a20: aa0003e2    	mov	x2, x0
100011a24: 910343e1    	add	x1, sp, #0xd0
100011a28: 94074182    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
100011a2c: 94076c6d    	bl	0x1001ecbe0 <_ImplicitExceptions_throwOptClassCastException_UXv9fg3pe9HQF6muVBaSh5>
100011a30: 34000641    	cbz	w1, 0x100011af8 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x678>
100011a34: f90033e0    	str	x0, [sp, #0x60]
100011a38: 2a0103e1    	mov	w1, w1
100011a3c: 8b010f61    	add	x1, x27, x1, lsl #3
100011a40: aa0103e0    	mov	x0, x1
100011a44: 940aaaa7    	bl	0x1002bc4e0 <_Class_toString_EntbmjmqdNIJmJ087QDxJE>
100011a48: f90013e0    	str	x0, [sp, #0x20]
100011a4c: 52916a61    	mov	w1, #0x8b53             ; =35667
100011a50: 72a000c1    	movk	w1, #0x6, lsl #16
100011a54: 8b010f61    	add	x1, x27, x1, lsl #3
100011a58: aa0003e2    	mov	x2, x0
100011a5c: aa0103e0    	mov	x0, x1
100011a60: aa0203e1    	mov	x1, x2
100011a64: 94073f6f    	bl	0x1001e1820 <_SubstrateStringConcatHelper_simpleConcat_raaFOKQ8p40ENYVQkgHRc9>
100011a68: f90017e0    	str	x0, [sp, #0x28]
100011a6c: a9438782    	ldp	x2, x1, [x28, #0x38]
100011a70: 91008023    	add	x3, x1, #0x20
100011a74: 52883804    	mov	w4, #0x41c0             ; =16832
100011a78: 72a00404    	movk	w4, #0x20, lsl #16
100011a7c: eb03005f    	cmp	x2, x3
100011a80: 54000683    	b.lo	0x100011b50 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x6d0>
100011a84: f9002383    	str	x3, [x28, #0x40]
100011a88: b9000024    	str	w4, [x1]
100011a8c: b900043f    	str	wzr, [x1, #0x4]
100011a90: a900fc3f    	stp	xzr, xzr, [x1, #0x8]
100011a94: f9000c3f    	str	xzr, [x1, #0x18]
100011a98: 52850022    	mov	w2, #0x2801             ; =10241
100011a9c: 72a00122    	movk	w2, #0x9, lsl #16
100011aa0: b9001422    	str	w2, [x1, #0x14]
100011aa4: 52850062    	mov	w2, #0x2803             ; =10243
100011aa8: 72a00122    	movk	w2, #0x9, lsl #16
100011aac: b9001022    	str	w2, [x1, #0x10]
100011ab0: cb1b0022    	sub	x2, x1, x27
100011ab4: d343fc42    	lsr	x2, x2, #3
100011ab8: b9000c22    	str	w2, [x1, #0xc]
100011abc: d5033abf    	dmb	ishst
100011ac0: aa0103e0    	mov	x0, x1
100011ac4: f90013e1    	str	x1, [sp, #0x20]
100011ac8: 2a1f03e1    	mov	w1, wzr
100011acc: 940c7859    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100011ad0: f94017e0    	ldr	x0, [sp, #0x28]
100011ad4: cb1b0000    	sub	x0, x0, x27
100011ad8: d343fc00    	lsr	x0, x0, #3
100011adc: f94013e2    	ldr	x2, [sp, #0x20]
100011ae0: b9000840    	str	w0, [x2, #0x8]
100011ae4: b9400040    	ldr	w0, [x2]
100011ae8: 37080120    	tbnz	w0, #0x1, 0x100011b0c <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x68c>
100011aec: 910343e1    	add	x1, sp, #0xd0
100011af0: aa0203e0    	mov	x0, x2
100011af4: 9407414f    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
100011af8: f90033e0    	str	x0, [sp, #0x60]
100011afc: 52915e20    	mov	w0, #0x8af1             ; =35569
100011b00: 72a000c0    	movk	w0, #0x6, lsl #16
100011b04: 8b000f60    	add	x0, x27, x0, lsl #3
100011b08: 17ffffd0    	b	0x100011a48 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x5c8>
100011b0c: aa0203e1    	mov	x1, x2
100011b10: b26db3e0    	mov	x0, #-0x80000           ; =-524288
100011b14: 8a000020    	and	x0, x1, x0
100011b18: 9121001d    	add	x29, x0, #0x840
100011b1c: cb1d003d    	sub	x29, x1, x29
100011b20: b27d0bfe    	orr	x30, xzr, #0x38
100011b24: 8b5d27dd    	add	x29, x30, x29, lsr #9
100011b28: 38fd681e    	ldrsb	w30, [x0, x29]
100011b2c: 34fffe1e    	cbz	w30, 0x100011aec <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x66c>
100011b30: 383d681f    	strb	wzr, [x0, x29]
100011b34: 17ffffee    	b	0x100011aec <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x66c>
100011b38: a94c7bfd    	ldp	x29, x30, [sp, #0xc0]
100011b3c: 910343ff    	add	sp, sp, #0xd0
100011b40: b9401389    	ldr	w9, [x28, #0x10]
100011b44: 7100013f    	cmp	w9, #0x0
100011b48: 540001cd    	b.le	0x100011b80 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x700>
100011b4c: d65f03c0    	ret
100011b50: 92407c82    	and	x2, x4, #0xffffffff
100011b54: aa0203e0    	mov	x0, x2
100011b58: 9402e792    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100011b5c: aa0003e1    	mov	x1, x0
100011b60: 17ffffce    	b	0x100011a98 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x618>
100011b64: 52806020    	mov	w0, #0x301              ; =769
100011b68: 72a00020    	movk	w0, #0x1, lsl #16
100011b6c: 8b000f60    	add	x0, x27, x0, lsl #3
100011b70: 940107cc    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100011b74: 17fffe50    	b	0x1000114b4 <_Jvm$GeneratedCriticalFfm_run_71wsi1deFa9NLiGP3XjxfA+0x34>
100011b78: 94076dfa    	bl	0x1001ed360 <_ImplicitExceptions_throwOptOutOfBoundsException_1u8LgzEFx5HG3Dm1iVN34B>
100011b7c: 14031c7d    	b	0x1000d8d70 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100011b80: 1407bb78    	b	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100011b84: cccccccc    	<unknown>
100011b88: cccccccc    	<unknown>
100011b8c: cccccccc    	<unknown>


; ffm-aot/manual-bound-critical-ffm
0000000100012e00 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF>:
100012e00: d10343e9    	sub	x9, sp, #0xd0
100012e04: f940078a    	ldr	x10, [x28, #0x8]
100012e08: eb0a013f    	cmp	x9, x10
100012e0c: 54003789    	b.ls	0x1000134fc <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x6fc>
100012e10: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100012e14: 9100013f    	mov	sp, x9
100012e18: b900bbe1    	str	w1, [sp, #0xb8]
100012e1c: 52847a40    	mov	w0, #0x23d2             ; =9170
100012e20: 72a00120    	movk	w0, #0x9, lsl #16
100012e24: 8b000f60    	add	x0, x27, x0, lsl #3
100012e28: 91008409    	add	x9, x0, #0x21
100012e2c: 08dffd20    	ldarb	w0, [x9]
100012e30: 350035a0    	cbnz	w0, 0x1000134e4 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x6e4>
100012e34: 52804403    	mov	w3, #0x220              ; =544
100012e38: 72a00023    	movk	w3, #0x1, lsl #16
100012e3c: 8b030f63    	add	x3, x27, x3, lsl #3
100012e40: f90017e3    	str	x3, [sp, #0x28]
100012e44: 52804604    	mov	w4, #0x230              ; =560
100012e48: 72a00024    	movk	w4, #0x1, lsl #16
100012e4c: b90077e4    	str	w4, [sp, #0x74]
100012e50: 52804745    	mov	w5, #0x23a              ; =570
100012e54: 72a00025    	movk	w5, #0x1, lsl #16
100012e58: 8b050f65    	add	x5, x27, x5, lsl #3
100012e5c: f9001be5    	str	x5, [sp, #0x30]
100012e60: 52804a06    	mov	w6, #0x250              ; =592
100012e64: 72a00026    	movk	w6, #0x1, lsl #16
100012e68: b9007fe6    	str	w6, [sp, #0x7c]
100012e6c: 52804b47    	mov	w7, #0x25a              ; =602
100012e70: 72a00027    	movk	w7, #0x1, lsl #16
100012e74: 8b070f67    	add	x7, x27, x7, lsl #3
100012e78: f9001fe7    	str	x7, [sp, #0x38]
100012e7c: 52804e08    	mov	w8, #0x270              ; =624
100012e80: 72a00028    	movk	w8, #0x1, lsl #16
100012e84: b90083e8    	str	w8, [sp, #0x80]
100012e88: 52804f4b    	mov	w11, #0x27a             ; =634
100012e8c: 72a0002b    	movk	w11, #0x1, lsl #16
100012e90: 8b0b0f6b    	add	x11, x27, x11, lsl #3
100012e94: f90023eb    	str	x11, [sp, #0x40]
100012e98: 5280514c    	mov	w12, #0x28a             ; =650
100012e9c: 72a0002c    	movk	w12, #0x1, lsl #16
100012ea0: b90087ec    	str	w12, [sp, #0x84]
100012ea4: 5280528d    	mov	w13, #0x294             ; =660
100012ea8: 72a0002d    	movk	w13, #0x1, lsl #16
100012eac: 8b0d0f6d    	add	x13, x27, x13, lsl #3
100012eb0: f90027ed    	str	x13, [sp, #0x48]
100012eb4: 5280554e    	mov	w14, #0x2aa             ; =682
100012eb8: 72a0002e    	movk	w14, #0x1, lsl #16
100012ebc: b9008bee    	str	w14, [sp, #0x88]
100012ec0: 5280568f    	mov	w15, #0x2b4             ; =692
100012ec4: 72a0002f    	movk	w15, #0x1, lsl #16
100012ec8: 8b0f0f6f    	add	x15, x27, x15, lsl #3
100012ecc: f90013ef    	str	x15, [sp, #0x20]
100012ed0: 52805950    	mov	w16, #0x2ca             ; =714
100012ed4: 72a00030    	movk	w16, #0x1, lsl #16
100012ed8: b9008ff0    	str	w16, [sp, #0x8c]
100012edc: 528b5011    	mov	w17, #0x5a80            ; =23168
100012ee0: 72a00411    	movk	w17, #0x20, lsl #16
100012ee4: 92407e33    	and	x19, x17, #0xffffffff
100012ee8: f90053f3    	str	x19, [sp, #0xa0]
100012eec: 528a5014    	mov	w20, #0x5280            ; =21120
100012ef0: 72a00414    	movk	w20, #0x20, lsl #16
100012ef4: 92407e95    	and	x21, x20, #0xffffffff
100012ef8: f9004ff5    	str	x21, [sp, #0x98]
100012efc: 52850456    	mov	w22, #0x2822            ; =10274
100012f00: 72a00136    	movk	w22, #0x9, lsl #16
100012f04: 8b160f76    	add	x22, x27, x22, lsl #3
100012f08: f9002ff6    	str	x22, [sp, #0x58]
100012f0c: 52851497    	mov	w23, #0x28a4            ; =10404
100012f10: 72a00137    	movk	w23, #0x9, lsl #16
100012f14: b9007bf7    	str	w23, [sp, #0x78]
100012f18: 5284e018    	mov	w24, #0x2700            ; =9984
100012f1c: 72a00138    	movk	w24, #0x9, lsl #16
100012f20: 8b180f78    	add	x24, x27, x24, lsl #3
100012f24: f9002bf8    	str	x24, [sp, #0x50]
100012f28: aa0203e0    	mov	x0, x2
100012f2c: 2a1f03e2    	mov	w2, wzr
100012f30: 6b01005f    	cmp	w2, w1
100012f34: 54002c2a    	b.ge	0x1000134b8 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x6b8>
100012f38: 4b020039    	sub	w25, w1, w2
100012f3c: 7140073f    	cmp	w25, #0x1, lsl #12      ; =0x1000
100012f40: 321403fd    	orr	w29, wzr, #0x1000
100012f44: 1a9d9339    	csel	w25, w25, w29, ls
100012f48: 7100073f    	cmp	w25, #0x1
100012f4c: 54001c6b    	b.lt	0x1000132d8 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x4d8>
100012f50: b900bfe2    	str	w2, [sp, #0xbc]
100012f54: b900b3f9    	str	w25, [sp, #0xb0]
100012f58: 2a1f03e4    	mov	w4, wzr
100012f5c: aa0003e3    	mov	x3, x0
100012f60: 14000066    	b	0x1000130f8 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x2f8>
100012f64: d503201f    	nop
100012f68: d503201f    	nop
100012f6c: d503201f    	nop
100012f70: d503201f    	nop
100012f74: d503201f    	nop
100012f78: d503201f    	nop
100012f7c: d503201f    	nop
100012f80: 2a0003e0    	mov	w0, w0
100012f84: 8b000f60    	add	x0, x27, x0, lsl #3
100012f88: b9400401    	ldr	w1, [x0, #0x4]
100012f8c: 0b020085    	add	w5, w4, w2
100012f90: 6b17003f    	cmp	w1, w23
100012f94: 54001f41    	b.ne	0x10001337c <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x57c>
100012f98: b9400801    	ldr	w1, [x0, #0x8]
100012f9c: 34001c41    	cbz	w1, 0x100013324 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x524>
100012fa0: b900afe4    	str	w4, [sp, #0xac]
100012fa4: f90037e0    	str	x0, [sp, #0x68]
100012fa8: 91020061    	add	x1, x3, #0x80
100012fac: f104003f    	cmp	x1, #0x100
100012fb0: 54001002    	b.hs	0x1000131b0 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x3b0>
100012fb4: 11020061    	add	w1, w3, #0x80
100012fb8: 7104003f    	cmp	w1, #0x100
100012fbc: 540029e2    	b.hs	0x1000134f8 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x6f8>
100012fc0: b27d03e0    	orr	x0, xzr, #0x8
100012fc4: 8b214800    	add	x0, x0, w1, uxtw #2
100012fc8: b8606ac0    	ldr	w0, [x22, x0]
100012fcc: 2a0003e0    	mov	w0, w0
100012fd0: 8b000f60    	add	x0, x27, x0, lsl #3
100012fd4: f90033e0    	str	x0, [sp, #0x60]
100012fd8: a9438782    	ldp	x2, x1, [x28, #0x38]
100012fdc: 91004020    	add	x0, x1, #0x10
100012fe0: eb00005f    	cmp	x2, x0
100012fe4: 54001363    	b.lo	0x100013250 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x450>
100012fe8: f9002380    	str	x0, [x28, #0x40]
100012fec: b9000031    	str	w17, [x1]
100012ff0: 320003e0    	orr	w0, wzr, #0x1
100012ff4: b9000420    	str	w0, [x1, #0x4]
100012ff8: f900043f    	str	xzr, [x1, #0x8]
100012ffc: f94033e0    	ldr	x0, [sp, #0x60]
100013000: b900b7e5    	str	w5, [sp, #0xb4]
100013004: f94037e2    	ldr	x2, [sp, #0x68]
100013008: f9004be3    	str	x3, [sp, #0x90]
10001300c: cb1b0000    	sub	x0, x0, x27
100013010: d343fc00    	lsr	x0, x0, #3
100013014: b9000820    	str	w0, [x1, #0x8]
100013018: d5033abf    	dmb	ishst
10001301c: aa0203e0    	mov	x0, x2
100013020: 940e0a9c    	bl	0x100395a90 <_MethodHandle_invokeBasic_w1WLuXKv6oKBRkg53Rcaw3>
100013024: f94037e2    	ldr	x2, [sp, #0x68]
100013028: b9400441    	ldr	w1, [x2, #0x4]
10001302c: 340017c1    	cbz	w1, 0x100013324 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x524>
100013030: eb1b001f    	cmp	x0, x27
100013034: 54000d40    	b.eq	0x1000131dc <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x3dc>
100013038: b9400003    	ldr	w3, [x0]
10001303c: 53057c63    	lsr	w3, w3, #5
100013040: 2a0303e3    	mov	w3, w3
100013044: 8b030f63    	add	x3, x27, x3, lsl #3
100013048: b9408ff0    	ldr	w16, [sp, #0x8c]
10001304c: 2a0103e1    	mov	w1, w1
100013050: 8b010f61    	add	x1, x27, x1, lsl #3
100013054: b9400421    	ldr	w1, [x1, #0x4]
100013058: 6b10003f    	cmp	w1, w16
10001305c: b9408bee    	ldr	w14, [sp, #0x88]
100013060: 54000520    	b.eq	0x100013104 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x304>
100013064: 6b0e003f    	cmp	w1, w14
100013068: 540006a0    	b.eq	0x10001313c <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x33c>
10001306c: b94087ec    	ldr	w12, [sp, #0x84]
100013070: 6b0c003f    	cmp	w1, w12
100013074: 54000820    	b.eq	0x100013178 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x378>
100013078: b94083e8    	ldr	w8, [sp, #0x80]
10001307c: 6b08003f    	cmp	w1, w8
100013080: 54000b20    	b.eq	0x1000131e4 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x3e4>
100013084: b9407fe6    	ldr	w6, [sp, #0x7c]
100013088: 6b06003f    	cmp	w1, w6
10001308c: 54000c80    	b.eq	0x10001321c <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x41c>
100013090: b94077e4    	ldr	w4, [sp, #0x74]
100013094: 6b04003f    	cmp	w1, w4
100013098: 54001481    	b.ne	0x100013328 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x528>
10001309c: eb1b001f    	cmp	x0, x27
1000130a0: 54001420    	b.eq	0x100013324 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x524>
1000130a4: f94017e2    	ldr	x2, [sp, #0x28]
1000130a8: eb02007f    	cmp	x3, x2
1000130ac: 54001801    	b.ne	0x1000133ac <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x5ac>
1000130b0: 79400800    	ldrh	w0, [x0, #0x4]
1000130b4: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
1000130b8: f94013ef    	ldr	x15, [sp, #0x20]
1000130bc: a94437eb    	ldp	x11, x13, [sp, #0x40]
1000130c0: a9431fe5    	ldp	x5, x7, [sp, #0x30]
1000130c4: 11000461    	add	w1, w3, #0x1
1000130c8: 6b19003f    	cmp	w1, w25
1000130cc: 54000f0a    	b.ge	0x1000132ac <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x4ac>
1000130d0: aa0003e3    	mov	x3, x0
1000130d4: f9402bf8    	ldr	x24, [sp, #0x50]
1000130d8: b9407bf7    	ldr	w23, [sp, #0x78]
1000130dc: f9402ff6    	ldr	x22, [sp, #0x58]
1000130e0: 528a5014    	mov	w20, #0x5280            ; =21120
1000130e4: 72a00414    	movk	w20, #0x20, lsl #16
1000130e8: 528b5011    	mov	w17, #0x5a80            ; =23168
1000130ec: 72a00411    	movk	w17, #0x20, lsl #16
1000130f0: 2a0103e4    	mov	w4, w1
1000130f4: b940bfe2    	ldr	w2, [sp, #0xbc]
1000130f8: b940b300    	ldr	w0, [x24, #0xb0]
1000130fc: 35fff420    	cbnz	w0, 0x100012f80 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x180>
100013100: 14000089    	b	0x100013324 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x524>
100013104: 295033e8    	ldp	w8, w12, [sp, #0x80]
100013108: b9407fe6    	ldr	w6, [sp, #0x7c]
10001310c: b94077e4    	ldr	w4, [sp, #0x74]
100013110: f94017e2    	ldr	x2, [sp, #0x28]
100013114: eb1b001f    	cmp	x0, x27
100013118: 54001060    	b.eq	0x100013324 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x524>
10001311c: f94013ef    	ldr	x15, [sp, #0x20]
100013120: eb0f007f    	cmp	x3, x15
100013124: 54001441    	b.ne	0x1000133ac <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x5ac>
100013128: b9800400    	ldrsw	x0, [x0, #0x4]
10001312c: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
100013130: a94437eb    	ldp	x11, x13, [sp, #0x40]
100013134: a9431fe5    	ldp	x5, x7, [sp, #0x30]
100013138: 17ffffe3    	b	0x1000130c4 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x2c4>
10001313c: f94013ef    	ldr	x15, [sp, #0x20]
100013140: 295033e8    	ldp	w8, w12, [sp, #0x80]
100013144: b9407fe6    	ldr	w6, [sp, #0x7c]
100013148: b94077e4    	ldr	w4, [sp, #0x74]
10001314c: f94017e2    	ldr	x2, [sp, #0x28]
100013150: eb1b001f    	cmp	x0, x27
100013154: 54000e80    	b.eq	0x100013324 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x524>
100013158: f94027ed    	ldr	x13, [sp, #0x48]
10001315c: eb0d007f    	cmp	x3, x13
100013160: 54001261    	b.ne	0x1000133ac <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x5ac>
100013164: f9400400    	ldr	x0, [x0, #0x8]
100013168: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
10001316c: a943afe7    	ldp	x7, x11, [sp, #0x38]
100013170: f9401be5    	ldr	x5, [sp, #0x30]
100013174: 17ffffd4    	b	0x1000130c4 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x2c4>
100013178: f94013ef    	ldr	x15, [sp, #0x20]
10001317c: f94027ed    	ldr	x13, [sp, #0x48]
100013180: 294fa3e6    	ldp	w6, w8, [sp, #0x7c]
100013184: b94077e4    	ldr	w4, [sp, #0x74]
100013188: f94017e2    	ldr	x2, [sp, #0x28]
10001318c: eb1b001f    	cmp	x0, x27
100013190: 54000ca0    	b.eq	0x100013324 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x524>
100013194: f94023eb    	ldr	x11, [sp, #0x40]
100013198: eb0b007f    	cmp	x3, x11
10001319c: 54001081    	b.ne	0x1000133ac <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x5ac>
1000131a0: 39401000    	ldrb	w0, [x0, #0x4]
1000131a4: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
1000131a8: a9431fe5    	ldp	x5, x7, [sp, #0x30]
1000131ac: 17ffffc6    	b	0x1000130c4 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x2c4>
1000131b0: a9438381    	ldp	x1, x0, [x28, #0x38]
1000131b4: 91004002    	add	x2, x0, #0x10
1000131b8: eb02003f    	cmp	x1, x2
1000131bc: 540005a3    	b.lo	0x100013270 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x470>
1000131c0: f9002382    	str	x2, [x28, #0x40]
1000131c4: b9000014    	str	w20, [x0]
1000131c8: b900041f    	str	wzr, [x0, #0x4]
1000131cc: f900041f    	str	xzr, [x0, #0x8]
1000131d0: f9000403    	str	x3, [x0, #0x8]
1000131d4: d5033abf    	dmb	ishst
1000131d8: 17ffff7f    	b	0x100012fd4 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x1d4>
1000131dc: aa1b03e3    	mov	x3, x27
1000131e0: 17ffff9a    	b	0x100013048 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x248>
1000131e4: f94013ef    	ldr	x15, [sp, #0x20]
1000131e8: a94437eb    	ldp	x11, x13, [sp, #0x40]
1000131ec: b9407fe6    	ldr	w6, [sp, #0x7c]
1000131f0: b94077e4    	ldr	w4, [sp, #0x74]
1000131f4: f94017e2    	ldr	x2, [sp, #0x28]
1000131f8: eb1b001f    	cmp	x0, x27
1000131fc: 54000940    	b.eq	0x100013324 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x524>
100013200: f9401fe7    	ldr	x7, [sp, #0x38]
100013204: eb07007f    	cmp	x3, x7
100013208: 54000d21    	b.ne	0x1000133ac <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x5ac>
10001320c: 79800800    	ldrsh	x0, [x0, #0x4]
100013210: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
100013214: f9401be5    	ldr	x5, [sp, #0x30]
100013218: 17ffffab    	b	0x1000130c4 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x2c4>
10001321c: f94013ef    	ldr	x15, [sp, #0x20]
100013220: a94437eb    	ldp	x11, x13, [sp, #0x40]
100013224: f9401fe7    	ldr	x7, [sp, #0x38]
100013228: b94077e4    	ldr	w4, [sp, #0x74]
10001322c: f94017e2    	ldr	x2, [sp, #0x28]
100013230: eb1b001f    	cmp	x0, x27
100013234: 54000780    	b.eq	0x100013324 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x524>
100013238: f9401be5    	ldr	x5, [sp, #0x30]
10001323c: eb05007f    	cmp	x3, x5
100013240: 54000b61    	b.ne	0x1000133ac <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x5ac>
100013244: 39801000    	ldrsb	x0, [x0, #0x4]
100013248: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
10001324c: 17ffff9e    	b	0x1000130c4 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x2c4>
100013250: b900b7e5    	str	w5, [sp, #0xb4]
100013254: f9004be3    	str	x3, [sp, #0x90]
100013258: f94053e0    	ldr	x0, [sp, #0xa0]
10001325c: 320003e1    	orr	w1, wzr, #0x1
100013260: 9402e16c    	bl	0x1000cb810 <_GenScavengeAllocationSupport_slowNewArray_RH1677CN3I59XJMcBPW18F>
100013264: aa0003e1    	mov	x1, x0
100013268: a9460be0    	ldp	x0, x2, [sp, #0x60]
10001326c: 17ffff68    	b	0x10001300c <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x20c>
100013270: b900b7e5    	str	w5, [sp, #0xb4]
100013274: f9004be3    	str	x3, [sp, #0x90]
100013278: f9404fe0    	ldr	x0, [sp, #0x98]
10001327c: 9402e1c9    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100013280: b940b7e5    	ldr	w5, [sp, #0xb4]
100013284: b940afe4    	ldr	w4, [sp, #0xac]
100013288: f9404be3    	ldr	x3, [sp, #0x90]
10001328c: f9402bf8    	ldr	x24, [sp, #0x50]
100013290: b9407bf7    	ldr	w23, [sp, #0x78]
100013294: f9402ff6    	ldr	x22, [sp, #0x58]
100013298: 528a5014    	mov	w20, #0x5280            ; =21120
10001329c: 72a00414    	movk	w20, #0x20, lsl #16
1000132a0: 528b5011    	mov	w17, #0x5a80            ; =23168
1000132a4: 72a00411    	movk	w17, #0x20, lsl #16
1000132a8: 17ffffca    	b	0x1000131d0 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x3d0>
1000132ac: b940bfe2    	ldr	w2, [sp, #0xbc]
1000132b0: 0b020022    	add	w2, w1, w2
1000132b4: f9402bf8    	ldr	x24, [sp, #0x50]
1000132b8: b9407bf7    	ldr	w23, [sp, #0x78]
1000132bc: f9402ff6    	ldr	x22, [sp, #0x58]
1000132c0: 528a5014    	mov	w20, #0x5280            ; =21120
1000132c4: 72a00414    	movk	w20, #0x20, lsl #16
1000132c8: 528b5011    	mov	w17, #0x5a80            ; =23168
1000132cc: 72a00411    	movk	w17, #0x20, lsl #16
1000132d0: b940bbe1    	ldr	w1, [sp, #0xb8]
1000132d4: 17ffff17    	b	0x100012f30 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x130>
1000132d8: b9401389    	ldr	w9, [x28, #0x10]
1000132dc: 34000129    	cbz	w9, 0x100013300 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x500>
1000132e0: f9402bf8    	ldr	x24, [sp, #0x50]
1000132e4: b9407bf7    	ldr	w23, [sp, #0x78]
1000132e8: f9402ff6    	ldr	x22, [sp, #0x58]
1000132ec: 528a5014    	mov	w20, #0x5280            ; =21120
1000132f0: 72a00414    	movk	w20, #0x20, lsl #16
1000132f4: 528b5011    	mov	w17, #0x5a80            ; =23168
1000132f8: 72a00411    	movk	w17, #0x20, lsl #16
1000132fc: 17ffff0d    	b	0x100012f30 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x130>
100013300: 9407b598    	bl	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100013304: f9402bf8    	ldr	x24, [sp, #0x50]
100013308: b9407bf7    	ldr	w23, [sp, #0x78]
10001330c: f9402ff6    	ldr	x22, [sp, #0x58]
100013310: 528a5014    	mov	w20, #0x5280            ; =21120
100013314: 72a00414    	movk	w20, #0x20, lsl #16
100013318: 528b5011    	mov	w17, #0x5a80            ; =23168
10001331c: 72a00411    	movk	w17, #0x20, lsl #16
100013320: 17ffff04    	b	0x100012f30 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x130>
100013324: 9407676f    	bl	0x1001ed0e0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
100013328: 52805c62    	mov	w2, #0x2e3              ; =739
10001332c: 72a00022    	movk	w2, #0x1, lsl #16
100013330: 52952ea3    	mov	w3, #0xa975             ; =43381
100013334: 72a000e3    	movk	w3, #0x7, lsl #16
100013338: 8b030f63    	add	x3, x27, x3, lsl #3
10001333c: 6b02003f    	cmp	w1, w2
100013340: 54000160    	b.eq	0x10001336c <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x56c>
100013344: 52805da2    	mov	w2, #0x2ed              ; =749
100013348: 72a00022    	movk	w2, #0x1, lsl #16
10001334c: 6b02003f    	cmp	w1, w2
100013350: 54000120    	b.eq	0x100013374 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x574>
100013354: 52805ee2    	mov	w2, #0x2f7              ; =759
100013358: 72a00022    	movk	w2, #0x1, lsl #16
10001335c: 6b02003f    	cmp	w1, w2
100013360: 54000281    	b.ne	0x1000133b0 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x5b0>
100013364: aa0303e0    	mov	x0, x3
100013368: 94090206    	bl	0x100253b80 <_VMError_shouldNotReachHere_OJ2hHRdF16AAw9xjrSjhG4>
10001336c: aa0303e0    	mov	x0, x3
100013370: 94090204    	bl	0x100253b80 <_VMError_shouldNotReachHere_OJ2hHRdF16AAw9xjrSjhG4>
100013374: aa0303e0    	mov	x0, x3
100013378: 94090202    	bl	0x100253b80 <_VMError_shouldNotReachHere_OJ2hHRdF16AAw9xjrSjhG4>
10001337c: b900b7e5    	str	w5, [sp, #0xb4]
100013380: 2a0103e1    	mov	w1, w1
100013384: 8b010f61    	add	x1, x27, x1, lsl #3
100013388: 52851482    	mov	w2, #0x28a4             ; =10404
10001338c: 72a00122    	movk	w2, #0x9, lsl #16
100013390: 8b020f62    	add	x2, x27, x2, lsl #3
100013394: aa0103e0    	mov	x0, x1
100013398: aa0203e1    	mov	x1, x2
10001339c: 940d4021    	bl	0x100363420 <_Invokers_newWrongMethodTypeException_jNF0cwCrfG6sx7E5xixKa5>
1000133a0: aa0003e2    	mov	x2, x0
1000133a4: 910343e1    	add	x1, sp, #0xd0
1000133a8: 94073b22    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
1000133ac: 9407660d    	bl	0x1001ecbe0 <_ImplicitExceptions_throwOptClassCastException_UXv9fg3pe9HQF6muVBaSh5>
1000133b0: 34000641    	cbz	w1, 0x100013478 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x678>
1000133b4: f90033e0    	str	x0, [sp, #0x60]
1000133b8: 2a0103e1    	mov	w1, w1
1000133bc: 8b010f61    	add	x1, x27, x1, lsl #3
1000133c0: aa0103e0    	mov	x0, x1
1000133c4: 940aa447    	bl	0x1002bc4e0 <_Class_toString_EntbmjmqdNIJmJ087QDxJE>
1000133c8: f90013e0    	str	x0, [sp, #0x20]
1000133cc: 52916a61    	mov	w1, #0x8b53             ; =35667
1000133d0: 72a000c1    	movk	w1, #0x6, lsl #16
1000133d4: 8b010f61    	add	x1, x27, x1, lsl #3
1000133d8: aa0003e2    	mov	x2, x0
1000133dc: aa0103e0    	mov	x0, x1
1000133e0: aa0203e1    	mov	x1, x2
1000133e4: 9407390f    	bl	0x1001e1820 <_SubstrateStringConcatHelper_simpleConcat_raaFOKQ8p40ENYVQkgHRc9>
1000133e8: f90017e0    	str	x0, [sp, #0x28]
1000133ec: a9438782    	ldp	x2, x1, [x28, #0x38]
1000133f0: 91008023    	add	x3, x1, #0x20
1000133f4: 52883804    	mov	w4, #0x41c0             ; =16832
1000133f8: 72a00404    	movk	w4, #0x20, lsl #16
1000133fc: eb03005f    	cmp	x2, x3
100013400: 54000683    	b.lo	0x1000134d0 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x6d0>
100013404: f9002383    	str	x3, [x28, #0x40]
100013408: b9000024    	str	w4, [x1]
10001340c: b900043f    	str	wzr, [x1, #0x4]
100013410: a900fc3f    	stp	xzr, xzr, [x1, #0x8]
100013414: f9000c3f    	str	xzr, [x1, #0x18]
100013418: 52850022    	mov	w2, #0x2801             ; =10241
10001341c: 72a00122    	movk	w2, #0x9, lsl #16
100013420: b9001422    	str	w2, [x1, #0x14]
100013424: 52850062    	mov	w2, #0x2803             ; =10243
100013428: 72a00122    	movk	w2, #0x9, lsl #16
10001342c: b9001022    	str	w2, [x1, #0x10]
100013430: cb1b0022    	sub	x2, x1, x27
100013434: d343fc42    	lsr	x2, x2, #3
100013438: b9000c22    	str	w2, [x1, #0xc]
10001343c: d5033abf    	dmb	ishst
100013440: aa0103e0    	mov	x0, x1
100013444: f90013e1    	str	x1, [sp, #0x20]
100013448: 2a1f03e1    	mov	w1, wzr
10001344c: 940c71f9    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100013450: f94017e0    	ldr	x0, [sp, #0x28]
100013454: cb1b0000    	sub	x0, x0, x27
100013458: d343fc00    	lsr	x0, x0, #3
10001345c: f94013e2    	ldr	x2, [sp, #0x20]
100013460: b9000840    	str	w0, [x2, #0x8]
100013464: b9400040    	ldr	w0, [x2]
100013468: 37080120    	tbnz	w0, #0x1, 0x10001348c <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x68c>
10001346c: 910343e1    	add	x1, sp, #0xd0
100013470: aa0203e0    	mov	x0, x2
100013474: 94073aef    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
100013478: f90033e0    	str	x0, [sp, #0x60]
10001347c: 52915e20    	mov	w0, #0x8af1             ; =35569
100013480: 72a000c0    	movk	w0, #0x6, lsl #16
100013484: 8b000f60    	add	x0, x27, x0, lsl #3
100013488: 17ffffd0    	b	0x1000133c8 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x5c8>
10001348c: aa0203e1    	mov	x1, x2
100013490: b26db3e0    	mov	x0, #-0x80000           ; =-524288
100013494: 8a000020    	and	x0, x1, x0
100013498: 9121001d    	add	x29, x0, #0x840
10001349c: cb1d003d    	sub	x29, x1, x29
1000134a0: b27d0bfe    	orr	x30, xzr, #0x38
1000134a4: 8b5d27dd    	add	x29, x30, x29, lsr #9
1000134a8: 38fd681e    	ldrsb	w30, [x0, x29]
1000134ac: 34fffe1e    	cbz	w30, 0x10001346c <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x66c>
1000134b0: 383d681f    	strb	wzr, [x0, x29]
1000134b4: 17ffffee    	b	0x10001346c <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x66c>
1000134b8: a94c7bfd    	ldp	x29, x30, [sp, #0xc0]
1000134bc: 910343ff    	add	sp, sp, #0xd0
1000134c0: b9401389    	ldr	w9, [x28, #0x10]
1000134c4: 7100013f    	cmp	w9, #0x0
1000134c8: 540001cd    	b.le	0x100013500 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x700>
1000134cc: d65f03c0    	ret
1000134d0: 92407c82    	and	x2, x4, #0xffffffff
1000134d4: aa0203e0    	mov	x0, x2
1000134d8: 9402e132    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
1000134dc: aa0003e1    	mov	x1, x0
1000134e0: 17ffffce    	b	0x100013418 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x618>
1000134e4: 52806020    	mov	w0, #0x301              ; =769
1000134e8: 72a00020    	movk	w0, #0x1, lsl #16
1000134ec: 8b000f60    	add	x0, x27, x0, lsl #3
1000134f0: 9401016c    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
1000134f4: 17fffe50    	b	0x100012e34 <_Jvm$ManualBoundCriticalFfm_run_Sf0t7EqK9sAkXBprsjRRNF+0x34>
1000134f8: 9407679a    	bl	0x1001ed360 <_ImplicitExceptions_throwOptOutOfBoundsException_1u8LgzEFx5HG3Dm1iVN34B>
1000134fc: 1403161d    	b	0x1000d8d70 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100013500: 1407b518    	b	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100013504: cccccccc    	<unknown>
100013508: cccccccc    	<unknown>
10001350c: cccccccc    	<unknown>


; ffm-aot/manual-bound-ffm
0000000100013510 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6>:
100013510: d10343e9    	sub	x9, sp, #0xd0
100013514: f940078a    	ldr	x10, [x28, #0x8]
100013518: eb0a013f    	cmp	x9, x10
10001351c: 54003789    	b.ls	0x100013c0c <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x6fc>
100013520: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100013524: 9100013f    	mov	sp, x9
100013528: b900bbe1    	str	w1, [sp, #0xb8]
10001352c: 52847a40    	mov	w0, #0x23d2             ; =9170
100013530: 72a00120    	movk	w0, #0x9, lsl #16
100013534: 8b000f60    	add	x0, x27, x0, lsl #3
100013538: 91008409    	add	x9, x0, #0x21
10001353c: 08dffd20    	ldarb	w0, [x9]
100013540: 350035a0    	cbnz	w0, 0x100013bf4 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x6e4>
100013544: 52804403    	mov	w3, #0x220              ; =544
100013548: 72a00023    	movk	w3, #0x1, lsl #16
10001354c: 8b030f63    	add	x3, x27, x3, lsl #3
100013550: f90017e3    	str	x3, [sp, #0x28]
100013554: 52804604    	mov	w4, #0x230              ; =560
100013558: 72a00024    	movk	w4, #0x1, lsl #16
10001355c: b90077e4    	str	w4, [sp, #0x74]
100013560: 52804745    	mov	w5, #0x23a              ; =570
100013564: 72a00025    	movk	w5, #0x1, lsl #16
100013568: 8b050f65    	add	x5, x27, x5, lsl #3
10001356c: f9001be5    	str	x5, [sp, #0x30]
100013570: 52804a06    	mov	w6, #0x250              ; =592
100013574: 72a00026    	movk	w6, #0x1, lsl #16
100013578: b9007fe6    	str	w6, [sp, #0x7c]
10001357c: 52804b47    	mov	w7, #0x25a              ; =602
100013580: 72a00027    	movk	w7, #0x1, lsl #16
100013584: 8b070f67    	add	x7, x27, x7, lsl #3
100013588: f9001fe7    	str	x7, [sp, #0x38]
10001358c: 52804e08    	mov	w8, #0x270              ; =624
100013590: 72a00028    	movk	w8, #0x1, lsl #16
100013594: b90083e8    	str	w8, [sp, #0x80]
100013598: 52804f4b    	mov	w11, #0x27a             ; =634
10001359c: 72a0002b    	movk	w11, #0x1, lsl #16
1000135a0: 8b0b0f6b    	add	x11, x27, x11, lsl #3
1000135a4: f90023eb    	str	x11, [sp, #0x40]
1000135a8: 5280514c    	mov	w12, #0x28a             ; =650
1000135ac: 72a0002c    	movk	w12, #0x1, lsl #16
1000135b0: b90087ec    	str	w12, [sp, #0x84]
1000135b4: 5280528d    	mov	w13, #0x294             ; =660
1000135b8: 72a0002d    	movk	w13, #0x1, lsl #16
1000135bc: 8b0d0f6d    	add	x13, x27, x13, lsl #3
1000135c0: f90027ed    	str	x13, [sp, #0x48]
1000135c4: 5280554e    	mov	w14, #0x2aa             ; =682
1000135c8: 72a0002e    	movk	w14, #0x1, lsl #16
1000135cc: b9008bee    	str	w14, [sp, #0x88]
1000135d0: 5280568f    	mov	w15, #0x2b4             ; =692
1000135d4: 72a0002f    	movk	w15, #0x1, lsl #16
1000135d8: 8b0f0f6f    	add	x15, x27, x15, lsl #3
1000135dc: f90013ef    	str	x15, [sp, #0x20]
1000135e0: 52805950    	mov	w16, #0x2ca             ; =714
1000135e4: 72a00030    	movk	w16, #0x1, lsl #16
1000135e8: b9008ff0    	str	w16, [sp, #0x8c]
1000135ec: 528b5011    	mov	w17, #0x5a80            ; =23168
1000135f0: 72a00411    	movk	w17, #0x20, lsl #16
1000135f4: 92407e33    	and	x19, x17, #0xffffffff
1000135f8: f90053f3    	str	x19, [sp, #0xa0]
1000135fc: 528a5014    	mov	w20, #0x5280            ; =21120
100013600: 72a00414    	movk	w20, #0x20, lsl #16
100013604: 92407e95    	and	x21, x20, #0xffffffff
100013608: f9004ff5    	str	x21, [sp, #0x98]
10001360c: 52850456    	mov	w22, #0x2822            ; =10274
100013610: 72a00136    	movk	w22, #0x9, lsl #16
100013614: 8b160f76    	add	x22, x27, x22, lsl #3
100013618: f9002ff6    	str	x22, [sp, #0x58]
10001361c: 52851497    	mov	w23, #0x28a4            ; =10404
100013620: 72a00137    	movk	w23, #0x9, lsl #16
100013624: b9007bf7    	str	w23, [sp, #0x78]
100013628: 5284e018    	mov	w24, #0x2700            ; =9984
10001362c: 72a00138    	movk	w24, #0x9, lsl #16
100013630: 8b180f78    	add	x24, x27, x24, lsl #3
100013634: f9002bf8    	str	x24, [sp, #0x50]
100013638: aa0203e0    	mov	x0, x2
10001363c: 2a1f03e2    	mov	w2, wzr
100013640: 6b01005f    	cmp	w2, w1
100013644: 54002c2a    	b.ge	0x100013bc8 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x6b8>
100013648: 4b020039    	sub	w25, w1, w2
10001364c: 7140073f    	cmp	w25, #0x1, lsl #12      ; =0x1000
100013650: 321403fd    	orr	w29, wzr, #0x1000
100013654: 1a9d9339    	csel	w25, w25, w29, ls
100013658: 7100073f    	cmp	w25, #0x1
10001365c: 54001c6b    	b.lt	0x1000139e8 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x4d8>
100013660: b900bfe2    	str	w2, [sp, #0xbc]
100013664: b900b3f9    	str	w25, [sp, #0xb0]
100013668: 2a1f03e4    	mov	w4, wzr
10001366c: aa0003e3    	mov	x3, x0
100013670: 14000066    	b	0x100013808 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x2f8>
100013674: d503201f    	nop
100013678: d503201f    	nop
10001367c: d503201f    	nop
100013680: d503201f    	nop
100013684: d503201f    	nop
100013688: d503201f    	nop
10001368c: d503201f    	nop
100013690: 2a0003e0    	mov	w0, w0
100013694: 8b000f60    	add	x0, x27, x0, lsl #3
100013698: b9400401    	ldr	w1, [x0, #0x4]
10001369c: 0b020085    	add	w5, w4, w2
1000136a0: 6b17003f    	cmp	w1, w23
1000136a4: 54001f41    	b.ne	0x100013a8c <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x57c>
1000136a8: b9400801    	ldr	w1, [x0, #0x8]
1000136ac: 34001c41    	cbz	w1, 0x100013a34 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x524>
1000136b0: b900afe4    	str	w4, [sp, #0xac]
1000136b4: f90037e0    	str	x0, [sp, #0x68]
1000136b8: 91020061    	add	x1, x3, #0x80
1000136bc: f104003f    	cmp	x1, #0x100
1000136c0: 54001002    	b.hs	0x1000138c0 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x3b0>
1000136c4: 11020061    	add	w1, w3, #0x80
1000136c8: 7104003f    	cmp	w1, #0x100
1000136cc: 540029e2    	b.hs	0x100013c08 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x6f8>
1000136d0: b27d03e0    	orr	x0, xzr, #0x8
1000136d4: 8b214800    	add	x0, x0, w1, uxtw #2
1000136d8: b8606ac0    	ldr	w0, [x22, x0]
1000136dc: 2a0003e0    	mov	w0, w0
1000136e0: 8b000f60    	add	x0, x27, x0, lsl #3
1000136e4: f90033e0    	str	x0, [sp, #0x60]
1000136e8: a9438782    	ldp	x2, x1, [x28, #0x38]
1000136ec: 91004020    	add	x0, x1, #0x10
1000136f0: eb00005f    	cmp	x2, x0
1000136f4: 54001363    	b.lo	0x100013960 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x450>
1000136f8: f9002380    	str	x0, [x28, #0x40]
1000136fc: b9000031    	str	w17, [x1]
100013700: 320003e0    	orr	w0, wzr, #0x1
100013704: b9000420    	str	w0, [x1, #0x4]
100013708: f900043f    	str	xzr, [x1, #0x8]
10001370c: f94033e0    	ldr	x0, [sp, #0x60]
100013710: b900b7e5    	str	w5, [sp, #0xb4]
100013714: f94037e2    	ldr	x2, [sp, #0x68]
100013718: f9004be3    	str	x3, [sp, #0x90]
10001371c: cb1b0000    	sub	x0, x0, x27
100013720: d343fc00    	lsr	x0, x0, #3
100013724: b9000820    	str	w0, [x1, #0x8]
100013728: d5033abf    	dmb	ishst
10001372c: aa0203e0    	mov	x0, x2
100013730: 940e08d8    	bl	0x100395a90 <_MethodHandle_invokeBasic_w1WLuXKv6oKBRkg53Rcaw3>
100013734: f94037e2    	ldr	x2, [sp, #0x68]
100013738: b9400441    	ldr	w1, [x2, #0x4]
10001373c: 340017c1    	cbz	w1, 0x100013a34 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x524>
100013740: eb1b001f    	cmp	x0, x27
100013744: 54000d40    	b.eq	0x1000138ec <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x3dc>
100013748: b9400003    	ldr	w3, [x0]
10001374c: 53057c63    	lsr	w3, w3, #5
100013750: 2a0303e3    	mov	w3, w3
100013754: 8b030f63    	add	x3, x27, x3, lsl #3
100013758: b9408ff0    	ldr	w16, [sp, #0x8c]
10001375c: 2a0103e1    	mov	w1, w1
100013760: 8b010f61    	add	x1, x27, x1, lsl #3
100013764: b9400421    	ldr	w1, [x1, #0x4]
100013768: 6b10003f    	cmp	w1, w16
10001376c: b9408bee    	ldr	w14, [sp, #0x88]
100013770: 54000520    	b.eq	0x100013814 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x304>
100013774: 6b0e003f    	cmp	w1, w14
100013778: 540006a0    	b.eq	0x10001384c <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x33c>
10001377c: b94087ec    	ldr	w12, [sp, #0x84]
100013780: 6b0c003f    	cmp	w1, w12
100013784: 54000820    	b.eq	0x100013888 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x378>
100013788: b94083e8    	ldr	w8, [sp, #0x80]
10001378c: 6b08003f    	cmp	w1, w8
100013790: 54000b20    	b.eq	0x1000138f4 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x3e4>
100013794: b9407fe6    	ldr	w6, [sp, #0x7c]
100013798: 6b06003f    	cmp	w1, w6
10001379c: 54000c80    	b.eq	0x10001392c <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x41c>
1000137a0: b94077e4    	ldr	w4, [sp, #0x74]
1000137a4: 6b04003f    	cmp	w1, w4
1000137a8: 54001481    	b.ne	0x100013a38 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x528>
1000137ac: eb1b001f    	cmp	x0, x27
1000137b0: 54001420    	b.eq	0x100013a34 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x524>
1000137b4: f94017e2    	ldr	x2, [sp, #0x28]
1000137b8: eb02007f    	cmp	x3, x2
1000137bc: 54001801    	b.ne	0x100013abc <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x5ac>
1000137c0: 79400800    	ldrh	w0, [x0, #0x4]
1000137c4: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
1000137c8: f94013ef    	ldr	x15, [sp, #0x20]
1000137cc: a94437eb    	ldp	x11, x13, [sp, #0x40]
1000137d0: a9431fe5    	ldp	x5, x7, [sp, #0x30]
1000137d4: 11000461    	add	w1, w3, #0x1
1000137d8: 6b19003f    	cmp	w1, w25
1000137dc: 54000f0a    	b.ge	0x1000139bc <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x4ac>
1000137e0: aa0003e3    	mov	x3, x0
1000137e4: f9402bf8    	ldr	x24, [sp, #0x50]
1000137e8: b9407bf7    	ldr	w23, [sp, #0x78]
1000137ec: f9402ff6    	ldr	x22, [sp, #0x58]
1000137f0: 528a5014    	mov	w20, #0x5280            ; =21120
1000137f4: 72a00414    	movk	w20, #0x20, lsl #16
1000137f8: 528b5011    	mov	w17, #0x5a80            ; =23168
1000137fc: 72a00411    	movk	w17, #0x20, lsl #16
100013800: 2a0103e4    	mov	w4, w1
100013804: b940bfe2    	ldr	w2, [sp, #0xbc]
100013808: b940a700    	ldr	w0, [x24, #0xa4]
10001380c: 35fff420    	cbnz	w0, 0x100013690 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x180>
100013810: 14000089    	b	0x100013a34 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x524>
100013814: 295033e8    	ldp	w8, w12, [sp, #0x80]
100013818: b9407fe6    	ldr	w6, [sp, #0x7c]
10001381c: b94077e4    	ldr	w4, [sp, #0x74]
100013820: f94017e2    	ldr	x2, [sp, #0x28]
100013824: eb1b001f    	cmp	x0, x27
100013828: 54001060    	b.eq	0x100013a34 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x524>
10001382c: f94013ef    	ldr	x15, [sp, #0x20]
100013830: eb0f007f    	cmp	x3, x15
100013834: 54001441    	b.ne	0x100013abc <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x5ac>
100013838: b9800400    	ldrsw	x0, [x0, #0x4]
10001383c: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
100013840: a94437eb    	ldp	x11, x13, [sp, #0x40]
100013844: a9431fe5    	ldp	x5, x7, [sp, #0x30]
100013848: 17ffffe3    	b	0x1000137d4 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x2c4>
10001384c: f94013ef    	ldr	x15, [sp, #0x20]
100013850: 295033e8    	ldp	w8, w12, [sp, #0x80]
100013854: b9407fe6    	ldr	w6, [sp, #0x7c]
100013858: b94077e4    	ldr	w4, [sp, #0x74]
10001385c: f94017e2    	ldr	x2, [sp, #0x28]
100013860: eb1b001f    	cmp	x0, x27
100013864: 54000e80    	b.eq	0x100013a34 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x524>
100013868: f94027ed    	ldr	x13, [sp, #0x48]
10001386c: eb0d007f    	cmp	x3, x13
100013870: 54001261    	b.ne	0x100013abc <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x5ac>
100013874: f9400400    	ldr	x0, [x0, #0x8]
100013878: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
10001387c: a943afe7    	ldp	x7, x11, [sp, #0x38]
100013880: f9401be5    	ldr	x5, [sp, #0x30]
100013884: 17ffffd4    	b	0x1000137d4 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x2c4>
100013888: f94013ef    	ldr	x15, [sp, #0x20]
10001388c: f94027ed    	ldr	x13, [sp, #0x48]
100013890: 294fa3e6    	ldp	w6, w8, [sp, #0x7c]
100013894: b94077e4    	ldr	w4, [sp, #0x74]
100013898: f94017e2    	ldr	x2, [sp, #0x28]
10001389c: eb1b001f    	cmp	x0, x27
1000138a0: 54000ca0    	b.eq	0x100013a34 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x524>
1000138a4: f94023eb    	ldr	x11, [sp, #0x40]
1000138a8: eb0b007f    	cmp	x3, x11
1000138ac: 54001081    	b.ne	0x100013abc <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x5ac>
1000138b0: 39401000    	ldrb	w0, [x0, #0x4]
1000138b4: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
1000138b8: a9431fe5    	ldp	x5, x7, [sp, #0x30]
1000138bc: 17ffffc6    	b	0x1000137d4 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x2c4>
1000138c0: a9438381    	ldp	x1, x0, [x28, #0x38]
1000138c4: 91004002    	add	x2, x0, #0x10
1000138c8: eb02003f    	cmp	x1, x2
1000138cc: 540005a3    	b.lo	0x100013980 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x470>
1000138d0: f9002382    	str	x2, [x28, #0x40]
1000138d4: b9000014    	str	w20, [x0]
1000138d8: b900041f    	str	wzr, [x0, #0x4]
1000138dc: f900041f    	str	xzr, [x0, #0x8]
1000138e0: f9000403    	str	x3, [x0, #0x8]
1000138e4: d5033abf    	dmb	ishst
1000138e8: 17ffff7f    	b	0x1000136e4 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x1d4>
1000138ec: aa1b03e3    	mov	x3, x27
1000138f0: 17ffff9a    	b	0x100013758 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x248>
1000138f4: f94013ef    	ldr	x15, [sp, #0x20]
1000138f8: a94437eb    	ldp	x11, x13, [sp, #0x40]
1000138fc: b9407fe6    	ldr	w6, [sp, #0x7c]
100013900: b94077e4    	ldr	w4, [sp, #0x74]
100013904: f94017e2    	ldr	x2, [sp, #0x28]
100013908: eb1b001f    	cmp	x0, x27
10001390c: 54000940    	b.eq	0x100013a34 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x524>
100013910: f9401fe7    	ldr	x7, [sp, #0x38]
100013914: eb07007f    	cmp	x3, x7
100013918: 54000d21    	b.ne	0x100013abc <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x5ac>
10001391c: 79800800    	ldrsh	x0, [x0, #0x4]
100013920: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
100013924: f9401be5    	ldr	x5, [sp, #0x30]
100013928: 17ffffab    	b	0x1000137d4 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x2c4>
10001392c: f94013ef    	ldr	x15, [sp, #0x20]
100013930: a94437eb    	ldp	x11, x13, [sp, #0x40]
100013934: f9401fe7    	ldr	x7, [sp, #0x38]
100013938: b94077e4    	ldr	w4, [sp, #0x74]
10001393c: f94017e2    	ldr	x2, [sp, #0x28]
100013940: eb1b001f    	cmp	x0, x27
100013944: 54000780    	b.eq	0x100013a34 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x524>
100013948: f9401be5    	ldr	x5, [sp, #0x30]
10001394c: eb05007f    	cmp	x3, x5
100013950: 54000b61    	b.ne	0x100013abc <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x5ac>
100013954: 39801000    	ldrsb	x0, [x0, #0x4]
100013958: 2955e7e3    	ldp	w3, w25, [sp, #0xac]
10001395c: 17ffff9e    	b	0x1000137d4 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x2c4>
100013960: b900b7e5    	str	w5, [sp, #0xb4]
100013964: f9004be3    	str	x3, [sp, #0x90]
100013968: f94053e0    	ldr	x0, [sp, #0xa0]
10001396c: 320003e1    	orr	w1, wzr, #0x1
100013970: 9402dfa8    	bl	0x1000cb810 <_GenScavengeAllocationSupport_slowNewArray_RH1677CN3I59XJMcBPW18F>
100013974: aa0003e1    	mov	x1, x0
100013978: a9460be0    	ldp	x0, x2, [sp, #0x60]
10001397c: 17ffff68    	b	0x10001371c <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x20c>
100013980: b900b7e5    	str	w5, [sp, #0xb4]
100013984: f9004be3    	str	x3, [sp, #0x90]
100013988: f9404fe0    	ldr	x0, [sp, #0x98]
10001398c: 9402e005    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100013990: b940b7e5    	ldr	w5, [sp, #0xb4]
100013994: b940afe4    	ldr	w4, [sp, #0xac]
100013998: f9404be3    	ldr	x3, [sp, #0x90]
10001399c: f9402bf8    	ldr	x24, [sp, #0x50]
1000139a0: b9407bf7    	ldr	w23, [sp, #0x78]
1000139a4: f9402ff6    	ldr	x22, [sp, #0x58]
1000139a8: 528a5014    	mov	w20, #0x5280            ; =21120
1000139ac: 72a00414    	movk	w20, #0x20, lsl #16
1000139b0: 528b5011    	mov	w17, #0x5a80            ; =23168
1000139b4: 72a00411    	movk	w17, #0x20, lsl #16
1000139b8: 17ffffca    	b	0x1000138e0 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x3d0>
1000139bc: b940bfe2    	ldr	w2, [sp, #0xbc]
1000139c0: 0b020022    	add	w2, w1, w2
1000139c4: f9402bf8    	ldr	x24, [sp, #0x50]
1000139c8: b9407bf7    	ldr	w23, [sp, #0x78]
1000139cc: f9402ff6    	ldr	x22, [sp, #0x58]
1000139d0: 528a5014    	mov	w20, #0x5280            ; =21120
1000139d4: 72a00414    	movk	w20, #0x20, lsl #16
1000139d8: 528b5011    	mov	w17, #0x5a80            ; =23168
1000139dc: 72a00411    	movk	w17, #0x20, lsl #16
1000139e0: b940bbe1    	ldr	w1, [sp, #0xb8]
1000139e4: 17ffff17    	b	0x100013640 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x130>
1000139e8: b9401389    	ldr	w9, [x28, #0x10]
1000139ec: 34000129    	cbz	w9, 0x100013a10 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x500>
1000139f0: f9402bf8    	ldr	x24, [sp, #0x50]
1000139f4: b9407bf7    	ldr	w23, [sp, #0x78]
1000139f8: f9402ff6    	ldr	x22, [sp, #0x58]
1000139fc: 528a5014    	mov	w20, #0x5280            ; =21120
100013a00: 72a00414    	movk	w20, #0x20, lsl #16
100013a04: 528b5011    	mov	w17, #0x5a80            ; =23168
100013a08: 72a00411    	movk	w17, #0x20, lsl #16
100013a0c: 17ffff0d    	b	0x100013640 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x130>
100013a10: 9407b3d4    	bl	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100013a14: f9402bf8    	ldr	x24, [sp, #0x50]
100013a18: b9407bf7    	ldr	w23, [sp, #0x78]
100013a1c: f9402ff6    	ldr	x22, [sp, #0x58]
100013a20: 528a5014    	mov	w20, #0x5280            ; =21120
100013a24: 72a00414    	movk	w20, #0x20, lsl #16
100013a28: 528b5011    	mov	w17, #0x5a80            ; =23168
100013a2c: 72a00411    	movk	w17, #0x20, lsl #16
100013a30: 17ffff04    	b	0x100013640 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x130>
100013a34: 940765ab    	bl	0x1001ed0e0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
100013a38: 52805c62    	mov	w2, #0x2e3              ; =739
100013a3c: 72a00022    	movk	w2, #0x1, lsl #16
100013a40: 52952ea3    	mov	w3, #0xa975             ; =43381
100013a44: 72a000e3    	movk	w3, #0x7, lsl #16
100013a48: 8b030f63    	add	x3, x27, x3, lsl #3
100013a4c: 6b02003f    	cmp	w1, w2
100013a50: 54000160    	b.eq	0x100013a7c <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x56c>
100013a54: 52805da2    	mov	w2, #0x2ed              ; =749
100013a58: 72a00022    	movk	w2, #0x1, lsl #16
100013a5c: 6b02003f    	cmp	w1, w2
100013a60: 54000120    	b.eq	0x100013a84 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x574>
100013a64: 52805ee2    	mov	w2, #0x2f7              ; =759
100013a68: 72a00022    	movk	w2, #0x1, lsl #16
100013a6c: 6b02003f    	cmp	w1, w2
100013a70: 54000281    	b.ne	0x100013ac0 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x5b0>
100013a74: aa0303e0    	mov	x0, x3
100013a78: 94090042    	bl	0x100253b80 <_VMError_shouldNotReachHere_OJ2hHRdF16AAw9xjrSjhG4>
100013a7c: aa0303e0    	mov	x0, x3
100013a80: 94090040    	bl	0x100253b80 <_VMError_shouldNotReachHere_OJ2hHRdF16AAw9xjrSjhG4>
100013a84: aa0303e0    	mov	x0, x3
100013a88: 9409003e    	bl	0x100253b80 <_VMError_shouldNotReachHere_OJ2hHRdF16AAw9xjrSjhG4>
100013a8c: b900b7e5    	str	w5, [sp, #0xb4]
100013a90: 2a0103e1    	mov	w1, w1
100013a94: 8b010f61    	add	x1, x27, x1, lsl #3
100013a98: 52851482    	mov	w2, #0x28a4             ; =10404
100013a9c: 72a00122    	movk	w2, #0x9, lsl #16
100013aa0: 8b020f62    	add	x2, x27, x2, lsl #3
100013aa4: aa0103e0    	mov	x0, x1
100013aa8: aa0203e1    	mov	x1, x2
100013aac: 940d3e5d    	bl	0x100363420 <_Invokers_newWrongMethodTypeException_jNF0cwCrfG6sx7E5xixKa5>
100013ab0: aa0003e2    	mov	x2, x0
100013ab4: 910343e1    	add	x1, sp, #0xd0
100013ab8: 9407395e    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
100013abc: 94076449    	bl	0x1001ecbe0 <_ImplicitExceptions_throwOptClassCastException_UXv9fg3pe9HQF6muVBaSh5>
100013ac0: 34000641    	cbz	w1, 0x100013b88 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x678>
100013ac4: f90033e0    	str	x0, [sp, #0x60]
100013ac8: 2a0103e1    	mov	w1, w1
100013acc: 8b010f61    	add	x1, x27, x1, lsl #3
100013ad0: aa0103e0    	mov	x0, x1
100013ad4: 940aa283    	bl	0x1002bc4e0 <_Class_toString_EntbmjmqdNIJmJ087QDxJE>
100013ad8: f90013e0    	str	x0, [sp, #0x20]
100013adc: 52916a61    	mov	w1, #0x8b53             ; =35667
100013ae0: 72a000c1    	movk	w1, #0x6, lsl #16
100013ae4: 8b010f61    	add	x1, x27, x1, lsl #3
100013ae8: aa0003e2    	mov	x2, x0
100013aec: aa0103e0    	mov	x0, x1
100013af0: aa0203e1    	mov	x1, x2
100013af4: 9407374b    	bl	0x1001e1820 <_SubstrateStringConcatHelper_simpleConcat_raaFOKQ8p40ENYVQkgHRc9>
100013af8: f90017e0    	str	x0, [sp, #0x28]
100013afc: a9438782    	ldp	x2, x1, [x28, #0x38]
100013b00: 91008023    	add	x3, x1, #0x20
100013b04: 52883804    	mov	w4, #0x41c0             ; =16832
100013b08: 72a00404    	movk	w4, #0x20, lsl #16
100013b0c: eb03005f    	cmp	x2, x3
100013b10: 54000683    	b.lo	0x100013be0 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x6d0>
100013b14: f9002383    	str	x3, [x28, #0x40]
100013b18: b9000024    	str	w4, [x1]
100013b1c: b900043f    	str	wzr, [x1, #0x4]
100013b20: a900fc3f    	stp	xzr, xzr, [x1, #0x8]
100013b24: f9000c3f    	str	xzr, [x1, #0x18]
100013b28: 52850022    	mov	w2, #0x2801             ; =10241
100013b2c: 72a00122    	movk	w2, #0x9, lsl #16
100013b30: b9001422    	str	w2, [x1, #0x14]
100013b34: 52850062    	mov	w2, #0x2803             ; =10243
100013b38: 72a00122    	movk	w2, #0x9, lsl #16
100013b3c: b9001022    	str	w2, [x1, #0x10]
100013b40: cb1b0022    	sub	x2, x1, x27
100013b44: d343fc42    	lsr	x2, x2, #3
100013b48: b9000c22    	str	w2, [x1, #0xc]
100013b4c: d5033abf    	dmb	ishst
100013b50: aa0103e0    	mov	x0, x1
100013b54: f90013e1    	str	x1, [sp, #0x20]
100013b58: 2a1f03e1    	mov	w1, wzr
100013b5c: 940c7035    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100013b60: f94017e0    	ldr	x0, [sp, #0x28]
100013b64: cb1b0000    	sub	x0, x0, x27
100013b68: d343fc00    	lsr	x0, x0, #3
100013b6c: f94013e2    	ldr	x2, [sp, #0x20]
100013b70: b9000840    	str	w0, [x2, #0x8]
100013b74: b9400040    	ldr	w0, [x2]
100013b78: 37080120    	tbnz	w0, #0x1, 0x100013b9c <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x68c>
100013b7c: 910343e1    	add	x1, sp, #0xd0
100013b80: aa0203e0    	mov	x0, x2
100013b84: 9407392b    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
100013b88: f90033e0    	str	x0, [sp, #0x60]
100013b8c: 52915e20    	mov	w0, #0x8af1             ; =35569
100013b90: 72a000c0    	movk	w0, #0x6, lsl #16
100013b94: 8b000f60    	add	x0, x27, x0, lsl #3
100013b98: 17ffffd0    	b	0x100013ad8 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x5c8>
100013b9c: aa0203e1    	mov	x1, x2
100013ba0: b26db3e0    	mov	x0, #-0x80000           ; =-524288
100013ba4: 8a000020    	and	x0, x1, x0
100013ba8: 9121001d    	add	x29, x0, #0x840
100013bac: cb1d003d    	sub	x29, x1, x29
100013bb0: b27d0bfe    	orr	x30, xzr, #0x38
100013bb4: 8b5d27dd    	add	x29, x30, x29, lsr #9
100013bb8: 38fd681e    	ldrsb	w30, [x0, x29]
100013bbc: 34fffe1e    	cbz	w30, 0x100013b7c <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x66c>
100013bc0: 383d681f    	strb	wzr, [x0, x29]
100013bc4: 17ffffee    	b	0x100013b7c <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x66c>
100013bc8: a94c7bfd    	ldp	x29, x30, [sp, #0xc0]
100013bcc: 910343ff    	add	sp, sp, #0xd0
100013bd0: b9401389    	ldr	w9, [x28, #0x10]
100013bd4: 7100013f    	cmp	w9, #0x0
100013bd8: 540001cd    	b.le	0x100013c10 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x700>
100013bdc: d65f03c0    	ret
100013be0: 92407c82    	and	x2, x4, #0xffffffff
100013be4: aa0203e0    	mov	x0, x2
100013be8: 9402df6e    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100013bec: aa0003e1    	mov	x1, x0
100013bf0: 17ffffce    	b	0x100013b28 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x618>
100013bf4: 52806020    	mov	w0, #0x301              ; =769
100013bf8: 72a00020    	movk	w0, #0x1, lsl #16
100013bfc: 8b000f60    	add	x0, x27, x0, lsl #3
100013c00: 9400ffa8    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100013c04: 17fffe50    	b	0x100013544 <_Jvm$ManualBoundFfm_run_ZsKYQp8raX1DUYK7NQKUv6+0x34>
100013c08: 940765d6    	bl	0x1001ed360 <_ImplicitExceptions_throwOptOutOfBoundsException_1u8LgzEFx5HG3Dm1iVN34B>
100013c0c: 14031459    	b	0x1000d8d70 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100013c10: 1407b354    	b	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100013c14: cccccccc    	<unknown>
100013c18: cccccccc    	<unknown>
100013c1c: cccccccc    	<unknown>


; ffm-aot/generated-typed-critical-ffm
0000000100012720 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ>:
100012720: d10203e9    	sub	x9, sp, #0x80
100012724: f940078a    	ldr	x10, [x28, #0x8]
100012728: eb0a013f    	cmp	x9, x10
10001272c: 54002ae9    	b.ls	0x100012c88 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x568>
100012730: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100012734: 9100013f    	mov	sp, x9
100012738: b90067e1    	str	w1, [sp, #0x64]
10001273c: 52847a40    	mov	w0, #0x23d2             ; =9170
100012740: 72a00120    	movk	w0, #0x9, lsl #16
100012744: 8b000f60    	add	x0, x27, x0, lsl #3
100012748: 91008409    	add	x9, x0, #0x21
10001274c: 08dffd20    	ldarb	w0, [x9]
100012750: 35002920    	cbnz	w0, 0x100012c74 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x554>
100012754: 52806163    	mov	w3, #0x30b              ; =779
100012758: 72a00023    	movk	w3, #0x1, lsl #16
10001275c: b9004be3    	str	w3, [sp, #0x48]
100012760: 528063c4    	mov	w4, #0x31e              ; =798
100012764: 72a00024    	movk	w4, #0x1, lsl #16
100012768: b9003fe4    	str	w4, [sp, #0x3c]
10001276c: 32007be5    	orr	w5, wzr, #0x7fffffff
100012770: 52806626    	mov	w6, #0x331              ; =817
100012774: 72a00026    	movk	w6, #0x1, lsl #16
100012778: b90047e6    	str	w6, [sp, #0x44]
10001277c: 52802de7    	mov	w7, #0x16f              ; =367
100012780: 72a00027    	movk	w7, #0x1, lsl #16
100012784: b90043e7    	str	w7, [sp, #0x40]
100012788: 5284e008    	mov	w8, #0x2700             ; =9984
10001278c: 72a00128    	movk	w8, #0x9, lsl #16
100012790: 8b080f68    	add	x8, x27, x8, lsl #3
100012794: f90013e8    	str	x8, [sp, #0x20]
100012798: aa0203e0    	mov	x0, x2
10001279c: 2a1f03e2    	mov	w2, wzr
1000127a0: 6b01005f    	cmp	w2, w1
1000127a4: 540025ca    	b.ge	0x100012c5c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x53c>
1000127a8: 4b02003e    	sub	w30, w1, w2
1000127ac: 714007df    	cmp	w30, #0x1, lsl #12      ; =0x1000
1000127b0: 321403eb    	orr	w11, wzr, #0x1000
1000127b4: 1a8b93cb    	csel	w11, w30, w11, ls
1000127b8: 7100057f    	cmp	w11, #0x1
1000127bc: 540012eb    	b.lt	0x100012a18 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x2f8>
1000127c0: 290d0beb    	stp	w11, w2, [sp, #0x68]
1000127c4: 2a1f03e5    	mov	w5, wzr
1000127c8: f9002be0    	str	x0, [sp, #0x50]
1000127cc: 1400004b    	b	0x1000128f8 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x1d8>
1000127d0: d503201f    	nop
1000127d4: d503201f    	nop
1000127d8: d503201f    	nop
1000127dc: d503201f    	nop
1000127e0: 2a0003e0    	mov	w0, w0
1000127e4: 8b000f60    	add	x0, x27, x0, lsl #3
1000127e8: b9400400    	ldr	w0, [x0, #0x4]
1000127ec: 34001300    	cbz	w0, 0x100012a4c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x32c>
1000127f0: 2a0003e0    	mov	w0, w0
1000127f4: 8b000f60    	add	x0, x27, x0, lsl #3
1000127f8: b9400400    	ldr	w0, [x0, #0x4]
1000127fc: 0b0200ae    	add	w14, w5, w2
100012800: 340012a0    	cbz	w0, 0x100012a54 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x334>
100012804: 2a0003e0    	mov	w0, w0
100012808: 8b000f60    	add	x0, x27, x0, lsl #3
10001280c: b940001e    	ldr	w30, [x0]
100012810: 53057fde    	lsr	w30, w30, #5
100012814: 6b0703df    	cmp	w30, w7
100012818: 540011c1    	b.ne	0x100012a50 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x330>
10001281c: f9400c0f    	ldr	x15, [x0, #0x18]
100012820: b400160f    	cbz	x15, 0x100012ae0 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x3c0>
100012824: b940041e    	ldr	w30, [x0, #0x4]
100012828: 3400113e    	cbz	w30, 0x100012a4c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x32c>
10001282c: 2a1e03fe    	mov	w30, w30
100012830: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100012834: f90017fe    	str	x30, [sp, #0x28]
100012838: b94003d0    	ldr	w16, [x30]
10001283c: 53057e10    	lsr	w16, w16, #5
100012840: 6b06021f    	cmp	w16, w6
100012844: 540006e0    	b.eq	0x100012920 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x200>
100012848: b94003d1    	ldr	w17, [x30]
10001284c: 53057e33    	lsr	w19, w17, #5
100012850: 2a1303f3    	mov	w19, w19
100012854: 8b130f73    	add	x19, x27, x19, lsl #3
100012858: 53057e31    	lsr	w17, w17, #5
10001285c: 6b04023f    	cmp	w17, w4
100012860: 54000520    	b.eq	0x100012904 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x1e4>
100012864: 6b03023f    	cmp	w17, w3
100012868: 54000960    	b.eq	0x100012994 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x274>
10001286c: b9004ff0    	str	w16, [sp, #0x4c]
100012870: f9000fe0    	str	x0, [sp, #0x18]
100012874: 290bbbe5    	stp	w5, w14, [sp, #0x5c]
100012878: f9403a6f    	ldr	x15, [x19, #0x70]
10001287c: 8b1a01ef    	add	x15, x15, x26
100012880: aa1e03e0    	mov	x0, x30
100012884: aa0f03fe    	mov	x30, x15
100012888: f9001bf3    	str	x19, [sp, #0x30]
10001288c: d63f03c0    	blr	x30
100012890: f9400fe0    	ldr	x0, [sp, #0x18]
100012894: f9400c1e    	ldr	x30, [x0, #0x18]
100012898: f9402be0    	ldr	x0, [sp, #0x50]
10001289c: d63f03c0    	blr	x30
1000128a0: f9002be0    	str	x0, [sp, #0x50]
1000128a4: b9404ff0    	ldr	w16, [sp, #0x4c]
1000128a8: b9403fe4    	ldr	w4, [sp, #0x3c]
1000128ac: 6b04021f    	cmp	w16, w4
1000128b0: 540007c0    	b.eq	0x1000129a8 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x288>
1000128b4: f9401bf3    	ldr	x19, [sp, #0x30]
1000128b8: f940467e    	ldr	x30, [x19, #0x88]
1000128bc: 8b1a03de    	add	x30, x30, x26
1000128c0: f94017e0    	ldr	x0, [sp, #0x28]
1000128c4: d63f03c0    	blr	x30
1000128c8: f9402be0    	ldr	x0, [sp, #0x50]
1000128cc: b9405fe5    	ldr	w5, [sp, #0x5c]
1000128d0: b9406beb    	ldr	w11, [sp, #0x68]
1000128d4: 110004a5    	add	w5, w5, #0x1
1000128d8: 6b0b00bf    	cmp	w5, w11
1000128dc: 540007ca    	b.ge	0x1000129d4 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x2b4>
1000128e0: f9002be0    	str	x0, [sp, #0x50]
1000128e4: b9406fe2    	ldr	w2, [sp, #0x6c]
1000128e8: f94013e8    	ldr	x8, [sp, #0x20]
1000128ec: 29481be7    	ldp	w7, w6, [sp, #0x40]
1000128f0: b9403fe4    	ldr	w4, [sp, #0x3c]
1000128f4: b9404be3    	ldr	w3, [sp, #0x48]
1000128f8: b940ad00    	ldr	w0, [x8, #0xac]
1000128fc: 35fff720    	cbnz	w0, 0x1000127e0 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0xc0>
100012900: 14000053    	b	0x100012a4c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x32c>
100012904: f9001bf3    	str	x19, [sp, #0x30]
100012908: b9004ff0    	str	w16, [sp, #0x4c]
10001290c: 290bbbe5    	stp	w5, w14, [sp, #0x5c]
100012910: f9402be0    	ldr	x0, [sp, #0x50]
100012914: aa0f03fe    	mov	x30, x15
100012918: d63f03c0    	blr	x30
10001291c: 17ffffe1    	b	0x1000128a0 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x180>
100012920: 290bbbe5    	stp	w5, w14, [sp, #0x5c]
100012924: 294187c2    	ldp	w2, w1, [x30, #0xc]
100012928: b9400bc3    	ldr	w3, [x30, #0x8]
10001292c: 34000403    	cbz	w3, 0x1000129ac <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x28c>
100012930: b9402b84    	ldr	w4, [x28, #0x28]
100012934: 6b03009f    	cmp	w4, w3
100012938: 540011a1    	b.ne	0x100012b6c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x44c>
10001293c: 7100005f    	cmp	w2, #0x0
100012940: 5400140b    	b.lt	0x100012bc0 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x4a0>
100012944: 32007be5    	orr	w5, wzr, #0x7fffffff
100012948: 6b05003f    	cmp	w1, w5
10001294c: 54000ce0    	b.eq	0x100012ae8 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x3c8>
100012950: 11000420    	add	w0, w1, #0x1
100012954: b90013c0    	str	w0, [x30, #0x10]
100012958: f9402be0    	ldr	x0, [sp, #0x50]
10001295c: aa0f03fe    	mov	x30, x15
100012960: d63f03c0    	blr	x30
100012964: b9402b9e    	ldr	w30, [x28, #0x28]
100012968: f94017e1    	ldr	x1, [sp, #0x28]
10001296c: b9400822    	ldr	w2, [x1, #0x8]
100012970: 6b0203df    	cmp	w30, w2
100012974: 54000280    	b.eq	0x1000129c4 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x2a4>
100012978: 9100503e    	add	x30, x1, #0x14
10001297c: 885f7fc1    	ldxr	w1, [x30]
100012980: 11000429    	add	w9, w1, #0x1
100012984: 880affc9    	stlxr	w10, w9, [x30]
100012988: 35ffffaa    	cbnz	w10, 0x10001297c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x25c>
10001298c: d5033bbf    	dmb	ish
100012990: 17ffffcf    	b	0x1000128cc <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x1ac>
100012994: 290bbbe5    	stp	w5, w14, [sp, #0x5c]
100012998: f9402be0    	ldr	x0, [sp, #0x50]
10001299c: aa0f03fe    	mov	x30, x15
1000129a0: d63f03c0    	blr	x30
1000129a4: 17ffffca    	b	0x1000128cc <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x1ac>
1000129a8: 17ffffc9    	b	0x1000128cc <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x1ac>
1000129ac: 32007be5    	orr	w5, wzr, #0x7fffffff
1000129b0: 7100005f    	cmp	w2, #0x0
1000129b4: 54000fcb    	b.lt	0x100012bac <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x48c>
1000129b8: 6b05003f    	cmp	w1, w5
1000129bc: 54fffca1    	b.ne	0x100012950 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x230>
1000129c0: 1400004a    	b	0x100012ae8 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x3c8>
1000129c4: b940103e    	ldr	w30, [x1, #0x10]
1000129c8: 510007de    	sub	w30, w30, #0x1
1000129cc: b900103e    	str	w30, [x1, #0x10]
1000129d0: 17ffffbf    	b	0x1000128cc <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x1ac>
1000129d4: b9406fe2    	ldr	w2, [sp, #0x6c]
1000129d8: 0b0200a2    	add	w2, w5, w2
1000129dc: b9401389    	ldr	w9, [x28, #0x10]
1000129e0: 340000e9    	cbz	w9, 0x1000129fc <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x2dc>
1000129e4: f94013e8    	ldr	x8, [sp, #0x20]
1000129e8: 29481be7    	ldp	w7, w6, [sp, #0x40]
1000129ec: b9403fe4    	ldr	w4, [sp, #0x3c]
1000129f0: b9404be3    	ldr	w3, [sp, #0x48]
1000129f4: b94067e1    	ldr	w1, [sp, #0x64]
1000129f8: 17ffff6a    	b	0x1000127a0 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x80>
1000129fc: 9407b7d9    	bl	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100012a00: f94013e8    	ldr	x8, [sp, #0x20]
100012a04: 29481be7    	ldp	w7, w6, [sp, #0x40]
100012a08: b9403fe4    	ldr	w4, [sp, #0x3c]
100012a0c: b9404be3    	ldr	w3, [sp, #0x48]
100012a10: b94067e1    	ldr	w1, [sp, #0x64]
100012a14: 17ffff63    	b	0x1000127a0 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x80>
100012a18: b9401389    	ldr	w9, [x28, #0x10]
100012a1c: 340000c9    	cbz	w9, 0x100012a34 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x314>
100012a20: f94013e8    	ldr	x8, [sp, #0x20]
100012a24: 29481be7    	ldp	w7, w6, [sp, #0x40]
100012a28: b9403fe4    	ldr	w4, [sp, #0x3c]
100012a2c: b9404be3    	ldr	w3, [sp, #0x48]
100012a30: 17ffff5c    	b	0x1000127a0 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x80>
100012a34: 9407b7cb    	bl	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100012a38: f94013e8    	ldr	x8, [sp, #0x20]
100012a3c: 29481be7    	ldp	w7, w6, [sp, #0x40]
100012a40: b9403fe4    	ldr	w4, [sp, #0x3c]
100012a44: b9404be3    	ldr	w3, [sp, #0x48]
100012a48: 17ffff56    	b	0x1000127a0 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x80>
100012a4c: 940769a5    	bl	0x1001ed0e0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
100012a50: 94076864    	bl	0x1001ecbe0 <_ImplicitExceptions_throwOptClassCastException_UXv9fg3pe9HQF6muVBaSh5>
100012a54: b90063ee    	str	w14, [sp, #0x60]
100012a58: a9438381    	ldp	x1, x0, [x28, #0x38]
100012a5c: 91008002    	add	x2, x0, #0x20
100012a60: 52845c03    	mov	w3, #0x22e0             ; =8928
100012a64: 72a00403    	movk	w3, #0x20, lsl #16
100012a68: eb02003f    	cmp	x1, x2
100012a6c: 54000e83    	b.lo	0x100012c3c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x51c>
100012a70: f9002382    	str	x2, [x28, #0x40]
100012a74: b9000003    	str	w3, [x0]
100012a78: b900041f    	str	wzr, [x0, #0x4]
100012a7c: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
100012a80: f9000c1f    	str	xzr, [x0, #0x18]
100012a84: 52850061    	mov	w1, #0x2803             ; =10243
100012a88: 72a00121    	movk	w1, #0x9, lsl #16
100012a8c: 52850022    	mov	w2, #0x2801             ; =10241
100012a90: 72a00122    	movk	w2, #0x9, lsl #16
100012a94: 29020801    	stp	w1, w2, [x0, #0x10]
100012a98: cb1b0001    	sub	x1, x0, x27
100012a9c: d343fc21    	lsr	x1, x1, #3
100012aa0: b9000c01    	str	w1, [x0, #0xc]
100012aa4: d5033abf    	dmb	ishst
100012aa8: aa0003e2    	mov	x2, x0
100012aac: 2a1f03e1    	mov	w1, wzr
100012ab0: f90017e2    	str	x2, [sp, #0x28]
100012ab4: 940c745f    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100012ab8: 5291d600    	mov	w0, #0x8eb0             ; =36528
100012abc: 72a000c0    	movk	w0, #0x6, lsl #16
100012ac0: f94017e1    	ldr	x1, [sp, #0x28]
100012ac4: b9000820    	str	w0, [x1, #0x8]
100012ac8: b9400020    	ldr	w0, [x1]
100012acc: 37080840    	tbnz	w0, #0x1, 0x100012bd4 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x4b4>
100012ad0: aa0103e2    	mov	x2, x1
100012ad4: 910203e1    	add	x1, sp, #0x80
100012ad8: aa0203e0    	mov	x0, x2
100012adc: 94073d55    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
100012ae0: b90063ee    	str	w14, [sp, #0x60]
100012ae4: 9401ffab    	bl	0x100092990 <_SubstrateForeignUtil_throwSymbolIsNullException_eG3tVnjFmBKOZbMYv5OKpB>
100012ae8: f9000fe0    	str	x0, [sp, #0x18]
100012aec: a9438381    	ldp	x1, x0, [x28, #0x38]
100012af0: 91008002    	add	x2, x0, #0x20
100012af4: 5287f003    	mov	w3, #0x3f80             ; =16256
100012af8: 72a00403    	movk	w3, #0x20, lsl #16
100012afc: eb02003f    	cmp	x1, x2
100012b00: 54000a83    	b.lo	0x100012c50 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x530>
100012b04: f9002382    	str	x2, [x28, #0x40]
100012b08: b9000003    	str	w3, [x0]
100012b0c: b900041f    	str	wzr, [x0, #0x4]
100012b10: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
100012b14: f9000c1f    	str	xzr, [x0, #0x18]
100012b18: 52850061    	mov	w1, #0x2803             ; =10243
100012b1c: 72a00121    	movk	w1, #0x9, lsl #16
100012b20: 52850022    	mov	w2, #0x2801             ; =10241
100012b24: 72a00122    	movk	w2, #0x9, lsl #16
100012b28: 29020801    	stp	w1, w2, [x0, #0x10]
100012b2c: cb1b0001    	sub	x1, x0, x27
100012b30: d343fc21    	lsr	x1, x1, #3
100012b34: b9000c01    	str	w1, [x0, #0xc]
100012b38: d5033abf    	dmb	ishst
100012b3c: aa0003e2    	mov	x2, x0
100012b40: 2a1f03e1    	mov	w1, wzr
100012b44: f9001be2    	str	x2, [sp, #0x30]
100012b48: 940c743a    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100012b4c: 52986a00    	mov	w0, #0xc350             ; =50000
100012b50: 72a000e0    	movk	w0, #0x7, lsl #16
100012b54: f9401be1    	ldr	x1, [sp, #0x30]
100012b58: b9000820    	str	w0, [x1, #0x8]
100012b5c: b9400020    	ldr	w0, [x1]
100012b60: 37080560    	tbnz	w0, #0x1, 0x100012c0c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x4ec>
100012b64: aa0103e2    	mov	x2, x1
100012b68: 17ffffdb    	b	0x100012ad4 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x3b4>
100012b6c: aa1e03e1    	mov	x1, x30
100012b70: 5285155e    	mov	w30, #0x28aa            ; =10410
100012b74: 72a0013e    	movk	w30, #0x9, lsl #16
100012b78: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100012b7c: f9000fe0    	str	x0, [sp, #0x18]
100012b80: b94003c2    	ldr	w2, [x30]
100012b84: 53057c42    	lsr	w2, w2, #5
100012b88: 2a0203e2    	mov	w2, w2
100012b8c: 8b020f62    	add	x2, x27, x2, lsl #3
100012b90: f9403c42    	ldr	x2, [x2, #0x78]
100012b94: 8b1a0042    	add	x2, x2, x26
100012b98: aa1e03e0    	mov	x0, x30
100012b9c: aa0203fe    	mov	x30, x2
100012ba0: d63f03c0    	blr	x30
100012ba4: aa0003e2    	mov	x2, x0
100012ba8: 17ffffcb    	b	0x100012ad4 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x3b4>
100012bac: aa1e03e1    	mov	x1, x30
100012bb0: 5285151e    	mov	w30, #0x28a8            ; =10408
100012bb4: 72a0013e    	movk	w30, #0x9, lsl #16
100012bb8: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100012bbc: 17fffff0    	b	0x100012b7c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x45c>
100012bc0: aa1e03e1    	mov	x1, x30
100012bc4: 5285151e    	mov	w30, #0x28a8            ; =10408
100012bc8: 72a0013e    	movk	w30, #0x9, lsl #16
100012bcc: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100012bd0: 17ffffeb    	b	0x100012b7c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x45c>
100012bd4: b26db3e0    	mov	x0, #-0x80000           ; =-524288
100012bd8: aa0103e2    	mov	x2, x1
100012bdc: 8a000040    	and	x0, x2, x0
100012be0: 91210003    	add	x3, x0, #0x840
100012be4: cb030042    	sub	x2, x2, x3
100012be8: b27d0be3    	orr	x3, xzr, #0x38
100012bec: 8b422462    	add	x2, x3, x2, lsr #9
100012bf0: 38e26803    	ldrsb	w3, [x0, x2]
100012bf4: 34000083    	cbz	w3, 0x100012c04 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x4e4>
100012bf8: 3822681f    	strb	wzr, [x0, x2]
100012bfc: aa0103e2    	mov	x2, x1
100012c00: 17ffffb5    	b	0x100012ad4 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x3b4>
100012c04: aa0103e2    	mov	x2, x1
100012c08: 17ffffb3    	b	0x100012ad4 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x3b4>
100012c0c: b26db3e0    	mov	x0, #-0x80000           ; =-524288
100012c10: aa0103e2    	mov	x2, x1
100012c14: 8a000040    	and	x0, x2, x0
100012c18: 91210003    	add	x3, x0, #0x840
100012c1c: cb030042    	sub	x2, x2, x3
100012c20: b27d0be3    	orr	x3, xzr, #0x38
100012c24: 8b422462    	add	x2, x3, x2, lsr #9
100012c28: 38e26803    	ldrsb	w3, [x0, x2]
100012c2c: 340000e3    	cbz	w3, 0x100012c48 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x528>
100012c30: 3822681f    	strb	wzr, [x0, x2]
100012c34: aa0103e2    	mov	x2, x1
100012c38: 17ffffa7    	b	0x100012ad4 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x3b4>
100012c3c: 92407c60    	and	x0, x3, #0xffffffff
100012c40: 9402e358    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100012c44: 17ffff90    	b	0x100012a84 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x364>
100012c48: aa0103e2    	mov	x2, x1
100012c4c: 17ffffa2    	b	0x100012ad4 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x3b4>
100012c50: 92407c60    	and	x0, x3, #0xffffffff
100012c54: 9402e353    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100012c58: 17ffffb0    	b	0x100012b18 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x3f8>
100012c5c: a9477bfd    	ldp	x29, x30, [sp, #0x70]
100012c60: 910203ff    	add	sp, sp, #0x80
100012c64: b9401389    	ldr	w9, [x28, #0x10]
100012c68: 7100013f    	cmp	w9, #0x0
100012c6c: 5400010d    	b.le	0x100012c8c <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x56c>
100012c70: d65f03c0    	ret
100012c74: 52806020    	mov	w0, #0x301              ; =769
100012c78: 72a00020    	movk	w0, #0x1, lsl #16
100012c7c: 8b000f60    	add	x0, x27, x0, lsl #3
100012c80: 94010388    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100012c84: 17fffeb4    	b	0x100012754 <_Jvm$GeneratedTypedCriticalFfm_run_amgVAMJjtsLwy3bU6mRupJ+0x34>
100012c88: 1403183a    	b	0x1000d8d70 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100012c8c: 1407b735    	b	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>


; ffm-aot/manual-unbound-critical-ffm
0000000100014680 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4>:
100014680: d10203e9    	sub	x9, sp, #0x80
100014684: f940078a    	ldr	x10, [x28, #0x8]
100014688: eb0a013f    	cmp	x9, x10
10001468c: 540026c9    	b.ls	0x100014b64 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x4e4>
100014690: a93f7bfd    	stp	x29, x30, [sp, #-0x10]
100014694: 9100013f    	mov	sp, x9
100014698: b90067e1    	str	w1, [sp, #0x64]
10001469c: 52847a40    	mov	w0, #0x23d2             ; =9170
1000146a0: 72a00120    	movk	w0, #0x9, lsl #16
1000146a4: 8b000f60    	add	x0, x27, x0, lsl #3
1000146a8: 91008409    	add	x9, x0, #0x21
1000146ac: 08dffd20    	ldarb	w0, [x9]
1000146b0: 35002500    	cbnz	w0, 0x100014b50 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x4d0>
1000146b4: 52806163    	mov	w3, #0x30b              ; =779
1000146b8: 72a00023    	movk	w3, #0x1, lsl #16
1000146bc: b9004be3    	str	w3, [sp, #0x48]
1000146c0: 528063c4    	mov	w4, #0x31e              ; =798
1000146c4: 72a00024    	movk	w4, #0x1, lsl #16
1000146c8: b90043e4    	str	w4, [sp, #0x40]
1000146cc: 32007be5    	orr	w5, wzr, #0x7fffffff
1000146d0: 52806626    	mov	w6, #0x331              ; =817
1000146d4: 72a00026    	movk	w6, #0x1, lsl #16
1000146d8: b90047e6    	str	w6, [sp, #0x44]
1000146dc: 5284e007    	mov	w7, #0x2700             ; =9984
1000146e0: 72a00127    	movk	w7, #0x9, lsl #16
1000146e4: 8b070f67    	add	x7, x27, x7, lsl #3
1000146e8: f9001be7    	str	x7, [sp, #0x30]
1000146ec: aa0203e0    	mov	x0, x2
1000146f0: 2a1f03e2    	mov	w2, wzr
1000146f4: d503201f    	nop
1000146f8: d503201f    	nop
1000146fc: d503201f    	nop
100014700: 6b01005f    	cmp	w2, w1
100014704: 540021aa    	b.ge	0x100014b38 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x4b8>
100014708: 4b02003e    	sub	w30, w1, w2
10001470c: 714007df    	cmp	w30, #0x1, lsl #12      ; =0x1000
100014710: 321403e8    	orr	w8, wzr, #0x1000
100014714: 1a8893c8    	csel	w8, w30, w8, ls
100014718: 7100051f    	cmp	w8, #0x1
10001471c: 5400112b    	b.lt	0x100014940 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x2c0>
100014720: 290d0be8    	stp	w8, w2, [sp, #0x68]
100014724: 2a1f03e5    	mov	w5, wzr
100014728: f9002be0    	str	x0, [sp, #0x50]
10001472c: 1400003c    	b	0x10001481c <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x19c>
100014730: d503201f    	nop
100014734: d503201f    	nop
100014738: d503201f    	nop
10001473c: d503201f    	nop
100014740: f9400daf    	ldr	x15, [x13, #0x18]
100014744: b400150f    	cbz	x15, 0x1000149e4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x364>
100014748: b94005a0    	ldr	w0, [x13, #0x4]
10001474c: 34001540    	cbz	w0, 0x1000149f4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x374>
100014750: 2a0003e0    	mov	w0, w0
100014754: 8b000f60    	add	x0, x27, x0, lsl #3
100014758: f90017e0    	str	x0, [sp, #0x28]
10001475c: b940001e    	ldr	w30, [x0]
100014760: 53057fde    	lsr	w30, w30, #5
100014764: 6b0603df    	cmp	w30, w6
100014768: 54000740    	b.eq	0x100014850 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x1d0>
10001476c: b9400010    	ldr	w16, [x0]
100014770: 53057e11    	lsr	w17, w16, #5
100014774: 2a1103f1    	mov	w17, w17
100014778: 8b110f71    	add	x17, x27, x17, lsl #3
10001477c: 53057e10    	lsr	w16, w16, #5
100014780: 6b04021f    	cmp	w16, w4
100014784: 54000580    	b.eq	0x100014834 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x1b4>
100014788: 6b03021f    	cmp	w16, w3
10001478c: 540009e0    	b.eq	0x1000148c8 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x248>
100014790: b9004ffe    	str	w30, [sp, #0x4c]
100014794: b90063ee    	str	w14, [sp, #0x60]
100014798: f90013ed    	str	x13, [sp, #0x20]
10001479c: b9005fe5    	str	w5, [sp, #0x5c]
1000147a0: f9403a2f    	ldr	x15, [x17, #0x70]
1000147a4: 8b1a01ef    	add	x15, x15, x26
1000147a8: aa0003f0    	mov	x16, x0
1000147ac: aa0f03fe    	mov	x30, x15
1000147b0: f9001ff1    	str	x17, [sp, #0x38]
1000147b4: d63f03c0    	blr	x30
1000147b8: f94013ed    	ldr	x13, [sp, #0x20]
1000147bc: f9400dbe    	ldr	x30, [x13, #0x18]
1000147c0: f9402be0    	ldr	x0, [sp, #0x50]
1000147c4: d63f03c0    	blr	x30
1000147c8: f90013e0    	str	x0, [sp, #0x20]
1000147cc: b9404ffe    	ldr	w30, [sp, #0x4c]
1000147d0: b94043e4    	ldr	w4, [sp, #0x40]
1000147d4: 6b0403df    	cmp	w30, w4
1000147d8: 54000760    	b.eq	0x1000148c4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x244>
1000147dc: f9401ff1    	ldr	x17, [sp, #0x38]
1000147e0: f940463e    	ldr	x30, [x17, #0x88]
1000147e4: 8b1a03de    	add	x30, x30, x26
1000147e8: f94017e0    	ldr	x0, [sp, #0x28]
1000147ec: d63f03c0    	blr	x30
1000147f0: f94013e0    	ldr	x0, [sp, #0x20]
1000147f4: b9405fe5    	ldr	w5, [sp, #0x5c]
1000147f8: b9406be8    	ldr	w8, [sp, #0x68]
1000147fc: 110004a5    	add	w5, w5, #0x1
100014800: 6b0800bf    	cmp	w5, w8
100014804: 5400080a    	b.ge	0x100014904 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x284>
100014808: f9002be0    	str	x0, [sp, #0x50]
10001480c: b9406fe2    	ldr	w2, [sp, #0x6c]
100014810: f9401be7    	ldr	x7, [sp, #0x30]
100014814: 29481be4    	ldp	w4, w6, [sp, #0x40]
100014818: b9404be3    	ldr	w3, [sp, #0x48]
10001481c: b940a0e0    	ldr	w0, [x7, #0xa0]
100014820: 2a0003ed    	mov	w13, w0
100014824: 8b0d0f6d    	add	x13, x27, x13, lsl #3
100014828: 0b0200ae    	add	w14, w5, w2
10001482c: 35fff8a0    	cbnz	w0, 0x100014740 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0xc0>
100014830: 1400004f    	b	0x10001496c <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x2ec>
100014834: f9001ff1    	str	x17, [sp, #0x38]
100014838: b9004ffe    	str	w30, [sp, #0x4c]
10001483c: 290bbbe5    	stp	w5, w14, [sp, #0x5c]
100014840: f9402be0    	ldr	x0, [sp, #0x50]
100014844: aa0f03fe    	mov	x30, x15
100014848: d63f03c0    	blr	x30
10001484c: 17ffffdf    	b	0x1000147c8 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x148>
100014850: 290bbbe5    	stp	w5, w14, [sp, #0x5c]
100014854: 29418402    	ldp	w2, w1, [x0, #0xc]
100014858: b940081e    	ldr	w30, [x0, #0x8]
10001485c: 3400041e    	cbz	w30, 0x1000148dc <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x25c>
100014860: b9402b83    	ldr	w3, [x28, #0x28]
100014864: 6b1e007f    	cmp	w3, w30
100014868: 540010a1    	b.ne	0x100014a7c <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x3fc>
10001486c: 7100005f    	cmp	w2, #0x0
100014870: 540012eb    	b.lt	0x100014acc <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x44c>
100014874: 32007be5    	orr	w5, wzr, #0x7fffffff
100014878: 6b05003f    	cmp	w1, w5
10001487c: 54000be0    	b.eq	0x1000149f8 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x378>
100014880: 1100043e    	add	w30, w1, #0x1
100014884: b900101e    	str	w30, [x0, #0x10]
100014888: f9402be0    	ldr	x0, [sp, #0x50]
10001488c: aa0f03fe    	mov	x30, x15
100014890: d63f03c0    	blr	x30
100014894: b9402b9e    	ldr	w30, [x28, #0x28]
100014898: f94017e1    	ldr	x1, [sp, #0x28]
10001489c: b9400822    	ldr	w2, [x1, #0x8]
1000148a0: 6b0203df    	cmp	w30, w2
1000148a4: 54000280    	b.eq	0x1000148f4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x274>
1000148a8: 9100503e    	add	x30, x1, #0x14
1000148ac: 885f7fc1    	ldxr	w1, [x30]
1000148b0: 11000429    	add	w9, w1, #0x1
1000148b4: 880affc9    	stlxr	w10, w9, [x30]
1000148b8: 35ffffaa    	cbnz	w10, 0x1000148ac <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x22c>
1000148bc: d5033bbf    	dmb	ish
1000148c0: 17ffffcd    	b	0x1000147f4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x174>
1000148c4: 17ffffcc    	b	0x1000147f4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x174>
1000148c8: 290bbbe5    	stp	w5, w14, [sp, #0x5c]
1000148cc: f9402be0    	ldr	x0, [sp, #0x50]
1000148d0: aa0f03fe    	mov	x30, x15
1000148d4: d63f03c0    	blr	x30
1000148d8: 17ffffc7    	b	0x1000147f4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x174>
1000148dc: 7100005f    	cmp	w2, #0x0
1000148e0: 54000eab    	b.lt	0x100014ab4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x434>
1000148e4: 32007be5    	orr	w5, wzr, #0x7fffffff
1000148e8: 6b05003f    	cmp	w1, w5
1000148ec: 54fffca1    	b.ne	0x100014880 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x200>
1000148f0: 14000042    	b	0x1000149f8 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x378>
1000148f4: b940103e    	ldr	w30, [x1, #0x10]
1000148f8: 510007de    	sub	w30, w30, #0x1
1000148fc: b900103e    	str	w30, [x1, #0x10]
100014900: 17ffffbd    	b	0x1000147f4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x174>
100014904: b9406fe2    	ldr	w2, [sp, #0x6c]
100014908: 0b0200a2    	add	w2, w5, w2
10001490c: b9401389    	ldr	w9, [x28, #0x10]
100014910: 340000c9    	cbz	w9, 0x100014928 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x2a8>
100014914: f9401be7    	ldr	x7, [sp, #0x30]
100014918: 29481be4    	ldp	w4, w6, [sp, #0x40]
10001491c: b9404be3    	ldr	w3, [sp, #0x48]
100014920: b94067e1    	ldr	w1, [sp, #0x64]
100014924: 17ffff77    	b	0x100014700 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x80>
100014928: 9407b00e    	bl	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001492c: f9401be7    	ldr	x7, [sp, #0x30]
100014930: 29481be4    	ldp	w4, w6, [sp, #0x40]
100014934: b9404be3    	ldr	w3, [sp, #0x48]
100014938: b94067e1    	ldr	w1, [sp, #0x64]
10001493c: 17ffff71    	b	0x100014700 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x80>
100014940: b9401389    	ldr	w9, [x28, #0x10]
100014944: 340000a9    	cbz	w9, 0x100014958 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x2d8>
100014948: f9401be7    	ldr	x7, [sp, #0x30]
10001494c: 29481be4    	ldp	w4, w6, [sp, #0x40]
100014950: b9404be3    	ldr	w3, [sp, #0x48]
100014954: 17ffff6b    	b	0x100014700 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x80>
100014958: 9407b002    	bl	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
10001495c: f9401be7    	ldr	x7, [sp, #0x30]
100014960: 29481be4    	ldp	w4, w6, [sp, #0x40]
100014964: b9404be3    	ldr	w3, [sp, #0x48]
100014968: 17ffff66    	b	0x100014700 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x80>
10001496c: b90063ee    	str	w14, [sp, #0x60]
100014970: a9438381    	ldp	x1, x0, [x28, #0x38]
100014974: 9100a002    	add	x2, x0, #0x28
100014978: 528d1003    	mov	w3, #0x6880             ; =26752
10001497c: 72a00403    	movk	w3, #0x20, lsl #16
100014980: eb02003f    	cmp	x1, x2
100014984: 54000c83    	b.lo	0x100014b14 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x494>
100014988: f9002382    	str	x2, [x28, #0x40]
10001498c: b9000003    	str	w3, [x0]
100014990: b900041f    	str	wzr, [x0, #0x4]
100014994: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
100014998: a901fc1f    	stp	xzr, xzr, [x0, #0x18]
10001499c: f90013ed    	str	x13, [sp, #0x20]
1000149a0: 52850061    	mov	w1, #0x2803             ; =10243
1000149a4: 72a00121    	movk	w1, #0x9, lsl #16
1000149a8: 52850022    	mov	w2, #0x2801             ; =10241
1000149ac: 72a00122    	movk	w2, #0x9, lsl #16
1000149b0: 29020801    	stp	w1, w2, [x0, #0x10]
1000149b4: cb1b0001    	sub	x1, x0, x27
1000149b8: d343fc21    	lsr	x1, x1, #3
1000149bc: b9000c01    	str	w1, [x0, #0xc]
1000149c0: d5033abf    	dmb	ishst
1000149c4: aa0003e2    	mov	x2, x0
1000149c8: 2a1f03e1    	mov	w1, wzr
1000149cc: f90017e2    	str	x2, [sp, #0x28]
1000149d0: 940c6c98    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
1000149d4: f94017e2    	ldr	x2, [sp, #0x28]
1000149d8: 910203e1    	add	x1, sp, #0x80
1000149dc: aa0203e0    	mov	x0, x2
1000149e0: 94073594    	bl	0x1001e2030 <_ExceptionUnwind_unwindExceptionWithoutCalleeSavedRegisters_D2uDbIIG0NJynRdXlWueWI>
1000149e4: b90063ee    	str	w14, [sp, #0x60]
1000149e8: f90013ed    	str	x13, [sp, #0x20]
1000149ec: f94013e0    	ldr	x0, [sp, #0x20]
1000149f0: 9401f7e8    	bl	0x100092990 <_SubstrateForeignUtil_throwSymbolIsNullException_eG3tVnjFmBKOZbMYv5OKpB>
1000149f4: 940761bb    	bl	0x1001ed0e0 <_ImplicitExceptions_throwOptNullPointerException_L5YebwHOF167z7SdrY01p8>
1000149f8: f90013ed    	str	x13, [sp, #0x20]
1000149fc: a9438381    	ldp	x1, x0, [x28, #0x38]
100014a00: 91008002    	add	x2, x0, #0x20
100014a04: 5287f003    	mov	w3, #0x3f80             ; =16256
100014a08: 72a00403    	movk	w3, #0x20, lsl #16
100014a0c: eb02003f    	cmp	x1, x2
100014a10: 540008e3    	b.lo	0x100014b2c <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x4ac>
100014a14: f9002382    	str	x2, [x28, #0x40]
100014a18: b9000003    	str	w3, [x0]
100014a1c: b900041f    	str	wzr, [x0, #0x4]
100014a20: a900fc1f    	stp	xzr, xzr, [x0, #0x8]
100014a24: f9000c1f    	str	xzr, [x0, #0x18]
100014a28: 52850061    	mov	w1, #0x2803             ; =10243
100014a2c: 72a00121    	movk	w1, #0x9, lsl #16
100014a30: 52850022    	mov	w2, #0x2801             ; =10241
100014a34: 72a00122    	movk	w2, #0x9, lsl #16
100014a38: 29020801    	stp	w1, w2, [x0, #0x10]
100014a3c: cb1b0001    	sub	x1, x0, x27
100014a40: d343fc21    	lsr	x1, x1, #3
100014a44: b9000c01    	str	w1, [x0, #0xc]
100014a48: d5033abf    	dmb	ishst
100014a4c: aa0003e2    	mov	x2, x0
100014a50: 2a1f03e1    	mov	w1, wzr
100014a54: f9001fe2    	str	x2, [sp, #0x38]
100014a58: 940c6c76    	bl	0x10032fc30 <_Throwable_fillInStackTrace_r1XmDo89qjHSNaXbvF0Ch6>
100014a5c: 52986a00    	mov	w0, #0xc350             ; =50000
100014a60: 72a000e0    	movk	w0, #0x7, lsl #16
100014a64: f9401fe1    	ldr	x1, [sp, #0x38]
100014a68: b9000820    	str	w0, [x1, #0x8]
100014a6c: b9400020    	ldr	w0, [x1]
100014a70: 370803a0    	tbnz	w0, #0x1, 0x100014ae4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x464>
100014a74: aa0103e2    	mov	x2, x1
100014a78: 17ffffd8    	b	0x1000149d8 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x358>
100014a7c: aa0003e1    	mov	x1, x0
100014a80: f90013ed    	str	x13, [sp, #0x20]
100014a84: 52851540    	mov	w0, #0x28aa             ; =10410
100014a88: 72a00120    	movk	w0, #0x9, lsl #16
100014a8c: 8b000f60    	add	x0, x27, x0, lsl #3
100014a90: b940001e    	ldr	w30, [x0]
100014a94: 53057fde    	lsr	w30, w30, #5
100014a98: 2a1e03fe    	mov	w30, w30
100014a9c: 8b1e0f7e    	add	x30, x27, x30, lsl #3
100014aa0: f9403fde    	ldr	x30, [x30, #0x78]
100014aa4: 8b1a03de    	add	x30, x30, x26
100014aa8: d63f03c0    	blr	x30
100014aac: aa0003e2    	mov	x2, x0
100014ab0: 17ffffca    	b	0x1000149d8 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x358>
100014ab4: f94017e1    	ldr	x1, [sp, #0x28]
100014ab8: f90013ed    	str	x13, [sp, #0x20]
100014abc: 52851500    	mov	w0, #0x28a8             ; =10408
100014ac0: 72a00120    	movk	w0, #0x9, lsl #16
100014ac4: 8b000f60    	add	x0, x27, x0, lsl #3
100014ac8: 17fffff2    	b	0x100014a90 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x410>
100014acc: aa0003e1    	mov	x1, x0
100014ad0: f90013ed    	str	x13, [sp, #0x20]
100014ad4: 52851500    	mov	w0, #0x28a8             ; =10408
100014ad8: 72a00120    	movk	w0, #0x9, lsl #16
100014adc: 8b000f60    	add	x0, x27, x0, lsl #3
100014ae0: 17ffffec    	b	0x100014a90 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x410>
100014ae4: aa0103e0    	mov	x0, x1
100014ae8: b26db3e2    	mov	x2, #-0x80000           ; =-524288
100014aec: 8a020002    	and	x2, x0, x2
100014af0: 91210043    	add	x3, x2, #0x840
100014af4: cb030000    	sub	x0, x0, x3
100014af8: b27d0be3    	orr	x3, xzr, #0x38
100014afc: 8b402460    	add	x0, x3, x0, lsr #9
100014b00: 38e06843    	ldrsb	w3, [x2, x0]
100014b04: 34000103    	cbz	w3, 0x100014b24 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x4a4>
100014b08: 3820685f    	strb	wzr, [x2, x0]
100014b0c: aa0103e2    	mov	x2, x1
100014b10: 17ffffb2    	b	0x1000149d8 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x358>
100014b14: f90013ed    	str	x13, [sp, #0x20]
100014b18: 92407c60    	and	x0, x3, #0xffffffff
100014b1c: 9402dba1    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100014b20: 17ffffa0    	b	0x1000149a0 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x320>
100014b24: aa0103e2    	mov	x2, x1
100014b28: 17ffffac    	b	0x1000149d8 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x358>
100014b2c: 92407c60    	and	x0, x3, #0xffffffff
100014b30: 9402db9c    	bl	0x1000cb9a0 <_GenScavengeAllocationSupport_slowNewInstance_cVwSGnaka62OV0G4bo2NyI>
100014b34: 17ffffbd    	b	0x100014a28 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x3a8>
100014b38: a9477bfd    	ldp	x29, x30, [sp, #0x70]
100014b3c: 910203ff    	add	sp, sp, #0x80
100014b40: b9401389    	ldr	w9, [x28, #0x10]
100014b44: 7100013f    	cmp	w9, #0x0
100014b48: 5400010d    	b.le	0x100014b68 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x4e8>
100014b4c: d65f03c0    	ret
100014b50: 52806020    	mov	w0, #0x301              ; =769
100014b54: 72a00020    	movk	w0, #0x1, lsl #16
100014b58: 8b000f60    	add	x0, x27, x0, lsl #3
100014b5c: 9400fbd1    	bl	0x100053aa0 <_ClassInitializationInfo_slowPath_4IkmzbbP3iDWUlsbaqoCg7>
100014b60: 17fffed5    	b	0x1000146b4 <_Jvm$ManualUnboundCriticalFfm_run_Y6QXTjyvVbBkiwRU5sIfg4+0x34>
100014b64: 14031083    	b	0x1000d8d70 <_StackOverflowCheckImpl_throwNewStackOverflowError_CiGyUiZvwzBgNjjkTo2w7K>
100014b68: 1407af7e    	b	0x100200960 <_SafepointSlowpath_enterSlowPathSafepointCheckCalleeSavedCCONV_NJQ7tMr7bSJNP3XSqfMbAB>
100014b6c: cccccccc    	<unknown>
