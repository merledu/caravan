package caravan.bus.tilelink
import caravan.bus.common.HostAdapter
import chisel3._
import chisel3.experimental.DataMirror
import chisel3.stage.ChiselStage
import chisel3.util.{Decoupled, Enum, MuxLookup}

class TilelinkHost(implicit val config: TilelinkConfig) extends HostAdapter with OpCodes {
    val io = IO(new Bundle {
        val tlMasterTransmitter = Decoupled(new TilelinkMaster())
        val tlSlaveReceiver  = Flipped(Decoupled(new TilelinkSlave()))
        val reqIn = Flipped(Decoupled(new TLRequest()))
        val rspOut = Decoupled(new TLResponse())
    })

    io.tlSlaveReceiver.ready := true.B
    io.reqIn.ready := true.B


    
    io.tlMasterTransmitter.bits.a_opcode := Mux(io.reqIn.bits.isWrite, Mux(io.reqIn.bits.activeByteLane === "b1111".U, PutFullData.U, PutPartialData.U) , Get.U)
    io.tlMasterTransmitter.bits.a_data := io.reqIn.bits.dataRequest
    io.tlMasterTransmitter.bits.a_address := io.reqIn.bits.addrRequest
    io.tlMasterTransmitter.bits.a_param := 0.U
    io.tlMasterTransmitter.bits.a_source := 2.U 
    io.tlMasterTransmitter.bits.a_size := MuxLookup(config.w.U, 2.U,Array(                    // default 32-bit
                                                                            (1.U) -> 0.U,
                                                                            (2.U) -> 1.U,
                                                                            (4.U) -> 2.U,
                                                                            (8.U) -> 3.U
                                                                        ))
    io.tlMasterTransmitter.bits.a_mask := io.reqIn.bits.activeByteLane
    io.tlMasterTransmitter.bits.a_corrupt := false.B
    io.tlMasterTransmitter.valid := true.B


    io.rspOut.bits.dataResponse := io.tlSlaveReceiver.bits.d_data
    io.rspOut.bits.error := io.tlSlaveReceiver.bits.d_corrupt
    io.rspOut.valid := true.B
    




}