JVM-in-SGX
##########

.. raw:: html

   <iframe width="700" height="393" src="https://www.youtube.com/embed/CClZqVp0kTA" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe><br><br>

JVM-in-SGX is an advanced platform for the development of *oblivious software*, or software whose processing cannot be observed due to the
use of cryptography. JVM-in-SGX lets you write *enclavelets*, small pieces of software which run inside Intel SGX protected memory spaces
and which can be audited over the internet using *remote attestation*.

Features
--------

* An embedded Java Virtual Machine, capable of running the full Java 8 platform inside an enclave with encrypted RAM. You can eliminate all memory
  management errors that would undermine the security of your enclave, thanks to the built-in generational garbage collector.
* Write enclaves in Java, Kotlin, Scala, Haskell (via Eta) or any other language that can target bytecode.
* Full support for auditing enclaves over the internet, including remote attestation and fully deterministic, reproducible builds.
  A user can verify what the source code of the remotely running enclave is, to ensure it will behave as they expect.
* An easy and powerful *handler tree* abstraction that makes it easy to compose encryption, authentication and data serialisation frameworks
  together, so you can focus on your application logic.
* A set of Gradle build plugins, so compiling and signing your enclave is handled automatically.
* A powerful unit testing framework to verify the operation of your enclave and remote attestation functionality, using just JUnit.
* Integration with protocol buffers.
* A generic host infrastructure capable of acting as the untrusted world side for any enclave.
* API designs that guide you towards SGX best practices and avoidance of security pitfalls.
* Many tutorials, guides and commercial support from the SGX experts at R3.

Roadmap
-------

Future versions of the platform will offer:

* High availability support with joint sealing and cross-enclave failover.
* Automatic side channel elimination via robust cryptographic abstractions like ORAM, randomised movement of heap
  data, padding of messages etc.
* Integration with the Corda platform to provide simple, authenticated inter-business oblivious workflows and encryption
  of the global ledger.
* Ability to sandbox internal components of the enclave using JVM type sandboxing, to reduce the work involved in
  auditing changes to enclave source code.

In the coming years the framework will evolve to support use of pure cryptographic techniques like zero knowledge proofs
and multi-party computation, when the nature of your enclavelet and your performance requirements allow for it.

.. important:: JVM-in-SGX does not currently provide a stable API. The enclavelet API may change between releases without notice.

This website will teach you how SGX works and how to write enclavelets.

.. toctree::
   :maxdepth: 2
   :caption: Tutorials

   what-is-sgx
   writing-an-enclavelet

.. toctree::
   :maxdepth: 2
   :caption: Development

   troubleshooting