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
class New_Test extends FreeSpec with ChiselScalatestTester with MemoryDumpFileHelper {
    
    "New_Test" in {
        implicit val config = TilelinkConfig()
    // val programFile = getFile
        test(new TilelinkHarness()).withAnnotations(Seq(VerilatorBackendAnnotation)) {c =>
            c.io.valid.poke(true.B)
            c.io.addrReq.poke(0.U)
            c.io.dataReq.poke(45.U)
            c.io.isWrite.poke(true.B)
            c.io.byteLane.poke("b1111".U)

           
            //c.io.is_arithmetic.get.poke(false.B)
            //c.io.is_logical.get.poke(false.B)
            //c.io.is_intent.get.poke(false.B)
            //c.io.param.get.poke(0.U)
            //c.io.size.get.poke(2.U)

            c.clock.step(1)
            c.io.valid.poke(false.B)
            c.clock.step(10)

            c.io.valid.poke(true.B)

            c.io.isWrite.poke(false.B)

            c.clock.step(2)
            c.io.dataResp.expect(45.U)
            c.io.valid.poke(false.B)
            c.clock.step(10)
            

            

            
        
        }
        
    }
        
}