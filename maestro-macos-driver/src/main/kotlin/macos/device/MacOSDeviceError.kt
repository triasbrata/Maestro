package macos.device

sealed class MacOSDeviceError : Throwable() {
    class AppCrash(val errorMessage: String) : MacOSDeviceError()
    class OperationTimeout(val errorMessage: String) : MacOSDeviceError()

    class Unreachable(val callName: String, cause: Throwable) : MacOSDeviceError() {
        init { initCause(cause) }
        override val message: String = "macOS device became unreachable while processing $callName"
    }
}
