import AppKit
import CoreGraphics
import FlyingFox
import Foundation

struct PressKeyHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        let pressRequest: PressKeyRequest

        do {
            pressRequest = try decoder.decode(PressKeyRequest.self, from: try await request.bodyData)
        } catch {
            let errorBody = ["error": "Invalid request body: \(error.localizedDescription)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .badRequest,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        guard let keyCode = KeyCodeMapper.keyCode(for: pressRequest.name) else {
            let errorBody = ["error": "Unknown key name: \(pressRequest.name)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .badRequest,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        CGEventHelper.postKeyEvent(keyCode: keyCode, keyDown: true)
        CGEventHelper.postKeyEvent(keyCode: keyCode, keyDown: false)

        let responseBody = ["status": "ok"]
        let data = try JSONEncoder().encode(responseBody)
        return HTTPResponse(
            statusCode: .ok,
            headers: [.contentType: "application/json"],
            body: data
        )
    }
}
