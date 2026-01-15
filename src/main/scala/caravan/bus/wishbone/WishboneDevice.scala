package caravan.bus.wishbone
import caravan.bus.common.{DeviceAdapter, DeviceAdapterIO}
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.Decoupled

class WishboneDeviceIO(implicit val config: WishboneConfig) extends DeviceAdapterIO
{
    val slaveTransmitter    = Decoupled(new WishboneSlave())
    val masterReceiver      = Flipped(Decoupled(new WishboneMaster()))
    val reqOut                = Decoupled(new WBRequest())
    val rspIn                 = Flipped(Decoupled(new WBResponse()))
}

class WishboneDevice(implicit val config: WishboneConfig) extends DeviceAdapter {
  val io = IO(new WishboneDeviceIO())

  /** fire() is a handy function indicating whenever the master sends a valid request */
  def fire(): Bool = io.masterReceiver.valid && io.masterReceiver.bits.cyc && io.masterReceiver.bits.stb
  val ack = WireInit(false.B)
  /** FIXME: Assuming wishbone slave is always ready to accept master req */
  io.masterReceiver.ready := true.B
  dontTouch(io.masterReceiver.ready)
  dontTouch(io.slaveTransmitter.ready)
  /** FIXME: Assuming wishbone slave is always ready to accept ip response data */
  io.rspIn.ready := true.B

  when(fire()) {
    when(!io.masterReceiver.bits.we) {
      // READ CYCLE
      val addr = io.masterReceiver.bits.adr
      val activeByteLane = io.masterReceiver.bits.sel
      /** FIXME: Assuming ip is always ready to accept wishbone slave's request */
      io.reqOut.valid := true.B
      io.reqOut.bits.addrRequest := addr
      io.reqOut.bits.dataRequest := DontCare
      io.reqOut.bits.activeByteLane := activeByteLane
      io.reqOut.bits.isWrite := false.B
      when(io.rspIn.valid && !io.rspIn.bits.error) {
        /** FIXME: Assuming wishbone master is always ready to accept slave's data response */
        io.slaveTransmitter.valid := true.B
        ack := true.B
        io.slaveTransmitter.bits.err := false.B
        io.slaveTransmitter.bits.dat := io.rspIn.bits.dataResponse
      } .elsewhen(io.rspIn.valid && io.rspIn.bits.error) {
        io.slaveTransmitter.valid := true.B
        ack := false.B
        io.slaveTransmitter.bits.err := true.B
        io.slaveTransmitter.bits.dat := io.rspIn.bits.dataResponse
      } .otherwise {
        io.slaveTransmitter.valid := false.B
        ack := false.B
        io.slaveTransmitter.bits.err := false.B
        io.slaveTransmitter.bits.dat := DontCare
      }
    } .otherwise {
      // WRITE CYCLE
      io.reqOut.valid := true.B
      io.reqOut.bits.addrRequest := io.masterReceiver.bits.adr
      io.reqOut.bits.dataRequest := io.masterReceiver.bits.dat
      io.reqOut.bits.activeByteLane := io.masterReceiver.bits.sel
      io.reqOut.bits.isWrite := io.masterReceiver.bits.we
      when(io.rspIn.valid && !io.rspIn.bits.error) {
        io.slaveTransmitter.valid := true.B
        ack := true.B
        io.slaveTransmitter.bits.err := false.B
        io.slaveTransmitter.bits.dat := DontCare
      } .elsewhen(io.rspIn.valid && io.rspIn.bits.error) {
        io.slaveTransmitter.valid := true.B
        ack := false.B
        io.slaveTransmitter.bits.err := true.B
        io.slaveTransmitter.bits.dat := DontCare
      } .otherwise {
        io.slaveTransmitter.valid := false.B
        ack := false.B
        io.slaveTransmitter.bits.err := false.B
        io.slaveTransmitter.bits.err := false.B
        io.slaveTransmitter.bits.dat := DontCare
      }

    }
  } .otherwise {
    // No valid bus request from host
    io.reqOut.valid := false.B
    io.reqOut.bits.addrRequest := DontCare
    io.reqOut.bits.dataRequest := DontCare
    io.reqOut.bits.activeByteLane := DontCare
    io.reqOut.bits.isWrite := DontCare

    io.slaveTransmitter.valid := false.B
    ack := false.B
    io.slaveTransmitter.bits.err := false.B
    io.slaveTransmitter.bits.dat := DontCare
  }

  io.slaveTransmitter.bits.ack := ack
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