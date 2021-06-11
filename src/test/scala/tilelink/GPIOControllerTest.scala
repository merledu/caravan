package tilelink
import caravan.bus.tilelink.{DummyGpioController, TilelinkConfig}
import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.VerilatorBackendAnnotation

class GpioControllerTest extends FreeSpec with ChiselScalatestTester {
  "write 40 in OUTPUT_EN_REG and read it back" in {
    implicit val config = TilelinkConfig()
    test(new DummyGpioController()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.req.valid.poke(true.B)
      c.io.req.bits.addrRequest.poke("h40000000".U)
      c.io.req.bits.dataRequest.poke(40.U)
      c.io.req.bits.activeByteLane.poke("b1111".U)
      c.io.req.bits.isWrite.poke(true.B)
      c.clock.step(1)
      c.io.req.valid.poke(false.B)
      c.clock.step(1)
      c.io.req.valid.poke(true.B)
      c.io.req.bits.isWrite.poke(false.B)
      c.clock.step(1)
      c.io.rsp.valid.expect(true.B)
      c.io.rsp.bits.dataResponse.expect(40.U)
    }
  }

  "write 40 in WDATA_REG and read it back" in {
    implicit val config = TilelinkConfig()
    test(new DummyGpioController()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.req.valid.poke(true.B)
      c.io.req.bits.addrRequest.poke("h40000004".U)
      c.io.req.bits.dataRequest.poke(40.U)
      c.io.req.bits.activeByteLane.poke("b1111".U)
      c.io.req.bits.isWrite.poke(true.B)
      c.clock.step(1)
      c.io.req.valid.poke(false.B)
      c.clock.step(1)
      c.io.req.valid.poke(true.B)
      c.io.req.bits.isWrite.poke(false.B)
      c.clock.step(1)
      c.io.rsp.valid.expect(true.B)
      c.io.rsp.bits.dataResponse.expect(40.U)
    }
  }

  "write 40 in RDATA_REG and read it back" in {
    implicit val config = TilelinkConfig()
    test(new DummyGpioController()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.req.valid.poke(true.B)
      c.io.req.bits.addrRequest.poke("h40000008".U)
      c.io.req.bits.dataRequest.poke(40.U)
      c.io.req.bits.activeByteLane.poke("b1111".U)
      c.io.req.bits.isWrite.poke(true.B)
      c.clock.step(1)
      c.io.req.valid.poke(false.B)
      c.clock.step(1)
      c.io.req.valid.poke(true.B)
      c.io.req.bits.isWrite.poke(false.B)
      c.clock.step(1)
      c.io.rsp.valid.expect(true.B)
      c.io.rsp.bits.dataResponse.expect(40.U)
    }
  }

  "write 40 in a register not available causing error" in {
    implicit val config = TilelinkConfig()
    test(new DummyGpioController()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.req.valid.poke(true.B)
      c.io.req.bits.addrRequest.poke("h4000000c".U)
      c.io.req.bits.dataRequest.poke(40.U)
      c.io.req.bits.activeByteLane.poke("b1111".U)
      c.io.req.bits.isWrite.poke(true.B)
      c.clock.step(1)
      c.io.rsp.valid.expect(true.B)
      c.io.rsp.bits.error.expect(true.B)
      c.io.req.valid.poke(false.B)
      c.clock.step(1)
      c.io.req.valid.poke(true.B)
      c.io.req.bits.isWrite.poke(false.B)
      c.clock.step(1)
      c.io.rsp.valid.expect(true.B)
      c.io.rsp.bits.error.expect(true.B)
    }
  }
}