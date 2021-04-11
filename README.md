Caravan
=======================
![Apache License](https://img.shields.io/github/license/merledu/caravan?style=plastic)
![GitHub contributors](https://img.shields.io/github/contributors/merledu/caravan?style=plastic)
![GitHub issues](https://img.shields.io/github/issues/merledu/caravan?color=green&label=issues&style=plastic)
![GitHub contribution](https://img.shields.io/badge/contribution-open%20for%20everyone-informational)

Caravan intends to be equipped with a fully fledged API for easily creating open source bus protocols in Chisel based designs

## Motivation
There are fairly limited resources available for the Chisel developers to use pre-built opensource bus protocols. Even if they are present, they are tightly integrated inside a complex project or outdated or not properly managed in a single place. This project aims to be a one-stop platform for all Chisel based bus protocols that are supported to work on the FPGAs as well as on the ASIC flow.

## Prerequisite
#### JDK 8 or newer

We recommend LTS releases Java 8 and Java 11. You can install the JDK as recommended by your operating system, or use the prebuilt binaries from [AdoptOpenJDK](https://adoptopenjdk.net/).

#### SBT

SBT is the most common built tool in the Scala community. You can download it [here](https://www.scala-sbt.org/download.html).


## Getting Started
Here is how to get started and ensure everything is correctly set up:
```console
foo@bar:~$ git clone https://github.com/merledu/caravan.git
foo@bar:~$ cd caravan
foo@bar:~/caravan$ sbt
sbt:Caravan> test
```
If you get an output saying "All tests passed", Congratulations! you have setup the project correctly and can start working immediately.

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

![wb_read_write](https://github.com/merledu/caravan/blob/main/doc/wb_timing.png)

To reproduce the following waveform in a VCD format run the test in the following way:
```console
foo@bar:~/caravan$ sbt
sbt:Caravan> testOnly wishbone.HarnessTest -- -DwriteVcd=1
```

## ToDo

1. Test the standard READ/WRITE protocol from B4 Wishbone specification.
2. Allow creating Shared Bus Interconnection with multiple masters and multiple slaves.


## Problems? Questions?

Feel free to send questions or problems at hadirkhan10@gmail.com. (this will be changed in future to a focused Gitter channel)

## Contribution
This platform can only be built by your efforts. Open issues and send PRs so that they can be merged and make Caravan awesome! 
