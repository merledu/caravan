package caravan.bus.common
import caravan.bus.wishbone.{WBDevice, WBHost, WishboneConfig, WishboneMaster, WishboneSlave}
import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.{Decoupled, log2Ceil}

class Switch1toN[A <: BusHost, B <: BusDevice](mb: A, sb: B, N: Int) extends Module {
  val io = IO(new Bundle {
    val hostIn = Flipped(Decoupled(mb))
    val hostOut = Decoupled(sb)
    val devOut = Vec(N, Decoupled(mb))
    val devIn = Flipped(Vec(N, Decoupled(sb)))
    val devSel = Input(UInt(log2Ceil(N + 1).W))
  })

  /** FIXME: assuming the socket is always ready to accept data from the bus host */
  io.hostIn.ready := true.B
  /** FIXME: assuming the socket is always ready to accept data from all the devices */
  io.devIn.map(b => b.ready := true.B)


  for (i <- 0 until N) {
    when(io.devSel === i.asUInt()) {
      io.hostOut <> io.devIn(i)
    }.otherwise {
      io.hostOut.valid := false.B
      io.hostOut.bits := DontCare
    }
  }

  for (i <- 0 until N) {
    io.devOut(i) <> io.hostIn
    io.devOut(i).valid := io.hostIn.valid && (io.devSel === i.asUInt())
  }
}


object Switch1toNDriver extends App {
  implicit val config = WishboneConfig(addressWidth = 32, dataWidth = 32)
  println((new ChiselStage).emitVerilog(new Switch1toN[WBHost, WBDevice](new WishboneMaster(), new WishboneSlave(), 3)))
}
