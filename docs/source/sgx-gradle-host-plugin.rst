.. _sgx-gradle-host-plugin:

SGX Host Plugin
===============

The SGX Host plugin creates Gradle tasks to copy your own signed Enclavelet
(as built by the SGX Enclave plugin) into the ``oblivium/enclavelet-host`` base
Docker image and then bake them together into a new Docker image. This new
image can also be launched inside a Docker container so that you can run tests
against it.

You can apply this plugin as follows:

.. parsed-literal::

    plugins {
        id 'com.r3.sgx.host'
    }

..

which will implicitly apply both the :ref:`sgx-gradle-enclave-plugin` and `Docker Remote API <https://bmuschko.github.io/gradle-docker-plugin>`__ plugins as well.

Configuration
-------------

The plugin registers the following Gradle extensions, where ``<Type>`` is a
placeholder for either ``Simulation``, ``Debug`` or ``Release``. Almost all of
these properties should already have acceptable default values; they are listed
here for reference:

.. parsed-literal::

    enclaveImage<Type> {
        // URL for your own Docker repository.
        repositoryUrl = docker.registryCredentials.url

        // Location of your signed enclavelet file, set by default by the
        // SGX Enclave plugin.
        enclaveObject = file('path/to/signed-enclave.so')

        // Docker base image containing the Oblivium host code.
        baseImageName = 'oblivium/enclavelet-host'

        // Version tag for the Oblivium host base image.
        baseTag = '|OBLIVIUM_VERSION|'

        // Your Docker image's chosen name.
        publishImageName = '${project.group}/${project.name}'

        // Your Docker image's version tag.
        publishTag = '|OBLIVIUM_VERSION|'

        // Configuration properties for your test containers.
        testing {
            // gRPC port number for your test host.
            grpcPort = 30080

            // Number of seconds to wait for your test host to start.
            startTimeout = 30

            // An optional YAML file to configure your test host.
            configFile = file('path/to/host/config.yml')

            // Whether your test container should be removed automatically
            // when it exits. Useful for debugging start-up errors.
            removeOnExit = (TRUE | false)
        }
    }

..

You will also need to configure the Docker Remote API plugin for use with your
own Docker repository:

.. parsed-literal::

    docker {
        registryCredentials {
            url = 'URL for your Docker registry'
            username = 'username for your Docker registry'
            password = 'password for your Docker registry'
        }
    }

..

Tasks
-----

The plugin creates the following tasks for each of the ``Simulation``, ``Debug``
and ``Release`` build types:

* ``prepareEnclaveImage<Type>``
    Gathers the artifacts to include in your Docker image into a single
    directory and writes a ``Dockerfile`` for them.
* ``buildEnclaveImage<Type>``
    Builds a Docker image from the output of ``prepareEnclaveImage<Type>``.
* ``pushEnclaveImage<Type>``
    Pushes the Docker image built by ``buildEnclaveImage<Type>`` into your
    Docker repository, tagging it as both ``$publishTag`` and ``latest``.
* ``createEnclaveContainer<Type>``
    Creates a Docker container for the image built by
    ``buildEnclaveImage<Type>``, with its gRPC port exposed on ``$grpcPort``.
    The container will also include ``$configFile``, should one be provided.
    This allows you to execute tests against a running enclavelet host.
* ``startEnclaveContainer<Type>``
    Starts the container built by ``createEnclaveContainer<Type>``, and waits
    ``$startTimeout`` seconds for its gRPC port to become available for new
    connections. This task is automatically finalized by
    ``stopEnclaveContainer<Type>``, and so your testing task must be configured
    as:

    .. parsed-literal::

        test {
            dependsOn startEnclaveContainer<Type>
            finalizedBy stopEnclaveContainer<Type>
        }

    ..

    to ensure that the container is not destroyed until *after* the tests have
    completed.

* ``stopEnclaveContainer<Type>``
    Stops the container that was started by ``startEnclaveContainer<Type>``.

