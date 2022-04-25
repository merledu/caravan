package caravan.bus.tilelink
import caravan.bus.common.DeviceAdapter
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._

class TilelinkDevice(implicit val config: TilelinkConfig) extends DeviceAdapter with OpCodes {
    val io = IO(new Bundle {
        val tlSlaveTransmitter = Decoupled(new TilelinkSlave())
        val tlMasterReceiver = Flipped(Decoupled(new TilelinkMaster()))
        val reqOut = Decoupled(new TLRequest())
        val rspIn = Flipped(Decoupled(new TLResponse()))
    })


    val idle :: wait_for_resp :: Nil = Enum(2)
    val stateReg = RegInit(idle)

    io.tlMasterReceiver.ready := true.B
    io.rspIn.ready := false.B


    io.reqOut.bits.addrRequest      := 0.U
    io.reqOut.bits.dataRequest      := 0.U
    io.reqOut.bits.activeByteLane   := 0.U
    io.reqOut.bits.isWrite          := 0.U
    io.reqOut.valid                 := 0.U

    io.tlSlaveTransmitter.bits.d_opcode     := 0.U
    io.tlSlaveTransmitter.bits.d_data       := 0.U
    io.tlSlaveTransmitter.bits.d_param      := 0.U
    io.tlSlaveTransmitter.bits.d_size       := 0.U
    io.tlSlaveTransmitter.bits.d_source     := 0.U
    io.tlSlaveTransmitter.bits.d_sink       := 0.U
    io.tlSlaveTransmitter.bits.d_denied     := 0.U     // d_denied pin is used for representing Mem error
    io.tlSlaveTransmitter.bits.d_corrupt    := 0.U
    io.tlSlaveTransmitter.valid             := 0.U
    
    // val stall = Module(new stallUnit)

    when(stateReg === idle){

        when(io.tlMasterReceiver.valid){

            io.reqOut.bits.addrRequest := io.tlMasterReceiver.bits.a_address
            io.reqOut.bits.dataRequest := io.tlMasterReceiver.bits.a_data
            io.reqOut.bits.activeByteLane := io.tlMasterReceiver.bits.a_mask
            io.reqOut.bits.isWrite := io.tlMasterReceiver.bits.a_opcode === PutFullData.U || io.tlMasterReceiver.bits.a_opcode === PutPartialData.U
            io.reqOut.valid := true.B

            stateReg := wait_for_resp
            io.rspIn.ready := true.B

        }

    }.elsewhen(stateReg === wait_for_resp){

        io.rspIn.ready := true.B

        when(io.rspIn.valid){

            io.tlSlaveTransmitter.bits.d_opcode := AccessAckData.U
            io.tlSlaveTransmitter.bits.d_data := io.rspIn.bits.dataResponse
            io.tlSlaveTransmitter.bits.d_param := 0.U
            io.tlSlaveTransmitter.bits.d_size := io.tlMasterReceiver.bits.a_size
            io.tlSlaveTransmitter.bits.d_source := io.tlMasterReceiver.bits.a_source
            io.tlSlaveTransmitter.bits.d_sink := 0.U
            io.tlSlaveTransmitter.bits.d_denied := io.rspIn.bits.error      // d_denied pin is used for representing Mem error
            io.tlSlaveTransmitter.bits.d_corrupt := 0.U
            io.tlSlaveTransmitter.valid := io.rspIn.valid

            stateReg := idle

        }

    }


    // Sending Response coming from Memory in the STALL to delay the response one cycle
   

    
}