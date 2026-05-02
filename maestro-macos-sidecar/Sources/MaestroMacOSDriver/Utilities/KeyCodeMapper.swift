import CoreGraphics
import Foundation

enum KeyCodeMapper {
    /// Maps a key name string to the corresponding CGKeyCode.
    /// Returns nil for unrecognized key names.
    static func keyCode(for name: String) -> CGKeyCode? {
        switch name.lowercased() {
        case "enter", "return":
            return 36
        case "backspace", "delete":
            return 51
        case "escape":
            return 53
        case "tab":
            return 48
        case "space":
            return 49
        case "left":
            return 123
        case "right":
            return 124
        case "down":
            return 125
        case "up":
            return 126
        case "home":
            return 115
        case "end":
            return 119
        case "pageup":
            return 116
        case "pagedown":
            return 121
        default:
            return nil
        }
    }
}
