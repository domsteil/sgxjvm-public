package com.r3.sgx.rng.client

import picocli.CommandLine
import java.util.concurrent.Callable

@CommandLine.Command(subcommands = [
    GetAttestationCommand::class,
    PrintAttestationCommand::class,
    VerifyAttestationCommand::class,
    GetRandomCommand::class
])
class RngClientCli : Callable<Unit> {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CommandLine.call(RngClientCli(), *args)
        }
    }

    override fun call() {
        CommandLine.usage(this, System.out)
    }
}
