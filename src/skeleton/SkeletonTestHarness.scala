package sifive.skeleton

import chisel3._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.devices.debug.Debug
import freechips.rocketchip.diplomaticobjectmodel.ConstructOM

class SkeletonTestHarness()(implicit p: Parameters) extends LazyModule with LazyScope
{
  val dut = LazyModule(new SkeletonDUT(this))
  lazy val module = new LazyModuleImp(this) {
    dut.module.dontTouchPorts()
    ConstructOM.constructOM()
    Debug.tieoffDebug(dut.module.debug)
  }
}
