package macos.device

import macos.api.DeviceInfo
import macos.hierarchy.ViewHierarchy
import okio.Sink

interface MacOSDevice : AutoCloseable {

    val deviceId: String?

    fun open()

    fun deviceInfo(): DeviceInfo

    fun viewHierarchy(excludeKeyboardElements: Boolean): ViewHierarchy

    fun tap(x: Int, y: Int)

    fun longPress(x: Int, y: Int, durationMs: Long)

    fun swipe(
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        duration: Double,
    )

    fun input(text: String)

    fun launch(
        id: String,
        launchArguments: Map<String, Any> = emptyMap(),
    )

    fun stop(id: String)

    fun takeScreenshot(out: Sink, compressed: Boolean)

    fun isShutdown(): Boolean

    fun isScreenStatic(): Boolean

    fun pressKey(name: String)

    fun eraseText(charactersToErase: Int)

    fun runningAppId(appIds: Set<String>): String?
}
