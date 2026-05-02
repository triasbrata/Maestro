import CoreGraphics
import Foundation

enum CGEventHelper {

    static func postMouseEvent(type: CGEventType, point: CGPoint) {
        let event = CGEvent(
            mouseEventSource: nil,
            mouseType: type,
            mouseCursorPosition: point,
            mouseButton: .left
        )
        event?.post(tap: .cghidEventTap)
    }

    static func postKeyEvent(keyCode: CGKeyCode, keyDown: Bool) {
        let event = CGEvent(keyboardEventSource: nil, virtualKey: keyCode, keyDown: keyDown)
        event?.post(tap: .cghidEventTap)
    }

    static func postScrollEvent(deltaX: Int32, deltaY: Int32) {
        let event = CGEvent(
            scrollWheelEvent2Source: nil,
            units: .pixel,
            wheelCount: 1,
            wheel1: deltaY,
            wheel2: 0,
            wheel3: 0
        )
        event?.post(tap: .cghidEventTap)
    }

    static func postTextCharacter(_ char: Character) {
        let string = String(char)
        let utf16Units = Array(string.utf16)
        let length = utf16Units.count
        guard let eventDown = CGEvent(keyboardEventSource: nil, virtualKey: 0, keyDown: true),
              let eventUp = CGEvent(keyboardEventSource: nil, virtualKey: 0, keyDown: false) else {
            return
        }
        utf16Units.withUnsafeBufferPointer { buffer in
            eventDown.keyboardSetUnicodeString(stringLength: length, unicodeString: buffer.baseAddress)
            eventUp.keyboardSetUnicodeString(stringLength: length, unicodeString: buffer.baseAddress)
        }
        eventDown.post(tap: .cghidEventTap)
        eventUp.post(tap: .cghidEventTap)
    }

    static func postMouseClick(point: CGPoint) {
        postMouseEvent(type: .leftMouseDown, point: point)
        postMouseEvent(type: .leftMouseUp, point: point)
    }

    static func postMouseLongPress(point: CGPoint, duration: TimeInterval) {
        postMouseEvent(type: .leftMouseDown, point: point)
        Thread.sleep(forTimeInterval: duration)
        postMouseEvent(type: .leftMouseUp, point: point)
    }

    static func postDrag(from start: CGPoint, to end: CGPoint, duration: TimeInterval, steps: Int = 10) {
        postMouseEvent(type: .leftMouseDown, point: start)

        let stepDuration = duration / Double(steps)
        for i in 1...steps {
            let fraction = Double(i) / Double(steps)
            let point = CGPoint(
                x: start.x + (end.x - start.x) * fraction,
                y: start.y + (end.y - start.y) * fraction
            )
            postMouseEvent(type: .leftMouseDragged, point: point)
            Thread.sleep(forTimeInterval: stepDuration)
        }

        postMouseEvent(type: .leftMouseUp, point: end)
    }
}
