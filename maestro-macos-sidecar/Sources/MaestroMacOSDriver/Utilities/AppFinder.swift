import AppKit

struct AppFinder {
    /// Returns the running application matching the given bundle identifier, if any.
    static func findApp(bundleId: String) -> NSRunningApplication? {
        NSWorkspace.shared.runningApplications.first {
            $0.bundleIdentifier == bundleId
        }
    }

    /// Returns the bundle identifier of the frontmost (active) application, if any.
    static func foregroundAppBundleId() -> String? {
        NSWorkspace.shared.frontmostApplication?.bundleIdentifier
    }
}
