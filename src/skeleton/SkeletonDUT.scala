package sifive.skeleton

import chisel3._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.system._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.util.{DontTouch, ECCParams}

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
  def resetVector: BigInt = 0x80000000L

  // set eccBytes equal to beatBytes so we only generate a single memory
  val main_mem_sram = LazyModule(new TLRAM(
    address = AddressSet(resetVector, 0x10000000-1),
    beatBytes = mbus.beatBytes,
    ecc = ECCParams(bytes = mbus.beatBytes),
    parentLogicalTreeNode = Some(logicalTreeNode),
    // Warning: This devName has to match the instance name of the SRAM in the
    // hierarchical path in
    // https://github.com/sifive/api-generator-sifive/blob/2746926805ee00f91aacf883a8bb830c27f69ed2/vsrc/TestDriver.sv#L189
    // so don't change it until that hardcoded path is paremeterized
    devName = Some("mem"),
    dtsCompat = Some(Seq("memory"))
  ))

  main_mem_sram.node := TLFragmenter(mbus) := mbus.toDRAMController(Some("main_mem_sram"))()

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
