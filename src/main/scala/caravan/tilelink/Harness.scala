package caravan.bus.tileink
import caravan.bus.common.{AddressMap, BusDecoder, DeviceAdapter, Switch1toN}
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.stage.ChiselStage
import chisel3.util.{Cat, Decoupled, MuxLookup}
import chisel3.util.experimental.loadMemoryFromFile


class DummyMemController(programFile: Option[String])(implicit val config: TilelinkConfig) extends Module {
    val io = IO(new Bundle {
        val req = Flipped(Decoupled(new TLRequest()))
        val rsp = Decoupled(new TLResponse())
    })

    val mem = SyncReadMem(1024, UInt((config.w*8).W))

    

    when(io.req.bits.isWrite){
        mem.write(io.req.bits.addrRequest, io.req.bits.dataRequest)
        io.rsp.bits.dataResponse := io.req.bits.dataRequest
        io.rsp.bits.error := false.B
    }.otherwise{
        io.rsp.bits.dataResponse := MuxLookup(io.req.bits.activeByteLane, 
        mem.read(io.req.bits.addrRequest),
        Array(
          ("b0001".U) -> Cat(0.U(24.W), mem.read(io.req.bits.addrRequest)(7,0)),
          ("b0011".U) -> Cat(0.U(16.W), mem.read(io.req.bits.addrRequest)(15,0)),
          ("b0111".U) -> Cat(0.U(8.W), mem.read(io.req.bits.addrRequest)(23,0)),
          ("b1111".U) -> mem.read(io.req.bits.addrRequest),
        ))
        // io.rsp.bits.dataResponse := mem.read(io.req.bits.addrRequest, io.req.valid)
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
    val byteLane = Input(UInt(config.w.W))
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


