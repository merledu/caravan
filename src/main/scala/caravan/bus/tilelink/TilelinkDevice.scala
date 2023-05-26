package caravan.bus.tilelink
import caravan.bus.common.DeviceAdapter
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util._
import scala.math._
class TilelinkDevice(implicit val config: TilelinkConfig) extends DeviceAdapter with OpCodes{
    val io = IO(new Bundle {
        val tlSlaveTransmitter = Decoupled(new TilelinkSlave())
        val tlMasterReceiver = Flipped(Decoupled(new TilelinkMaster()))
        val reqOut = Decoupled(new TLRequest())
        val rspIn = Flipped(Decoupled(new TLResponse()))
    })
    val idle :: uh :: wait_for_resp :: Nil = Enum(3)
    val stateReg = RegInit(idle)
    val rspData = RegInit(0.U)

    val add_reg_D = RegInit(0.U)
    val mask_reg_D = RegInit(0.U)
    val counter_D = RegInit(UInt(config.z.W),0.U)
    val op_reg_D = RegInit(6.U)

    io.tlMasterReceiver.ready := true.B
    io.rspIn.ready := false.B
    dontTouch(io.rspIn.ready)

    if (config.uh){
    io.reqOut.bits.is_arithmetic.get := false.B
    io.reqOut.bits.is_logical.get := false.B
    io.reqOut.bits.is_intent.get := false.B
    io.reqOut.bits.param.get := 0.U
    io.reqOut.bits.size.get := 0.U
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

    when(counter_D === 0.U){
        when(io.tlMasterReceiver.valid){
            op_reg_D := io.tlMasterReceiver.bits.a_opcode
        }.otherwise{
            op_reg_D := 6.U
        }
        add_reg_D := 0.U
        mask_reg_D := 0.U
    }

    when(config.uh.asBool() && (op_reg_D === Arithmetic.U 
        || op_reg_D === Logical.U )){
       when(stateReg === idle){
        when(io.tlMasterReceiver.valid){

            when(counter_D > 0.U && ((1.U << io.tlMasterReceiver.bits.a_size).asUInt() > config.w.U)){
                io.reqOut.bits.addrRequest := add_reg_D + config.w.U
                add_reg_D := add_reg_D + config.w.U
                counter_D := counter_D - 1.U
            }.otherwise{
                io.reqOut.bits.addrRequest := io.tlMasterReceiver.bits.a_address
            }
            io.reqOut.bits.dataRequest := io.tlMasterReceiver.bits.a_data
            io.reqOut.bits.activeByteLane := io.tlMasterReceiver.bits.a_mask
            io.reqOut.bits.isWrite := false.B
            io.reqOut.valid := true.B
            io.rspIn.ready := true.B
            stateReg := uh
            when(counter_D === 0.U && ((1.U << io.tlMasterReceiver.bits.a_size).asUInt() > config.w.U)){
                add_reg_D := io.tlMasterReceiver.bits.a_address
                counter_D := ((1.U << io.tlMasterReceiver.bits.a_size).asUInt() / config.w.U)- 1.U
            }
        }
       }
       .elsewhen(stateReg === uh){
        io.reqOut.valid := false.B
        io.rspIn.ready := true.B
        when(io.rspIn.valid){
                io.reqOut.bits.addrRequest := add_reg_D
                io.reqOut.bits.dataRequest := MuxCase(io.rspIn.bits.dataResponse,  Array(
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U && io.tlMasterReceiver.bits.a_param === 0.U) -> Mux(io.tlMasterReceiver.bits.a_data < io.rspIn.bits.dataResponse,
                                                                                                                                                                                                io.tlMasterReceiver.bits.a_data,io.rspIn.bits.dataResponse).asUInt,
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U && io.tlMasterReceiver.bits.a_param === 1.U) -> Mux(io.tlMasterReceiver.bits.a_data < io.rspIn.bits.dataResponse,
                                                                                                                                                                                                io.rspIn.bits.dataResponse,io.tlMasterReceiver.bits.a_data).asUInt,
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U && io.tlMasterReceiver.bits.a_param === 2.U) -> Mux(io.tlMasterReceiver.bits.a_data < io.rspIn.bits.dataResponse,
                                                                                                                                                                                                io.tlMasterReceiver.bits.a_data,io.rspIn.bits.dataResponse),
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U && io.tlMasterReceiver.bits.a_param === 3.U) -> Mux(io.tlMasterReceiver.bits.a_data < io.rspIn.bits.dataResponse,
                                                                                                                                                                                                io.rspIn.bits.dataResponse,io.tlMasterReceiver.bits.a_data),
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U && io.tlMasterReceiver.bits.a_param === 4.U) -> (io.tlMasterReceiver.bits.a_data + io.rspIn.bits.dataResponse),
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Logical.U && io.tlMasterReceiver.bits.a_param === 0.U) -> (io.tlMasterReceiver.bits.a_data ^ io.rspIn.bits.dataResponse).asUInt,
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Logical.U && io.tlMasterReceiver.bits.a_param === 1.U) -> (io.tlMasterReceiver.bits.a_data | io.rspIn.bits.dataResponse).asUInt,
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Logical.U && io.tlMasterReceiver.bits.a_param === 2.U) -> (io.tlMasterReceiver.bits.a_data & io.rspIn.bits.dataResponse).asUInt,
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Logical.U && io.tlMasterReceiver.bits.a_param === 3.U) -> io.tlMasterReceiver.bits.a_data
                                                                                        ))
                io.reqOut.bits.activeByteLane := io.tlMasterReceiver.bits.a_mask
                io.reqOut.bits.isWrite := true.B
                io.reqOut.valid := true.B
                rspData := io.rspIn.bits.dataResponse
                stateReg := wait_for_resp
                
        }
       }

       .elsewhen(stateReg === wait_for_resp){
            io.rspIn.ready := true.B
            io.reqOut.valid := false.B

            when(io.rspIn.valid){
                io.rspIn.ready := false.B
                io.tlSlaveTransmitter.bits.d_opcode := AccessAckData.U
                                                                        
                io.tlSlaveTransmitter.bits.d_data := rspData
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

    }.otherwise{
        when(config.uh.asBool && counter_D > 0.U && op_reg_D === Get.U){
            io.reqOut.bits.addrRequest :=  add_reg_D + config.w.U
            io.reqOut.bits.activeByteLane := mask_reg_D
            io.reqOut.bits.isWrite := false.B
            counter_D := counter_D -1.U
            io.reqOut.valid := true.B
            io.rspIn.ready := true.B
            add_reg_D := add_reg_D + config.w.U
           
            
        }
        .elsewhen(io.tlMasterReceiver.valid){
            
            io.reqOut.bits.addrRequest := io.tlMasterReceiver.bits.a_address
            io.reqOut.bits.dataRequest := io.tlMasterReceiver.bits.a_data
            io.reqOut.bits.activeByteLane := io.tlMasterReceiver.bits.a_mask
            io.reqOut.bits.isWrite := io.tlMasterReceiver.bits.a_opcode === PutFullData.U || io.tlMasterReceiver.bits.a_opcode === PutPartialData.U
            io.reqOut.valid := true.B

            //stateReg := wait_for_resp
            io.rspIn.ready := true.B

            when(((1.U << io.tlMasterReceiver.bits.a_size).asUInt() > config.w.U) && io.tlMasterReceiver.bits.a_opcode === Get.U){
                op_reg_D := io.tlMasterReceiver.bits.a_opcode
                add_reg_D := io.tlMasterReceiver.bits.a_address
                mask_reg_D := io.tlMasterReceiver.bits.a_mask
                counter_D := ((1.U << io.tlMasterReceiver.bits.a_size).asUInt /config.w.U)-1.U 
            }
            

        }

        //}.elsewhen(stateReg === wait_for_resp){

        // io.rspIn.ready := true.B
       
        when(io.rspIn.valid && config.uh.asBool && counter_D > 0.U && op_reg_D =/= Get.U){
            io.rspIn.ready := false.B
            counter_D := counter_D -1.U

            io.tlSlaveTransmitter.bits.d_opcode := 3.U
            io.tlSlaveTransmitter.bits.d_data := 0.U
            io.tlSlaveTransmitter.bits.d_param := 0.U
            io.tlSlaveTransmitter.bits.d_size := 0.U
            io.tlSlaveTransmitter.bits.d_source := 0.U
            io.tlSlaveTransmitter.bits.d_sink := 0.U
            io.tlSlaveTransmitter.bits.d_denied := 0.U      // d_denied pin is used for representing Mem error
            io.tlSlaveTransmitter.bits.d_corrupt := 0.U
            io.tlSlaveTransmitter.valid := 0.U
        }
        .elsewhen(io.rspIn.valid){

            when(io.tlMasterReceiver.bits.a_opcode === Get.U){
                io.tlSlaveTransmitter.bits.d_opcode := AccessAck.U
            }.otherwise{
            io.tlSlaveTransmitter.bits.d_opcode := AccessAckData.U}
            io.tlSlaveTransmitter.bits.d_data := io.rspIn.bits.dataResponse
            io.tlSlaveTransmitter.bits.d_param := 0.U
            io.tlSlaveTransmitter.bits.d_size := io.tlMasterReceiver.bits.a_size
            io.tlSlaveTransmitter.bits.d_source := io.tlMasterReceiver.bits.a_source
            io.tlSlaveTransmitter.bits.d_sink := 0.U
            io.tlSlaveTransmitter.bits.d_denied := io.rspIn.bits.error      // d_denied pin is used for representing Mem error
            io.tlSlaveTransmitter.bits.d_corrupt := 0.U
            io.tlSlaveTransmitter.valid := io.rspIn.valid
            //io.reqOut.valid := false.B

            when(((1.U << io.tlMasterReceiver.bits.a_size).asUInt > config.w.U) && io.tlMasterReceiver.bits.a_opcode =/= Get.U){
                counter_D := ((1.U << io.tlMasterReceiver.bits.a_size).asUInt /config.w.U)-1.U
                op_reg_D := io.tlMasterReceiver.bits.a_opcode
            }
            //stateReg := idle
            io.rspIn.ready := false.B

            
        }

    }
    

    // Sending Response coming from Memory in the STALL to delay the response one cycle
   

}