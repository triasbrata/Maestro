import Foundation

struct MacOSAXElement: Codable {
    struct Frame: Codable, Equatable {
        let x: Double
        let y: Double
        let width: Double
        let height: Double

        static let zero = Frame(x: 0, y: 0, width: 0, height: 0)
    }

    let role: String
    let label: String?
    let value: String?
    let identifier: String?
    let title: String?
    let placeholderValue: String?
    let enabled: Bool?
    let focused: Bool?
    let selected: Bool?
    let frame: Frame
    let children: [MacOSAXElement]?

    init(
        role: String,
        label: String? = nil,
        value: String? = nil,
        identifier: String? = nil,
        title: String? = nil,
        placeholderValue: String? = nil,
        enabled: Bool? = nil,
        focused: Bool? = nil,
        selected: Bool? = nil,
        frame: Frame = .zero,
        children: [MacOSAXElement]? = nil
    ) {
        self.role = role
        self.label = label
        self.value = value
        self.identifier = identifier
        self.title = title
        self.placeholderValue = placeholderValue
        self.enabled = enabled
        self.focused = focused
        self.selected = selected
        self.frame = frame
        self.children = children
    }

    func depth() -> Int {
        guard let children = children else { return 1 }
        let max = children.map { $0.depth() + 1 }.max()
        return max ?? 1
    }
}
