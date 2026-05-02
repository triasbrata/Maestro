import AppKit
import CoreGraphics
import FlyingFox
import Foundation

struct SwipeHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        let swipeRequest: SwipeRequest

        do {
            swipeRequest = try decoder.decode(SwipeRequest.self, from: try await request.bodyData)
        } catch {
            let errorBody = ["error": "Invalid request body: \(error.localizedDescription)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .badRequest,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        let start = CGPoint(x: swipeRequest.startX, y: swipeRequest.startY)
        let end = CGPoint(x: swipeRequest.endX, y: swipeRequest.endY)
        let duration = swipeRequest.duration

        CGEventHelper.postDrag(from: start, to: end, duration: duration)

        let responseBody = ["status": "ok"]
        let data = try JSONEncoder().encode(responseBody)
        return HTTPResponse(
            statusCode: .ok,
            headers: [.contentType: "application/json"],
            body: data
        )
    }
}
