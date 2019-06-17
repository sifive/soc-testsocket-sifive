package sifive.skeleton

import chisel3._
import chisel3.util.HasBlackBoxInline
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.tilelink._

class TestFinisherBB(val statusFile: String) extends BlackBox(Map(
  "STATUS_FILE" -> core.StringParam(statusFile)
)) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val valid = Input(Bool())
    val status = Input(UInt(32.W))
  })
  override val desiredName = "TestFinisher"

  setInline("TestFinisher.v",
    """
      |module TestFinisher #(parameter STATUS_FILE="") (
      |    input       clock,
      |    input       reset,
      |    input       valid,
      |    input [31:0] status);
      |
      |  integer f;
      |
      |  initial begin
      |    f = $fopen(STATUS_FILE, "w");
      |  end
      |
      |  always @(posedge clock) begin
      |    if (!reset && valid) begin
      |      if (status[15:0] == 16'h3333) begin
      |        $fwrite(f, "FAILED with status 0x%0x", status[31:16]);
      |        $fatal(status);
      |        $finish;
      |      end else if (status[15:0] == 16'h5555) begin
      |        $fwrite(f, "PASSED");
      |        $finish;
      |      end else begin
      |        $fwrite(f, "FAILED with status 0x%0x", status);
      |        $fatal(status);
      |        $finish;
      |      end
      |    end
      |  end
      |
      |endmodule
      |""".stripMargin)
}

case class TestFinisherParams(
  address:    BigInt,
  statusFile: String)

abstract class TestFinisher(busWidthBytes: Int, c: TestFinisherParams)(implicit p: Parameters)
    extends RegisterRouter(
      RegisterRouterParams(
        name = "test-finisher",
        compat = Seq("sifive,test0"),
        base = c.address,
        beatBytes = busWidthBytes))
    with HasInterruptSources
{
  def nInterrupts = 1

  ResourceBinding {
    Resource(ResourceAnchors.aliases, "test-finisher").bind(ResourceString("&L9:11520"))
  }

  lazy val module = new LazyModuleImp(this) {
    interrupts(0) := false.B

    val blackbox = Module(new TestFinisherBB(c.statusFile))
    blackbox.io.clock := clock
    blackbox.io.reset := reset

    regmap(
      0 -> RegFieldGroup("testFinisher", None, Seq(
        RegField.w(32, RegWriteFn((valid, data) => {
          blackbox.io.valid := valid
          blackbox.io.status := data
          Bool(true)}), RegFieldDesc("finisher","Finish with status")))))
  }
}

class TLTestFinisher(busWidthBytes: Int, c: TestFinisherParams)(implicit p: Parameters)
  extends TestFinisher(busWidthBytes, c) with HasTLControlRegMap

object TestFinisherAttach {
  def attach(c: TestFinisherParams)(bap: BlockAttachParams): TLTestFinisher = {
    implicit val p: Parameters = bap.p
    // Connect a TestFinisher for simulation output
    val testFinisher = LazyModule(new TLTestFinisher(bap.pbus.beatBytes, c))
    bap.pbus.coupleTo(s"slave_named_test_finisher") { testFinisher.controlXing(NoCrossing) := TLFragmenter(bap.pbus) := _ }
    bap.ibus := testFinisher.intXing(NoCrossing)
    testFinisher
  }
}
