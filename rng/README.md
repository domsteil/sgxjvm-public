What is this
============
This is a sample project showing how one can build a JVM based SGX
enclave and a host that can load it. Functionality-wise the enclave
receives requests for random numbers and generates the requested amount,
signing the numbers with a key that can be traced back to an attestation
quote.

How do I run the enclave
========================

Requirements
------------
* GNU/Linux
* binutils or equivalent, specifically ld and objcopy
* openssl for the dummy MRSIGNER key generation
* For non-Simulation: an SGX-capable CPU

To build the enclave
--------------------
```bash
./gradlew rng:rng-enclave:buildSignedEnclaveSimulation
```

The above will build an enclave linked against **simulation libraries**.
This means this enclave won't be loaded as a proper SGX enclave, but
will use mocked behaviour instead. This is useful for
development/debugging or playing around with the tech without access to
SGX.

To build a properly linked enclave use:
```
./gradlew rng:rng-enclave:buildSignedEnclaveDebug # Build enclave with debug symbols.
./gradlew rng:rng-enclave:buildSignedEnclaveRelease # Build an optimized enclave with stripped symbols.
```

The above enclaves can be loaded onto an SGX device, however loading
these requires further setup. See the later *SGX setup* section.

To test the enclave
-------------------

```bash
./gradlew rng:rng-enclave:test
```

The above will sign the enclave with a dummy MRSIGNER key and run a test
against it. The test will load the enclave in DEBUG mode, request some
random numbers, and check the enclave signature.

See `src/test/kotlin/com/r3/sgx/enclave/rng/RngEnclaveTest.kt` for the
test.

Note that by default the test will use a simulation enclave. If you
have the proper setup you can change the test to use a Debug/Release
enclave by changing the test configuration in `enclave/build.gradle`:

```
test {
    // dependsOn buildSignedEnclaveSimulation
    // systemProperty("com.r3.sgx.enclave.path", buildSignedEnclaveSimulation.signedEnclavePath())
    dependsOn buildSignedEnclaveRelease
    systemProperty("com.r3.sgx.enclave.path", buildSignedEnclaveRelease.signedEnclavePath())
}
```

Furthermore you'll need to change the test's host libraries as well:
```
dependencies {
    (...)
    // testRuntimeOnly "com.r3.sgx:native-host-simulation:$oblivium_version"
    testRuntimeOnly "com.r3.sgx:native-host-release:$oblivium_version"
}

```

Note that in this case you may also want to extend the test to do a
roundtrip to IAS to verify the correctness of the quote. This will
require a whitelisted TLS client key, as explained here: https://software.intel.com/en-us/articles/certificate-requirements-for-intel-attestation-services.

How do I run the host
=====================

SGX setup
---------

In order to run the host we need a working SGX setup. For this:

1. You need an SGX-capable CPU. See
    https://github.com/ayeks/SGX-hardware for a non-comprehensive list
    of models. This repo also includes an executable that may be used to
    check your machine.
2. You need to enable SGX in the BIOS. How to do this depends on your
    particular setup.
3. The SGX driver must be installed. This is a driver that implements
    low-level SGX functionality like paging.
    ```bash
    curl --output /tmp/driver.bin https://download.01.org/intel-sgx/linux-2.3.1/ubuntu18.04/sgx_linux_x64_driver_4d69b9c.bin
    /usr/bin/env bash /tmp/driver.bin
    depmod
    modprobe isgx
    ```
    To see whether this was successful check `dmesg` logs. If you see
    ```
    [36300.017753] intel_sgx: SGX is not enabled
    ```
    It means SGX is not available/enabled. Double check your BIOS setup.
    If you see
    ```
    [4976352.319148] intel_sgx: Intel SGX Driver v0.11
    [4976352.319171] intel_sgx INT0E0C:00: EPC bank 0xb0200000-0xb5f80000
    [4976352.321180] intel_sgx: second initialization call skipped
    ```
    It means the driver loaded successfully! You may also see that
    loading this driver taints the kernel, this is normal.
