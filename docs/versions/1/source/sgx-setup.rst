..  _sgx-setup:

SGX Setup
#########

In order to run a fully fledged enclave host we need a working SGX setup. For this:

#. You need to run a Linux-based distribution.
    Currently, only ELF-based enclaves are supported.

#. You need an SGX-capable CPU.
    See `<https://github.com/ayeks/SGX-hardware>`_ for a non-comprehensive list
    of models. This repo also includes an executable that may be used to
    check your machine.

#. You need to enable SGX in the BIOS.
    How to do this depends on your particular setup.

#. The SGX driver must be installed.
    This is a driver that implements low-level SGX functionality like paging.

    .. sourcecode:: bash

        curl --output /tmp/driver.bin https://download.01.org/intel-sgx/linux-2.3.1/ubuntu18.04/sgx_linux_x64_driver_4d69b9c.bin
        /usr/bin/env bash /tmp/driver.bin
        depmod
        modprobe isgx

    To see whether this was successful check ``dmesg`` logs. If you see

    .. sourcecode:: none

        [36300.017753] intel_sgx: SGX is not enabled

    It means SGX is not available/enabled. Double check your BIOS setup.

    If you see

    .. sourcecode:: none

        [4976352.319148] intel_sgx: Intel SGX Driver v0.11
        [4976352.319171] intel_sgx INT0E0C:00: EPC bank 0xb0200000-0xb5f80000
        [4976352.321180] intel_sgx: second initialization call skipped

    It means the driver loaded successfully! You may also see that
    loading this driver taints the kernel, this is normal.

#. The aesmd daemon must be running.
    This is an intel-provided daemon interfacing with Intel's enclaves. In particular the Launching
    Enclave which checks the MRSIGNER whitelist (also handled by aesmd)
    before generating a token required for loading other enclaves, and
    the PCE/PvE/QE trio, which are used during EPID provisioning and
    attestation.

    You can run it manually, or use our pre-built docker image. For the latter
    you'll first need to login:

    .. parsed-literal::
        docker login --username "<Dev Preview Username>" --password "<Dev Preview Password>" |OBLIVIUM_CONTAINER_REGISTRY_URL|

    Then to run the container:

    .. parsed-literal::

        docker run --rm -it -v /run/aesmd:/run/aesmd --device /dev/isgx |OBLIVIUM_CONTAINER_REGISTRY_URL|/com.r3.sgx/aesmd:latest

    The above starts aesmd in a container with access to the SGX device,
    and exposes the UNIX domain socket our host will use to communicate
    with aesmd.

    You should see something like this:

    .. sourcecode:: none

        aesm_service[8]: [ADMIN]White List update requested
        aesm_service[8]: The server sock is 0x55e9e3464960
        aesm_service[8]: [ADMIN]Platform Services initializing
        aesm_service[8]: [ADMIN]Platform Services initialization failed due to DAL error
        aesm_service[8]: [ADMIN]White list update request successful for Version: 43

    The DAL error is irrelevant. It's related to SGX functionality that
    is generally not safe to enable as it relies on the ME.
