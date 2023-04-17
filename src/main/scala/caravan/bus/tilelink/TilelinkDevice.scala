package caravan.bus.tilelink
import caravan.bus.common.DeviceAdapter
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import scala.math._
class TilelinkDevice(implicit val config: TilelinkConfig) extends DeviceAdapter with OpCodes {
    val io = IO(new Bundle {
        val tlSlaveTransmitter = Decoupled(new TilelinkSlave())
        val tlMasterReceiver = Flipped(Decoupled(new TilelinkMaster()))
        val reqOut = Decoupled(new TLRequest())
        val rspIn = Flipped(Decoupled(new TLResponse()))
    })


    //val idle :: wait_for_resp :: Nil = Enum(2)
    //val stateReg = RegInit(idle)

    io.tlMasterReceiver.ready := true.B
    io.rspIn.ready := false.B

    if (config.uh){
    io.reqOut.bits.is_arithmetic.get := false.B
    io.reqOut.bits.is_logical.get := false.B
    io.reqOut.bits.is_intent.get := false.B
    io.reqOut.bits.param.get := 0.U
  }

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

    //when(stateReg === idle){

        when(io.tlMasterReceiver.valid){

            io.reqOut.bits.addrRequest := io.tlMasterReceiver.bits.a_address
            io.reqOut.bits.dataRequest := io.tlMasterReceiver.bits.a_data
            io.reqOut.bits.activeByteLane := io.tlMasterReceiver.bits.a_mask
            io.reqOut.bits.isWrite := io.tlMasterReceiver.bits.a_opcode === PutFullData.U || io.tlMasterReceiver.bits.a_opcode === PutPartialData.U
            io.reqOut.valid := true.B

            //stateReg := wait_for_resp
            io.rspIn.ready := true.B

        }
        
        when(config.uh.asBool() && io.tlMasterReceiver.bits.a_opcode =/= PutFullData.U && io.tlMasterReceiver.bits.a_opcode =/= PutPartialData.U && io.tlMasterReceiver.bits.a_opcode =/= Get.U){
            io.reqOut.bits.addrRequest := io.tlMasterReceiver.bits.a_address
            io.reqOut.bits.dataRequest := MuxCase(io.tlMasterReceiver.bits.a_data, Array(
                                                                                    (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U && io.tlMasterReceiver.bits.a_param === 0.U) -> Mux(io.tlMasterReceiver.bits.a_data.asSInt < io.rspIn.bits.dataResponse.asSInt,
                                                                                                                                                                                            io.tlMasterReceiver.bits.a_data.asSInt,io.rspIn.bits.dataResponse.asSInt).asUInt,

                                                                                    (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U && io.tlMasterReceiver.bits.a_param === 1.U) -> Mux(io.tlMasterReceiver.bits.a_data.asSInt < io.rspIn.bits.dataResponse.asSInt,
                                                                                                                                                                                            io.rspIn.bits.dataResponse.asSInt,io.tlMasterReceiver.bits.a_data.asSInt).asUInt,

                                                                                    (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U && io.tlMasterReceiver.bits.a_param === 2.U) -> Mux(io.tlMasterReceiver.bits.a_data < io.rspIn.bits.dataResponse,
                                                                                                                                                                                            io.tlMasterReceiver.bits.a_data,io.rspIn.bits.dataResponse),

                                                                                    (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U && io.tlMasterReceiver.bits.a_param === 3.U) -> Mux(io.tlMasterReceiver.bits.a_data < io.rspIn.bits.dataResponse,
                                                                                                                                                                                            io.rspIn.bits.dataResponse,io.tlMasterReceiver.bits.a_data),

                                                                                    (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U && io.tlMasterReceiver.bits.a_param === 4.U) -> (io.tlMasterReceiver.bits.a_data.asUInt + io.rspIn.bits.dataResponse.asUInt).asUInt,
                                                                                    (io.tlMasterReceiver.bits.a_opcode === Logical.U && io.tlMasterReceiver.bits.a_param === 0.U) -> (io.tlMasterReceiver.bits.a_data ^ io.rspIn.bits.dataResponse).asUInt,
                                                                                    (io.tlMasterReceiver.bits.a_opcode === Logical.U && io.tlMasterReceiver.bits.a_param === 1.U) -> (io.tlMasterReceiver.bits.a_data | io.rspIn.bits.dataResponse).asUInt,
                                                                                    (io.tlMasterReceiver.bits.a_opcode === Logical.U && io.tlMasterReceiver.bits.a_param === 2.U) -> (io.tlMasterReceiver.bits.a_data & io.rspIn.bits.dataResponse).asUInt,
                                                                                    (io.tlMasterReceiver.bits.a_opcode === Logical.U && io.tlMasterReceiver.bits.a_param === 3.U) -> io.tlMasterReceiver.bits.a_data
                                                                                    ))
            io.reqOut.bits.activeByteLane := io.tlMasterReceiver.bits.a_mask
            io.reqOut.bits.isWrite := true.B
            io.reqOut.valid := true.B
            io.rspIn.ready := true.B                                                             

        }

        //}.elsewhen(stateReg === wait_for_resp){

        // io.rspIn.ready := true.B

        when(io.rspIn.valid){

            io.tlSlaveTransmitter.bits.d_opcode := MuxCase(0.U, Array(
                                                                    (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U || io.tlMasterReceiver.bits.a_opcode === Logical.U || io.tlMasterReceiver.bits.a_opcode === PutPartialData.U 
                                                                    || io.tlMasterReceiver.bits.a_opcode === PutFullData.U) -> AccessAckData.U,

                                                                    (io.tlMasterReceiver.bits.a_opcode === Intent.U) -> HintAck.U,
                                                                    (io.tlMasterReceiver.bits.a_opcode === Get.U) -> AccessAck.U
                                                                    ))
            io.tlSlaveTransmitter.bits.d_data := Mux(io.tlMasterReceiver.bits.a_opcode === PutFullData.U || io.tlMasterReceiver.bits.a_opcode === PutPartialData.U ,0.U,io.rspIn.bits.dataResponse)/*MuxCase(io.rspIn.bits.dataResponse, Array(
                                                                    (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U || io.tlMasterReceiver.bits.a_opcode === Logical.U) -> rspData
                                                                    ))*/
            io.tlSlaveTransmitter.bits.d_param := 0.U
            io.tlSlaveTransmitter.bits.d_size := io.tlMasterReceiver.bits.a_size
            io.tlSlaveTransmitter.bits.d_source := io.tlMasterReceiver.bits.a_source
            io.tlSlaveTransmitter.bits.d_sink := 0.U
            io.tlSlaveTransmitter.bits.d_denied := io.rspIn.bits.error      // d_denied pin is used for representing Mem error
            io.tlSlaveTransmitter.bits.d_corrupt := 0.U
            io.tlSlaveTransmitter.valid := io.rspIn.valid

            //stateReg := idle
            io.rspIn.ready := false.B
        }

    //}


    // Sending Response coming from Memory in the STALL to delay the response one cycle
   

    
}