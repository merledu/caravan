package caravan.bus.wishbone
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.{Cat, Decoupled}
import chisel3.util.experimental.loadMemoryFromFile

class Harness(programFile: String)(implicit val config: WishboneConfig) extends Module {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val addrReq = Input(UInt(config.addressWidth.W))
    val dataReq = Input(UInt(config.dataWidth.W))
    val byteLane = Input(UInt((config.dataWidth/config.granularity).W))
    val isWrite = Input(Bool())

    val validResp = Output(Bool())
    val dataResp = Output(UInt(32.W))
  })

  val wbHost = Module(new WishboneHost())
  val wbSlave = Module(new WishboneDevice())
  val memCtrl = Module(new DummyMemController(programFile))

  wbHost.io.rspOut.ready := true.B  // IP always ready to accept data from wb host

  wbHost.io.wbMasterTransmitter <> wbSlave.io.wbMasterReceiver
  wbSlave.io.wbSlaveTransmitter <> wbHost.io.wbSlaveReceiver

  wbHost.io.reqIn.valid := Mux(wbHost.io.reqIn.ready, io.valid, false.B)
  wbHost.io.reqIn.bits.addrRequest := io.addrReq
  wbHost.io.reqIn.bits.dataRequest := io.dataReq
  wbHost.io.reqIn.bits.activeByteLane := io.byteLane
  wbHost.io.reqIn.bits.isWrite := io.isWrite



  wbSlave.io.reqOut <> memCtrl.io.req
  wbSlave.io.rspIn <> memCtrl.io.rsp

  io.dataResp := wbHost.io.rspOut.bits.dataResponse
  io.validResp := wbHost.io.rspOut.valid

}

class DummyMemController(programFile: String)(implicit val config: WishboneConfig) extends Module {
  val io = IO(new Bundle {
    val req = Flipped(Decoupled(new Request()))
    val rsp = Decoupled(new Response())
  })
  // the register that sends valid along with the data read from memory
  // a register is used so that it synchronizes along with the data that comes after one cycle
  val validReg = RegInit(false.B)
  io.rsp.valid := validReg
  io.req.ready := true.B // always ready to accept requests from device
  val mem = SyncReadMem(1024, UInt(32.W))
  loadMemoryFromFile(mem, programFile)
  when(io.req.valid && !io.req.bits.isWrite) {
    when(io.req.bits.activeByteLane === "b0001".U) {
      io.rsp.bits.dataResponse := Cat(0.U(24.W), mem.read(io.req.bits.addrRequest/4.U)(7,0))
      validReg := true.B
    } .elsewhen(io.req.bits.activeByteLane === "b0011".U) {
      io.rsp.bits.dataResponse := Cat(0.U(16.W), mem.read(io.req.bits.addrRequest/4.U)(15,0))
      validReg := true.B
    } .elsewhen(io.req.bits.activeByteLane === "b0111".U) {
      io.rsp.bits.dataResponse := Cat(0.U(8.W), mem.read(io.req.bits.addrRequest/4.U)(23,0))
      validReg := true.B
    } .elsewhen(io.req.bits.activeByteLane === "b1111".U) {
      io.rsp.bits.dataResponse := mem.read(io.req.bits.addrRequest/4.U)
      validReg := true.B
    } .otherwise {
      io.rsp.bits.dataResponse := DontCare
      validReg := false.B
    }
  } .elsewhen(io.req.valid && io.req.bits.isWrite) {
    mem.write(io.req.bits.addrRequest/4.U, io.req.bits.dataRequest)
    validReg := true.B
    io.rsp.bits.dataResponse := DontCare
  }. otherwise {
    validReg := false.B
    io.rsp.bits.dataResponse := DontCare
  }
}

object HarnessDriver extends App {
  implicit val config = WishboneConfig(addressWidth = 10, dataWidth = 32)
  println((new ChiselStage).emitVerilog(new Harness("/Users/mbp/Desktop/mem1.txt")))
}
