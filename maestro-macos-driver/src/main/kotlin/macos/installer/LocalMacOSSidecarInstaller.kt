package macos.installer

import macos.MacOSClient
import maestro.utils.HttpClient
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class LocalMacOSSidecarInstaller(
    private val host: String = "127.0.0.1",
    private val defaultPort: Int = 22088,
    private val httpClient: OkHttpClient = HttpClient.build(
        name = "MacOSSidecarStatusCheck",
        connectTimeout = 1.seconds,
        readTimeout = 100.seconds,
    ),
) : MacOSSidecarInstaller {

    private val logger = LoggerFactory.getLogger(LocalMacOSSidecarInstaller::class.java)
    private var sidecarProcess: Process? = null

    override fun start(): MacOSClient {
        logger.info("start()")

        val sidecarPath = findSidecarBinary()
        logger.info("Sidecar binary path: $sidecarPath")

        sidecarProcess = ProcessBuilder(listOf(sidecarPath, "--port", defaultPort.toString()))
            .start()

        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < SERVER_LAUNCH_TIMEOUT_MS) {
            runCatching {
                if (isChannelAlive()) return MacOSClient(host, defaultPort)
            }
            Thread.sleep(500)
        }

        throw IllegalStateException("macOS sidecar not ready in time, consider increasing timeout by configuring MAESTRO_DRIVER_STARTUP_TIMEOUT env variable")
    }

    override fun isChannelAlive(): Boolean {
        val url = HttpUrl.Builder()
            .scheme("http")
            .host(host)
            .addPathSegment("status")
            .port(defaultPort)
            .build()

        val request = Request.Builder()
            .get()
            .url(url)
            .build()

        return try {
            httpClient.newCall(request).execute().use {
                it.isSuccessful
            }
        } catch (ignore: IOException) {
            false
        }
    }

    private fun findSidecarBinary(): String {
        val fromSystemProperty = System.getProperty("maestro.macos.sidecar.path")
        if (fromSystemProperty != null) return fromSystemProperty

        val fromEnvVar = System.getenv("MAESTRO_MACOS_SIDECAR_PATH")
        if (fromEnvVar != null) return fromEnvVar

        val defaultPath = File("../maestro-macos-sidecar/.build/debug/MaestroMacOSDriver")
        if (defaultPath.exists()) return defaultPath.absolutePath

        throw IllegalStateException("Cannot find macOS sidecar binary. Set maestro.macos.sidecar.path system property or MAESTRO_MACOS_SIDECAR_PATH env var")
    }

    override fun uninstall(): Boolean {
        return try {
            sidecarProcess?.let {
                if (it.isAlive) {
                    logger.trace("Killing sidecar process")
                    it.destroy()
                    it.waitFor()
                }
            }
            sidecarProcess = null
            true
        } catch (e: Exception) {
            logger.error("Failed to kill sidecar process", e)
            false
        }
    }

    override fun close() {
        uninstall()
    }

    companion object {
        private const val SERVER_LAUNCH_TIMEOUT_MS = 120000L
    }
}
