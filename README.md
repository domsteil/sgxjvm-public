This is a project enabling the development of Java SGX enclaves. See
the [documentation](https://docs.corda.net/sgxjvm/) for
details.

###  Quickstart:

We'll connect to an sample random number generating enclave hosted by R3.

First we'll reproduce the enclave running remotely:

1. Checkout the `release-1` branch.
```
git checkout release-1
```
2. Build the Release version enclave. This will attach a production signature
that only applies to this specific enclave.
```
./gradlew samples:rng:rng-enclave:buildSignedEnclaveRelease
```

Now we'll connect to the enclave using a client tool.

3. Build the client tool:
```
./gradlew samples:rng:rng-client:shadowJar
```

4. Retrieve the attestation data of the remote enclave and view it:
```
java -jar samples/rng/rng-client/build/libs/rng-client.jar get-attestation sgxjvm.r3.com:8001 > attestation
java -jar samples/rng/rng-client/build/libs/rng-client.jar print-attestation < attestation
```

5. Verify the attestation data using the measurement of the reproduced enclave:

```
java -jar samples/rng/rng-client/build/libs/rng-client.jar verify-attestation -e samples/rng/rng-enclave/build/enclave/Release/enclave.metadata.yml < attestation
```

6. Retrieve randomness from the enclave. This step also checks the
attestation, and in addition the enclave's signature over the randomness:

```
java -jar samples/rng/rng-client/build/libs/rng-client.jar get-random sgxjvm.r3.com:8001 -e samples/rng/rng-enclave/build/enclave/Release/enclave.metadata.yml < attestation
```

If everything's correct this will print some randomness.

Note that we also host a nightly build of this enclave on
`sgxjvm.r3.com:8000`, however this enclave is not signed by a production
key. It's a good exercise to check what happens if you connect to this
enclave.