.. |rng-client-jar| replace:: samples/rng/rng-client/build/libs/rng-client.jar
.. |rng-enclave-debug-metadata| replace:: samples/rng/rng-enclave/build/enclave/Debug/enclave.metadata.yml

.. _rng-client:

RNG Client
##########

.. contents::

The project contains a simple CLI tool that can connect to an :ref:`rng-host`,
retrieve and verify attestation data, and request a securely-generated 1Kb random byte array.

Build the client
----------------

.. parsed-literal::

    ./gradlew samples:rng:rng-client:shadowJar

Usage
-----

To see the full usage:

.. parsed-literal::

    java -jar |rng-client-jar| --help

Attestations
~~~~~~~~~~~~

Outside of simulation mode, we can ask the enclave to produce an attestation.

Retrieving attestations
^^^^^^^^^^^^^^^^^^^^^^^

To retrieve the attestation data:

.. parsed-literal::

    java -jar |rng-client-jar| get-attestation sgxjvm.r3.com:8001 > attestation

The above will connect to a host running on ``sgxjvm.r3.com:8001``, retrieve and pipe the attestation data into a file
``attestation``.

Viewing attestations
^^^^^^^^^^^^^^^^^^^^

To view the attestation data:

.. parsed-literal::

    java -jar |rng-client-jar| print-attestation < attestation

The above will read the attestation data from the ``attestation`` file and print a (mostly) human-readable
representation of it.

Verifying attestations
^^^^^^^^^^^^^^^^^^^^^^

To verify the attestation data we need to tell the client what enclave measurement to trust. For this we can use the
metadata file generated while building the enclave itself.

.. parsed-literal::

    ./gradlew samples:rng:rng-enclave:buildSignedEnclaveDebug
    # Or
    ./gradlew samples:rng:rng-enclave:buildSignedEnclaveRelease

The above will build a debug/release RNG enclave and generate a metadata file next to it. For example for the Debug
enclave this will be |rng-enclave-debug-metadata|.

.. warning:: In debug mode, each machine will generate a unique measurement for a given enclave, and you must therefore
   make sure that the host you're connecting to loads this specific enclave.

To then do the verification:

.. parsed-literal::

    java -jar |rng-client-jar| verify-attestation -e |rng-enclave-debug-metadata| < attestation

No output indicates that the verification was successful.

Note that the above will *fail* if the enclave is loaded in DEBUG mode. To temporarily accept DEBUG quotes for testing
use:

.. parsed-literal::

    java -jar |rng-client-jar| verify-attestation -e |rng-enclave-debug-metadata| --accept-debug < attestation

Similarly there are some non-critical IAS responses that you may want to
accept. See the command-specific ``--help`` for more options.

Computations
~~~~~~~~~~~~

Finally, to retrieve a securely-generated 1Kb random byte array:

.. parsed-literal::

    java -jar |rng-client-jar| get-random sgxjvm.r3.com:8001 -e |rng-enclave-debug-metadata|

As before, the above will *fail* if the enclave is loaded in DEBUG mode. To temporarily accept DEBUG quotes for testing
use:

.. parsed-literal::

    java -jar |rng-client-jar| get-random sgxjvm.r3.com:8001 -e |rng-enclave-debug-metadata| --accept-debug

``get-random`` accepts similar flags to ``verify-attestation``, as it does full verification of the quote. In addition
it checks that the signature over the returned bytes is done by the key in the quote. If successful, it will print some
base64 encoded randomness it received from the enclave.
