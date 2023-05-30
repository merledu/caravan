package caravan.bus.tilelink
import caravan.bus.common.HostAdapter
import chisel3._
import chisel3.experimental.DataMirror
import chisel3.stage.ChiselStage
import chisel3.util._
class TilelinkHost(implicit val config: TilelinkConfig) extends HostAdapter with OpCodes {
    val io = IO(new Bundle {
        val tlMasterTransmitter = Decoupled(new TilelinkMaster())
        val tlSlaveReceiver  = Flipped(Decoupled(new TilelinkSlave()))
        val reqIn = Flipped(Decoupled(new TLRequest()))
        val rspOut = Decoupled(new TLResponse())
    })


    val opReg = RegInit(NoOp.U)
    val paramReg = RegInit(0.U)
    val sizeReg = RegInit(0.U)
    val addReg = RegInit(0.U)
    val sourceReg = RegInit(0.U)
    val counterHost = RegInit(UInt(config.z.W),0.U)

    dontTouch(io.reqIn.valid)

    io.tlSlaveReceiver.ready    := false.B
    io.reqIn.ready              := true.B
    dontTouch(io.reqIn.ready)
    io.tlMasterTransmitter.bits.a_opcode    := 0.U
    io.tlMasterTransmitter.bits.a_data      := 0.U
    io.tlMasterTransmitter.bits.a_address   := addReg
    io.tlMasterTransmitter.bits.a_param     := 0.U
    io.tlMasterTransmitter.bits.a_source    := 0.U
    io.tlMasterTransmitter.bits.a_size      := 0.U
    io.tlMasterTransmitter.bits.a_mask      := 0.U
    io.tlMasterTransmitter.bits.a_corrupt   := 0.U
    io.tlMasterTransmitter.valid            := 0.U
    io.rspOut.bits.dataResponse             := 0.U
    io.rspOut.bits.error                    := 0.U
    io.rspOut.valid                         := false.B

    when(~(io.reqIn.valid) && counterHost > 0.U){
        counterHost := 0.U
    }
    when(counterHost === 0.U){
        opReg := NoOp.U
        paramReg := 0.U
        sizeReg := 0.U
        sourceReg := 0.U
        counterHost := 0.U
    }
    
    
    when(io.reqIn.valid){
        when(config.uh.asBool && counterHost > 0.U){
            when(opReg =/= Get.U){
                io.tlMasterTransmitter.bits.a_opcode := opReg
                io.tlMasterTransmitter.bits.a_param := paramReg
                io.tlMasterTransmitter.bits.a_size := sizeReg
                io.tlMasterTransmitter.bits.a_source := sourceReg
                when(opReg =/= Arithmetic.U && opReg =/= Logical.U){
                    io.tlMasterTransmitter.bits.a_address := addReg + config.w.U
                    addReg := addReg + config.w.U
                }
                .otherwise{
                    io.tlMasterTransmitter.bits.a_address := addReg
                    }
                io.tlMasterTransmitter.bits.a_data      := io.reqIn.bits.dataRequest
                io.tlMasterTransmitter.bits.a_mask      := io.reqIn.bits.activeByteLane
                io.tlMasterTransmitter.bits.a_corrupt   := false.B
                io.tlMasterTransmitter.valid            := io.reqIn.valid
                io.reqIn.ready           := false.B
                counterHost := counterHost - 1.U
                io.tlSlaveReceiver.ready := true.B
            }
        .elsewhen(opReg === Get.U){
            counterHost := counterHost - 1.U
            io.reqIn.ready           := false.B
            io.tlSlaveReceiver.ready := true.B
        }
    }.otherwise{
       
        if (config.uh){
            io.tlMasterTransmitter.bits.a_opcode    := Mux1H(Cat(io.reqIn.bits.isIntent.get,io.reqIn.bits.isLogical.get,io.reqIn.bits.isArithmetic.get,~(io.reqIn.bits.isIntent.get | io.reqIn.bits.isLogical.get | io.reqIn.bits.isArithmetic.get))
                                                        ,Seq(Mux(io.reqIn.bits.isWrite, Mux(io.reqIn.bits.activeByteLane === "b1111".U, PutFullData.U, PutPartialData.U) , Get.U),Arithmetic.U,Logical.U,Intent.U
                                                        ))
        }else{
            io.tlMasterTransmitter.bits.a_opcode := Mux(io.reqIn.bits.isWrite, Mux(io.reqIn.bits.activeByteLane === "b1111".U, PutFullData.U, PutPartialData.U) , Get.U)
        }
        io.tlMasterTransmitter.bits.a_data      := io.reqIn.bits.dataRequest
        io.tlMasterTransmitter.bits.a_address   := io.reqIn.bits.addrRequest
        if (config.uh){
            io.tlMasterTransmitter.bits.a_param     := io.reqIn.bits.param.get.asUInt
        }else{
            io.tlMasterTransmitter.bits.a_param := 0.U
        }
        io.tlMasterTransmitter.bits.a_source    := 2.U
        if(config.uh){
            io.tlMasterTransmitter.bits.a_size := io.reqIn.bits.size.get
            when(((1.U << io.tlMasterTransmitter.bits.a_size).asUInt > config.w.U)){
                opReg := io.tlMasterTransmitter.bits.a_opcode
                paramReg := io.tlMasterTransmitter.bits.a_param
                sizeReg := io.tlMasterTransmitter.bits.a_size
                sourceReg := io.tlMasterTransmitter.bits.a_source
                when(io.tlMasterTransmitter.bits.a_opcode === Arithmetic.U || io.tlMasterTransmitter.bits.a_opcode === Logical.U){
                    counterHost := 3.U * ((1.U << io.tlMasterTransmitter.bits.a_size.asUInt)/config.w.U).asUInt - 1.U
                }
                .otherwise{
                    counterHost := ((1.U << io.tlMasterTransmitter.bits.a_size.asUInt)/config.w.U)-1.U
                }
        }
        }else{
            io.tlMasterTransmitter.bits.a_size      := MuxLookup(config.w.U, 2.U,Array(                    // default 32-bit
                                                                                (1.U) -> 0.U,
                                                                                (2.U) -> 1.U,
                                                                                (4.U) -> 2.U,
                                                                                (8.U) -> 3.U
                                                                            ))
        }
        io.tlMasterTransmitter.bits.a_mask      := io.reqIn.bits.activeByteLane
        io.tlMasterTransmitter.bits.a_corrupt   := false.B
        io.tlMasterTransmitter.valid            := io.reqIn.valid
       
        io.reqIn.ready           := false.B
        io.tlSlaveReceiver.ready := true.B
        addReg := io.reqIn.bits.addrRequest
    }
    }
    
    when(io.tlSlaveReceiver.valid){
        
        io.rspOut.bits.dataResponse := io.tlSlaveReceiver.bits.d_data
        io.rspOut.bits.error := io.tlSlaveReceiver.bits.d_denied
     
        io.rspOut.valid := io.tlSlaveReceiver.valid
      
        io.tlSlaveReceiver.ready := false.B
        when(counterHost > 0.U){
            io.reqIn.ready := false.B
        }.otherwise{
            io.reqIn.ready           := true.B
        }
    }
}