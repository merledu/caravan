package caravan.bus.wishbone
import chisel3._
import caravan.bus.common.{FromBus, FromIp}

/** class that allows the client to create a wishbone bus
 * @param config accepts a WishboneConfig type that configures various parameters for the bus
 * @example {{{
 *         val masterBus = WishboneBus(WishboneConfig(addressWidth=32, dataWidth=32))
 *         val slaveBus = Flipped(WishboneBus(WishboneConfig(addressWidth=32, dataWidth=32))
 *         }}}
 *         */
class WishboneMaster(implicit val config: WishboneConfig) extends Bundle {
  /**
   * cyc ->  indicates that a valid bus cycle is in progress
   * stb ->  indicates a valid data transfer cycle
   * we ->  indicates whether the current bus cycle is a READ or WRITE cycle
   * adr ->  carries the address for the current bus cycle
   * dat ->  contains the data output from the master
   * sel -> the sel output which indicates where valid data lane is expected on the dat_i for READs or dat_o for WRITEs
   * */
  val cyc        = Bool()
  val stb        = Bool()
  val we         = Bool()
  val adr        = UInt(config.addressWidth.W)
  val dat        = UInt(config.dataWidth.W)
  val sel        = UInt((config.dataWidth/config.granularity).W)
}

class WishboneSlave(implicit val config: WishboneConfig) extends Bundle {
  /**
   * ack ->  indicates a normal termination of bus cycle
   * dat ->  contains the data output from the slave
   */
  val ack = Bool()
  val dat = UInt(config.dataWidth.W)
}

class IPToWishboneAdapter(implicit val config: WishboneConfig) extends FromIp {
  override val addrRequest: UInt = UInt(config.addressWidth.W)
  override val dataRequest: UInt = UInt(config.dataWidth.W)
  override val activeByteLane: UInt = UInt((config.dataWidth/config.granularity).W)
  override val isWrite: Bool = Bool()
}

class WishboneToIPAdapter(implicit val config: WishboneConfig) extends FromBus {
  override val dataResponse: UInt = UInt(config.dataWidth.W)
  override val ackWrite: Bool = Bool()
}

