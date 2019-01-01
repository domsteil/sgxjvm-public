Troubleshooting
###############

Here's a collection of tips for issues you may encounter whilst developing your enclave.

Printing to the console
-----------------------

``System.out`` and ``System.err`` work inside the enclave and will be forwarded to the host console. But remember that
the host is untrusted, so don't use this as a general debug logging system - even logging what commands the enclave
is receiving may leak valuable data.

malloc failure / out of memory
------------------------------

A message like ``malloc(68184760) returned NULL. Aborting early to avoid memory corruption`` from inside the enclave
means you ran out of memory. Try increasing the max heap size in your :ref:`enclave_xml` file.

Error 8201
----------

This error during enclave launch indicates you're trying to load an unsigned enclave. Remember to use the
``