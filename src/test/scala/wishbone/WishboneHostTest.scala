package wishbone
import caravan.bus.wishbone.{WishboneConfig, WishboneHost}
import org.scalatest._
import chiseltest._
import chisel3._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.VerilatorBackendAnnotation

class WishboneHostTest extends FreeSpec with ChiselScalatestTester {

  "just work" in {
    implicit val config = WishboneConfig(10, 32)
    test(new WishboneHost).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(14)
      c.io.reqIn.valid.poke(true.B)
      c.io.reqIn.bits.addrRequest.poke(10.U)
      c.io.reqIn.bits.dataRequest.poke(0.U)
      c.io.reqIn.bits.activeByteLane.poke("b1111".U)
      c.io.reqIn.bits.isWrite.poke(false.B)
      c.clock.step(4)

    }
  }
}
