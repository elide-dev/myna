// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
// A no_std implementer without a global allocator that includes seam.rs for a module with batches
// and reads one through the generated view: seam.rs must build here, so it allocates nothing.
#![no_std]
#[allow(dead_code, non_snake_case)]
mod seam { include!(concat!(env!("SEAM_GENERATED"), "/seam.rs")); }
#[panic_handler]
fn panic(_: &core::panic::PanicInfo) -> ! { loop {} }
#[unsafe(no_mangle)]
pub unsafe extern "C" fn batch_view_total(sources: *const *mut u8, lengths: *const u64, count: u64) -> u64 {
    let batch = unsafe { seam::BufferBatch::from_raw(sources, lengths, count) };
    batch.iter().map(|bytes| bytes.iter().map(|&byte| u64::from(byte)).sum::<u64>()).sum()
}
// The caller side too: both wrapper forms instantiate the lending helpers. The extern symbol is
// referenced, not linked, as a staticlib leaves it to the final link.
#[unsafe(no_mangle)]
pub extern "C" fn batch_view_lend(first: u8, second: u8) -> u64 {
    let (a, b) = ([first], [second, first]);
    let buffers: [&[u8]; 2] = [&a, &b];
    let mut pointers = [core::mem::MaybeUninit::uninit(); 2];
    let mut lengths = [core::mem::MaybeUninit::uninit(); 2];
    let scratch = seam::BatchScratch::new(&mut pointers, &mut lengths);
    seam::ownership::digest(&buffers) ^ seam::ownership::digest_with_scratch(&buffers, scratch)
}
