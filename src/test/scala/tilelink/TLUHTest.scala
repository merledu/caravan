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
        implicit val config = TilelinkConfig(uh = true, z = 8)
    // val programFile = getFile
        test(new TilelinkHarness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
            val array_op    = Array(2, 3)
            val opCode = array_op(Random.nextInt(array_op.length))
            val param = Random.nextInt(5)
            // this value behaves like (2**size_value = x Bytes operation)
            /*
                Recently it is 3 which means all operations are 8 bytes,but
                we have (w=4 bytes) data width. we divide (8/4=2) which means
                all operations (either PUT or GET or ATOMIC) take 2 beats.
            */
            val size_value = 4
            var counter_test = 0
            var index_counter = 0
            val data1 = Array.fill(math.pow(2,size_value).toInt/config.w)(Random.nextLong() & 0xFFFFFFFFL)
            val data2 = Array.fill(math.pow(2,size_value).toInt/config.w)(Random.nextLong() & 0xFFFFFFFFL)
            val operate = (a:Long,b:Long) => {
                                    if (opCode == 2 && param == 0){
                                         min(a, b)
                                    }
                                    else if (opCode == 2 && param == 1){
                                        max(a, b)
                                    }
                                    else if (opCode == 2 && param == 2){
                                        min(a, b)
                                    }
                                    else if (opCode == 2 && param == 3){
                                        max(a, b)
                                    }
                                    else if (opCode == 2 && param == 4){
                                        a + b
                                    }
                                    else if (opCode == 3 && param == 0){
                                        a ^ b
                                    }
                                    else if (opCode == 3 && param == 1){
                                        a | b
                                    }
                                    else if (opCode == 3 && param == 2){
                                        a & b
                                    }
                                    else if (opCode == 3 && param == 3){
                                        b
                                    }
                                    else{
                                        a
                                    }
            }
            val final_result:((Long,Long) => Long,Long,Long)=> BigInt=(f:(Long,Long)=>Long,a:Long,b:Long)=>{
                val r = f(a,b)
                if (r < 0)
                    (BigInt (0xFFFFFFFFL) + r +1) & 0xFFFFFFFFL
                else r & 0xFFFFFFFFL
            }
            val result = data1.zip(data2).map{case(a,b)=> final_result(operate,a,b)}
            c.clock.step(5)
            c.io.valid.poke(true.B)
            c.io.addrReq.poke(0.U)
            c.io.dataReq.poke(data1(index_counter).U)
            if(config.uh){
                c.io.isArithmetic.get.poke(false.B)
                c.io.isLogical.get.poke(false.B)
                c.io.isIntent.get.poke(false.B)
                c.io.param.get.poke(0.U)
                c.io.size.get.poke(size_value.U)
                if((math.pow(2,c.io.size.get.peek().litValue.toDouble) > config.w)){
                counter_test = (math.pow(2,c.io.size.get.peek().litValue.toDouble)/config.w).toInt-1
            }
            }
            c.io.byteLane.poke("b1111".U)
            c.io.isWrite.poke(true.B)
            c.clock.step(1)
            c.io.valid.poke(false.B)
            println(s"VALID RESPONSE FOR PUT BEAT${index_counter}= " + c.io.validResp.peek().litToBoolean.toString)
            while(c.io.validResp.peek().litToBoolean != true) {
                println("wait")
                c.clock.step(1)
            }
            index_counter += 1
            while((counter_test > 0)){
                c.io.valid.poke(true.B)
                //c.io.addrReq.poke((c.io.addrReq.peek().litValue+config.w).asUInt)
                c.io.dataReq.poke(data1(index_counter).U)
                counter_test = counter_test - 1
                index_counter += 1
                c.clock.step(1)
                //c.io.valid.poke(false.B)
            }
            c.io.valid.poke(false.B)
            c.clock.step(2)
            index_counter = 0
            println("Got the response now try atomic operation")
            c.io.valid.poke(true.B)
            c.io.addrReq.poke(0.U)
            c.io.dataReq.poke(data2(index_counter).U)
            c.io.isWrite.poke(false.B)
            c.io.byteLane.poke("b1111".U)
            if(config.uh){
              if (opCode == 2){
                  println("Got the response now try arithmetic operation")
                  c.io.isArithmetic.get.poke(true.B)
              }
              else
                  c.io.isArithmetic.get.poke(false.B)
              if (opCode == 3){
                  println("Got the response now try logic operation")
                  c.io.isLogical.get.poke(true.B)
              }
              else
                  c.io.isLogical.get.poke(false.B)
              c.io.isIntent.get.poke(false.B)
              c.io.param.get.poke(param.U)
              c.io.size.get.poke(size_value.U)
              if((math.pow(2,c.io.size.get.peek().litValue.toDouble) > config.w)){
                counter_test = (math.pow(2,c.io.size.get.peek().litValue.toDouble)/config.w).toInt-1
            }
            }
            c.clock.step(1)
            println(s"VALID RESPONSE FOR ATOMIC BEAT${index_counter} = " + c.io.validResp.peek().litToBoolean.toString)
            while(c.io.validResp.peek().litToBoolean != true) {
                println("wait")
                c.clock.step(1)
            }
            c.io.dataResp.expect(data1(index_counter).asUInt)
            println(s"EXPECTED DATA FOR ATOMIC IN BEAT${index_counter} IS: ${data1(index_counter).asUInt} GOT " + c.io.dataResp.peek())
            index_counter += 1
            while((counter_test > 0)){
                c.io.dataReq.poke(data2(index_counter).U)
                //c.io.addrReq.poke((c.io.addrReq.peek().litValue+config.w).asUInt)
                counter_test = counter_test - 1
                c.clock.step(1)
                println(s"VALID RESPONSE FOR ATOMIC BEAT${index_counter} = " + c.io.validResp.peek().litToBoolean.toString)
                while(c.io.validResp.peek().litToBoolean != true) {
                    println("wait")
                    c.clock.step(1)
                }
                c.io.dataResp.expect(data1(index_counter).asUInt)
                println(s"EXPECTED DATA FOR ATOMIC IN BEAT${index_counter} IS: ${data1(index_counter).asUInt} GOT " + c.io.dataResp.peek())
                index_counter +=1
            }
            c.io.valid.poke(false.B)
            index_counter = 0
            println("Got the response now reading expected data")
            c.clock.step(2)
            c.io.addrReq.poke(0.U)
            c.io.dataReq.poke(0.U)
            c.io.isWrite.poke(false.B)
            c.io.valid.poke(true.B)
            if(config.uh){
              c.io.isArithmetic.get.poke(false.B)
              c.io.isLogical.get.poke(false.B)
              c.io.isIntent.get.poke(false.B)
              c.io.param.get.poke(0.U)
              c.io.size.get.poke(size_value.U)
              if((math.pow(2,c.io.size.get.peek().litValue.toDouble) > config.w)){
                counter_test = (math.pow(2,c.io.size.get.peek().litValue.toDouble)/config.w).toInt-1
            }
            }
            c.clock.step(1)
            println(s"VALID RESPONSE FOR GET BEAT${index_counter} = " + c.io.validResp.peek().litToBoolean.toString)
            while(c.io.validResp.peek().litToBoolean != true) {
                println("wait")
                c.clock.step(1)
            }
            c.io.dataResp.expect(result(index_counter).U)
            println(s"EXPECTED DATA FOR GET IN BEAT${index_counter} IS: ${result(index_counter).asUInt} GOT " + c.io.dataResp.peek())
            index_counter +=1
            //c.io.valid.poke(false.B)
            while((counter_test > 0)){
                c.io.valid.poke(true.B)
                //val data_3 = Random.nextLong() & 0xFFFFFFFFL
                c.io.dataReq.poke(0.U)
                //c.io.addrReq.poke((c.io.addrReq.peek().litValue+config.w).asUInt)
                counter_test = counter_test - 1
                c.clock.step(1)
                println(s"VALID RESPONSE FOR GET BEAT${index_counter} = " + c.io.validResp.peek().litToBoolean.toString)
                while(c.io.validResp.peek().litToBoolean != true) {
                    println("wait")
                    c.clock.step(1)
            }
                c.io.dataResp.expect(result(index_counter).U)
                println(s"EXPECTED DATA FOR GET IN BEAT${index_counter} IS: ${result(index_counter).asUInt} GOT " + c.io.dataResp.peek())
                index_counter +=1
            }
            c.io.valid.poke(false.B)
            //c.clock.step(1)
        }
    }
}