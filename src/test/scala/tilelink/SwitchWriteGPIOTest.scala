package tilelink
import caravan.bus.tilelink.{SwitchHarness, TilelinkConfig, TLResponse, TLRequest}
import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.ChiselScalatestTester
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest.FreeSpec

import common.MemoryDumpFileHelper // necessary to import

class SwitchWriteGPIOTest extends FreeSpec with ChiselScalatestTester with MemoryDumpFileHelper {

  "should write to all GPIO registers and read them back" in {
    implicit val config = TilelinkConfig()
    // val programFile = getFile
    test(new SwitchHarness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
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
      c.clock.step(1)
      c.io.dataResp.expect(1.U)
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
      c.clock.step(1)
      c.io.dataResp.expect(2.U)
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
      c.clock.step(1)
      c.io.dataResp.expect(3.U)
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
}