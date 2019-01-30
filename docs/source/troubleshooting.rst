Troubleshooting
###############

Here's a collection of tips for issues you may encounter whilst developing your enclave.

Printing to the console
-----------------------

``System.out`` and ``System.err`` work inside the enclave and will be forwarded to the host console. However note that
this logging is disabled when the enclave is loaded in non-DEBUG mode! This is to avoid accidental privacy leaks.

malloc failure / out of memory
------------------------------

A message like the following:

.. parsed-literal::

    malloc(68184760) returned NULL. Aborting early to avoid memory corruption

from inside the enclave means the enclave JVM ran out of memory. Try increasing the max heap size in your
:ref:`enclave_xml` file.

Error 8201
----------

This error during enclave launch indicates you're trying to load an unsigned enclave. Remember to use the
``buildSignedEnclave*`` gradle tasks as opposed to the ``buildUnsignedEnclave*`` ones.

Can't open SGX device
---------------------

An error like the following:

.. parsed-literal::

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

indicates that your SGX setup isn't fully working. Double check that the sgx driver is loaded:

.. parsed-literal::

    lsmod | grep isgx

That aesmd is running:

.. parsed-literal::

    ps aux | grep aesm_service

And that the aesmd socket is exposed properly:

.. parsed-literal::

    ls -al /run/aesmd/aesm.socket

For more info see :ref:`sgx-setup`.
