package caravan.bus.wishbone
import chisel3._
import chisel3.experimental.DataMirror
import chisel3.stage.ChiselStage
import chisel3.util.Enum


// Support only for Single READ/WRITE cycles for now
class WishboneHost(implicit val config: WishboneConfig) extends Module {
  val io = IO(new Bundle {
    val mbus = new WishboneBus()
    val wbTrans = new WishboneTransaction()
  })

  when(reset.asBool() === true.B) {
    /**
     * Rule 3.20: Following signals must be negated when reset is asserted:
     * stb_o
     * cyc_o
     * all other signals are in an undefined state
     */

    io.mbus.getElements.filter(w => DataMirror.directionOf(w) == ActualDirection.Output).map(_ := 0.U)
  }

  val dataReg = RegInit(0.U(config.dataWidth.W))
  val respReg = RegInit(false.B)
  // state machine to conform to the wishbone protocol of negating stb and cyc when data latched
  val idle :: latch_data :: Nil = Enum(2)
  val stateReg = RegInit(idle)

  if(!config.waitState) {
    /**
     * If host does not produce wait states then stb_o and cyc_o may be assigned the same signal.
     */
    when(io.wbTrans.isWrite === false.B && io.wbTrans.validRequest) {
      /**
       * SINGLE READ CYCLE
       * host asserts adr_o, we_o, sel_o, stb_o and cyc_o
       */
      io.mbus.stb := true.B
      io.mbus.cyc := io.mbus.stb
      io.mbus.we := io.wbTrans.isWrite
      io.mbus.adr := io.wbTrans.addrRequest
      io.mbus.dat_mosi := DontCare
      io.mbus.sel := io.wbTrans.activeByteLane

      when(io.mbus.ack) {
        dataReg := io.mbus.dat_miso
        respReg := true.B
      }


    } .otherwise {
      /**
       * SINGLE WRITE CYCLE
       */
      io.mbus.stb := DontCare
      io.mbus.cyc := DontCare
      io.mbus.we := DontCare
      io.mbus.adr := DontCare
      io.mbus.dat_mosi := DontCare
      io.mbus.sel := DontCare
    }

    when(stateReg === idle) {
      stateReg := Mux(io.mbus.ack, latch_data, idle)
    } .elsewhen(stateReg === latch_data) {
      io.mbus.stb := false.B
      io.mbus.cyc := io.mbus.stb
    }

    io.wbTrans.validResponse := respReg
    io.wbTrans.dataResponse := dataReg
  }





  /**
   * Host initiates the transfer cycle by asserting cyc_o. When cyc_o is negated, all other
   * host signals are invalid.
   *
   * Device interface only respond to other device signals only when cyc_i is asserted.
   */

  /**
   * Rule 3.25: Host interfaces MUST assert cyc_o for the duration of SINGLE READ/WRITE, BLOCK and RMW cycles.
   * cyc_o must be asserted in the same rising edge that qualifies the assertion of stb_o
   * cyc_o must be negated in the same rising edge that qualifies the negation of stb_o
   */

  /**
   * Host asserts stb_o when it is ready to transfer data.
   * stb_o remains asserted until the device asserts one of its cycle termination signals:
   * ack_i
   * err_i
   * rty_i
   *
   * if any of the above signals are asserted then the stb_o is negated.
   */

  /**
   * Rule 3.60: Host interfaces must qualify the following signals with stb_o:
   * adr_o
   * dat_mosi
   * sel_o
   * we_o
   * tagn_o
   */


}

object WishboneHostDriver extends App {
  implicit val config = WishboneConfig(addressWidth = 32, dataWidth = 32)
  println((new ChiselStage).emitVerilog(new WishboneHost()))
}