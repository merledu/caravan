package caravan.bus.tileink
import caravan.bus.common.DeviceAdapter
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.Decoupled

class TilelinkDevice(implicit val config: TilelinkConfig) extends DeviceAdapter with OpCodes {
    val io = IO(new Bundle {
        val tlSlaveTransmitter = Decoupled(new TilelinkSlave())
        val tlMasterReceiver = Flipped(Decoupled(new TilelinkMaster()))
        val reqOut = Decoupled(new TLRequest())
        val rspIn = Flipped(Decoupled(new TLResponse()))
    })

    io.tlMasterReceiver.ready := true.B
    io.rspIn.ready := true.B

    io.tlSlaveTransmitter.bits.d_opcode := Mux(io.tlMasterReceiver.bits.a_opcode === Get.U, AccessAckData.U, AccessAck.U)
    io.tlSlaveTransmitter.bits.d_data := io.rspIn.bits.dataResponse
    io.tlSlaveTransmitter.bits.d_param := 0.U
    io.tlSlaveTransmitter.bits.d_size := io.tlMasterReceiver.bits.a_size
    io.tlSlaveTransmitter.bits.d_source := io.tlMasterReceiver.bits.a_source
    io.tlSlaveTransmitter.bits.d_sink := 0.U
    io.tlSlaveTransmitter.bits.d_denied := 0.U
    io.tlSlaveTransmitter.bits.d_corrupt := io.rspIn.bits.error
    io.tlSlaveTransmitter.valid := true.B

    io.reqOut.bits.addrRequest := io.tlMasterReceiver.bits.a_address
    io.reqOut.bits.dataRequest := io.tlMasterReceiver.bits.a_data
    io.reqOut.bits.activeByteLane := io.tlMasterReceiver.bits.a_mask
    io.reqOut.bits.isWrite := io.tlMasterReceiver.bits.a_opcode === PutFullData.U
    io.reqOut.valid := true.B

}