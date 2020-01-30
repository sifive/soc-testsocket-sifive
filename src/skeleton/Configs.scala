package sifive.skeleton

import freechips.rocketchip.config.Config
import freechips.rocketchip.devices.tilelink.BootROMParams
import freechips.rocketchip.system.{DefaultConfig => RCDefaultConfig}
import freechips.rocketchip.subsystem.RocketTilesKey
import freechips.rocketchip.devices.debug.DebugModuleKey

class BaseSkeletonConfig extends Config((site, here, up) => {
  case DebugModuleKey => None
  case BootROMParams => up(BootROMParams, site).copy(hang = 0x10000)
  case RocketTilesKey =>
    Seq.fill(2)(up(RocketTilesKey, site).map { x =>
      x.copy(
        dcache = x.dcache.map(_.copy(nMMIOs = 7)),
        core = x.core.copy(useVM = false)
      )
    }).flatten
})

class DefaultConfig extends Config(new BaseSkeletonConfig ++ new RCDefaultConfig)

/// FOR CI integration tests only
class WithSimUART extends Config((site, here, up) => {
  case BlockDescriptorKey =>
    BlockDescriptor(
      name = "simUART",
      place=SimUARTAttach.attach(SimUARTParams(0x04000000L, "console.log"))) +: up(BlockDescriptorKey, site)
})

/// FOR CI integration tests only
class WithTestFinisher extends Config((site, here, up) => {
  case BlockDescriptorKey =>
    BlockDescriptor(
      name = "simTestFinisher",
      place=TestFinisherAttach.attach(TestFinisherParams(0x05000000L, "status.log"))) +: up(BlockDescriptorKey, site)
})
