import AppKit
import CoreGraphics
import FlyingFox
import Foundation

struct TouchHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        let touchRequest: TouchRequest

        do {
            touchRequest = try decoder.decode(TouchRequest.self, from: try await request.bodyData)
        } catch {
            let errorBody = ["error": "Invalid request body: \(error.localizedDescription)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .badRequest,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        let point = CGPoint(x: touchRequest.x, y: touchRequest.y)
        let clamped = ScreenPointMapper.clampToScreen(point)

        if let duration = touchRequest.duration, duration > 0 {
            CGEventHelper.postMouseLongPress(point: clamped, duration: duration)
        } else {
            CGEventHelper.postMouseClick(point: clamped)
        }

        let responseBody = ["status": "ok"]
        let data = try JSONEncoder().encode(responseBody)
        return HTTPResponse(
            statusCode: .ok,
            headers: [.contentType: "application/json"],
            body: data
        )
    }
}
