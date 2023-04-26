package tilelink
import caravan.bus.tilelink.{TilelinkHarness, TilelinkConfig}
import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.ChiselScalatestTester
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.experimental.TestOptionBuilder._
import org.scalatest.FreeSpec
import scala.math._

import scala . util . Random

import common.MemoryDumpFileHelper // necessary to import

class TLUHTest extends FreeSpec with ChiselScalatestTester with MemoryDumpFileHelper {
    "TL-UH Tests" in {
        implicit val config = TilelinkConfig()
    // val programFile = getFile

    test(new TilelinkHarness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
        val array_op    = Array(2, 3)

        for ( i <- 0 until 20){
            val data_1 = Random.nextLong() & 0xFFFFFFFFL
            val data_2 = Random.nextLong() & 0xFFFFFFFFL
            val index  = Random.nextInt(2)
            val opCode = array_op(index)
            val param = Random.nextInt(5)

            println (data_1.U)
            println (data_2.U)
            println (opCode.asUInt)
            println (param.asUInt)

            val result = (opCode, param) match {
                                            case (a,b) if a == 2 && b == 0 => min(data_1, data_2)
                                            case (a,b) if a == 2 && b == 1 => max(data_1, data_2)
                                            case (a,b) if a == 2 && b == 2 => min(data_1, data_2)
                                            case (a,b) if a == 2 && b == 3 => max(data_1, data_2)
                                            case (a,b) if a == 2 && b == 4 => data_1 + data_2
                                            case (a,b) if a == 3 && b == 0 => data_1 ^ data_2
                                            case (a,b) if a == 3 && b == 1 => data_1 | data_2
                                            case (a,b) if a == 3 && b == 2 => data_1 & data_2
                                            case (a,b) if a == 3 && b == 3 => data_2                
            }

            val result1 : BigInt = if (result < 0)
                                        (BigInt (0xFFFFFFFFL) + result +1) & 0xFFFFFFFFL
                                   else result & 0xFFFFFFFFL

            c.clock.step(5)
            c.io.valid.poke(true.B)
            c.io.addrReq.poke(0.U)
            c.io.dataReq.poke(data_1.U)
            if(config.uh){
                c.io.is_arithmetic.get.poke(false.B)
                c.io.is_logical.get.poke(false.B)
                c.io.is_intent.get.poke(false.B)
                c.io.param.get.poke(0.U)
            }
            c.io.byteLane.poke("b1111".U)
            c.io.isWrite.poke(true.B)
            c.clock.step(1)
            c.io.valid.poke(false.B)
            println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
            while(c.io.validResp.peek().litToBoolean != true) {
                println("wait")
                c.clock.step(1)
            }
      
      println("Got the response now try atomic operation")
      c.clock.step(2)
      c.io.valid.poke(true.B)
      c.io.addrReq.poke(0.U)
      c.io.dataReq.poke(data_2.U)
      c.io.isWrite.poke(false.B)
      c.io.byteLane.poke("b1111".U)
      if(config.uh){
        if (opCode == 2){
            println("Got the response now try arithmetic operation")
            c.io.is_arithmetic.get.poke(true.B)
        }
        else
            c.io.is_arithmetic.get.poke(false.B)

        if (opCode == 3){
            println("Got the response now try logic operation")
            c.io.is_logical.get.poke(true.B)
        }
        else
            c.io.is_logical.get.poke(false.B)

        c.io.is_intent.get.poke(false.B)
        c.io.param.get.poke(param.U)
      }
      c.clock.step(2)
      
      println("VALID RESPONSE = " + c.io.validResp.peek().litToBoolean.toString)
      while(c.io.validResp.peek().litToBoolean != true) {
        println("wait")
        c.clock.step(1)
      }
      c.io.dataResp.expect(data_1.U)
      println(s"EXPECTED DATA IS: ${data_1.U} GOT " + c.io.dataResp.peek().litValue().toInt.toString)
      c.io.valid.poke(false.B)
      println("Got the response now reading expected data")
      c.clock.step(2)
      c.io.dataReq.poke(0.U)
      c.io.isWrite.poke(false.B)
      c.io.valid.poke(true.B)
      if(config.uh){
        c.io.is_arithmetic.get.poke(false.B)
        c.io.is_logical.get.poke(false.B)
        c.io.is_intent.get.poke(false.B)
        c.io.param.get.poke(0.U)
      }
            //c.clock.step(1)
            //c.io.valid.poke(false.B)
            c.clock.step(1)
            c.io.dataResp.expect(result1.U)
            println(s"EXPECTED DATA IS: ${result1.U} GOT " + c.io.dataResp.peek().litValue().toInt.toString)

        }
    }
} 
}