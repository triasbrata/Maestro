import Foundation

struct ViewHierarchyResponse: Codable {
    let axElement: MacOSAXElement
    let depth: Int
}
