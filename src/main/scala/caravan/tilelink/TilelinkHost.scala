package caravan.bus.tileink
import caravan.bus.common.HostAdapter
import chisel3._
import chisel3.experimental.DataMirror
import chisel3.stage.ChiselStage
import chisel3.util.{Decoupled, Enum, MuxCase}

class TilelinkHost(implicit val config: TilelinkConfig) extends HostAdapter with OpCodes {
    val io = IO(new Bundle {
        val tlMasterTransmitter = Decoupled(new TilelinkMaster())
        val tlSlaveReceiver  = Flipped(Decoupled(new TilelinkSlave()))
        val reqIn = Flipped(Decoupled(new TLRequest()))
        val rspOut = Decoupled(new TLResponse())
    })

    io.tlSlaveReceiver.ready := true.B
    io.reqIn.ready := true.B
    
    io.tlMasterTransmitter.bits.a_opcode := Mux(io.reqIn.bits.isWrite, PutFullData.U, Get.U)
    io.tlMasterTransmitter.bits.a_data := io.reqIn.bits.dataRequest
    io.tlMasterTransmitter.bits.a_address := io.reqIn.bits.addrRequest
    io.tlMasterTransmitter.bits.a_param := 0.U
    io.tlMasterTransmitter.bits.a_source := 2.U
    io.tlMasterTransmitter.bits.a_size := 2.U
    io.tlMasterTransmitter.bits.a_mask := 1.U
    io.tlMasterTransmitter.bits.a_corrupt := 0.U
    io.tlMasterTransmitter.valid := true.B


    io.rspOut.bits.dataResponse := io.tlSlaveReceiver.bits.d_data
    io.rspOut.bits.error := io.tlSlaveReceiver.bits.d_corrupt
    io.rspOut.valid := true.B
    




}