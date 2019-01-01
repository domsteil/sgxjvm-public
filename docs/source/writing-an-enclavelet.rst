Writing an Enclavelet
#####################

Writing an enclavelet involves the following steps:

1. Creating a build environment with Gradle.
2. Writing a subclass of the `Enclavelet` class.
3. Creating a handler class that will handle inbound messages and let you send outbound messages.
4. Setting up report data and using it in remote attestation.
5. Writing unit tests.
6. Implementing the host server.
7. Implementing a client app or library that users will utilise to work with the enclave.

The client app will connect to the host server, perform the remote attestation protocol, compare the measurement
against a set of known-good measurements, then encrypt and provision secrets.

In this tutorial we will:

* Learn how enclavelets use handler trees to abstract communication.
* Study the source of the *random number generator* enclavelet. This is a hello world style application for SGX.

.. toctree::

   handler-trees
   rng-enclavelet
   rng-host