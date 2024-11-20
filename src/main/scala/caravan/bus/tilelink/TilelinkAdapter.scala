package caravan.bus.tilelink

import chisel3._ 
import chisel3.util._

import caravan.bus.common._

class TilelinkAdapterIO(implicit val config:TilelinkConfig) extends BusAdapterIO{

    /*  MASTER SIDE  */
    val reqIn =  Flipped(Decoupled(new TLRequest))
    val rspOut = Decoupled(new TLResponse)

    /*  SLAVE SIDE */
    val reqOut = Decoupled(new TLRequest)
    val rspIn = Flipped(Decoupled(new TLResponse))

}

class TilelinkAdapter(implicit val config:TilelinkConfig) extends BusAdapter {
    val io = IO(new TilelinkAdapterIO)

    val tlHost = Module(new TilelinkHost)
    val tlSlave = Module(new TilelinkDevice)

    /*  Connecting Master Interconnects  */
    tlHost.io.masterTransmitter <> tlSlave.io.masterReceiver

    /*  Connecting Slave Interconnects  */
    tlSlave.io.slaveTransmitter <> tlHost.io.slaveReceiver

    /*  Sending Request in Master  */
    tlHost.io.reqIn <> io.reqIn

    /*  Sending Response out from Master  */
    io.rspOut <> tlHost.io.rspOut

    /*  Sending Request out from Slave  */
    io.reqOut <> tlSlave.io.reqOut

    /*  Sending Response in Slave  */
    tlSlave.io.rspIn <> io.rspIn
}