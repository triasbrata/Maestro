import AppKit

struct ScreenInfo {
    /// The size of the main screen in points.
    static func mainScreenSize() -> CGSize {
        NSScreen.main?.frame.size ?? .zero
    }

    /// The backing scale factor (e.g. 2.0 for Retina displays).
    static func screenScale() -> CGFloat {
        NSScreen.main?.backingScaleFactor ?? 1.0
    }

    /// The main screen size in physical pixels.
    static func mainScreenPixelSize() -> CGSize {
        let size = mainScreenSize()
        let scale = screenScale()
        return CGSize(width: size.width * scale, height: size.height * scale)
    }
}
