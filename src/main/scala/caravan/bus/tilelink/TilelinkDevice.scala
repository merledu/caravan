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
    /*
    NOTICE: This state logic is only for Atomic Operations
    idle          : Address sent to memory to perform READ
    uh            : The read data is MODIFIED and WRITTEN back to same memory location
    waitResponse : In this state the old data value is responded to the host
    */
    val idle :: uh :: waitResponse :: Nil = Enum(3)
    val stateReg = RegInit(idle)
    val rspData = RegInit(0.U)

    val addRegD = RegInit(0.U)
    val maskRegD = RegInit(0.U)
    val counterD = RegInit(UInt(config.z.W),0.U)
    val opRegD = RegInit(NoOp.U)

    io.tlMasterReceiver.ready := true.B
    io.rspIn.ready := false.B
    dontTouch(io.rspIn.ready)

    if (config.uh){
    io.reqOut.bits.isArithmetic.get := false.B
    io.reqOut.bits.isLogical.get := false.B
    io.reqOut.bits.isIntent.get := false.B
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
    

    when(counterD === 0.U){
        when(io.tlMasterReceiver.valid){
            opRegD := io.tlMasterReceiver.bits.a_opcode
        }.otherwise{
            opRegD := NoOp.U
        }
        addRegD := 0.U
        maskRegD := 0.U
    }

    when(config.uh.asBool() && (opRegD === Arithmetic.U || opRegD === Logical.U )){
       when(stateReg === idle){
        when(io.tlMasterReceiver.valid){
            
            // Address for Atomic operations changes in Tilelink Device during Burst Operations
            when(counterD > 0.U && ((1.U << io.tlMasterReceiver.bits.a_size).asUInt() > config.w.U)){
                io.reqOut.bits.addrRequest := addRegD + config.w.U
                addRegD := addRegD + config.w.U
                counterD := counterD - 1.U
            }.otherwise{
                io.reqOut.bits.addrRequest := io.tlMasterReceiver.bits.a_address
            }
            io.reqOut.bits.dataRequest := io.tlMasterReceiver.bits.a_data
            io.reqOut.bits.activeByteLane := io.tlMasterReceiver.bits.a_mask
            io.reqOut.bits.isWrite := false.B
            io.reqOut.valid := true.B
            io.rspIn.ready := true.B
            stateReg := uh
            when(counterD === 0.U && ((1.U << io.tlMasterReceiver.bits.a_size).asUInt() > config.w.U)){
                addRegD := io.tlMasterReceiver.bits.a_address
                counterD := ((1.U << io.tlMasterReceiver.bits.a_size).asUInt() / config.w.U)- 1.U
            }
        }
       }
       .elsewhen(stateReg === uh){
            io.reqOut.valid := false.B
            io.rspIn.ready := true.B
            when(io.rspIn.valid){
                io.reqOut.bits.addrRequest := addRegD
                io.reqOut.bits.dataRequest := MuxCase(io.rspIn.bits.dataResponse,  Array(
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Arithmetic.U) -> MuxCase(io.rspIn.bits.dataResponse,Array(
                                                                                                                                                        (io.tlMasterReceiver.bits.a_param === 0.U)-> Mux(io.tlMasterReceiver.bits.a_data < io.rspIn.bits.dataResponse,
                                                                                                                                                                                                         io.tlMasterReceiver.bits.a_data,io.rspIn.bits.dataResponse).asUInt,

                                                                                                                                                        (io.tlMasterReceiver.bits.a_param === 1.U)-> Mux(io.tlMasterReceiver.bits.a_data < io.rspIn.bits.dataResponse,
                                                                                                                                                                                                         io.rspIn.bits.dataResponse,io.tlMasterReceiver.bits.a_data).asUInt,

                                                                                                                                                        (io.tlMasterReceiver.bits.a_param === 2.U)-> Mux(io.tlMasterReceiver.bits.a_data < io.rspIn.bits.dataResponse,
                                                                                                                                                                                                         io.tlMasterReceiver.bits.a_data,io.rspIn.bits.dataResponse),
                                                                                                                                                        
                                                                                                                                                        (io.tlMasterReceiver.bits.a_param === 3.U)-> Mux(io.tlMasterReceiver.bits.a_data < io.rspIn.bits.dataResponse,
                                                                                                                                                                                                         io.rspIn.bits.dataResponse,io.tlMasterReceiver.bits.a_data),                                     
                                                                                                                                                        
                                                                                                                                                        (io.tlMasterReceiver.bits.a_param === 4.U)-> (io.tlMasterReceiver.bits.a_data + io.rspIn.bits.dataResponse)
                                                                                                                                                        )),
                                                                                        (io.tlMasterReceiver.bits.a_opcode === Logical.U) -> MuxCase(io.rspIn.bits.dataResponse,Array(
                                                                                                                                                        (io.tlMasterReceiver.bits.a_param === 0.U)-> (io.tlMasterReceiver.bits.a_data ^ io.rspIn.bits.dataResponse).asUInt,
                                                                                                                                                        (io.tlMasterReceiver.bits.a_param === 1.U)-> (io.tlMasterReceiver.bits.a_data | io.rspIn.bits.dataResponse).asUInt,
                                                                                                                                                        (io.tlMasterReceiver.bits.a_param === 2.U)-> (io.tlMasterReceiver.bits.a_data & io.rspIn.bits.dataResponse).asUInt,
                                                                                                                                                        (io.tlMasterReceiver.bits.a_param === 3.U)->  io.tlMasterReceiver.bits.a_data
                                                                                                                                                ))
                                                                                        ))
                                                                                    
                io.reqOut.bits.activeByteLane := io.tlMasterReceiver.bits.a_mask
                io.reqOut.bits.isWrite := true.B
                io.reqOut.valid := true.B
                rspData := io.rspIn.bits.dataResponse
                stateReg := waitResponse
                
        }
       }

       .elsewhen(stateReg === waitResponse){
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
        when(config.uh.asBool && counterD > 0.U && opRegD === Get.U){
            io.reqOut.bits.addrRequest :=  addRegD + config.w.U
            io.reqOut.bits.activeByteLane := maskRegD
            io.reqOut.bits.isWrite := false.B
            counterD := counterD -1.U
            io.reqOut.valid := true.B
            io.rspIn.ready := true.B
            addRegD := addRegD + config.w.U
           
            
        }
        .elsewhen(io.tlMasterReceiver.valid){
            
            io.reqOut.bits.addrRequest := io.tlMasterReceiver.bits.a_address
            io.reqOut.bits.dataRequest := io.tlMasterReceiver.bits.a_data
            io.reqOut.bits.activeByteLane := io.tlMasterReceiver.bits.a_mask
            io.reqOut.bits.isWrite := io.tlMasterReceiver.bits.a_opcode === PutFullData.U || io.tlMasterReceiver.bits.a_opcode === PutPartialData.U
            io.reqOut.valid := true.B

            io.rspIn.ready := true.B

            when(((1.U << io.tlMasterReceiver.bits.a_size).asUInt() > config.w.U) && io.tlMasterReceiver.bits.a_opcode === Get.U){
                opRegD := io.tlMasterReceiver.bits.a_opcode
                addRegD := io.tlMasterReceiver.bits.a_address
                maskRegD := io.tlMasterReceiver.bits.a_mask
                counterD := ((1.U << io.tlMasterReceiver.bits.a_size).asUInt /config.w.U)-1.U 
            }
            

        }

       
        when(io.rspIn.valid && config.uh.asBool && counterD > 0.U && opRegD =/= Get.U){
            io.rspIn.ready := false.B
            counterD := counterD -1.U

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

            when(((1.U << io.tlMasterReceiver.bits.a_size).asUInt > config.w.U) && io.tlMasterReceiver.bits.a_opcode =/= Get.U){
                counterD := ((1.U << io.tlMasterReceiver.bits.a_size).asUInt /config.w.U)-1.U
                opRegD := io.tlMasterReceiver.bits.a_opcode
            }
            io.rspIn.ready := false.B

            
        }

    }
   

}