4. The aesmd daemon must be running. This is an intel-provided daemon
    interfacing with Intel's enclaves. In particular the Launching
    Enclave which checks the MRSIGNER whitelist (also handled by aesmd)
    before generating a token required for loading other enclaves, and
    the PCE/PvE/QE trio, which are used during EPID provisioning and
    attestation.

    ```bash
    docker run --rm -it -v /run/aesmd:/run/aesmd --device /dev/isgx oblivium-docker-release.corda.r3cev.com/oblivium/aesmd:latest
    ```
    The above starts aesmd in a container with access to the SGX device,
    and exposes the UNIX domain socket our host will use to communicate
    with aesmd.

    You should see something like this:
    ```
    aesm_service[8]: [ADMIN]White List update requested
    aesm_service[8]: The server sock is 0x55e9e3464960
    aesm_service[8]: [ADMIN]Platform Services initializing
    aesm_service[8]: [ADMIN]Platform Services initialization failed due to DAL error
    aesm_service[8]: [ADMIN]White list update request successful for Version: 43
    ```

    The DAL error is irrelevant, it's related to SGX functionality that
    is generally not safe to enable as it relies on the ME.

Configuration
-------------

To start the host you will need to provide a configuration file. In the
following we'll demonstrate how to load a Debug-build enclave in DEBUG
mode. Replace Debug with Simulation or Release as needed.

(
Note: "Debug" is quite overloaded in this context. In general native
artifacts (including host artifacts) have three build types, Simulation,
Debug and Release.

- Simulation: no SGX hardware required, SGX-specific behaviour is mocked.
- Debug: SGX hardware is used, and the native artifacts include debug symbols.
- Release: SGX hardware is used, and the native artifacts are optimized and stripped.

In addition to this the enclave may be loaded in DEBUG or non-DEBUG mode.
- DEBUG: a debugger may be attached to the enclave. This mode is
  reflected in the enclave quote and should be checked. It is *not* safe
  to trust a DEBUG-loaded enclave!
- non-DEBUG: full SGX guarantees. The enclave cannot be debugged. An
  enclave may only be loaded in this mode if it's signed with a
  whitelisted production key.
)

```bash
cat > host-config-debug.yml
bindPort: 8080                                                       # The gRPC port to bind
attestationServiceUrl: https://test-as.sgx.trustedservices.intel.com # The Intel Attestation Service endpoint to use
threadPoolSize: 8                                                    # The size of the connection handling threadpool
epidSpid: "84D402C36BA9EF9B0A86EF1A9CC8CE4F"                         # The EPID SPID to use when quoting
epidQuoteType: LINKABLE                                              # What type of EPID quote we want (LINKABLE or UNLINKABLE)
enclaveIsDebug: true                                                 # Whether to load the enclave in DEBUG mode
```

Note that the last setting may only be set to true when the enclave
loaded is signed with a whitelisted MRSIGNER key. Note also that even
a Release enclave build may be loaded in DEBUG mode.

To run the host.jar directly
----------------------------

Download the latest host.jar artifact from https://ci-artifactory.corda.r3cev.com/artifactory/webapp/#/artifacts/browse/tree/General/oblivium/com/r3/sgx/enclavelet-host-server.

For example:
```bash
wget 'http://ci-artifactory.corda.r3cev.com/artifactory/oblivium-maven-nightly/com/r3/sgx/enclavelet-host-server/1.0-nightly-71-g877ad11538/enclavelet-host-server-1.0-nightly-71-g877ad11538-all.jar' -O host.jar
```

Then to run:
```
java -Doblivium.build=Debug -jar host.jar enclave/build/enclave/Debug/enclave.signed.so --config host-config-debug.yml
```

The above will hopefully load the enclave and bind port 8080, where
the host will be accepting incoming enclave connections.

To run the docker image
-----------------------

TODO rewrite this once the image management has settled

The `com.r3.sgx.host` plugin provides a way to build and publish Docker
images embedding the enclave. The build is configured to take
docker-specific parameters from the environment, see `enclave/build.gradle`
for details.

To build an image:
```bash
REGISTRY_URL=localhost:5000 ./gradlew rng:rng-enclave:buildEnclaveImageDebug
```

The above will build an image targeting a local docker registry. Note
that the registry need not be running as we're merely building the image
at this stage.

