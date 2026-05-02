/*
 *
 *  Copyright (c) 2022 mobile.dev inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package maestro.drivers

import macos.device.MacOSDevice
import macos.device.MacOSDeviceError
import macos.hierarchy.AXNode
import maestro.Capability
import maestro.DeviceInfo
import maestro.device.DeviceOrientation
import maestro.DeviceUnreachableException
import maestro.Driver
import maestro.KeyCode
import maestro.MaestroException
import maestro.Point
import maestro.ScreenRecording
import maestro.SwipeDirection
import maestro.TreeNode
import maestro.ViewHierarchy
import maestro.utils.MaestroTimer
import maestro.utils.ScreenshotUtils
import okio.Sink
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.roundToInt

class MacOSDriver(
    private val macosDevice: MacOSDevice,
) : Driver {

    private var appId: String? = null

    override fun name(): String {
        return NAME
    }

    override fun open() {
        macosDevice.open()
    }

    override fun close() {
        macosDevice.close()
        appId = null
    }

    override fun deviceInfo(): DeviceInfo {
        return runDeviceCall("deviceInfo") { macosDevice.deviceInfo().toCommonDeviceInfo() }
    }

    override fun launchApp(
        appId: String,
        launchArguments: Map<String, Any>,
    ) {
        macosDevice.launch(appId, launchArguments)
        this.appId = appId
    }

    override fun stopApp(appId: String) {
        macosDevice.stop(appId)
    }

    override fun killApp(appId: String) {
        stopApp(appId)
    }

    override fun clearAppState(appId: String) {
        LOGGER.warn("clearAppState is not supported on macOS")
    }

    override fun clearKeychain() {
        LOGGER.warn("clearKeychain is not supported on macOS")
    }

    override fun tap(point: Point) {
        runDeviceCall("tap") { macosDevice.tap(point.x, point.y) }
    }

    override fun longPress(point: Point) {
        runDeviceCall("longPress") { macosDevice.longPress(point.x, point.y, 500) }
    }

    override fun pressKey(code: KeyCode) {
        val name = when (code) {
            KeyCode.ENTER -> "return"
            KeyCode.BACKSPACE -> "delete"
            KeyCode.BACK -> "escape"
            KeyCode.ESCAPE -> "escape"
            KeyCode.HOME -> "home"
            KeyCode.TAB -> "tab"
            else -> {
                LOGGER.warn("Unsupported key code $code on macOS")
                return
            }
        }
        runDeviceCall("pressKey") { macosDevice.pressKey(name) }
    }

    override fun contentDescriptor(excludeKeyboardElements: Boolean): TreeNode {
        return runDeviceCall("snapshot") { viewHierarchy(excludeKeyboardElements) }
    }

    private fun viewHierarchy(excludeKeyboardElements: Boolean): TreeNode {
        val hierarchy = macosDevice.viewHierarchy(excludeKeyboardElements)
        val root = hierarchy.root ?: return TreeNode()
        return mapViewHierarchy(root)
    }

    private fun mapViewHierarchy(element: AXNode): TreeNode {
        val attributes = mutableMapOf<String, String>()
        element.label?.let { attributes["accessibilityText"] = it }
        element.identifier?.let { attributes["resource-id"] = it }
        element.type?.let { attributes["class"] = it }
        element.frame?.let { frame ->
            val left = frame.x.roundToInt()
            val top = frame.y.roundToInt()
            val right = (frame.x + frame.width).roundToInt()
            val bottom = (frame.y + frame.height).roundToInt()
            attributes["bounds"] = "[$left,$top][$right,$bottom]"
        }

        val children = (element.children ?: emptyList()).map {
            mapViewHierarchy(it)
        }

        return TreeNode(
            attributes = attributes,
            children = children,
        )
    }

    override fun isUnicodeInputSupported(): Boolean {
        return true
    }

    override fun scrollVertical() {
        val deviceInfo = deviceInfo()
        val width = deviceInfo.widthGrid
        val height = deviceInfo.heightGrid

        swipe(
            start = Point((0.5 * width).roundToInt(), (0.7 * height).roundToInt()),
            end = Point((0.5 * width).roundToInt(), (0.3 * height).roundToInt()),
            durationMs = 200,
        )
    }

    override fun isKeyboardVisible(): Boolean {
        return false
    }

    override fun swipe(
        start: Point,
        end: Point,
        durationMs: Long
    ) {
        runDeviceCall("swipe") {
            macosDevice.swipe(
                startX = start.x.toDouble(),
                startY = start.y.toDouble(),
                endX = end.x.toDouble(),
                endY = end.y.toDouble(),
                duration = durationMs.toDouble() / 1000
            )
        }
    }

    override fun swipe(swipeDirection: SwipeDirection, durationMs: Long) {
        val deviceInfo = deviceInfo()
        val width = deviceInfo.widthGrid
        val height = deviceInfo.heightGrid

        val startPoint: Point
        val endPoint: Point

        when (swipeDirection) {
            SwipeDirection.UP -> {
                startPoint = Point(
                    x = (0.5 * width).roundToInt(),
                    y = (0.9 * height).roundToInt(),
                )
                endPoint = Point(
                    x = (0.5 * width).roundToInt(),
                    y = (0.1 * height).roundToInt(),
                )
            }

            SwipeDirection.DOWN -> {
                startPoint = Point(
                    x = (0.5 * width).roundToInt(),
                    y = (0.2 * height).roundToInt(),
                )
                endPoint = Point(
                    x = (0.5 * width).roundToInt(),
                    y = (0.9 * height).roundToInt(),
                )
            }

            SwipeDirection.RIGHT -> {
                startPoint = Point(
                    x = (0.1 * width).roundToInt(),
                    y = (0.5 * height).roundToInt(),
                )
                endPoint = Point(
                    x = (0.9 * width).roundToInt(),
                    y = (0.5 * height).roundToInt(),
                )
            }

            SwipeDirection.LEFT -> {
                startPoint = Point(
                    x = (0.9 * width).roundToInt(),
                    y = (0.5 * height).roundToInt(),
                )
                endPoint = Point(
                    x = (0.1 * width).roundToInt(),
                    y = (0.5 * height).roundToInt(),
                )
            }
        }
        swipe(startPoint, endPoint, durationMs)
    }

    override fun swipe(elementPoint: Point, direction: SwipeDirection, durationMs: Long) {
        val deviceInfo = deviceInfo()
        val width = deviceInfo.widthGrid
        val height = deviceInfo.heightGrid

        when (direction) {
            SwipeDirection.UP -> {
                val end = Point(x = elementPoint.x, y = (0.1 * height).roundToInt())
                swipe(elementPoint, end, durationMs)
            }

            SwipeDirection.DOWN -> {
                val end = Point(x = elementPoint.x, y = (0.9 * height).roundToInt())
                swipe(elementPoint, end, durationMs)
            }

            SwipeDirection.RIGHT -> {
                val end = Point(x = (0.9 * width).roundToInt(), y = elementPoint.y)
                swipe(elementPoint, end, durationMs)
            }

            SwipeDirection.LEFT -> {
                val end = Point(x = (0.1 * width).roundToInt(), y = elementPoint.y)
                swipe(elementPoint, end, durationMs)
            }
        }
    }

    override fun backPress() {
        macosDevice.pressKey("escape")
    }

    override fun hideKeyboard() {
        // No-op on macOS
    }

    override fun inputText(text: String) {
        runDeviceCall("inputText") { macosDevice.input(text) }
    }

    override fun openLink(link: String, appId: String?, autoVerify: Boolean, browser: Boolean) {
        throw UnsupportedOperationException("openLink is not supported on macOS")
    }

    override fun takeScreenshot(out: Sink, compressed: Boolean) {
        runDeviceCall("takeScreenshot") { macosDevice.takeScreenshot(out, compressed) }
    }

    override fun startScreenRecording(out: Sink): ScreenRecording {
        throw UnsupportedOperationException("Screen recording is not supported on macOS")
    }

    override fun setLocation(latitude: Double, longitude: Double) {
        LOGGER.warn("setLocation is not supported on macOS")
    }

    override fun setOrientation(orientation: DeviceOrientation) {
        LOGGER.warn("setOrientation is not supported on macOS")
    }

    override fun eraseText(charactersToErase: Int) {
        runDeviceCall("eraseText") { macosDevice.eraseText(charactersToErase) }
    }

    override fun setProxy(host: String, port: Int) {
        LOGGER.warn("setProxy is not supported on macOS")
    }

    override fun resetProxy() {
        LOGGER.warn("resetProxy is not supported on macOS")
    }

    override fun isShutdown(): Boolean {
        return macosDevice.isShutdown()
    }

    override fun waitUntilScreenIsStatic(timeoutMs: Long): Boolean {
        return MaestroTimer.retryUntilTrue(timeoutMs) {
            macosDevice.isScreenStatic()
        }
    }

    override fun waitForAppToSettle(initialHierarchy: ViewHierarchy?, appId: String?, timeoutMs: Int?): ViewHierarchy? {
        LOGGER.info("Waiting for animation to end with timeout $SCREEN_SETTLE_TIMEOUT_MS")
        val didFinishOnTime = waitUntilScreenIsStatic(SCREEN_SETTLE_TIMEOUT_MS)

        return if (didFinishOnTime) null else ScreenshotUtils.waitForAppToSettle(initialHierarchy, this, timeoutMs)
    }

    override fun capabilities(): List<Capability> {
        return emptyList()
    }

    override fun setPermissions(appId: String, permissions: Map<String, String>) {
        LOGGER.warn("setPermissions is not supported on macOS")
    }

    override fun addMedia(mediaFiles: List<File>) {
        LOGGER.warn("addMedia is not supported on macOS")
    }

    override fun isAirplaneModeEnabled(): Boolean {
        LOGGER.warn("Airplane mode is not available on macOS")
        return false
    }

    override fun setAirplaneMode(enabled: Boolean) {
        LOGGER.warn("Airplane mode is not available on macOS")
    }

    private fun <T> runDeviceCall(callName: String, call: () -> T): T {
        return try {
            call()
        } catch (unreachable: MacOSDeviceError.Unreachable) {
            LOGGER.error("Device unreachable while processing $callName command", unreachable)
            throw DeviceUnreachableException(unreachable.callName, unreachable)
        } catch (appCrashException: MacOSDeviceError.AppCrash) {
            LOGGER.error("Detected app crash during $callName command", appCrashException)
            throw MaestroException.AppCrash(appCrashException.errorMessage)
        } catch (timeoutException: MacOSDeviceError.OperationTimeout) {
            throw MaestroException.DriverTimeout(
                message = "Maestro driver timed out during $callName call with: ${timeoutException.errorMessage}",
            )
        }
    }

    companion object {
        const val NAME = "macOS"

        private val LOGGER = LoggerFactory.getLogger(MacOSDriver::class.java)

        private const val SCREEN_SETTLE_TIMEOUT_MS: Long = 3000
    }
}

private fun macos.api.DeviceInfo.toCommonDeviceInfo(): DeviceInfo {
    return DeviceInfo(
        platform = maestro.device.Platform.MACOS,
        widthPixels = width,
        heightPixels = height,
        widthGrid = width,
        heightGrid = height,
    )
}
