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
./gradlew enclave:buildSignedEnclaveSimulation
```

The above will build an enclave linked against **simulation libraries**.
This means this enclave won't be loaded as a proper SGX enclave, but
will use mocked behaviour instead. This is useful for
development/debugging or playing around with the tech without access to
SGX.

To build a properly linked enclave use:
```
./gradlew enclave:buildSignedEnclaveDebug # Build enclave with debug symbols.
./gradlew enclave:buildSignedEnclaveRelease # Build an optimized enclave with stripped symbols.
```

The above enclaves can be loaded onto an SGX device, however loading
these requires further setup. See the later *SGX setup* section.

To test the enclave
-------------------

```bash
./gradlew enclave:test
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

The `host` folder contains a sample enclave host project. This is a very
simple webserver that can be used to load the enclave, generate random
numbers and look at the chain of trust. It also includes further
explanation of the various components of the chain.

To build the host
-----------------

This host by default uses a proper **Release** enclave, however it loads
it in DEBUG mode, as you'll be able to tell when looking at the
generated quote. This is because in order to load the enclave in
non-DEBUG mode one needs to whitelist an enclave signing key (MRSIGNER)
with Intel, and sign the enclave with that key. This project generates
a dummy key for signing, which is therefore not whitelisted, so the
enclave cannot be loaded in non-DEBUG mode. For futher information see
https://software.intel.com/en-us/sgx/commercial-use-license-request

```bash
./gradlew host:backend:shadowJar
```

The above will build an enclave host bundled with a release build of the
enclave in `host/backend/build/libs/rng-host-all.jar`.

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

    TODO replace with link to published image.
    ```bash
    docker run --rm -it -v /run/aesmd:/run/aesmd --device /dev/isgx localhost:5000/oblivium/aesmd:latest
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

To run the host
---------------

Once SGX is all setup we can finally start the host:

```bash
java -jar host/backend/build/libs/rng-host-all.jar server host/backend/config.yml
```

Or alternatively, with gradle:
```bash
./gradlew host:backend:runShadow
```

The above will start the host which will immediately load the enclave.

You should see lots of logs about starting the webserver and loading the
enclave. Once you see the line
```
INFO  [2018-11-21 15:05:37,877] org.eclipse.jetty.server.Server: Started @3876ms
```
The host will have been started. Direct your browser to
`localhost:8080`.

If you press 'Generate bytes' the host will connect to the enclave and
request some random bytes together with an enclave signature. The page
will display the full chain of trust including the attestation quote
proving the authenticity of the enclave key. Note that the first time
you request the bytes it may take a couple of seconds, as the first call
will be the one initializing the enclave's JVM. Subsequent calls should
be faster.

If you press 'Reload enclave' the host will reload the enclave, doing a
fresh attestation roundtrip. Note how the enclave key used for signing
the random bytes is also fresh.

#### Troubleshooting

If you get an error like the following:
```
java.lang.RuntimeException: SGX_ERROR_NO_DEVICE: Can't open SGX device
	at com.r3.sgx.core.host.internal.Native.createEnclave(Native Method)
	at com.r3.sgx.core.host.NativeHostApi.createEnclave(NativeEnclaveFactory.kt:25)
	at com.r3.sgx.rng.host.RngEnclaveHostComponent$loadEnclave$1.invoke(RngEnclaveHostComponent.kt:66)
	at com.r3.sgx.rng.host.RngEnclaveHostComponent$loadEnclave$1.invoke(RngEnclaveHostComponent.kt:25)
	at com.r3.sgx.rng.host.RngEnclaveHostComponent$Companion.withTemporaryUnpackOfResource(RngEnclaveHostComponent.kt:84)
	at com.r3.sgx.rng.host.RngEnclaveHostComponent.loadEnclave(RngEnclaveHostComponent.kt:61)
	at com.r3.sgx.rng.host.RngEnclaveHostComponent.<init>(RngEnclaveHostComponent.kt:28)
	at com.r3.sgx.rng.host.RngEnclaveHostApplication.run(RngEnclaveHostApplication.kt:32)
	at com.r3.sgx.rng.host.RngEnclaveHostApplication.run(RngEnclaveHostApplication.kt:23)
	at io.dropwizard.cli.EnvironmentCommand.run(EnvironmentCommand.java:43)
	at io.dropwizard.cli.ConfiguredCommand.run(ConfiguredCommand.java:87)
	at io.dropwizard.cli.Cli.run(Cli.java:78)
	at io.dropwizard.Application.run(Application.java:93)
	at com.r3.sgx.rng.host.Main$Companion.main(Main.kt:7)
	at com.r3.sgx.rng.host.Main.main(Main.kt)
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
