package wishbone
import caravan.bus.wishbone.{SwitchHarness, WishboneConfig, WBResponse, WBRequest}
import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.ChiselScalatestTester
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest.FreeSpec

import common.MemoryDumpFileHelper // necessary to import

class SwitchHarnessTest extends FreeSpec with ChiselScalatestTester with MemoryDumpFileHelper {

  "should write to all GPIO registers and read them back" in {
    implicit val config = WishboneConfig(32, 32)
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

  "should write to a false GPIO register and produce error" in {
    implicit val config = WishboneConfig(32, 32)
    // val programFile = getFile
    test(new SwitchHarness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      sendRequest("h4000100c".U, 1.U, "b1111".U, true.B)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(1)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now reading expected error")
      c.io.errResp.expect(true.B)
      println("EXPECTED ERR IS: true GOT " + c.io.errResp.peek().litToBoolean.toString)
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

  // "should write data to multiple rows and read them back from memory" in {
  //   implicit val config = WishboneConfig(32, 32)
  //   val programFile = getFile
  //   test(new SwitchHarness(programFile)).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
  //     c.clock.step(5)
  //     sendRequestToMem("h40000000".U, "h00100120".U, "b1111".U, true.B)
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     while(c.io.validResp.peek().litToBoolean != true) {
  //       println("wait")
  //       c.clock.step(1)
  //     }
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     println("Got the response now sending new request")
  //     c.clock.step(2)
  //     sendRequestToMem("h40000004".U, "h00100124".U, "b1111".U, true.B)
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     while(c.io.validResp.peek().litToBoolean != true) {
  //       println("wait")
  //       c.clock.step(1)
  //     }
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     println("Got the response now sending new request")
  //     c.clock.step(2)
  //     sendRequestToMem("h40000008".U, "h00100128".U, "b1111".U, true.B)
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     while(c.io.validResp.peek().litToBoolean != true) {
  //       println("wait")
  //       c.clock.step(1)
  //     }
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     println("Got the response now sending new request")
  //     sendRequestToMem("h40000000".U, 0.U, "b1111".U, false.B)
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     while(c.io.validResp.peek().litToBoolean != true) {
  //       println("wait")
  //       c.clock.step(1)
  //     }
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     println("Got the response now reading expected data")
  //     c.io.dataResp.expect("h00100120".U)
  //     println("EXPECTED DATA IS: " + "h00100120".U.litValue().toInt.toString + " GOT " + c.io.dataResp.peek().litValue().toInt.toString)
  //     c.clock.step(2)
  //     sendRequestToMem("h40000004".U, 0.U, "b1111".U, false.B)
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     while(c.io.validResp.peek().litToBoolean != true) {
  //       println("wait")
  //       c.clock.step(1)
  //     }
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     println("Got the response now reading expected data")
  //     c.io.dataResp.expect("h00100124".U)
  //     println("EXPECTED DATA IS: " + "h00100124".U.litValue().toInt.toString + " GOT " + c.io.dataResp.peek().litValue().toInt.toString)
  //     c.clock.step(2)
  //     sendRequestToMem("h40000008".U, 0.U, "b1111".U, false.B)
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     while(c.io.validResp.peek().litToBoolean != true) {
  //       println("wait")
  //       c.clock.step(1)
  //     }
  //     println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
  //     println("Got the response now reading expected data")
  //     c.io.dataResp.expect("h00100128".U)
  //     println("EXPECTED DATA IS: " + "h00100128".U.litValue().toInt.toString + " GOT " + c.io.dataResp.peek().litValue().toInt.toString)
  //     c.clock.step(10)

  //     def sendRequestToMem(addr: UInt, data: UInt, byteLane: UInt, isWrite: Bool): Unit = {
  //       c.clock.step(1)
  //       c.io.valid.poke(true.B)
  //       c.io.addrReq.poke(addr)
  //       c.io.dataReq.poke(data)
  //       c.io.byteLane.poke(byteLane)
  //       c.io.isWrite.poke(isWrite)
  //       c.clock.step(1)
  //       c.io.valid.poke(false.B)
  //     }
  //   }
  // }

  "should write to a device that is not in memory map and produce error" in {
    implicit val config = WishboneConfig(32, 32)
    // val programFile = getFile
    test(new SwitchHarness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      sendRequest("h80000000".U, 1.U, "b1111".U, true.B)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(1)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now reading expected error")
      c.io.errResp.expect(true.B)
      println("EXPECTED ERR IS: true GOT " + c.io.errResp.peek().litToBoolean.toString)
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
