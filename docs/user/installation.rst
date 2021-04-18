Installing Caravan
==================

JDK 8 or newer
--------------

It is recommended to use Java 8 and Java 11 releases. You can install the JDK as recommended by your
operating system, or use the prebuilt binaries from `AdoptOpenJDK <https://adoptopenjdk.net/>`_

SBT
---

SBT is the most common built tool in the Scala community.
You can download it `here <https://www.scala-sbt.org/download.html>`_

Get the code
------------

The Caravan source code is maintained in a git repository hosted on GitHub.
To work on Caravan and improve it further, it is necessary to clone the git repository first.

.. code-block:: bash

   cd your/preferred/directory/
   git clone https://github.com/merledu/caravan
   cd caravan
   git checkout v0.1.0

Build the code and run tests
----------------------------

.. warning::

   You will have to change the file path for a program file before running the test. Unfortunately, it is not
   parameterized yet. The reason behind this step will be explained later.

Simply follow the steps below, details will be provided later:

- create a ``.txt`` file
- copy paste the following dump inside the file

.. note::

   the dump content can be any, however it is provided below for convenience.

.. code-block:: bash

   00100113
   00200193
   00310233

- save the ``.txt`` file and exit
- then open the file here: ``caravan/src/main/scala/caravan/wishbone/Harness.scala``
- on line 55 there is a function ``loadMemoryFromFile`` , pass the path to your text file here
- save the file and exit

Finally, now run the following commands:

.. code-block:: bash

   sbt "test"

Did you get the output:

.. code-block:: bash

   all tests passed


**YES, I got the correct output**

Congratulations! Caravan is built properly on your system.

**NO, It did not work for me**

Sorry for the inconvenience. Please join the `Caravan Community <https://gitter.im/merl-caravan/community>`_ channel and share your issues there.
