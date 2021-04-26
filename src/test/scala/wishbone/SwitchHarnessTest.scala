package wishbone
import caravan.bus.wishbone.{SwitchHarness, WishboneConfig}
import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.ChiselScalatestTester
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest.FreeSpec

class SwitchHarnessTest extends FreeSpec with ChiselScalatestTester {
  "should write 10 to GPIO OUTPUT_EN_REG and read it back" in {
    implicit val config = WishboneConfig(32, 32)
    require(scalaTestContext.value.get.configMap.contains("memFile"))
    val programFile = scalaTestContext.value.get.configMap("memFile")
    test(new SwitchHarness(programFile.toString)).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke("h40001000".U)
      c.io.dataReq.poke(10.U)
      c.io.byteLane.poke("b1111".U)
      c.io.isWrite.poke(true.B)
      c.clock.step(1)
      c.io.valid.poke(false.B)
      c.clock.step(1)
      c.io.valid.poke(true.B)
      c.io.isWrite.poke(false.B)
      c.clock.step(10)
    }
  }
}
