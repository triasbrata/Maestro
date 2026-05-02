import AppKit
import CoreGraphics
import FlyingFox
import Foundation

struct InputTextHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        let inputRequest: InputTextRequest

        do {
            inputRequest = try decoder.decode(InputTextRequest.self, from: try await request.bodyData)
        } catch {
            let errorBody = ["error": "Invalid request body: \(error.localizedDescription)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .badRequest,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        for char in inputRequest.text {
            CGEventHelper.postTextCharacter(char)
            // Small delay between characters to allow the application to process
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
