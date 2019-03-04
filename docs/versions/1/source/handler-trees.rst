Handler trees
#############

Secure communication between users, enclaves and host software requires a complex and intricate set of operations to
be performed in order to authenticate the enclave via remote attestation, set up an encrypted communications channel,
keep multiple parallel users separated, safely store data to disk, handle errors and so on. Handler trees are how
abstracts this complexity away from you.

The root of the tree processes raw calls in and out of the enclave (ECALLs/OCALLs). As the message propagates down
the tree it gets demultiplexed/processed and possibly passed to a downstream handler. This way we can compose
different pieces of functionality (like attestation) horizontally, and add intercepting functionality like
encryption/decryption. When a ``Handler`` is receiving a message it furthermore has access to a ``Sender`` which can
send messages the other way.

Handlers are provided to perform the following tasks, and you can write your own:

* Communicating the contents of Java exceptions.
* Multiplexing "channels" onto the stream of messages. This allows multiple users to interact with an enclave simultaneously.
* Handling remote attestation requests.
* Serializing and deserializing to protocol buffers.

.. note:: Future versions of the framework will offer handlers for encrypting channels and management of persistently
   encrypted data that may be stored to disk (sealed data).

Handler trees are mirrored on each side of the communication. As an example we may have the following Handler structure::

      Host                                Enclave

    A--\                                      /--A'
       |                                      |
    B--E---\                             /----E'-B'
       |   |                             |    |
    C--/   F--root ~ ECALL/OCALL ~ root--F'   \--C'
           |                             |
    D------/                             \-------D'

Here we have handlers A,B,C,D,E and F on the host side, and corresponding ones on the other. When say A wants to send
something to the enclave it will call through the ``Handler``/``Sender`` tree where each component will serialize its
part of the message. For example we may end up with a message like this::

    +----------+
    | F header |
    +----------+
    | E header |
    +----------+
    |  A body  |
    |   ...    |
    +----------+

When the message is sent it will first be handled by F' which will deserialize its part and forward it to its
downstream E', then A', etc.

At the root of the tree on both sides is a ``RootHandler``. This ``Handler`` deals with exception handling and
provides a way to add downstream handlers, the messages of which it will multiplex automatically. Exceptions
thrown are propagated back and rethrown on the calling side.

By subclassing ``Enclavelet`` you get these downstream handlers added to the ``RootHandler`` automatically:

- ``ChannelHandlingHandler``: allows the host to open/close channels to the enclave. Each new channel in turn has an
  associated downstream handler, created by ``createHandler``.
- ``EpidAttestationEnclaveHandler``: allows the host to request the report data from the enclave, to be embedded into
  a remote attestation quote.

This is the structure that a host of an ``Enclavelet`` must mirror with ``ChannelInitiatingHandler`` and
``EpidAttestationHostHandler`` respectively.

Most of the above complexity is hidden behind the ``Enclavelet`` abstract class. The two functions to implement are:

- ``createReportData``: the piece of data the enclave provides to be included in an attestation report/quote.
- ``createHandler``: a factory for Handlers that handle incoming connections to the enclave.

.. note:: Future versions will probably abstract you from the concept of report data entirely. A
    cryptographic key pair will be generated automatically, this will be the identity of the running enclave instance.
    You will be able to access this identity indirectly to derive further keys, sign data, or encrypt.
