import AppKit
import CoreGraphics
import FlyingFox
import Foundation

struct EraseTextHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        let eraseRequest: EraseTextRequest

        do {
            eraseRequest = try decoder.decode(EraseTextRequest.self, from: try await request.bodyData)
        } catch {
            let errorBody = ["error": "Invalid request body: \(error.localizedDescription)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .badRequest,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        let backspaceKeyCode: CGKeyCode = 51

        for _ in 0..<max(0, eraseRequest.charactersToErase) {
            CGEventHelper.postKeyEvent(keyCode: backspaceKeyCode, keyDown: true)
            CGEventHelper.postKeyEvent(keyCode: backspaceKeyCode, keyDown: false)
            try await Task.sleep(nanoseconds: 10_000_000) // 10ms
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
