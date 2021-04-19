Project Hierarchy
=================

Caravan is made to provided maximum re-usability to it's users as well as to the developers/contributors
creating their own bus protocol implementations.

Design structure
----------------
The proposed folder structure is shown below:

::

    main
      ├── scala
      │   ├── caravan
      │   │   ├── bus
      │   │   │   ├── common
      │   │   │   │   └── *.scala
      │   │   │   ├── wishbone
      │   │   │   │   ├── Harness.scala
      │   │   │   │   └── *.scala
      │   │   │   ├── amba
      │   │   │   │   ├── apb
      │   │   │   │   │   ├── Harness.scala
      │   │   │   │   │   └── *.scala
      │   │   │   ├── tilelink
      │   │   │   │   ├── tlul
      │   │   │   │   │   ├── Harness.scala
      │   │   │   │   │   └── *.scala

Inside the ``main`` folder all the source code for the design is present.

The ``common`` folder contains all the ``.scala`` helper classes and code that can be re-used for other bus topologies.

Other folders are bus specific such as the ``wishbone`` folder. The common file among all the bus specific folders
is the ``Harness.scala``, this file is the top level design that takes the stimuli from the test-bench and provides
it to the protocol adapters to verify their correct functionality.

Test structure
--------------

The proposed folder structure is shown below:

::

    test
      ├── scala
      │   ├── wishbone
      │   │   ├── HarnessTest.scala
      │   │   └── *.scala
      │   ├── apb
      │   │   ├── HarnessTest.scala
      │   │   └── *.scala
      │   ├── tlul
      │   │   ├── HarnessTest.scala
      │   │   └── *.scala

The ``test`` folder contains the test-benches of the whole design.

There are protocol specific folders such as ``apb``, ``wishbone``, ``tlul``. The mandatory test-bench to have
is the ``HarnessTest.scala`` for providing stimuli to the ``Harness`` module in the design. Other than that,
the developer can write as many unit tests as possible for the verification of the IP.