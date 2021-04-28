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
  "should write to all GPIO registers and read them back" in {
    implicit val config = WishboneConfig(32, 32)
    require(scalaTestContext.value.get.configMap.contains("memFile"))
    val programFile = scalaTestContext.value.get.configMap("memFile")
    test(new SwitchHarness(programFile.toString)).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      sendRequest("h40001000".U, 1.U, "b1111".U, true.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(1)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now sending new request")
      c.clock.step(2)
      sendRequest("h40001004".U, 2.U, "b1111".U, true.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(1)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now sending new request")
      c.clock.step(2)
      sendRequest("h40001008".U, 3.U, "b1111".U, true.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(1)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now sending new request")
      c.clock.step(2)
      sendRequest("h40001000".U, 0.U, "b1111".U, false.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(1)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now reading expected data")
      println("EXPECTED DATA IS: 1 GOT " + c.io.dataResp.peek().litValue().toInt.toString)
      c.clock.step(2)
      sendRequest("h40001004".U, 0.U, "b1111".U, false.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(1)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now reading expected data")
      println("EXPECTED DATA IS: 2 GOT " + c.io.dataResp.peek().litValue().toInt.toString)
      c.clock.step(2)
      sendRequest("h40001008".U, 0.U, "b1111".U, false.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(1)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now reading expected data")
      println("EXPECTED DATA IS: 3 GOT " + c.io.dataResp.peek().litValue().toInt.toString)
      c.clock.step(10)

      def sendRequest(addr: UInt, data: UInt, byteLane: UInt, isWrite: Bool): Unit = {
        c.clock.step(1)
        c.io.valid.poke(true.B)
        c.io.addrReq.poke(addr)
        c.io.dataReq.poke(data)
        c.io.byteLane.poke(byteLane)
        c.io.isWrite.poke(isWrite)
        c.clock.step(1)
        c.io.valid.poke(false.B)
      }
    }
  }

  "should write to a false GPIO register and produce error" in {
    implicit val config = WishboneConfig(32, 32)
    require(scalaTestContext.value.get.configMap.contains("memFile"))
    val programFile = scalaTestContext.value.get.configMap("memFile")
    test(new SwitchHarness(programFile.toString)).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      sendRequest("h4000100c".U, 1.U, "b1111".U, true.B)
      if(c.io.errResp.peek().litToBoolean){
        println("test is passing")
      }
      c.clock.step(10)

      def sendRequest(addr: UInt, data: UInt, byteLane: UInt, isWrite: Bool): Unit = {
        c.clock.step(1)
        c.io.valid.poke(true.B)
        c.io.addrReq.poke(addr)
        c.io.dataReq.poke(data)
        c.io.byteLane.poke(byteLane)
        c.io.isWrite.poke(isWrite)
        c.clock.step(1)
        c.io.valid.poke(false.B)
        c.clock.step(3)
      }
    }
  }

  "should write data to multiple rows and read them back from memory" in {
    implicit val config = WishboneConfig(32, 32)
    require(scalaTestContext.value.get.configMap.contains("memFile"))
    val programFile = scalaTestContext.value.get.configMap("memFile")
    test(new SwitchHarness(programFile.toString)).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      sendRequestToMem("h40000000".U, "h00100120".U, "b1111".U, true.B)
      sendRequestToMem("h40000004".U, "h00100124".U, "b1111".U, true.B)
      sendRequestToMem("h40000008".U, "h00100128".U, "b1111".U, true.B)
      sendRequestToMem("h40000000".U, 0.U, "b1111".U, false.B)
      sendRequestToMem("h40000004".U, 0.U, "b1111".U, false.B)
      sendRequestToMem("h40000008".U, 0.U, "b1111".U, false.B)
      c.clock.step(10)

      def sendRequestToMem(addr: UInt, data: UInt, byteLane: UInt, isWrite: Bool): Unit = {
        c.clock.step(1)
        c.io.valid.poke(true.B)
        c.io.addrReq.poke(addr)
        c.io.dataReq.poke(data)
        c.io.byteLane.poke(byteLane)
        c.io.isWrite.poke(isWrite)
        c.clock.step(1)
        c.io.valid.poke(false.B)
        c.clock.step(3)
      }
    }
  }

  "should write to a device that is not in memory map and produce error" in {
    implicit val config = WishboneConfig(32, 32)
    require(scalaTestContext.value.get.configMap.contains("memFile"))
    val programFile = scalaTestContext.value.get.configMap("memFile")
    test(new SwitchHarness(programFile.toString)).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      sendRequest("h80000000".U, 1.U, "b1111".U, true.B)
      c.clock.step(10)

      def sendRequest(addr: UInt, data: UInt, byteLane: UInt, isWrite: Bool): Unit = {
        c.clock.step(1)
        c.io.valid.poke(true.B)
        c.io.addrReq.poke(addr)
        c.io.dataReq.poke(data)
        c.io.byteLane.poke(byteLane)
        c.io.isWrite.poke(isWrite)
        c.clock.step(1)
        c.io.valid.poke(false.B)
        c.clock.step(3)
      }
    }
  }


}
