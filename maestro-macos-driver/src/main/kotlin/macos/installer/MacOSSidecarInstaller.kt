package macos.installer

import macos.MacOSClient

interface MacOSSidecarInstaller : AutoCloseable {
    fun start(): MacOSClient

    /**
     * Attempts to uninstall the macOS Sidecar.
     *
     * @return true if the macOS Sidecar was uninstalled, false otherwise.
     */
    fun uninstall(): Boolean

    fun isChannelAlive(): Boolean
}
