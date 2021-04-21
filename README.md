Caravan
=======================

[![Join the chat at https://gitter.im/merl-caravan/community](https://badges.gitter.im/merl-caravan/community.svg)](https://gitter.im/merl-caravan/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Documentation Status](https://readthedocs.org/projects/caravan/badge/?version=latest)](https://caravan.readthedocs.io/en/latest/?badge=latest)
![Apache License](https://img.shields.io/github/license/merledu/caravan?style=plastic)
![GitHub contributors](https://img.shields.io/github/contributors/merledu/caravan?style=plastic)

Caravan intends to be equipped with a fully fledged API for easily creating open source bus protocols in Chisel based designs

## Motivation
There are fairly limited resources available for the Chisel developers to use pre-built opensource bus protocols. Even if they are present, they are tightly integrated inside a complex project or outdated or not properly managed in a single place. This project aims to be a one-stop platform for all Chisel based bus protocols that are supported to work on the FPGAs as well as on the ASIC flow.

## Getting Started
It is highy recommended to follow the docs for installation guide and getting started. The documentation is up and running here:
[caravan.readthedocs.io](https://caravan.readthedocs.io/en/latest/index.html)


## Current Status
A standard Wishbone Classic READ/WRITE is supported in Point-Point interconnection right now. More verification is needed to check the correct functionality of the IP according to the [Wisbone B4 Specification](https://cdn.opencores.org/downloads/wbspec_b4.pdf).

As a quick start, you can use the wishbone host and slave IP in the following manner inside any other parent `Module` with one restriction that the parent module must be passed an implicit `WishboneConfig` which paramterizes the host as well as the slave:

```scala
class ParentModule(implicit val config: WishboneConfig) extends Module {
  val io = IO(new Bundle{})
  val wbHost = Module(new WishboneHost())
  val wbSlave = Module(new WishboneDevice())
  // Single Point-to-Point interconnect
  wbHost.io.wbMasterTransmitter <> wbSlave.io.wbMasterReceiver
  wbSlave.io.wbSlaveTransmitter <> wbHost.io.wbSlaveReceiver
}
```

Here is the waveform view of writing a data to a synchronous memory and then reading from the same address:

![wb_read_write](https://github.com/merledu/caravan/blob/main/docs/wb_timing.png)

To reproduce the above waveform in a VCD format follow the [caravan developer's guide](https://caravan.readthedocs.io/en/latest/dev/index.html)

## ToDo

1. Test the standard READ/WRITE protocol from B4 Wishbone specification.
2. Create a 1:N switch to enable multiple peripherals to be connected with the bus.


## Get Involved

We would love to have you join the community and ask/engage/solve problems related to the project here: https://gitter.im/merl-caravan/community

## Contribution
This platform can only be built by your efforts. Open issues and send PRs so that they can be merged and make Caravan awesome! 
