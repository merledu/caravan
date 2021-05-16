How to add another Bus Protocol?
================================

The Wisbone protocol implementation example gives a brief overview regarding what is needed to be done for adding a
new protocol support in Caravan.

It will be further discussed in the following guide.

.. note::

    We are using *MyBus* as the protocol name in this guide for the sake of explanation. This name can be replaced by APB,
    AHB, AXI, TileLink and any other protocol the developer wants to add support for in the Caravel.

Creating a Configuration Class
------------------------------

First of all, a configuration case class is needed that provides certain parameters to be used by the bus implementation
logic, such as address width, data width, and anything other that is protocol specific which extends the ``BusConfig`` trait.

.. code-block:: scala

    case class MyBusConfig
    (
        // protocol specific requirements
    ) extends BusConfig

Implementing the Request/Response Bundle
----------------------------------------

Secondly, the *abstract* ``AbstrRequest`` and ``AbstrResponse`` needs to be concretely implemented to provide the
bus adapter with an interface to communicate with the user's IP.

.. code-block:: scala

    class MyRequest(implicit val config: MyBusConfig) extends AbstrRequest {
        override val addrRequest: UInt = UInt(config.addressWidth.W)
        override val dataRequest: UInt = UInt(config.dataWidth.W)
        override val activeByteLane: UInt = UInt((config.dataWidth/config.granularity).W)
        override val isWrite: Bool = Bool()
    }

    class MyResponse(implicit val config: MyBusConfig) extends AbstrResponse {
        override val dataResponse: UInt = UInt(config.dataWidth.W)
        override val error: Bool = Bool()
    }


Creating the Master/Slave Bundles
---------------------------------

Thirdly, the master/slave bundles are needed to be created which will then be used by the host and slave adapters to
communicate with each other using the bus protocol.

For this purpose, first it is required to extend the ``BusHost`` and ``BusDevice`` classes.

.. code-block:: scala

    case class MyBusHost() extends BusHost
    case class MyBusDevice() extends BusDevice

then create the master/slave bundles which extend from these classes, like the following:

.. code-block:: scala

    class MyBusMaster(implicit val config: MyBusConfig) extends MyBusHost {
        // protocol specific signals
    }

    class MyBusSlave(implicit val config: MyBusConfig) extends MyBusDevice {
        // protocol specific signals
    }


Creating the Host/Device Adapters
---------------------------------

Finally, now it is the time to make a *Host Adapter* or a *Master* of the bus, which usually initiates the bus
transaction and a *Device Adapter* or *Slave*, which responds to the requests.

.. note::

    It is important to extend these adapters from ``HostAdapter/DeviceAdapter`` rather than a ``Module``.
    So, in our case ``MyBusHost`` must extend ``HostAdapter`` and ``MyBusDevice`` must extend ``DeviceAdapter``.

The logic of these adapters solely depend on the developer and the bus protocol they are trying to implement. Though
the interface of the adapters should be somewhat similar as the following:

.. code-block:: scala

    class MyBusHost(implicit val config: MyBusConfig) extends HostAdapter {
        val io = IO(new Bundle {
            val myBusMasterTransmitter = Decoupled(new MyBusMaster())
            val myBusSlaveReceiver  = Flipped(Decoupled(new MyBusSlave()))
            val reqIn = Flipped(Decoupled(new MyRequest()))
            val rspOut = Decoupled(new MyResponse())
        })

        // protocol specific implementation
    }

    class MyBusDevice(implicit val config: MyBusConfig) extends DeviceAdapter {
        val io = IO(new Bundle {
            val myBusSlaveTransmitter = Decoupled(new MyBusSlave())
            val myBusMasterReceiver = Flipped(Decoupled(new MyBusMaster()))
            val reqOut = Decoupled(new MyRequest())
            val rspIn = Flipped(Decoupled(new MyResponse()))
        })

        // protocol specific implementation
    }

Finally, the Harness
--------------------

The harness is created to test the adapters connected with each other, in order to verify the correct functionality of
the protocol conformance.













