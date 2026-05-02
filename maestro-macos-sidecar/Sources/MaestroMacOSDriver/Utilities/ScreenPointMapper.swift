import AppKit
import Foundation

enum ScreenPointMapper {
    /// Converts a point from the main screen's coordinate space to the
    /// flipped coordinate space used by accessibility APIs (origin at
    /// top-left instead of bottom-left).
    static func flipPoint(_ point: CGPoint) -> CGPoint {
        guard let screen = NSScreen.main else { return point }
        let height = screen.frame.height
        return CGPoint(x: point.x, y: height - point.y)
    }

    /// Returns a point clamped to the main screen bounds.
    static func clampToScreen(_ point: CGPoint) -> CGPoint {
        guard let screen = NSScreen.main else { return point }
        let frame = screen.frame
        return CGPoint(
            x: max(frame.minX, min(frame.maxX - 1, point.x)),
            y: max(frame.minY, min(frame.maxY - 1, point.y))
        )
    }
}
