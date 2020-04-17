package sifive.skeleton

import chisel3._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.diplomaticobjectmodel.logicaltree.LogicalTreeNode

case class BlockAttachParams(
  sbus: Option[TLBusWrapper] = None,	
  fbus: TLBusWrapper,
  mbus: TLBusWrapper,
  pbus: TLBusWrapper,
  ibus: IntInwardNode,
  testHarness: LazyScope,
  parentNode: LogicalTreeNode,
  )(implicit val p: Parameters)

case class BlockDescriptor(
  name:  String,
  place: BlockAttachParams => LazyModule)

case object BlockDescriptorKey extends Field[Seq[BlockDescriptor]](Nil)
