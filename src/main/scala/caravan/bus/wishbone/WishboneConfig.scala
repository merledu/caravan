package caravan.bus.wishbone

case class WishboneConfig
(
  val addressWidth: Int,
  val dataWidth: Int,
  val granularity: Int = 8
)

