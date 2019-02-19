.. |rng-client-jar| replace:: samples/rng/rng-client/build/libs/rng-client.jar

.. _rng-client:

RNG Client
##########

The project contains a simple CLI tool that can connect to an :ref:`rng-host`,
retrieve and verify attestation data, and request some RNG.

Build the client
----------------

.. parsed-literal::

    ./gradlew samples:rng:rng-client:shadowJar

Usage
-----

To see the full usage:

.. parsed-literal::

    java -jar |rng-client-jar| --help

Outside of simulation mode, we can ask the enclave to produce an attestation.

To retrieve the attestation data:

.. parsed-literal::

    java -jar |rng-client-jar| get-attestation localhost:8080 > attestation

The above will connect to a host running on ``localhost:8080``, retrieve and pipe the attestation data into a file
``attestation``.

To view the attestation data:

.. parsed-literal::

    java -jar |rng-client-jar| print-attestation < attestation

The above will read the attestation data from the ``attestation`` file and print a (mostly) human-readable
representation of it.

To verify the attestation data:

.. parsed-literal::

    java -jar |rng-client-jar| verify-attestation < attestation

Note that the above will *fail* if the enclave is loaded in DEBUG mode. To temporarily accept DEBUG quotes for testing
use:

.. parsed-literal::

    java -jar |rng-client-jar| verify-attestation --accept-debug < attestation

Similarly there are some non-critical IAS responses that you may want to
accept. See the command-specific ``--help`` for more options.

To retrieve a verified and signed RNG:

.. parsed-literal::

    java -jar |rng-client-jar| get-random localhost:8080

``get-random`` accepts similar flags to ``verify-attestation``, as it does full verification of the quote. If
successful, it will print some base64 encoded randomness it received from the enclave.
