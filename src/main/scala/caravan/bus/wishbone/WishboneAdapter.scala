package caravan.bus.wishbone

import chisel3._ 
import chisel3.util._

class WishboneAdapter(implicit val config:WishboneConfig) extends Module {
    val io = IO(new Bundle{

        /*  MASTER SIDE  */
        val reqIn =  Flipped(Decoupled(new WBRequest))
        val rspOut = Decoupled(new WBResponse)

        /*  SLAVE SIDE */
        val reqOut = Decoupled(new WBRequest)
        val rspIn = Flipped(Decoupled(new WBResponse))
    })

    val wbHost = Module(new WishboneHost)
    val wbSlave = Module(new WishboneSlave)

    /*  Connecting Master Interconnects  */
    wbHost.io.wbMasterTransmitter <> wbSlave.io.wbMasterReceiver

    /*  Connecting Slave Interconnects  */
    tlSlave.io.wbSlaveTransmitter <> wbHost.io.wbSlaveReceiver

    /*  Sending Request in Master  */
    wbHost.io.reqIn <> io.reqIn

    /*  Sending Response out from Master  */
    io.rspOut <> wbHost.io.rspOut

    /*  Sending Request out from Slave  */
    io.reqOut <> wbSlave.io.reqOut

    /*  Sending Response in Slave  */
    wbSlave.io.rspIn <> io.rspIn
}