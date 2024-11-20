package caravan.bus.tilelink
import caravan.bus.common.{HostAdapter, HostAdapterIO}
import chisel3._
import chisel3.experimental.DataMirror
import chisel3.stage.ChiselStage
import chisel3.util._

class TilelinkHostIO(implicit val config: TilelinkConfig) extends HostAdapterIO{
    val masterTransmitter   = Decoupled(new TilelinkMaster())
    val slaveReceiver       = Flipped(Decoupled(new TilelinkSlave()))
    val reqIn               = Flipped(Decoupled(new TLRequest()))
    val rspOut              = Decoupled(new TLResponse())
}

class TilelinkHost(implicit val config: TilelinkConfig) extends HostAdapter with OpCodes {
    val io = IO(new TilelinkHostIO)

    def getAddressPin: UInt = io.masterTransmitter.bits.a_address


    val opReg = RegInit(NoOp.U)
    val paramReg = RegInit(0.U)
    val sizeReg = RegInit(0.U)
    val addReg = RegInit(0.U)
    val sourceReg = RegInit(0.U)
    val counterHost = RegInit(UInt(config.z.W),0.U)

    dontTouch(io.reqIn.valid)

    io.slaveReceiver.ready    := false.B
    io.reqIn.ready              := true.B
    dontTouch(io.reqIn.ready)
    io.masterTransmitter.bits.a_opcode    := 0.U
    io.masterTransmitter.bits.a_data      := 0.U
    io.masterTransmitter.bits.a_address   := addReg
    io.masterTransmitter.bits.a_param     := 0.U
    io.masterTransmitter.bits.a_source    := 0.U
    io.masterTransmitter.bits.a_size      := 0.U
    io.masterTransmitter.bits.a_mask      := 0.U
    io.masterTransmitter.bits.a_corrupt   := 0.U
    io.masterTransmitter.valid            := 0.U
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
                io.masterTransmitter.bits.a_opcode := opReg
                io.masterTransmitter.bits.a_param := paramReg
                io.masterTransmitter.bits.a_size := sizeReg
                io.masterTransmitter.bits.a_source := sourceReg
                when(opReg =/= Arithmetic.U && opReg =/= Logical.U){
                    io.masterTransmitter.bits.a_address := addReg + config.w.U
                    addReg := addReg + config.w.U
                }
                .otherwise{
                    io.masterTransmitter.bits.a_address := addReg
                    }
                io.masterTransmitter.bits.a_data      := io.reqIn.bits.dataRequest
                io.masterTransmitter.bits.a_mask      := io.reqIn.bits.activeByteLane
                io.masterTransmitter.bits.a_corrupt   := false.B
                io.masterTransmitter.valid            := io.reqIn.valid
                io.reqIn.ready           := false.B
                counterHost := counterHost - 1.U
                io.slaveReceiver.ready := true.B
            }
        .elsewhen(opReg === Get.U){
            counterHost := counterHost - 1.U
            io.reqIn.ready           := false.B
            io.slaveReceiver.ready := true.B
        }
    }.otherwise{
       
        if (config.uh){
            io.masterTransmitter.bits.a_opcode    := Mux1H(Cat(io.reqIn.bits.isIntent.get,io.reqIn.bits.isLogical.get,io.reqIn.bits.isArithmetic.get,~(io.reqIn.bits.isIntent.get | io.reqIn.bits.isLogical.get | io.reqIn.bits.isArithmetic.get))
                                                        ,Seq(Mux(io.reqIn.bits.isWrite, Mux(io.reqIn.bits.activeByteLane === "b1111".U, PutFullData.U, PutPartialData.U) , Get.U),Arithmetic.U,Logical.U,Intent.U
                                                        ))
        }else{
            io.masterTransmitter.bits.a_opcode := Mux(io.reqIn.bits.isWrite, Mux(io.reqIn.bits.activeByteLane === "b1111".U, PutFullData.U, PutPartialData.U) , Get.U)
        }
        io.masterTransmitter.bits.a_data      := io.reqIn.bits.dataRequest
        io.masterTransmitter.bits.a_address   := io.reqIn.bits.addrRequest
        if (config.uh){
            io.masterTransmitter.bits.a_param     := io.reqIn.bits.param.get.asUInt
        }else{
            io.masterTransmitter.bits.a_param := 0.U
        }
        io.masterTransmitter.bits.a_source    := 2.U
        if(config.uh){
            io.masterTransmitter.bits.a_size := io.reqIn.bits.size.get
            when(((1.U << io.masterTransmitter.bits.a_size).asUInt > config.w.U)){
                opReg := io.masterTransmitter.bits.a_opcode
                paramReg := io.masterTransmitter.bits.a_param
                sizeReg := io.masterTransmitter.bits.a_size
                sourceReg := io.masterTransmitter.bits.a_source
                when(io.masterTransmitter.bits.a_opcode === Arithmetic.U || io.masterTransmitter.bits.a_opcode === Logical.U){
                    counterHost := 3.U * ((1.U << io.masterTransmitter.bits.a_size.asUInt)/config.w.U).asUInt - 1.U
                }
                .otherwise{
                    counterHost := ((1.U << io.masterTransmitter.bits.a_size.asUInt)/config.w.U)-1.U
                }
        }
        }else{
            io.masterTransmitter.bits.a_size      := MuxLookup(config.w.U, 2.U,Array(                    // default 32-bit
                                                                                (1.U) -> 0.U,
                                                                                (2.U) -> 1.U,
                                                                                (4.U) -> 2.U,
                                                                                (8.U) -> 3.U
                                                                            ))
        }
        io.masterTransmitter.bits.a_mask      := io.reqIn.bits.activeByteLane
        io.masterTransmitter.bits.a_corrupt   := false.B
        io.masterTransmitter.valid            := io.reqIn.valid
       
        io.reqIn.ready           := false.B
        io.slaveReceiver.ready := true.B
        addReg := io.reqIn.bits.addrRequest
    }
    }
    
    when(io.slaveReceiver.valid){
        
        io.rspOut.bits.dataResponse := io.slaveReceiver.bits.d_data
        io.rspOut.bits.error := io.slaveReceiver.bits.d_denied
     
        io.rspOut.valid := io.slaveReceiver.valid
      
        io.slaveReceiver.ready := false.B
        when(counterHost > 0.U){
            io.reqIn.ready := false.B
        }.otherwise{
            io.reqIn.ready           := true.B
        }
    }
}