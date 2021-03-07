package caravan.bus.wishbone
import chisel3._
import chisel3.stage.ChiselStage

class WishboneDevice(implicit val config: WishboneConfig) extends Module {
  val io = IO(new Bundle {
    val sbus = Flipped(new WishboneBus())
    val wbTrans = Flipped(new WishboneTransaction())
  })


  when(io.sbus.cyc && io.sbus.stb) {
    when(!io.sbus.we) {
      // READ CYCLE
      val addr = io.sbus.adr
      val activeByteLane = io.sbus.sel
      io.wbTrans.validRequest := true.B
      io.wbTrans.addrRequest := addr
      io.wbTrans.dataRequest := DontCare
      io.wbTrans.activeByteLane := activeByteLane
      io.wbTrans.isWrite := false.B
      when(io.wbTrans.validResponse) {
        io.sbus.ack := true.B
        io.sbus.dat_miso := io.wbTrans.dataResponse
      } .otherwise {
        io.sbus.ack := false.B
        io.sbus.dat_miso := DontCare
      }
    } .otherwise {
      // WRITE CYCLE
      io.wbTrans.validRequest := DontCare
      io.wbTrans.addrRequest := DontCare
      io.wbTrans.dataRequest := DontCare
      io.wbTrans.activeByteLane := DontCare
      io.wbTrans.isWrite := DontCare

      io.sbus.ack := DontCare
      io.sbus.dat_miso := DontCare
    }
  } .otherwise {
    // No valid bus request from host
    io.wbTrans.validRequest := DontCare
    io.wbTrans.addrRequest := DontCare
    io.wbTrans.dataRequest := DontCare
    io.wbTrans.activeByteLane := DontCare
    io.wbTrans.isWrite := DontCare

    io.sbus.ack := false.B
    io.sbus.dat_miso := DontCare
  }
  /**
   * Rule 3.35: In standard mode, the cycle terminating signals ack_o, err_o and rty_o must be generated
   * in response to the logical AND of cyc_i and stb_i.
   *
   * Other signals besides these two maybe included in the generation of terminating signals.
   */

  /**
   * Rule 3.45: If device supports err_o or rty_o signals then it should not assert more than one of the
   * following signals at any given time: ack_o, err_o, rty_o
   */

  /**
   * Rule: 3.50: Device interfaces MUST be designed so that the ack_o, err_o and rty_o signals are asserted
   * and negated in response to the assertion and negation of stb_i
   */

  /**
   * Rule 3.65: The device must qualify the dat_miso signal with ack_o, err_o or rty_o
   */
}

object WishboneDevice extends App {
  implicit val config = WishboneConfig(addressWidth = 10, dataWidth = 32)
  println((new ChiselStage).emitVerilog(new WishboneDevice()))
}