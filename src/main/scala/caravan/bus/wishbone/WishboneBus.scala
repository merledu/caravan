package caravan.bus.wishbone
import chisel3._

/** class that allows the client to create a wishbone bus
 * @param config accepts a WishboneConfig type that configures various parameters for the bus
 * @example {{{
 *         val masterBus = WishboneBus(WishboneConfig(addressWidth=32, dataWidth=32))
 *         val slaveBus = Flipped(WishboneBus(WishboneConfig(addressWidth=32, dataWidth=32))
 *         }}}
 *         */
case class WishboneBus(config: WishboneConfig) extends Bundle {
  /**
   * cyc_o ->  indicates that a valid bus cycle is in progress
   * stb_o ->  indicates a valid data transfer cycle
   * ack_i ->  indicates a normal termination of bus cycle
   * we_o  ->  indicates whether the current bus cycle is a READ or WRITE cycle
   * adr_o ->  carries the address for the current bus cycle
   * dat_mosi ->  contains the data output from the master
   * dat_miso ->  contains the data output from the slave
   * err_i ->  indicates an abnormal bus cycle termination
   * lock_o -> when asserted, indicates that the current bus cycle is uninterruptible
   * rty_i -> indicates that the interface is not ready to accept or send data, and the cycle should be retried
   * sel_o -> the sel output which indicates where valid data lane is expected on the dat_i for READs or dat_o for WRITEs
   * */
  val cyc_o        = Output(Bool())
  val stb_o        = Output(Bool())
  val ack_i        = Input(Bool())
  val we_o         = Output(Bool())
  val adr_o        = Output(UInt(config.addressWidth.W))
  val dat_mosi     = Output(UInt(config.dataWidth.W))
  val dat_miso     = Input(UInt(config.dataWidth.W))
  val err_i        = Input(Bool())
  val sel_o        = Output(UInt((config.dataWidth/config.granularity).W))
}

