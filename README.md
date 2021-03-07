Caravan
=======================
![Apache License](https://img.shields.io/github/license/merledu/caravan?style=plastic)
![GitHub contributors](https://img.shields.io/github/contributors/merledu/caravan?style=plastic)
![GitHub issues](https://img.shields.io/github/issues/merledu/caravan?color=green&label=issues&style=plastic)
![GitHub contribution](https://img.shields.io/badge/contribution-open%20for%20everyone-informational)

Caravan intends to be equipped with a fully fledged API for easily creating open source bus protocols in Chisel based designs

## Motivation
There are fairly limited resources available for the Chisel developers to use pre-built opensource bus protocols. Even if they are present, they are outdated or not properly managed in a single place. This project aims to build a one-stop platform for all Chisel based bus protocols that are supported to work on the FPGAs as well as on the ASIC flow.

### The pseudo code of bus implementation through Caravan
```scala
  val wbHost = WishboneHost(WishboneConfig(addrWidth = 32, dataWidth = 32))
  val wbDevice = Flipped(WishboneHost(WishboneConfig(addrWidth = 32, dataWidth = 32)))
  // Single Point-to-Point interconnect
  wbHost <> wbDevice
```

## ToDo

Currently, implement the Wishbone bus API and test the IP. More details in the projects tab.

### Dependencies

#### JDK 8 or newer

We recommend LTS releases Java 8 and Java 11. You can install the JDK as recommended by your operating system, or use the prebuilt binaries from [AdoptOpenJDK](https://adoptopenjdk.net/).

#### SBT

SBT is the most common built tool in the Scala community. You can download it [here](https://www.scala-sbt.org/download.html).


## Problems? Questions?

Feel free to send questions or problems at hadirkhan10@gmail.com. (this will be changed in future to a focused Gitter channel)

## Contribution
This platform can only be built by your efforts! The more the contributors the better the design and verification of the IPs.
