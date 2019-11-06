package sifive.testsocket.fpga

import sifive.skeleton._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.diplomacy._
import sifive.freedom.unleashed.DevKitWrapper

class TestSocketDevKitWrapper()(implicit p: Parameters) extends DevKitWrapper()(p) with LazyScope
{
  val harness = this

  val socket = LazyModule(new SimpleLazyModule {
    val attachParams = BlockAttachParams(
      fbus = topMod.fbus,
      mbus = topMod.mbus,
      pbus = topMod.pbus,
      ibus = topMod.ibus.fromSync,
      testHarness = harness,
      parentNode = topMod.logicalTreeNode)

    p(BlockDescriptorKey).foreach { block => block.place(attachParams) }
  })
}
