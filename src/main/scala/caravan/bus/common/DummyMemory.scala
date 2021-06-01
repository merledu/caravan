package caravan.bus.common
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.stage.ChiselStage
import chisel3.util.{Cat, Decoupled, MuxLookup}
import chisel3.util.experimental.loadMemoryFromFile


//implicit parameters for Config, Request and Response
class DummyMemController(programFile: Option[String])(implicit val config: BusConfig, implicit val request: AbstrRequest, implicit val response: AbstrResponse) extends Module {
    val io = IO(new Bundle {
        val req = Flipped(Decoupled(request))
        val rsp = Decoupled(response)
    })

    val validReg = RegInit(false.B)

    io.rsp.valid := validReg
    io.rsp.bits.error := false.B
    io.req.ready := true.B

    // masked memory init
    val mem = SyncReadMem(1024, Vec(4, UInt(8.W)))

    if (programFile.isDefined) {
        loadMemoryFromFile(mem, programFile.get)
    }

    

    when(io.req.bits.isWrite){

        // data is written with mask, by use of Vec[UInt(8.W)]  => Byte-wise
        mem.write(io.req.bits.addrRequest, io.req.bits.dataRequest.asTypeOf(Vec(4,UInt(8.W))), io.req.bits.activeByteLane.asBools)
        validReg := true.B
        io.rsp.bits.dataResponse := io.req.bits.dataRequest

    }.otherwise{

        //TODO: make reading dynamic; what if more than 4 bytes are used !!

        // data is read with mask, sending, either 0s or actiual data of the byte for all 4 bytes.
        io.rsp.bits.dataResponse := Cat(
            Mux(io.req.bits.activeByteLane.asBools()(3), mem.read(io.req.bits.addrRequest)(3), 0.U(8.W)),
            Mux(io.req.bits.activeByteLane.asBools()(2), mem.read(io.req.bits.addrRequest)(2), 0.U(8.W)),
            Mux(io.req.bits.activeByteLane.asBools()(1), mem.read(io.req.bits.addrRequest)(1), 0.U(8.W)),
            Mux(io.req.bits.activeByteLane.asBools()(0), mem.read(io.req.bits.addrRequest)(0), 0.U(8.W)),
        )
        validReg := true.B
        
    }

    

}