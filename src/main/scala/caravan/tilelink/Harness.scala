package caravan.bus.tileink
import caravan.bus.common.{AddressMap, BusDecoder, DeviceAdapter, Switch1toN}
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.stage.ChiselStage
import chisel3.util.{Cat, Decoupled}
import chisel3.util.experimental.loadMemoryFromFile


class DummyMemController(programFile: Option[String])(implicit val config: TilelinkConfig) extends Module {
    val io = IO(new Bundle {
        val req = Flipped(Decoupled(new TLRequest()))
        val rsp = Decoupled(new TLResponse())
    })

    val mem = SyncReadMem(1024, UInt(32.W))

    when(io.req.bits.isWrite){
        mem.write(io.req.bits.addrRequest, io.req.bits.dataRequest)
        io.rsp.bits.dataResponse := io.req.bits.dataRequest
        io.rsp.bits.error := false.B
    }.otherwise{
        io.rsp.bits.dataResponse := mem.read(io.req.bits.addrRequest)
        io.rsp.bits.error := false.B
    }
    io.rsp.valid := true.B
    io.req.ready := true.B

}

class Harness(programFile: Option[String])(implicit val config: TilelinkConfig) extends Module {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val addrReq = Input(UInt(config.a.W))
    val dataReq = Input(UInt((config.w * 8).W))
    val byteLane = Input(UInt(((config.w * 8)/config.granularity).W))
    val isWrite = Input(Bool())

    val validResp = Output(Bool())
    val dataResp = Output(UInt(32.W))
  })

  val tlHost = Module(new TilelinkHost())
  val tlSlave = Module(new TilelinkDevice())
  val memCtrl = Module(new DummyMemController(programFile))

  tlHost.io.rspOut.ready := true.B  // IP always ready to accept data from wb host

  tlHost.io.tlMasterTransmitter <> tlSlave.io.tlMasterReceiver
  tlSlave.io.tlSlaveTransmitter <> tlHost.io.tlSlaveReceiver

  tlHost.io.reqIn.valid := Mux(tlHost.io.reqIn.ready, io.valid, false.B)
  tlHost.io.reqIn.bits.addrRequest := io.addrReq
  tlHost.io.reqIn.bits.dataRequest := io.dataReq
  tlHost.io.reqIn.bits.activeByteLane := io.byteLane
  tlHost.io.reqIn.bits.isWrite := io.isWrite



  tlSlave.io.reqOut <> memCtrl.io.req
  tlSlave.io.rspIn <> memCtrl.io.rsp

  io.dataResp := tlHost.io.rspOut.bits.dataResponse
  io.validResp := tlHost.io.rspOut.valid

}


