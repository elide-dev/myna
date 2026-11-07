// Copyright the Myna contributors.
// SPDX-License-Identifier: Apache-2.0 OR MIT
package dev.elide.myna.truffle;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

/**
 * A language-independent root around one cell, so any Truffle language can call it through a call
 * target. A language that exposes the cell as one of its own functions inlines this root into its
 * call node, like any guest function.
 */
public final class NativeCellRootNode extends RootNode {
  @Child private NativeCell cell;

  public NativeCellRootNode(NativeCell cell) {
    super(null);
    this.cell = cell;
  }

  public NativeCell cell() {
    return cell;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return cell.execute(frame.getArguments());
  }

  @Override
  public String getName() {
    return cell.name();
  }

  @Override
  public boolean isInternal() {
    return true;
  }

  @Override
  public String toString() {
    return "NativeCell " + cell.name();
  }
}
