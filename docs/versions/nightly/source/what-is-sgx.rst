What is SGX?
############

Intel SGX is a new set of CPU instructions supported by any Intel CPU core based on the Skylake microarchitecture or
later. It allows for the creation of regions of RAM that are encrypted by the CPU in hardware, called *enclaves*.
Enclave memory is protected from being read by any piece of software running on the system other than software
running inside the enclave itself, including the operating system kernel and system firmware. The CPU assumes that
every part of the computer other than itself may be malicious, and thus inside an enclave data can be processed
that even the owner of the physical hardware is unable to inspect.

By itself this would not be very helpful, as hiding data from yourself is rarely a goal. SGX only really becomes useful when
you allow third parties to audit what is running inside the enclave, and establish an encrypted connection to the software
running inside. A third party that understands what software is running inside the enclave may choose to provision it
with secret data for processing. Because the third party has set up an encrypted connection to the enclave, the secrets
the enclave is working with can't be obtained by the owner of the physical hardware.

When would you want to do this? Here are some examples:

* You are hosting a joint computation on secret data, such as a shared mathematical computation where each input is
  contributed by a different third party and the answers will be returned to all of them.
* You wish to send digital documents to a remote computer for fast rendering and display, but don't want to allow the
  documents to be forwarded or copied.
* You wish to convince a third party of some fact about some data you possess without revealing it to them.

Key terminology
---------------

**Enclave.** Encrypted memory space that has access to certain CPU features, like the ability to create CPU specific
encryption/signing keys, generate random numbers, take part in remote attestation and so on. The enclave will contain
both code and data (because nothing can access the insides of the enclave, an enclave that holds only data without code
is not useful).

**Untrusted world.** The software running outside the enclave. In SGX based architectures, all software and hardware
on a computer is considered to be malicious, except the enclave itself. The CPU will protect the enclave against malicious
programs or hardware devices, but the enclave must also take steps to protect itself. Enclaves are quite limited in what
they can do - they cannot access hardware directly, nor can they communicate directly with the kernel. All actions
taken by the enclave must be in the form of requests to the host software running in the untrusted world, and all
input to the enclave comes from the untrusted world and must be verified before being acted on. As a consequence, any
data that passes through the untrusted world must be signed and encrypted.

**User.** A computer program that authenticates an enclave using remote attestation, before uploading secrets to it
over the network.

**Measurement.** The 256-bit value computed by hashing code with SHA2 as it's loaded into the enclave. The measurement
is not a direct hash of the enclave file on disk, but can be computed from it. Therefore by comparing the measurement
reported by a remote computer to the measurement derived from a local enclave file, you can verify that the remote
computer is running the same software you have got.

**Remote attestation.** A protocol that allows one computer to challenge another over the internet to produce a signed
data structure, stating what code has been loaded into the enclave. Remote attestation works by asking the remote
SGX enabled CPU to produce a *report*, which contains the measurement and 64 bytes of arbitrary *report data*. The
report data will almost always contain a public key that can be used to set up an encrypted channel to the enclave,
thus ensuring the untrusted world cannot tamper with any secrets that are provisioned.

**Sealing.** An enclave can derive a private key useful for encryption and signing, from a CPU specific secret value.
The derivation is deterministic so the same key can be derived over and over again, but it's tied to the enclave
measurement and the CPU identity so the key is accessible only to that enclave running on that CPU and no other software
on the system, not even the operating system kernel or motherboard firmware. Sealing is useful for encrypting data
before passing it to the untrusted world (i.e. host operating system and software) for storage or network transmission.

**MRSIGNER.** A 3072-bit RSA key with exponent 3, which signs the enclave. The public key must be whitelisted by Intel,
if a signed enclave is to be loaded in non-DEBUG mode. The key is also used during certain sealing key derivations, to
enable enclaves signed with the same signer to derive the same keys.

**Enclavelet.** An enclave implemented in a JVM bytecode language, running on this framework. Enclavelets have access to
infrastructure that doesn't come out of the box when using raw SGX directly. They interact with the world through
message passing.

**Secrets.** Any data that has been encrypted by a user using a key obtained from a remote attestation report, and
then provisioned to the enclave by uploading it through the untrusted world.

**RDRAND.** The ``RDRAND`` CPU instruction gives direct access to a hardware based random number generator implemented on the
CPU core itself. Because of this, the untrusted world is not involved in random number generation and thus the output
can be trusted by the enclave. You can read a `third party audit of the design of Intel's RNG circuitry here <_static/Intel_TRNG_Report_20120312.pdf>`_.

**Host.** A server running in the untrusted world that creates the enclave and loads this framework and your enclavelet into it.
The host also connects the enclave to hardware resources like network and disk.

A conceptual example
--------------------

You know someone who is an avid collector of viral cat videos and is willing to pay top dollar for only the very
best kitten comedy. You have found a video online of a cat doing something truly hilarious, and would like to sell
it to them. But you don't trust them and they don't trust you (this is the internet after all,
`they might be a dog <https://en.wikipedia.org/wiki/On_the_Internet%2C_nobody_knows_you're_a_dog>`_).
If you send them the video, they might just enjoy it without paying you for it. But if they pay you first, you might
not have a video at all and they would be out of pocket.

Luckily this is the 21st century, so the problem is solvable. In three easy steps:

1. Your counterparty trains a machine learning model on his extensive collection of cat videos, and embeds it in an
   enclave. The enclave knows how to accept a video upload and run it against the model, then output a score. It will
   not reveal the video itself.
2. He hosts it on his SGX capable CPU and sends you the network endpoint where it can be found, along with the source
   code. You read the code to ensure it does what he claims it does, compile the enclave and then perform a remote attestation
   with the remote computer to ensure it's running the same software you just compiled.
3. You upload your cat video to the remote server, safe in the knowledge it won't leak the video to the owner. The
   counterparty also uploads their ML model into the enclave, and gets back the score. He sees that it satisfied the
   ML model and is thus likely to please him, and buys an unencrypted copy of the video from you.

If the trade is integrated with a platform like Corda that supports digital tokens, even the payment can be made safe,
as the enclave could be programmed to reveal the unencrypted video if presented with a signed transaction that acts as a
proof of payment.

In an alternative approach, the enclave accepts an upload of the ML model and it's the counterparty that audits you,
not the other way around. You then upload the video locally. If the ML model is much smaller than the video this
approach would be make more efficient use of bandwidth.

References
----------

* `SGX Explained paper <https://eprint.iacr.org/2016/086.pdf>`_
* `Intel Attestation Service API <https://software.intel.com/sites/default/files/managed/7e/3b/ias-api-spec.pdf>`_
* `Intel SGX Developer Guide <https://download.01.org/intel-sgx/linux-2.4/docs/Intel_SGX_Developer_Guide.pdf>`_
* `Intel SGX Developer Reference <https://download.01.org/intel-sgx/linux-2.4/docs/Intel_SGX_Developer_Reference_Linux_2.4_Open_Source.pdf>`_
* `Intel x86 Instruction Reference <https://www.intel.co.uk/content/dam/www/public/us/en/documents/manuals/64-ia-32-architectures-software-developer-manual-325462.pdf>`_