To run the image:
```bash
docker run --rm -it -p 8080:8080 -v /run/aesmd:/run/aesmd -v $PWD/host-config-debug.yml:/app/config/config.yml --device=/dev/isgx localhost:5000/oblivium/oblivium-rng-enclave-server
```

The above will run the container, mount in the enclave, the config file,
expose the isgx device and the aesmd socket to the container, and
expose the bound gRPC port on 8080.

How do I interact with the host
===============================

The project contains a simple CLI tool that can connect to the host,
retrieve and verify attestation data, and request some RNG.

To build the CLI tool
---------------------

```bash
./gradlew rng:rng-client:shadowJar
```

To use the CLI tool
-------------------

To retrieve attestation data:

```bash
java -jar client/build/libs/rng-client.jar get-attestation localhost:8080 > attestation
```

The above will pipe the attestation data into a file `attestation`.

To view attestation data:
```bash
java -jar client/build/libs/rng-client.jar print-attestation < attestation
```

To verify attestation data:

```bash
java -jar client/build/libs/rng-client.jar verify-attestation < attestation
```

Note that the above will *fail* if the enclave is loaded in DEBUG mode.
To temporarily accept DEBUG quotes for testing:

```bash
java -jar client/build/libs/rng-client.jar verify-attestation --accept-debug < attestation
```

Similarly there are some non-critical IAS responses that you may want to
accept. See `--help`.

```bash
java -jar client/build/libs/rng-client.jar verify-attestation --help
```

To retrieve verified and signed RNG:

```bash
java -jar client/build/libs/rng-client.jar get-random localhost:8080
```

`get-random` accepts similar flags to `verify-attestation`. If
successful it will print some base64 encoded randomness.

#### Troubleshooting

If you get an error like the following:
```
Exception in thread "main" picocli.CommandLine$ExecutionException: Error while calling command (com.r3.sgx.enclavelethost.server.EnclaveletHostCLI@54a3ab8f): java.lang.RuntimeException: SGX_ERROR_NO_DEVICE: Can't open SGX device
	at picocli.CommandLine.execute(CommandLine.java:1016)
	at picocli.CommandLine.access$900(CommandLine.java:142)
	at picocli.CommandLine$RunLast.handle(CommandLine.java:1199)
	at picocli.CommandLine$RunLast.handle(CommandLine.java:1167)
	at picocli.CommandLine$AbstractParseResultHandler.handleParseResult(CommandLine.java:1075)
	at picocli.CommandLine.parseWithHandlers(CommandLine.java:1358)
	at picocli.CommandLine.call(CommandLine.java:1629)
	at picocli.CommandLine.call(CommandLine.java:1553)
	at com.r3.sgx.enclavelethost.server.EnclaveletHostCLI$Companion.main(EnclaveletHostCLI.kt:19)
	at com.r3.sgx.enclavelethost.server.EnclaveletHostCLI.main(EnclaveletHostCLI.kt)
Caused by: java.lang.RuntimeException: SGX_ERROR_NO_DEVICE: Can't open SGX device
	at com.r3.sgx.core.host.internal.Native.createEnclave(Native Method)
	at com.r3.sgx.core.host.NativeHostApi.createEnclave(NativeHostApi.kt:21)
	at com.r3.sgx.enclavelethost.server.internal.EnclaveletState$Companion.load(EnclaveletState.kt:69)
	at com.r3.sgx.enclavelethost.server.EnclaveletHostBuilder.build(EnclaveletHostBuilder.kt:39)
	at com.r3.sgx.enclavelethost.server.EnclaveletHostCLI.call(EnclaveletHostCLI.kt:46)
	at com.r3.sgx.enclavelethost.server.EnclaveletHostCLI.call(EnclaveletHostCLI.kt:13)
	at picocli.CommandLine.execute(CommandLine.java:1009)
	... 9 more
```

It indicates that your SGX setup isn't fully working. Double check that
the sgx driver is loaded:

```bash
lsmod | grep isgx
```
That aesmd is running
```bash
ps aux | grep aesm_service
```
And that the aesmd socket is exposed properly
```bash
ls -al /run/aesmd/aesm.socket
```
