package caravan.bus.tileink

import caravan.bus.common.BusConfig


case class TilelinkConfig
(

    /*
    
    w => Width of the data bus in bytes. 
    a => Width of each address field in bits.
    z => Width of each size field in bits.
    o => Number of bits needed to disambiguate per-link master sources.
    i => Number of bits needed to disambiguate per-link slave sinks.

    */

    val w: Int = 4,
    val a: Int = 32,
    val z: Int = 8,
    val o: Int = 32,
    val i: Int = 32,

) extends BusConfig

