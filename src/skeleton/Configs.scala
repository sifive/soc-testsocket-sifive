package sifive.skeleton

import freechips.rocketchip.config.{Config, Field}
import freechips.rocketchip.devices.tilelink.BootROMLocated
import freechips.rocketchip.subsystem.InSubsystem
import freechips.rocketchip.diplomacy.{AddressSet}
import freechips.rocketchip.system.{DefaultConfig => RCDefaultConfig}
import freechips.rocketchip.subsystem.{RocketTilesKey, InSubsystem, SubsystemExternalResetVectorKey}
import freechips.rocketchip.diplomacy.LazyScope

case object TestHarnessScope extends Field[Option[LazyScope]](None)

class BaseSkeletonConfig extends Config((site, here, up) => {
  case BootROMLocated(InSubsystem) => up(BootROMLocated(InSubsystem), site).map(_.copy(hang = 0x10000))
  case SubsystemExternalResetVectorKey => true
  case RocketTilesKey =>
    up(RocketTilesKey, site).map { x =>
      x.copy(
        dcache = x.dcache.map(_.copy(nMMIOs = 7)),
        core = x.core.copy(useVM = false)
      )
    }
})

class MemorySRAM extends Config((site, here, up) => {
  case BlockDescriptorKey =>
    BlockDescriptor(
      name = "testProgramSRAM",
      place = TLRAMAttach.attach(
        // Warning: Both arguments name devName has to match the instance path to the SRAM in the DUT
        // https://github.com/sifive/api-generator-sifive/blob/2746926805ee00f91aacf883a8bb830c27f69ed2/vsrc/TestDriver.sv#L189
        // so don't change it until that hardcoded path is paremeterized
        TLRAMParams(
          name = "main_mem_sram",
          address = AddressSet(site(SkeletonResetVector), 0x10000000-1),
          devName = Some("mem"),
          dtsCompat = Some(Seq("memory"))))
    ) +: up(BlockDescriptorKey, site)
})

class DefaultConfig extends Config(new MemorySRAM ++ new BaseSkeletonConfig ++ new RCDefaultConfig)

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

class WithAdditionalDUTSRAMs(addrs: Seq[AddressSet]) extends Config((site, here, up) => {
  case BlockDescriptorKey => addrs.zipWithIndex.map { case (addr, idx) =>
    BlockDescriptor(
      name = s"benchmarkingSRAM$idx",
      place = TLRAMAttach.attach(
        TLRAMParams(
          name = s"main_mem_sram_$idx",
          address = addr,
          devName = Some(s"mem-$idx"),
          dtsCompat = Some(Seq("memory"))))
    )
  } ++ up(BlockDescriptorKey, site)
})
