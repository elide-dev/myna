// Enum and flag returns and parameters through the ownership wrappers, under #![deny(warnings)].
#![deny(warnings)]
#[allow(dead_code, non_snake_case)]
mod seam {
    include!(concat!(env!("SEAM_GENERATED"), "/seam.rs"));
}
use seam::{Perms, Status, Ticket};

#[unsafe(no_mangle)]
pub extern "C" fn owned_ticket_create() -> *mut Ticket { core::ptr::null_mut() }
#[unsafe(no_mangle)]
pub extern "C" fn owned_ticket_destroy(_ticket: *mut Ticket) {}
#[unsafe(no_mangle)]
pub extern "C" fn owned_check(bytes: *const u8, count: u64, mode: Status) -> Status {
    if bytes.is_null() || count == 0 { Status::BAD } else { mode }
}
#[unsafe(no_mangle)]
pub extern "C" fn owned_permissions(_bytes: *const u8, count: u64, wanted: Perms) -> Perms {
    if count == 0 { Perms::NONE } else { wanted | Perms::READ }
}
seam::assert_implementations!(seam);

#[test]
fn wrappers_return_and_take_newtypes() {
    let data = [1u8, 2, 3];
    let status: Status = seam::ownership::check(&data, Status::OK);
    assert_eq!(status, Status::OK);
    let empty: Status = seam::ownership::check(&[], Status::OK);
    assert_eq!(empty, Status::BAD);
    let perms: Perms = seam::ownership::permissions(&data, Perms::WRITE);
    assert!(perms.contains(Perms::READ | Perms::WRITE));
    assert_eq!(seam::ownership::permissions(&[], Perms::WRITE), Perms::NONE);
}
