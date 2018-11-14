What is this
============
This is a sample project showing how one can build a JVM based SGX
enclave. This enclave receives requests for random numbers and generates
the requested amount, signing the numbers with a key that can be traced
back to an attestation quote.

How do I run it
===============

Requirements
------------
* GNU/Linux
* binutils or equivalent, specifically ld and objcopy
* openssl for the dummy MRSIGNER key generation

To build
--------
```bash
./gradlew buildEnclaveSimulation
```

The above will build an enclave linked against **simulation libraries**.
This means this enclave won't be loaded as a proper SGX enclave, but
will use mocked behaviour instead. This is useful for
development/debugging.

To build a properly linked enclave use:
```
./gradlew buildEnclaveDebug # Build enclave with debug symbols.
./gradlew buildEnclaveRelease # Build an optimized enclave with stripped symbols.
```

The above enclaves can be loaded onto an SGX device, however loading
these requires further setup.

TODO document sgx device/aesmd.

TODO explain debug enclave mode, requirement on MRSIGNER for release
enclave loading.

To test
-------

```bash
./gradlew test
```

The above will run a test against the enclave, requesting some random
numbers and checking the signature.

See `src/test/kotlin/com/r3/sgx/enclave/rng/RngEnclaveTest.kt` for the
test.

Note that by default the test will use the simulation enclave. If you
have the proper setup you can change the `sgxTest` part of `build.gradle`:

```
sgxTest {
    type = "Debug" // or "Release"
}
```

Note that in this case you may also want to extend the test to do a
roundtrip to IAS to verify the correctness of the quote. This will
require a whitelisted TLS client key, as explained here: https://software.intel.com/en-us/articles/certificate-requirements-for-intel-attestation-services.