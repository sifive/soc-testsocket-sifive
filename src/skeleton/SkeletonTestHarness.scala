package sifive.skeleton

import chisel3._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.devices.debug.Debug
import freechips.rocketchip.diplomaticobjectmodel.ConstructOM
import freechips.rocketchip.util.AsyncResetReg

class SkeletonTestHarness()(implicit p: Parameters) extends LazyModule with LazyScope
{
  val dut = LazyModule(new SkeletonDUT(this))
  lazy val module = new LazyModuleImp(this) {
    ConstructOM.constructOM()
    val dutImp = dut.module

    // Allow the debug ndreset to reset the dut, but not until the initial reset has completed
    //dutImp.reset := reset.asBool | dutImp.debug.map { debug => AsyncResetReg(debug.ndreset) }.getOrElse(false.B)
    dutImp.tieOffInterrupts()
    //Debug.connectDebug(dutImp.debug, dutImp.psd, clock, reset.asBool, Wire(Bool()))
  }

}
