package caravan.bus.tileink
import chisel3._
import caravan.bus.common.{AbstrRequest, AbstrResponse, BusDevice, BusHost}



class TLRequest(implicit val config: TilelinkConfig) extends AbstrRequest {
  override val addrRequest: UInt = UInt(config.a.W)
  override val dataRequest: UInt = UInt((config.w * 8).W)
  override val activeByteLane: UInt = UInt(config.w.W)
  override val isWrite: Bool = Bool()
}

class TLResponse(implicit val config: TilelinkConfig) extends AbstrResponse {
  override val dataResponse: UInt = UInt((config.w * 8).W)
  override val error: Bool = Bool()
}

// channel A -- Request Channel
class TilelinkMaster(implicit val config: TilelinkConfig) extends TLHost {
  
    val a_opcode = UInt(3.W)
    val a_param = UInt(3.W)
    val a_size = UInt(config.z.W)
    val a_source = UInt(config.o.W)
    val a_address = UInt(config.a.W)
    val a_mask = UInt(config.w.W)
    val a_corrupt = UInt(1.W)
    val a_data = UInt((config.w * 8).W)

}

// channel D -- Response Channel
class TilelinkSlave(implicit val config: TilelinkConfig) extends TLDevice {
    val d_opcode = UInt(3.W)
    val d_param = UInt(2.W)
    val d_size = UInt(config.z.W)
    val d_source = UInt(config.o.W)
    val d_sink = UInt(config.i.W)  
    val d_denied = UInt(1.W)
    val d_corrupt = UInt(1.W)
    val d_data = UInt((config.w * 8).W)
}

case class TLHost() extends BusHost
case class TLDevice() extends BusDevice