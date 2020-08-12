package sifive.testsocket.fpga

import freechips.rocketchip.config.{Parameters, Config}
import freechips.rocketchip.system.{DefaultConfig => RCDefaultConfig}

import sifive.blocks.devices.gpio.{PeripheryGPIOKey, GPIOParams}
import sifive.blocks.devices.spi.{PeripherySPIKey, SPIParams}
import sifive.blocks.devices.uart.{PeripheryUARTKey, UARTParams}
import freechips.rocketchip.subsystem._
import sifive.skeleton.BaseSkeletonConfig

import sifive.fpgashells.shell.DesignKey

class FPGATestSocketPeripherals extends Config((site, here, up) => {
  case PeripheryUARTKey => List(
    UARTParams(address = BigInt(0x64000000L)))
  case PeripherySPIKey => List(
    SPIParams(rAddress = BigInt(0x64001000L)))
  case PeripheryGPIOKey => List(
    GPIOParams(address = BigInt(0x64002000L), width = 4))
})

class FPGATestSocketConfig extends Config(
  new WithNExtTopInterrupts(0)
  ++ new WithJtagDTM
  ++ new WithNMemoryChannels(1)
  ++ new FPGATestSocketPeripherals
  ++ new BaseSkeletonConfig
  ++ new RCDefaultConfig
)

class WithTestSocketFPGADevKitDesign extends Config((site, here, up) => {
  case DesignKey => { (p: Parameters) => new TestSocketDevKitWrapper()(p) }
})
