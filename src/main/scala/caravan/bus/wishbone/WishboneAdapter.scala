package caravan.bus.wishbone

import chisel3._ 
import chisel3.util._
import caravan.bus.common._
import caravan.bus.wishbone._

class WishboneAdapterIO(implicit val config:WishboneConfig) extends BusAdapterIO{
    /*  MASTER SIDE  */
    val reqIn =  Flipped(Decoupled(new WBRequest))
    val rspOut = Decoupled(new WBResponse)

    /*  SLAVE SIDE */
    val reqOut = Decoupled(new WBRequest)
    val rspIn = Flipped(Decoupled(new WBResponse))
}

class WishboneAdapter(implicit val config:WishboneConfig) extends BusAdapter {
    val io = IO(new WishboneAdapterIO)

    val wbHost = Module(new WishboneHost)
    val wbSlave = Module(new WishboneDevice)

    /*  Connecting Master Interconnects  */
    wbHost.io.wbMasterTransmitter <> wbSlave.io.wbMasterReceiver

    /*  Connecting Slave Interconnects  */
    wbSlave.io.wbSlaveTransmitter <> wbHost.io.wbSlaveReceiver

    /*  Sending Request in Master  */
    wbHost.io.reqIn <> io.reqIn

    /*  Sending Response out from Master  */
    io.rspOut <> wbHost.io.rspOut

    /*  Sending Request out from Slave  */
    io.reqOut <> wbSlave.io.reqOut

    /*  Sending Response in Slave  */
    wbSlave.io.rspIn <> io.rspIn
}