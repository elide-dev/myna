// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.addresses;

/** Hands each by-value import's address to a C caller that uses the declared signature. */
public final class AddressesMain {
  private static void check(boolean condition, String what) {
    if (!condition) throw new AssertionError(what);
  }

  public static void main(String[] args) {
    long leaf = AddressApi.swapPair_address().rawValue();
    long slow = AddressApi.swapPairSlow_address().rawValue();
    check(leaf != 0 && slow != 0 && leaf != slow, "distinct linked addresses");
    // (3, 4) swapped and bumped by 1 is (5, 4); by 2 is (6, 5).
    check(AddressApi.callPair(leaf, 3, 4) == 5004, "leaf address is the declared by-value symbol");
    check(AddressApi.callPair(slow, 3, 4) == 6005, "transitioning address is the declared symbol");
    System.out.println("Native Image import addresses passed");
  }
}
