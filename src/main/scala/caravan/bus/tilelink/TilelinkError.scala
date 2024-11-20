package caravan.bus.tilelink
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.{Decoupled, Fill}

import caravan.bus.common.{ErrorDevice, ErrorDeviceIO}

// import caravan.bus.common.DeviceAdapter


class TilelinkErrorIO(implicit val config: TilelinkConfig) extends ErrorDeviceIO{
  val slaveTransmitter = Decoupled(new TilelinkSlave())
  val masterReceiver = Flipped(Decoupled(new TilelinkMaster()))
}


// abstract class TLErr extends Module
class TilelinkError(implicit val config: TilelinkConfig) extends ErrorDevice {
  val io = IO(new TilelinkErrorIO)

  // def fire(): Bool = io.masterReceiver.valid && io.masterReceiver.bits.cyc && io.masterReceiver.bits.stb

  val ackReg = RegInit(true.B)
  val dataReg = RegInit(0.U)
  val errReg = RegInit(false.B)
  val validReg = RegInit(false.B)
  val opCodeReg = RegInit(Mux(io.masterReceiver.bits.a_opcode === 4.U, 1.U, 0.U))
  val paramReg = RegInit(0.U)
  val sizeReg = RegInit(io.masterReceiver.bits.a_size)

  /** FIXME: Assuming tilelink slave is always ready to accept master req */
  io.masterReceiver.ready := true.B

  when(io.masterReceiver.fire()) {
    // a valid request from the host. The decoder pointed to us which means there was a wrong address given by the user
    // for writes we are going to ignore them completely.
    // for reads we are going to signal an err out and send all FFFs.
    errReg := true.B
    validReg := true.B
    when(io.masterReceiver.bits.a_opcode === 0.U || io.masterReceiver.bits.a_opcode === 1.U) {
      // WRITE
      dataReg := DontCare
    } .elsewhen(io.masterReceiver.bits.a_opcode === 4.U) {
      // READ
      dataReg := Fill((config.w * 8)/4, "hf".U)
    }

  } .otherwise {
      // no valid request from the host
    dataReg := 0.U
    errReg := false.B
    validReg := false.B
  }

  io.slaveTransmitter.valid := validReg
  io.slaveTransmitter.bits.d_denied := errReg
  io.slaveTransmitter.bits.d_data := dataReg
  io.slaveTransmitter.bits.d_corrupt := errReg

  io.slaveTransmitter.bits.d_opcode := opCodeReg
  io.slaveTransmitter.bits.d_param := paramReg
  io.slaveTransmitter.bits.d_size := sizeReg
  io.slaveTransmitter.bits.d_source := paramReg // TODO: Add dynamic logic for source
  io.slaveTransmitter.bits.d_sink := paramReg

}

// object TilelinkErrDriver extends App {
//   implicit val config = TilelinkConfig()
//   println("-------------------------------- -------- ------------ -------------------")
//   println((new ChiselStage).emitVerilog(new TilelinkError()))
// }
