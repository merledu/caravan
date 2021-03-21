package wishbone
import caravan.bus.wishbone.{Harness, WishboneConfig}
import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.ChiselScalatestTester
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest.FreeSpec

class HarnessTest extends FreeSpec with ChiselScalatestTester {
  "should send a valid request and full word" in {
    implicit val config = WishboneConfig(10, 32)
    test(new Harness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(1.U)
      c.io.dataReq.poke(0.U)
      c.io.byteLane.poke("b1111".U)
      c.io.isWrite.poke(false.B)
      c.clock.step(15)
    }
  }

  "should send a valid request and read 1 byte" in {
    implicit val config = WishboneConfig(10, 32)
    test(new Harness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(1.U)
      c.io.dataReq.poke(0.U)
      c.io.byteLane.poke("b0001".U)
      c.io.isWrite.poke(false.B)
      c.clock.step(15)
    }
  }

  "should send a valid request and read 2 bytes" in {
    implicit val config = WishboneConfig(10, 32)
    test(new Harness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke(0.U)
      c.io.byteLane.poke("b0011".U)
      c.io.isWrite.poke(false.B)
      c.clock.step(15)
    }
  }

  "should write a full word in memory" in {
    implicit val config = WishboneConfig(10, 32)
    test(new Harness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke("habcdef01".U)
      c.io.byteLane.poke("b1111".U)
      c.io.isWrite.poke(true.B)
      c.clock.step(2)
      c.io.valid.poke(false.B)
      c.clock.step(1)
      // new request
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke(0.U)
      c.io.byteLane.poke("b1111".U)
      c.io.isWrite.poke(false.B)
      c.clock.step(15)

    }
  }
}
