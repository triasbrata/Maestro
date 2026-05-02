import AppKit
import CoreGraphics
import Foundation

enum AccessibilityHelper {

    static let maxDepth = 50

    /// Returns the AXUIElement for the frontmost application.
    static func frontmostApplicationElement() -> AXUIElement? {
        guard let app = NSWorkspace.shared.frontmostApplication else { return nil }
        return AXUIElementCreateApplication(app.processIdentifier)
    }

    /// Returns the AXUIElement for the focused window of the given application element.
    static func focusedWindow(for app: AXUIElement) -> AXUIElement? {
        var ref: CFTypeRef?
        let error = AXUIElementCopyAttributeValue(
            app,
            kAXFocusedWindowAttribute as CFString,
            &ref
        )
        guard error == .success, let window = ref else { return nil }
        return (window as! AXUIElement)
    }

    // MARK: - Attribute readers

    static func getString(_ element: AXUIElement, _ attribute: String) -> String? {
        var ref: CFTypeRef?
        let error = AXUIElementCopyAttributeValue(element, attribute as CFString, &ref)
        guard error == .success, let value = ref else { return nil }
        if let string = value as? String {
            return string
        }
        // Some AX attributes return AXValue or other types; try CFString
        if CFGetTypeID(value) == CFStringGetTypeID() {
            return value as? String
        }
        return nil
    }

    static func getBool(_ element: AXUIElement, _ attribute: String) -> Bool? {
        var ref: CFTypeRef?
        let error = AXUIElementCopyAttributeValue(element, attribute as CFString, &ref)
        guard error == .success, let value = ref else { return nil }
        if let bool = value as? Bool {
            return bool
        }
        if CFGetTypeID(value) == CFBooleanGetTypeID() {
            return CFBooleanGetValue((value as! CFBoolean))
        }
        if let number = value as? NSNumber {
            return number.boolValue
        }
        return nil
    }

    static func getFrame(_ element: AXUIElement) -> MacOSAXElement.Frame? {
        var positionRef: CFTypeRef?
        var sizeRef: CFTypeRef?

        let posErr = AXUIElementCopyAttributeValue(element, kAXPositionAttribute as CFString, &positionRef)
        let sizeErr = AXUIElementCopyAttributeValue(element, kAXSizeAttribute as CFString, &sizeRef)

        guard posErr == .success, sizeErr == .success,
              let posVal = positionRef, let sizeVal = sizeRef else {
            return nil
        }

        var point = CGPoint.zero
        var size = CGSize.zero

        let gotPoint = AXValueGetValue(posVal as! AXValue, .cgPoint, &point)
        let gotSize = AXValueGetValue(sizeVal as! AXValue, .cgSize, &size)

        guard gotPoint, gotSize else { return nil }

        return MacOSAXElement.Frame(
            x: Double(point.x),
            y: Double(point.y),
            width: Double(size.width),
            height: Double(size.height)
        )
    }

    static func getChildren(_ element: AXUIElement) -> [AXUIElement]? {
        var ref: CFTypeRef?
        let error = AXUIElementCopyAttributeValue(element, kAXChildrenAttribute as CFString, &ref)
        guard error == .success, let array = ref as? [AXUIElement] else { return nil }
        return array
    }

    // MARK: - Tree walk

    static func buildAXElementTree(from element: AXUIElement, depth: Int = 0) -> MacOSAXElement? {
        guard depth < maxDepth else { return nil }

        let role = getString(element, kAXRoleAttribute) ?? "Unknown"
        let label = getString(element, kAXDescriptionAttribute)
        let value = getString(element, kAXValueAttribute)
        let identifier = getString(element, kAXIdentifierAttribute)
        let title = getString(element, kAXTitleAttribute)
        let placeholderValue = getString(element, kAXHelpAttribute)
        let enabled = getBool(element, kAXEnabledAttribute)
        let focused = getBool(element, kAXFocusedAttribute)
        let selected = getBool(element, kAXSelectedAttribute)
        let frame = getFrame(element) ?? .zero

        let rawChildren = getChildren(element)
        let childElements: [MacOSAXElement]?
        if let rawChildren = rawChildren {
            childElements = rawChildren.compactMap { buildAXElementTree(from: $0, depth: depth + 1) }
        } else {
            childElements = nil
        }

        return MacOSAXElement(
            role: role,
            label: label,
            value: value,
            identifier: identifier,
            title: title,
            placeholderValue: placeholderValue,
            enabled: enabled,
            focused: focused,
            selected: selected,
            frame: frame,
            children: childElements
        )
    }
}
