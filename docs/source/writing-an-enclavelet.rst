Running an Enclavelet
#####################

In this tutorial we will:

* Learn how enclavelets work, including how they use handler trees to abstract communication.
* Run the *random number generator* enclavelet. This is a hello world style application for SGX.

Writing an enclavelet involves the following steps:

1. Creating a build environment with Gradle.
2. Writing a subclass of the `Enclavelet` class.
3. Creating a handler class that handles inbound messages and lets you send outbound messages.
4. Setting up report data and using it in remote attestation.
5. Writing unit tests.
6. Implementing a client app or library that users will utilise to work with the enclave.

The client app will connect to the host server, perform the remote attestation protocol, compare the measurement
against a set of known-good measurements, then encrypt and provision secrets.

.. toctree::

   sgx-setup
   rng-enclavelet
   rng-host
   rng-client
   sgx-gradle-plugins
   handler-trees
