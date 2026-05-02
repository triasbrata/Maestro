package macos

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import macos.api.*
import macos.hierarchy.ViewHierarchy
import macos.installer.MacOSSidecarInstaller
import maestro.utils.HttpClient
import maestro.utils.network.XCUITestServerError
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.net.SocketTimeoutException
import kotlin.time.Duration.Companion.seconds

class MacOSDriverClient(
    private val installer: MacOSSidecarInstaller,
    private val okHttpClient: OkHttpClient = HttpClient.build(
        name = "MacOSDriverClient",
        readTimeout = 200.seconds,
        connectTimeout = 1.seconds,
        callTimeout = 200.seconds
    ),
) {
    private val logger = LoggerFactory.getLogger(MacOSDriverClient::class.java)

    private lateinit var client: MacOSClient

    // Latched on the first transport-level failure (socket timeout against the runner's
    // OkHttp socket). Subsequent HTTP calls fail-fast against this instead of issuing fresh
    // requests to a runner we already know isn't answering. Volatile because OkHttp callers
    // run on Dispatchers.IO worker threads (via runInterruptible in maestro.Maestro) — the
    // writer and any future reader may be different pool workers, so JMM visibility matters.
    @Volatile
    private var transportDead: XCUITestServerError.Unreachable? = null

    constructor(installer: MacOSSidecarInstaller, client: MacOSClient): this(installer) {
        this.client = client
    }

    constructor(
        installer: MacOSSidecarInstaller,
        client: MacOSClient,
        okHttpClient: OkHttpClient,
    ): this(installer, okHttpClient) {
        this.client = client
    }

    fun restartSidecar() {
        installer.uninstall()
        client = installer.start()
        transportDead = null
    }

    private fun <T> transportCall(callName: String, call: () -> T): T {
        transportDead?.let { throw it }
        return try {
            call()
        } catch (e: SocketTimeoutException) {
            val tripped = XCUITestServerError.Unreachable(callName, e)
            transportDead = tripped
            logger.error("Transport unreachable while processing $callName, latching", e)
            throw tripped
        }
    }

    private val mapper = jacksonObjectMapper()

    fun viewHierarchy(request: ViewHierarchyRequest): ViewHierarchy {
        val responseString = executeJsonRequest(
            "viewHierarchy",
            request
        )
        return mapper.readValue(responseString, ViewHierarchy::class.java)
    }

    fun screenshot(compressed: Boolean): ByteArray {
        val url = client.sidecarAPIBuilder("screenshot")
            .addQueryParameter("compressed", compressed.toString())
            .build()

        return executeJsonRequest(url)
    }

    fun terminateApp(request: TerminateAppRequest) {
        executeJsonRequest("terminateApp", request)
    }

    fun launchApp(request: LaunchAppRequest) {
        executeJsonRequest("launchApp", request)
    }

    fun isScreenStatic(): IsScreenStaticResponse {
        val responseString = executeJsonRequest("isScreenStatic")
        return mapper.readValue(responseString, IsScreenStaticResponse::class.java)
    }

    fun runningAppId(request: GetRunningAppRequest): GetRunningAppIdResponse {
        val response = executeJsonRequest(
            "runningApp",
            request
        )
        return mapper.readValue(response, GetRunningAppIdResponse::class.java)
    }

    fun swipeV2(request: SwipeRequest) {
        executeJsonRequest("swipe", request)
    }

    fun inputText(request: InputTextRequest) {
        executeJsonRequest("inputText", request)
    }

    fun tap(request: TouchRequest) {
        executeJsonRequest("touch", request)
    }

    fun pressKey(request: PressKeyRequest) {
        executeJsonRequest("pressKey", request)
    }

    fun eraseText(request: EraseTextRequest) {
        executeJsonRequest("eraseText", request)
    }

    fun deviceInfo(): DeviceInfo {
        val url = client.sidecarAPIBuilder("deviceInfo").build()
        val response = executeJsonRequest(url, Unit)
        return mapper.readValue(response, DeviceInfo::class.java)
    }

    fun keyboardInfo(request: KeyboardInfoRequest): KeyboardInfoResponse {
        val response = executeJsonRequest("keyboard", request)
        return mapper.readValue(response, KeyboardInfoResponse::class.java)
    }

    fun isChannelAlive(): Boolean {
        return installer.isChannelAlive()
    }

    fun close() {
        installer.close()
    }

    fun setPermissions(permissions: Map<String, String>) {
        executeJsonRequest("setPermissions", SetPermissionsRequest(permissions))
    }

    private fun executeJsonRequest(httpUrl: HttpUrl, body: Any): String =
        transportCall(httpUrl.callName()) {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val bodyData = mapper.writeValueAsString(body).toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .addHeader("Content-Type", "application/json")
                .url(httpUrl)
                .post(bodyData)

            okHttpClient
                .newCall(requestBuilder.build())
                .execute().use { processResponse(it, httpUrl.toString()) }
        }

    private fun executeJsonRequest(httpUrl: HttpUrl): ByteArray =
        transportCall(httpUrl.callName()) {
            val request = Request.Builder()
                .get()
                .url(httpUrl)
                .build()

            okHttpClient
                .newCall(request)
                .execute().use {
                    val bytes = it.body?.bytes() ?: ByteArray(0)
                    if (!it.isSuccessful) {
                        val responseBodyAsString = String(bytes)
                        handleExceptions(it.code, request.url.pathSegments.first(), responseBodyAsString)
                    }
                    bytes
                }
        }

    private fun executeJsonRequest(pathSegment: String, body: Any): String =
        transportCall(pathSegment) {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val bodyData = mapper.writeValueAsString(body).toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .addHeader("Content-Type", "application/json")
                .url(client.sidecarAPIBuilder(pathSegment).build())
                .post(bodyData)

            okHttpClient
                .newCall(requestBuilder.build())
                .execute().use { processResponse(it, pathSegment) }
        }

    private fun executeJsonRequest(pathSegment: String): String =
        transportCall(pathSegment) {
            val requestBuilder = Request.Builder()
                .url(client.sidecarAPIBuilder(pathSegment).build())
                .get()

            okHttpClient
                .newCall(requestBuilder.build())
                .execute().use { processResponse(it, pathSegment) }
        }

    private fun HttpUrl.callName(): String = pathSegments.firstOrNull().orEmpty().ifEmpty { "unknown" }

    private fun processResponse(response: Response, url: String): String {
        val responseBodyAsString = response.body?.bytes()?.let { bytes -> String(bytes) } ?: ""

        return if (!response.isSuccessful) {
            val code = response.code
            handleExceptions(code, url, responseBodyAsString)
        } else {
            responseBodyAsString
        }
    }

    private fun handleExceptions(
        code: Int,
        pathString: String,
        responseBodyAsString: String,
    ): String {
        logger.warn("MacOSDriver request failed. Status code: $code, path: $pathString, body: $responseBodyAsString");
        val error = try {
            mapper.readValue(responseBodyAsString, Error::class.java)
        } catch (_: JsonProcessingException) {
            Error("Unable to parse error", "unknown")
        }
        when {
            code == 408 -> {
                logger.error("Request for $pathString timeout, body: $responseBodyAsString")
                throw XCUITestServerError.OperationTimeout(error.errorMessage, pathString)
            }
            code in 400..499 -> {
                logger.error("Request for $pathString failed with bad request ${code}, body: $responseBodyAsString")
                throw XCUITestServerError.BadRequest(
                    "Request for $pathString failed with bad request ${code}, body: $responseBodyAsString",
                    responseBodyAsString
                )
            }
            error.errorMessage.contains("Lost connection to the application.*".toRegex()) -> {
                logger.error("Request for $pathString failed, because of app crash, body: $responseBodyAsString")
                throw XCUITestServerError.AppCrash(
                    "Request for $pathString failed, due to app crash with message ${error.errorMessage}"
                )
            }
            error.errorMessage.contains("Application [a-zA-Z0-9.]+ is not running".toRegex()) -> {
                logger.error("Request for $pathString failed, because of app crash, body: $responseBodyAsString")
                throw XCUITestServerError.AppCrash(
                    "Request for $pathString failed, due to app crash with message ${error.errorMessage}"
                )
            }
            error.errorMessage.contains("Error getting main window kAXErrorCannotComplete") -> {
                logger.error("Request for $pathString failed, because of app crash, body: $responseBodyAsString")
                throw XCUITestServerError.AppCrash(
                    "Request for $pathString failed, due to app crash with message ${error.errorMessage}"
                )
            }
            error.errorMessage.contains("Error getting main window.*".toRegex()) -> {
                logger.error("Request for $pathString failed, because of app crash, body: $responseBodyAsString")
                throw XCUITestServerError.AppCrash(
                    "Request for $pathString failed, due to app crash with message ${error.errorMessage}"
                )
            }
            else -> {
                logger.error("Request for $pathString failed, because of unknown reason, body: $responseBodyAsString")
                throw XCUITestServerError.UnknownFailure(
                    "Request for $pathString failed, code: ${code}, body: $responseBodyAsString"
                )
            }
        }
    }

}
