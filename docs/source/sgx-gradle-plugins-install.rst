.. _sgx-gradle-plugins-install:

Installation
============

We need to download the SGX plugins and add them to Gradle's plugins classpath.
Either include this ``buildscript`` in your root ``build.gradle`` file:

.. parsed-literal::

    buildscript {
        apply from: 'credentials.gradle', to: it
        dependencies {
            classpath "com.r3.sgx:sgx-jvm-plugin-enclave:|oblivium_version|"
            classpath "com.r3.sgx:sgx-jvm-plugin-host:|oblivium_version|"
        }
    }

..

Or you can use Gradle's ``plugins`` block in your root ``build.gradle`` instead:

.. parsed-literal::

    plugins {
        id 'com.r3.sgx.enclave' version '|oblivium_version|' apply false
        id 'com.r3.sgx.host' version '|oblivium_version|' apply false
    }

..

When using the ``plugins`` block, you must include the Artifactory by adding a
``pluginManagement`` block to the top of your project's ``settings.gradle``
file:

.. parsed-literal::

    pluginManagement {
        apply from: 'credentials.gradle', to: it
        repositories {
            gradlePluginPortal()
        }
    }

..
