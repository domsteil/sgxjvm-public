import com.r3.sgx.rng.host.GenerateResponse
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.get
import org.w3c.xhr.XMLHttpRequest
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.clear
import kotlin.reflect.KProperty1

data class Model(
    val generateButtonDisabled: Boolean,
    val generateButtonText: String,
    val reloadButtonDisabled: Boolean,
    val reloadButtonText: String,
    val responseOrError: ResponseOrError
)

sealed class ResponseOrError {
    data class Response(val response: GenerateResponse): ResponseOrError()
    data class Error(val error: String): ResponseOrError()
}

var currentModel = Model(
        generateButtonDisabled = false,
        generateButtonText = "Generate bytes",
        reloadButtonDisabled = false,
        reloadButtonText = "Reload enclave",
        responseOrError = ResponseOrError.Error("")
)

fun updateModel(model: Model) {
    currentModel = model
    renderModel(model)
}

fun main(args: Array<String>) {
    window.onload = {
        updateModel(currentModel)

        val generateButton = document.getElementById("generate_button_id") as HTMLButtonElement
        generateButton.addEventListener("click", {
            updateModel(currentModel.copy(
                    generateButtonDisabled = true,
                    generateButtonText = "Generating"
            ))
            generateBytes { request ->
                updateModel(currentModel.copy(
                        generateButtonDisabled = false,
                        generateButtonText = "Generate bytes",
                        responseOrError = when (request.status) {
                            200.toShort() -> ResponseOrError.Response(JSON.parse(request.responseText))
                            else -> ResponseOrError.Error(request.responseText)
                        }
                ))
            }
        })

        val reloadButton = document.getElementById("reload_button_id") as HTMLButtonElement
        reloadButton.addEventListener("click", {
            updateModel(currentModel.copy(
                    reloadButtonDisabled = true,
                    reloadButtonText = "Reloading"
            ))
            reloadEnclave { request ->
                val model = currentModel.copy(
                        reloadButtonDisabled = false,
                        reloadButtonText = "Reload enclave"
                )
                val modelWithErrorMaybe = when (request.status) {
                    200.toShort() -> model
                    else -> model.copy(responseOrError = ResponseOrError.Error(request.responseText))
                }
                updateModel(modelWithErrorMaybe)
            }
        })
    }
}

fun renderModel(model: Model) {
    val generateButton = document.getElementById("generate_button_id") as HTMLButtonElement
    generateButton.disabled = model.generateButtonDisabled
    generateButton.textContent = model.generateButtonText
    val reloadButton = document.getElementById("reload_button_id") as HTMLButtonElement
    reloadButton.disabled = model.reloadButtonDisabled
    reloadButton.textContent = model.reloadButtonText
    val chainOfTrust = document.getElementById("chain_of_trust_id") as HTMLDivElement
    val generateError = document.getElementById("generate_error_id") as HTMLDivElement
    when (model.responseOrError) {
        is ResponseOrError.Response -> {
            generateError.hide()
            val response = model.responseOrError.response
            response.renderToChainOfTrustData(GenerateResponse::iasCertificate, "ias_certificate_id")
            response.renderToChainOfTrustData(GenerateResponse::iasSignature, "ias_signature_id")
            response.renderToChainOfTrustData(GenerateResponse::iasResponse, "ias_response_id")
            response.renderToChainOfTrustData(GenerateResponse::signedQuote, "enclave_quote_id")
            response.renderToChainOfTrustData(GenerateResponse::enclavePublicKey, "enclave_public_key_id")
            response.renderToChainOfTrustData(GenerateResponse::enclaveSignature, "enclave_signature_id")
            response.renderToChainOfTrustData(GenerateResponse::generatedRandomBytes, "generated_bytes_id")
        }
        is ResponseOrError.Error -> {
            generateError.unhide()
            for (i in 0 until chainOfTrust.childElementCount) {
                chainOfTrust.children[i]!!.hide()
            }
            generateError.textContent = model.responseOrError.error
        }
    }
}

fun GenerateResponse.renderToChainOfTrustData(field: KProperty1<GenerateResponse, String>, id: String) {
    val element = document.getElementById(id) as HTMLDivElement
    val dataElement = element.children[0]!!
    dataElement.clear()
    for (child in field.get(this).split("\n").map { createDataLineDiv(it) }) {
        dataElement.appendChild(child)
    }
    element.hideIfEmpty()
}

fun createDataLineDiv(line: String): Element {
    val element = document.createElement("div")
    element.classList.add("data_line")
    val adjustedLine = if (line.length > 128) {
        line.take(125) + "..."
    } else {
        line
    }
    element.textContent = adjustedLine
    return element
}

fun Element.hide() {
    classList.add("hidden")
}

fun Element.unhide() {
    classList.remove("hidden")
}

fun Element.hideIfEmpty() {
    if (textContent.isNullOrEmpty()) {
        hide()
    } else {
        unhide()
    }
}

fun generateBytes(callback: (XMLHttpRequest) -> Unit) {
    val request = XMLHttpRequest()
    request.onloadend = { callback(request) }
    request.open("GET", "/api/generate", true)
    request.send()
}

fun reloadEnclave(callback: (XMLHttpRequest) -> Unit) {
    val request = XMLHttpRequest()
    request.onloadend = { callback(request) }
    request.open("GET", "/api/reload", true)
    request.send()
}
