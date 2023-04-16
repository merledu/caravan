package tilelink
import caravan.bus.tilelink.{TilelinkConfig, TilelinkError}
import chisel3._
import org.scalatest._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.VerilatorBackendAnnotation

class ErrorTest extends FreeSpec with ChiselScalatestTester {
    "Tilelink Testing Error Module" in {
        implicit val config = TilelinkConfig()
        test(new TilelinkError()).withAnnotations(Seq(VerilatorBackendAnnotation)) { c =>
            c.clock.step(20)
        }
    }
}