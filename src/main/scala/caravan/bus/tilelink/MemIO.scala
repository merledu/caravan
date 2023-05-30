package caravan.bus.tilelink

import chisel3._

/**
 * This abstract class provides a template for other protocols to implement the transaction wires.
 * This is used as a template for e.g when the core wants to communicate with the memory or with the peripheral registers.
 * It will set these signals up in order to talk to the Host adapter of the relevant bus protocol
 */
class MemRequestIO(implicit val config: TilelinkConfig) extends Bundle {
  val addrRequest: UInt = Input(UInt(32.W))
  val dataRequest: UInt = Input(UInt(32.W))
  val activeByteLane: UInt = Input(UInt(4.W))
  val isWrite: Bool = Input(Bool())
  val isArithmetic = if(config.uh) Some(Bool()) else None
  val isLogical = if(config.uh) Some(Bool()) else None
  val isIntent = if(config.uh) Some(Bool()) else None 
  val param = if(config.uh) Some(UInt(3.W)) else None
  val size = if (config.uh) Some(UInt(config.z.W)) else None

}

class MemResponseIO(implicit val config: TilelinkConfig) extends Bundle {
  val dataResponse: UInt = Input(UInt((config.w * 8).W))
  val error : Bool = Bool()
}
