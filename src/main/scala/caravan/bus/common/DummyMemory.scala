package caravan.bus.common
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.stage.ChiselStage
import chisel3.util.{Cat, Decoupled, MuxLookup}
import chisel3.util.experimental.loadMemoryFromFile

//implicit parameters for Config, Request and Response
class DummyMemController/*(programFile: Option[String])*/(implicit val config: BusConfig, implicit val request: AbstrRequest, implicit val response: AbstrResponse) extends Module {
    val io = IO(new Bundle {
        val req = Flipped(Decoupled(request))
        val rsp = Decoupled(response)
    })

    val validReg = RegInit(false.B)
    val add = Reg(UInt(32.W))
    // val ackWriteReg = RegInit(false.B)

    io.rsp.valid := validReg
    io.rsp.bits.error := false.B
    io.req.ready := true.B
    add := io.req.bits.addrRequest

    // masked memory init
    val mem = SyncReadMem(1024, Vec(4, UInt(8.W)))

    // if (programFile.isDefined) {
    //     loadMemoryFromFile(mem, programFile.get)
    // }

    // holds the data in byte vectors read from memory
    val rData = Reg(Vec(4,UInt(8.W)))
    // holds the bytes that must be read according to the activeByteLane
    val data = Wire(Vec(4,UInt(8.W)))

    when(io.req.fire() && io.req.bits.isWrite){

        mem.write(add, io.req.bits.dataRequest.asTypeOf(Vec(4,UInt(8.W))), io.req.bits.activeByteLane.asBools)
        rData map (_ := DontCare)
        validReg := true.B
        // ackWriteReg := true.B


    }.elsewhen(io.req.fire() && !io.req.bits.isWrite){
         
        rData := mem.read(add)
        validReg := true.B
        // ackWriteReg := false.B
        
    }.otherwise{
        
        rData map (_ := DontCare)
        validReg := false.B
        // ackWriteReg := false.B
        
    }

    data := io.req.bits.activeByteLane.asBools zip rData map {
        case (b:Bool, i:UInt) => Mux(b === true.B, i, 0.U)
    }

    io.rsp.valid := validReg
    io.rsp.bits.dataResponse := data.asUInt
    // io.rsp.bits.ackWrite := ackWriteReg

    

}
class BlockRamWithMasking[A <: AbstrRequest, B <: AbstrResponse]
                         (gen: A, gen1: B, rows: Int) extends Module {


  val io = IO(new Bundle {
    val req = Flipped(Decoupled(gen))
    val rsp = Decoupled(gen1)
  })

  // holds the data in byte vectors to be written in memory
  val wdata = Wire(Vec(4, UInt(8.W)))
  // holds the data in byte vectors read from memory
  val rdata = Wire(Vec(4, UInt(8.W)))
  // holds the mask signals to be used for byte masking in memory
  val mask = Wire(Vec(4, Bool()))
  // holds the bytes that must be read according to the activeByteLane
  val data = Wire(Vec(4, UInt(8.W)))

  wdata(0) := io.req.bits.dataRequest(7,0)
  wdata(1) := io.req.bits.dataRequest(15,8)
  wdata(2) := io.req.bits.dataRequest(23,16)
  wdata(3) := io.req.bits.dataRequest(31,24)


  // connecting the mask bits with activeByteLane bits
  val byteLane = io.req.bits.activeByteLane.asBools()
  mask zip byteLane map {case(m, b) =>
    m := b
  }


  // the register that sends valid along with the data read from memory
  // a register is used so that it synchronizes along with the data that comes after one cycle
  val validReg = RegInit(false.B)
  io.rsp.valid := validReg
  io.rsp.bits.error := false.B   // assuming memory controller would never return an error
  io.req.ready := true.B // assuming we are always ready to accept requests from device

  val mem = SyncReadMem(rows, Vec(4, UInt((32/4).W)))

  when(io.req.fire() && !io.req.bits.isWrite) {
    // READ
    rdata := mem.read(io.req.bits.addrRequest)
    validReg := true.B
  } .elsewhen(io.req.fire() && io.req.bits.isWrite) {
    // WRITE
    mem.write(io.req.bits.addrRequest, wdata, mask)
    validReg := true.B
    rdata map (_ := DontCare)
  } .otherwise {
    validReg := false.B
    rdata map (_ := DontCare)
  }

  /** return only those bytes which are enabled by mask
   * else return 0s*/
  data := mask zip rdata map {case (b: Bool, i: UInt) =>
    Mux(b === true.B, i, 0.U)
  }

  io.rsp.bits.dataResponse := Cat(data(3), data(2) ,data(1), data(0))


}