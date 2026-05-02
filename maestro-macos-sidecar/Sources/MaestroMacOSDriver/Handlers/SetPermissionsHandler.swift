import AppKit
import CoreGraphics
import FlyingFox
import Foundation

struct SetPermissionsHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        // macOS permissions (Accessibility, Screen Recording, etc.) are managed
        // via System Settings > Privacy & Security. This endpoint is a stub that
        // acknowledges the request and returns a helpful message.

        let responseBody: [String: String] = [
            "status": "ok",
            "message": "macOS permissions are managed via System Settings > Privacy & Security. Grant Accessibility and Screen Recording permissions to the terminal or application running the sidecar."
        ]

        let data = try JSONEncoder().encode(responseBody)
        return HTTPResponse(
            statusCode: .ok,
            headers: [.contentType: "application/json"],
            body: data
        )
    }
}
