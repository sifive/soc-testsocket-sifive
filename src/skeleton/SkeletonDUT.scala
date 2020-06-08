package sifive.skeleton

import chisel3._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.system._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.util.{DontTouch}

case object SkeletonResetVector extends Field[BigInt](0x80000000L)

class SkeletonDUTModuleImp[+L <: SkeletonDUT](_outer: L) extends RocketSubsystemModuleImp(_outer)
    with HasRTCModuleImp
    with HasResetVectorWire
    with DontTouch {
  global_reset_vector := outer.resetVector.U
}

trait HasAttachedBlocks { this: LazyModule =>
  def attachParams: BlockAttachParams
  val attachedBlocks = p(BlockDescriptorKey).map { block => block.place(attachParams) }
}

class SkeletonDUT(harness: LazyScope)(implicit p: Parameters) extends RocketSubsystem with HasAttachedBlocks
{
  def resetVector: BigInt = p(SkeletonResetVector)

  def attachParams = BlockAttachParams(
    sbus = Some(sbus),
    fbus = fbus,
    mbus = mbus,
    pbus = pbus,
    ibus = ibus.fromSync,
    testHarness = harness,
    parentNode = logicalTreeNode)

  override lazy val module = new SkeletonDUTModuleImp(this)
}
