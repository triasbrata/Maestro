import Foundation

struct SetPermissionsRequest: Codable {
    let permissions: [String: String]
}
