package caravan.bus.tilelink

import chisel3._ 
import chisel3.util._

class TilelinkAdapter(implicit val config:TilelinkConfig) extends Module {
    val io = IO(new Bundle{

        /*  MASTER SIDE  */
        val reqIn =  Flipped(Decoupled(new TLRequest))
        val rspOut = Decoupled(new TLResponse)

        /*  SLAVE SIDE */
        val reqOut = Decoupled(new TLRequest)
        val rspIn = Flipped(Decoupled(new TLResponse))
    })

    val tlHost = Module(new TilelinkHost)
    val tlSlave = Module(new TilelinkDevice)

    /*  Connecting Master Interconnects  */
    tlHost.io.tlMasterTransmitter <> tlSlave.io.tlMasterReceiver

    /*  Connecting Slave Interconnects  */
    tlSlave.io.tlSlaveTransmitter <> tlHost.io.tlSlaveReceiver

    /*  Sending Request in Master  */
    tlHost.io.reqIn <> io.reqIn

    /*  Sending Response out from Master  */
    io.rspOut <> tlHost.io.rspOut

    /*  Sending Request out from Slave  */
    io.reqOut <> tlSlave.io.reqOut

    /*  Sending Response in Slave  */
    tlSlave.io.rspIn <> io.rspIn
}