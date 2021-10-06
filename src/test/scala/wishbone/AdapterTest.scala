package wishbone

import chisel3._ 
import chiseltest._ 
import org.scalatest.FreeSpec

import chiseltest.experimental.TestOptionBuilder._ 
import chiseltest.internal.VerilatorBackendAnnotation

import caravan.bus.wishbone._

class AdapterTest extends FreeSpec with ChiselScalatestTester {
    "Wishbone Adapter Test" in {
        implicit val config = WishboneConfig(32,32)
        test(new WishboneAdapter()).withAnnotations(Seq(VerilatorBackendAnnotation)){ c =>
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
