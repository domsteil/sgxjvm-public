.. _rng-host:

RNG Host
########

.. contents::

Oblivium provides a generic host process that can load any enclavelet. Hosting of an enclave
requires a working :ref:`sgx-setup`.

Run the host.jar directly
-------------------------

From the top level of the `oblivium-public repository <https://github.com/corda/oblivium-public/>`_, download the
latest enclavelet host artifact:

.. parsed-literal::

    wget '|OBLIVIUM_MAVEN_URL|/|OBLIVIUM_MAVEN_REPOSITORY|/com/r3/sgx/enclavelet-host-server/|OBLIVIUM_VERSION|/enclavelet-host-server-|OBLIVIUM_VERSION|-all.jar' -O host.jar

To run the enclave in debug mode, run:

.. parsed-literal::

    java -jar host.jar -m DEBUG samples/rng/rng-enclave/build/enclave/Debug/enclave.signed.so

The above will hopefully load the enclave and bind port 8080, where the host will be accepting incoming gRPC
connections to forward to the enclave. ``-m DEBUG`` will cause the host to use native libraries with debug
symbols, and to load the enclave in DEBUG mode.

To run the enclave in simulation mode, run:

.. parsed-literal::

    java -jar host.jar -m SIMULATION samples/rng/rng-enclave/build/enclave/Simulation/enclave.signed.so

.. note:: In simulation mode the enclave quote is a fake one, so we there will be no roundtrip to the IAS.

For more command line options run:

.. parsed-literal::

    java -jar host.jar --help

There's also a configuration file you can provide that allows setting finer-grained options, as well as switch to a
different SPID:

.. literalinclude:: ../../oblivium/enclavelet-host/enclavelet-host-server/src/main/resources/default-host-settings.yml
    :language: yaml

Create/run a docker image
-------------------------

The ``com.r3.sgx.host`` plugin provides a way to build and publish Docker
images embedding a pre-configured host and enclave. The rng-enclave Gradle build is configured to take
docker-specific parameters from the environment, see ``samples/rng/rng-enclave/build.gradle``
for details.

To build an image:

.. parsed-literal::

    ./gradlew samples:rng:rng-enclave:buildEnclaveImageDebug

The above will build the image

.. parsed-literal::

    localhost:5000/com.r3.sgx/rng-enclave-debug:|OBLIVIUM_VERSION|

Note that the registry need not be running as we're merely building the image at this stage, not pushing.

To run the image create a configuration file, say ``host-config-debug.yml``, this may be empty if you're happy with the
defaults, however you will probably want to set at least ``enclaveLoadMode: DEBUG`` unless you're using a production
signed enclave. Then run:

.. parsed-literal::

    docker run --rm -it -p 8080:8080 -v /run/aesmd:/run/aesmd -v $PWD/host-config-debug.yml:/app/config/config.yml --device=/dev/isgx localhost:5000/com.r3.sgx/rng-enclave-debug:|OBLIVIUM_VERSION|

The above will run the container with the host and enclave, mount in the config file, expose the SGX device and the
aesmd socket to the container, and expose the bound gRPC port on 8080.

Interact with the host
----------------------

To interact with the host you can connect to it through gRPC. The interface:

.. literalinclude:: ../../oblivium/enclavelet-host/enclavelet-host-common/src/main/proto/enclavelet-host.proto
    :language: proto

There is also a sample :ref:`rng-client` showcasing how to interact with the host.
