package sifive.skeleton

import freechips.rocketchip.config.{Parameters}
import freechips.rocketchip.diplomacy.{AddressSet, LazyModule}
import freechips.rocketchip.tilelink.{TLRAM, TLFragmenter}
import freechips.rocketchip.util.{ECCParams}

case class TLRAMParams(
  name: String,
  address: AddressSet,
  cacheable: Boolean = true,
  executable: Boolean = true,
  atomics: Boolean = false,
  devName: Option[String] = None,
  dtsCompat: Option[Seq[String]] = None
)

object TLRAMAttach {
  def attach(params: TLRAMParams)(bap: BlockAttachParams): TLRAM = {
    implicit val p: Parameters = bap.p
    val sram = LazyModule(new TLRAM(
      address = params.address,
      cacheable = params.cacheable,
      executable = params.executable,
      atomics = params.atomics,
      beatBytes = bap.mbus.beatBytes,
      ecc = ECCParams(bytes = bap.mbus.beatBytes), // set eccBytes equal to beatBytes so we only generate a single memory
      parentLogicalTreeNode = Some(bap.parentNode),
      devName = params.devName,
      dtsCompat = params.dtsCompat
    ))
    sram.suggestName(params.name)
    sram.node := bap.mbus.coupleTo(params.name) { TLFragmenter(bap.mbus) := _ }
    sram
  }
}
