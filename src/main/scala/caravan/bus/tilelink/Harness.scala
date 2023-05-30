package caravan.bus.tilelink
import caravan.bus.common.{AddressMap, BusDecoder, DeviceAdapter, Switch1toN, DummyMemController,BlockRamWithMasking}
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.stage.ChiselStage
import chisel3.util.{Cat, Decoupled, MuxLookup}
import chisel3.util.experimental.loadMemoryFromFile


class TilelinkHarness/*(programFile: Option[String])*/(implicit val config: TilelinkConfig) extends Module {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val addrReq = Input(UInt(config.a.W))
    val dataReq = Input(UInt((config.w * 8).W))
    val byteLane = Input(UInt(config.w.W))
    val isWrite = Input(Bool())
    val isArithmetic = if(config.uh) Some(Input(Bool())) else None
    val isLogical = if(config.uh) Some(Input(Bool())) else None
    val isIntent = if(config.uh) Some(Input(Bool())) else None 
    val param = if(config.uh) Some(Input(UInt(3.W))) else None
    val size = if (config.uh) Some(Input(UInt(config.z.W))) else None


    val validResp = Output(Bool())
    val dataResp = Output(UInt(32.W))
    // val ackResp = Output(Bool())
  })

  implicit val request = new TLRequest()    
  implicit val response = new TLResponse()

  val tlHost = Module(new TilelinkHost())
  val tlSlave = Module(new TilelinkDevice())
  val memCtrl = Module(new BlockRamWithMasking(new TLRequest(),new TLResponse(),1024))

  tlHost.io.rspOut.ready := true.B  // IP always ready to accept data from wb host

  tlHost.io.tlMasterTransmitter <> tlSlave.io.tlMasterReceiver
  tlSlave.io.tlSlaveTransmitter <> tlHost.io.tlSlaveReceiver

  //tlHost.io.reqIn.valid := Mux(tlHost.io.reqIn.ready, io.valid, false.B)
  tlHost.io.reqIn.valid := io.valid
  tlHost.io.reqIn.bits.addrRequest := io.addrReq
  tlHost.io.reqIn.bits.dataRequest := io.dataReq.asUInt
  tlHost.io.reqIn.bits.activeByteLane := io.byteLane
  tlHost.io.reqIn.bits.isWrite := io.isWrite
  if (config.uh){
    tlHost.io.reqIn.bits.isArithmetic.get := io.isArithmetic.get
    tlHost.io.reqIn.bits.isLogical.get := io.isLogical.get
    tlHost.io.reqIn.bits.isIntent.get := io.isIntent.get
    tlHost.io.reqIn.bits.param.get := io.param.get
    tlHost.io.reqIn.bits.size.get := io.size.get
  }


  tlSlave.io.reqOut <> memCtrl.io.req
  tlSlave.io.rspIn <> memCtrl.io.rsp

  
  io.dataResp := tlHost.io.rspOut.bits.dataResponse
  io.validResp := tlHost.io.rspOut.valid
  // io.ackResp := tlHost.io.rspOut.bits.ackWrite

}

object TilelinkDriver extends App {
  implicit val config = TilelinkConfig()
  (new ChiselStage).emitVerilog(new TilelinkHarness())
}