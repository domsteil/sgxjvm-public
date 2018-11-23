package com.r3.sgx.rng.host

import com.r3.sgx.core.common.Cursor
import com.r3.sgx.core.common.SgxSpid
import io.dropwizard.Configuration

data class RngEnclaveHostConfiguration(
        @JvmField val spid: String = Hex.byteBufferToHex(Cursor.allocate(SgxSpid).getBuffer()),
        @JvmField val isQuoteLinkable: Boolean = true
) : Configuration()
