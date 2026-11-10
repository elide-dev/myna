// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package fixture.cells;
/** FFM-backend cells on HotSpot against the shared fixture library. */
public final class FfmCellsMain {
  public static void main(String[] args) throws Exception {
    System.load(args[0]);
    CellChecks.run(CellApiTruffle::function);
    if (CellApiTruffle.CELLS.length != 21) throw new AssertionError("cells: " + CellApiTruffle.CELLS.length);
    System.out.println("Truffle FFM cells passed");
  }
}
