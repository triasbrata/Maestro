import Foundation

struct SwipeRequest: Codable {
    let startX: Double
    let startY: Double
    let endX: Double
    let endY: Double
    let duration: Double
    let appIds: [String]?
}
