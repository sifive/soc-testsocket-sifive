package sifive.skeleton

import chisel3._
import chisel3.util.HasBlackBoxInline
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._

class LogFile(val file: String) extends BlackBox(Map(
  "FILE" -> core.StringParam(file)
)) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val valid = Input(Bool())
    val _byte = Input(UInt(8.W))
  })

  setInline("LogFile.v",
    """
      |module LogFile #(parameter FILE="") (
      |    input       clock,
      |    input       reset,
      |    input       valid,
      |    input [7:0] _byte);
      |
      |  integer f;
      |
      |  initial begin
      |    f = $fopen(FILE, "w");
      |  end
      |
      |  always @(posedge clock) begin
      |    if (!reset && valid) begin
      |      $fwrite(f, "%c", _byte);
      |    end
      |  end
      |
      |endmodule
      |""".stripMargin)
}

case class SimUARTParams(
  address:    BigInt,
  file:       String)

abstract class SimUART(busWidthBytes: Int, c: SimUARTParams)(implicit p: Parameters)
    extends RegisterRouter(
      RegisterRouterParams(
        name = "simSerial",
        compat = Seq("sifive,uart0"),
        base = c.address,
        beatBytes = busWidthBytes))
    with HasInterruptSources
{
  def nInterrupts = 1

  ResourceBinding {
    Resource(ResourceAnchors.aliases, "uart").bind(ResourceAlias(device.label))
  }

  lazy val module = new LazyModuleImp(this) {
    interrupts(0) := false.B

    val blackbox = Module(new LogFile(c.file))
    blackbox.io.clock := clock
    blackbox.io.reset := reset

    regmap(
      0 -> RegFieldGroup("txdata", None, Seq(
        RegField.w(8, RegWriteFn((valid, data) => {
          blackbox.io.valid := valid
          blackbox.io._byte := data
          true.B}), RegFieldDesc("data","Transmit data")))))
  }
}

class TLSimUART(busWidthBytes: Int, c: SimUARTParams)(implicit p: Parameters)
  extends SimUART(busWidthBytes, c) with HasTLControlRegMap

// Maybe reusable by System Integrator
object SimUARTAttach {
  def attach(c: SimUARTParams)(bap: BlockAttachParams): TLSimUART = {
    implicit val p: Parameters = bap.p
    // Connect a UART for simulation output
    val uart = LazyModule(new TLSimUART(bap.pbus.beatBytes, c))
    bap.pbus.coupleTo(s"slave_named_uart") { uart.controlXing(NoCrossing) := TLFragmenter(bap.pbus) := _ }
    bap.ibus := uart.intXing(NoCrossing)
    uart
  }
}
