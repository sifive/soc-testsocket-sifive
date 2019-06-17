package sifive.skeleton

import chisel3._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.subsystem._

case class BlockAttachParams(
  fbus: TLBusWrapper,
  mbus: TLBusWrapper,
  pbus: TLBusWrapper,
  ibus: IntInwardNode,
  testHarness: LazyScope,
  )(implicit val p: Parameters)

case class BlockDescriptor(
  name:  String,
  place: BlockAttachParams => Unit)

case object BlockDescriptorKey extends Field[Seq[BlockDescriptor]](Nil)
