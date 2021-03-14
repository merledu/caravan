package caravan.bus.wishbone
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.{Cat, Decoupled}
import chisel3.util.experimental.loadMemoryFromFile

class Harness(implicit val config: WishboneConfig) extends Module {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val addrReq = Input(UInt(config.addressWidth.W))
    val dataReq = Input(UInt(config.dataWidth.W))
    val byteLane = Input(UInt((config.dataWidth/config.granularity).W))
    val isWrite = Input(Bool())

    val validResp = Output(Bool())
    val dataResp = Output(UInt(32.W))
    val ackWrite = Output(Bool())
  })

  val wbHost = Module(new WishboneHost())
  val wbSlave = Module(new WishboneDevice())
  val memCtrl = Module(new DummyMemController())

  wbHost.io.rspOut.ready := true.B  // IP always ready to accept data from wb host

  wbHost.io.wbMasterTransmitter <> wbSlave.io.wbMasterReceiver
  wbSlave.io.wbSlaveTransmitter <> wbHost.io.wbSlaveReceiver

  wbHost.io.reqIn.valid := io.valid
  wbHost.io.reqIn.bits.addrRequest := io.addrReq
  wbHost.io.reqIn.bits.dataRequest := io.dataReq
  wbHost.io.reqIn.bits.activeByteLane := io.byteLane
  wbHost.io.reqIn.bits.isWrite := io.isWrite



  wbSlave.io.reqOut <> memCtrl.io.req
  wbSlave.io.rspIn <> memCtrl.io.rsp

  io.dataResp := wbHost.io.rspOut.bits.dataResponse
  io.ackWrite := wbHost.io.rspOut.bits.ackWrite
  io.validResp := wbHost.io.rspOut.valid

}

class DummyMemController(implicit val config: WishboneConfig) extends Module {
  val io = IO(new Bundle {
    val req = Flipped(Decoupled(new Request()))
    val rsp = Decoupled(new Response())
  })
  io.rsp.bits.ackWrite := false.B // only read support for now
  io.req.ready := true.B // always ready to accept requests from device
  val mem = SyncReadMem(1024, UInt(32.W))
  loadMemoryFromFile(mem, "/User/mbp/Desktop/mem1.txt")
  when(io.req.valid && !io.req.bits.isWrite) {
    io.rsp.valid := true.B
    when(io.req.bits.activeByteLane === "b0001".U) {
      io.rsp.bits.dataResponse := Cat(0.U(24.W), mem.read(io.req.bits.addrRequest))
    } .elsewhen(io.req.bits.activeByteLane === "b0011".U) {
      io.rsp.bits.dataResponse := Cat(0.U(16.W), mem.read(io.req.bits.addrRequest))
    } .elsewhen(io.req.bits.activeByteLane === "b0111".U) {
      io.rsp.bits.dataResponse := Cat(0.U(8.W), mem.read(io.req.bits.addrRequest))
    } .elsewhen(io.req.bits.activeByteLane === "b1111".U) {
      io.rsp.bits.dataResponse := mem.read(io.req.bits.addrRequest)
    } .otherwise {
      io.rsp.bits.dataResponse := DontCare
    }
  } .otherwise {
    io.rsp.valid := false.B
    io.rsp.bits.dataResponse := DontCare
  }
}

object HarnessDriver extends App {
  implicit val config = WishboneConfig(addressWidth = 10, dataWidth = 32)
  println((new ChiselStage).emitVerilog(new Harness()))
}
