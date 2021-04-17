package caravan.bus.common
import caravan.bus.wishbone.Peripherals
import caravan.bus.wishbone.Peripherals.Peripheral

import scala.collection._
import chisel3._

/** creating a type inside object so that it can be easily imported
 * BusMap type provides a short way of mentioning mutable.Map[Peripheral, (UInt, UInt)] everywhere */
object BusMap {
  type BusMap = mutable.Map[Peripheral, (UInt, UInt)]
}

import BusMap._
/** This class provides the user an API to define peripherals and their address mapping */
class AddressMap {
  private val map: BusMap = mutable.Map[Peripheral, (UInt, UInt)]()
  /** addDevice provides the user to add each device to the address map */
  /** FIXME: there is no restriction on adding two peripherals with same base address.
   * logically this should never happen, however user can add two peripherals with same base addresses
   * there is no check for this and would break the code in later steps when we decode the addr of a peripheral */
  def addDevice(peripheral: Peripheral, baseAddr: UInt, addrMask: UInt): Unit = map += (peripheral -> (baseAddr, addrMask))
  /** an helper function that returns the map [Encapsulation] */
  def getMap(): BusMap = map
}

/** BusDecoder provides an helpful utility to decode the address and send a device sel to the bus switch
 *  with which different peripherals are connected */
object BusDecoder {
  /** decode takes the addr from the host and an address map and figures out which peripheral's id should be sent as dev sel to the switch */
  def decode(addr: UInt, addressMap: AddressMap): UInt = {
    val matchingPeripheralList = addressMap.getMap().filter(d => filterAddr(d, addr)).toList
    /** assuming that AddressMap always has different addresses of peripherals.
     * so the filter would always return a Map with one matching peripheral that is converted into List
     * or an empty Map if the address did not match any peripheral and converted into an empty List
     * so we are checking if the List is empty, then send a dev sel equal to the size of map which would always be
     * one more than the peripherals in the map. This way the switch can see there is an error and return an error response
     * else it extracts the first (only) peripheral and finds its id and sends it to the switch */
    if (matchingPeripheralList.isEmpty)
      addressMap.getMap().size.asUInt
    else
      matchingPeripheralList.head._1.id.asUInt
  }
  /** helper functions used by the decode function above
   * selects the peripheral based on the address received */
  def filterAddr(map: (Peripheral, (UInt, UInt)), addr: UInt): Boolean = ((addr & map._2._2) === map._2._1).asInstanceOf[Boolean]
}
