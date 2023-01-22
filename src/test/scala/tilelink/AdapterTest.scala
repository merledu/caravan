package tilelink

import chisel3._ 
import chiseltest._
import org.scalatest._ 

import chiseltest.experimental.TestOptionBuilder._ 
import chiseltest.internal.VerilatorBackendAnnotation

import caravan.bus.tilelink._ 

class AdapterTest extends FreeSpec with ChiselScalatestTester {
    "Tilelink Adapter Test" in {
        implicit val config = TilelinkConfig()
        test(new TilelinkAdapter).withAnnotations(Seq(VerilatorBackendAnnotation)){ c =>
            c.io.reqIn.bits.isWrite.poke(true.B)
            c.io.reqIn.bits.addrRequest.poke(4.U)
            c.io.reqIn.bits.dataRequest.poke(10.U)
            c.io.reqIn.bits.activeByteLane.poke("b1111".U)
            c.io.reqIn.valid.poke(true.B)

            c.io.rspIn.bits.dataResponse.poke(12.U)
            c.io.rspIn.bits.error.poke(false.B)
            c.io.rspIn.valid.poke(true.B)

            c.clock.step(10)
        }
    }
}