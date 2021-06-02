package tilelink
import caravan.bus.tilelink.{Harness, TilelinkConfig}
import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.ChiselScalatestTester
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest.FreeSpec

import common.MemoryDumpFileHelper // necessary to import


class HarnessTest extends FreeSpec with ChiselScalatestTester with MemoryDumpFileHelper {
  "should write and read full word" in {
    implicit val config = TilelinkConfig()
    // val programFile = getFile
    test(new Harness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke(24.U)
      c.io.byteLane.poke("b1111".U)
      c.io.isWrite.poke(true.B)
      c.clock.step(2)
      c.io.valid.poke(false.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(2)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now reading expected data")
      c.io.dataResp.expect(24.U)
      println("EXPECTED DATA IS: 24 GOT " + c.io.dataResp.peek().litValue().toInt.toString)
    }
  }

  "should write full word and read first byte" in {
    implicit val config = TilelinkConfig()
    // val programFile = getFile
    test(new Harness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke("habcdef0f".U)
      c.io.byteLane.poke("b1111".U)
      c.io.isWrite.poke(true.B)
      c.clock.step(2)
      c.io.valid.poke(false.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(2)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now sending new request")
      c.clock.step(2)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke(0.U)
      c.io.byteLane.poke("b0001".U)
      c.io.isWrite.poke(false.B)
      c.clock.step(2)
      c.io.valid.poke(false.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(2)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now reading expected data")
      c.io.dataResp.expect("hf".U)
      println("EXPECTED DATA IS: " + "hf".U.litValue().toInt + " GOT " + c.io.dataResp.peek().litValue().toInt.toString)
    }
  }

  "should write full word and read first two bytes" in {
    implicit val config = TilelinkConfig()
    // val programFile = getFile
    test(new Harness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke("habcdefbf".U)
      c.io.byteLane.poke("b1111".U)
      c.io.isWrite.poke(true.B)
      c.clock.step(2)
      c.io.valid.poke(false.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(2)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now sending new request")
      c.clock.step(2)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke(0.U)
      c.io.byteLane.poke("b0011".U)
      c.io.isWrite.poke(false.B)
      c.clock.step(2)
      c.io.valid.poke(false.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(2)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now reading expected data")
      c.io.dataResp.expect("hefbf".U)
      println("EXPECTED DATA IS: " + "hefbf".U.litValue().toInt + " GOT " + c.io.dataResp.peek().litValue().toInt.toString)
    }
  }

  "should write a full word and read full word" in {
    implicit val config = TilelinkConfig()
    // val programFile = getFile
    test(new Harness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
      c.clock.step(5)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke("habcdefbf".U)
      c.io.byteLane.poke("b1111".U)
      c.io.isWrite.poke(true.B)
      c.clock.step(2)
      c.io.valid.poke(false.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(2)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now sending new request")
      c.clock.step(2)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke(0.U)
      c.io.byteLane.poke("b1111".U)
      c.io.isWrite.poke(false.B)
      c.clock.step(2)
      c.io.valid.poke(false.B)
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(2)
      }
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      println("Got the response now reading expected data")
      c.io.dataResp.expect("habcdefbf".U)
      println("EXPECTED DATA IS: " + "habcdefbf".U.litValue().toInt + " GOT " + c.io.dataResp.peek().litValue().toInt.toString)

    }
  }
}
