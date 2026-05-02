package macos.device

import macos.MacOSDriverClient
import macos.api.*
import macos.hierarchy.ViewHierarchy
import maestro.utils.network.XCUITestServerError
import okio.Sink
import okio.buffer
import org.slf4j.LoggerFactory

class LocalMacOSDevice(
    override val deviceId: String?,
    private val client: MacOSDriverClient,
) : MacOSDevice {

    private val logger = LoggerFactory.getLogger(LocalMacOSDevice::class.java)

    override fun open() {
        // no-op: sidecar already started by installer
    }

    override fun deviceInfo(): DeviceInfo {
        return execute { client.deviceInfo() }
    }

    override fun viewHierarchy(excludeKeyboardElements: Boolean): ViewHierarchy {
        return execute {
            client.viewHierarchy(
                ViewHierarchyRequest(
                    appIds = emptySet(),
                    excludeKeyboardElements = excludeKeyboardElements
                )
            )
        }
    }

    override fun tap(x: Int, y: Int) {
        execute {
            client.tap(TouchRequest(x = x.toFloat(), y = y.toFloat(), duration = null))
        }
    }

    override fun longPress(x: Int, y: Int, durationMs: Long) {
        execute {
            client.tap(TouchRequest(x = x.toFloat(), y = y.toFloat(), duration = durationMs.toDouble() / 1000))
        }
    }

    override fun swipe(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        duration: Double,
    ) {
        execute {
            client.swipeV2(SwipeRequest(startX = startX, startY = startY, endX = endX, endY = endY, duration = duration))
        }
    }

    override fun input(text: String) {
        execute {
            client.inputText(InputTextRequest(text = text))
        }
    }

    override fun launch(
        id: String,
        launchArguments: Map<String, Any>,
    ) {
        execute {
            client.launchApp(LaunchAppRequest(appId = id))
        }
    }

    override fun stop(id: String) {
        execute {
            client.terminateApp(TerminateAppRequest(appId = id))
        }
    }

    override fun takeScreenshot(out: Sink, compressed: Boolean) {
        execute {
            val bytes = client.screenshot(compressed)
            out.buffer().use { it.write(bytes) }
        }
    }

    override fun isShutdown(): Boolean {
        return !client.isChannelAlive()
    }

    override fun isScreenStatic(): Boolean {
        return execute { client.isScreenStatic().isStatic }
    }

    override fun pressKey(name: String) {
        execute {
            client.pressKey(PressKeyRequest(name = name))
        }
    }

    override fun eraseText(charactersToErase: Int) {
        execute {
            client.eraseText(EraseTextRequest(charactersToErase = charactersToErase))
        }
    }

    override fun runningAppId(appIds: Set<String>): String? {
        return execute { client.runningAppId(GetRunningAppRequest(appIds)).appId }
    }

    override fun close() {
        client.close()
    }

    private fun <T> execute(call: () -> T): T {
        return try {
            call()
        } catch (appCrashException: XCUITestServerError.AppCrash) {
            throw MacOSDeviceError.AppCrash(
                "App crashed or stopped while executing flow, please check diagnostic logs"
            )
        } catch (timeout: XCUITestServerError.OperationTimeout) {
            throw MacOSDeviceError.OperationTimeout(timeout.errorResponse)
        } catch (unreachable: XCUITestServerError.Unreachable) {
            throw MacOSDeviceError.Unreachable(unreachable.callName, unreachable)
        }
    }
}
