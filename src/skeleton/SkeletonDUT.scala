package sifive.skeleton

import chisel3._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.system._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.util.ECCParams

class SkeletonDUTModuleImp[+L <: SkeletonDUT](_outer: L) extends RocketSubsystemModuleImp(_outer)
    with HasRTCModuleImp
    with HasPeripheryBootROMModuleImp

class SkeletonDUT(harness: LazyScope)(implicit p: Parameters) extends RocketSubsystem
    with HasPeripheryBootROM
{
  sbus.crossToBus(cbus, NoCrossing)
  cbus.crossToBus(pbus, SynchronousCrossing())
  FlipRendering { implicit p => sbus.crossFromBus(fbus, SynchronousCrossing()) }

  private val BankedL2Params(nBanks, coherenceManager) = p(BankedL2Key)
  private val (in, out, halt) = coherenceManager(this)
  if (nBanks != 0) {
    sbus.coupleTo("coherence_manager") { in :*= _ }
    mbus.coupleFrom("coherence_manager") { _ :=* BankBinder(mbus.blockBytes * (nBanks-1)) :*= out }
  }

  // set eccBytes equal to beatBytes so we only generate a single memory
  val main_mem_sram = LazyModule(new TLRAM(AddressSet(0x80000000L, 0x10000000-1), beatBytes = mbus.beatBytes,
    ecc = ECCParams(bytes = mbus.beatBytes)))

  main_mem_sram.node := TLFragmenter(mbus) := mbus.toDRAMController(Some("main_mem_sram"))()


  val attachParams = BlockAttachParams(
    fbus = fbus,
    mbus = mbus,
    pbus = pbus,
    ibus = ibus.fromSync,
    testHarness = harness)

  p(BlockDescriptorKey).foreach { block => block.place(attachParams) }

  override lazy val module = new SkeletonDUTModuleImp(this)
}
