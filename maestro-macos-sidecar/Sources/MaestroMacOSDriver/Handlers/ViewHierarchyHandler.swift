import AppKit
import CoreGraphics
import FlyingFox
import Foundation

@MainActor
struct ViewHierarchyHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()

        do {
            _ = try decoder.decode(ViewHierarchyRequest.self, from: try await request.bodyData)
        } catch {
            let errorBody = ["error": "Invalid request body: \(error.localizedDescription)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .badRequest,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        guard let appElement = AccessibilityHelper.frontmostApplicationElement() else {
            let errorBody = ["error": "No foreground application found"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .internalServerError,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        // Start tree walk from the focused window, falling back to the app root
        let rootElement: AXUIElement
        if let window = AccessibilityHelper.focusedWindow(for: appElement) {
            rootElement = window
        } else {
            rootElement = appElement
        }

        guard let axElement = AccessibilityHelper.buildAXElementTree(from: rootElement) else {
            let errorBody = ["error": "Failed to build accessibility tree"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .internalServerError,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        let response = ViewHierarchyResponse(
            axElement: axElement,
            depth: axElement.depth()
        )

        let data = try JSONEncoder().encode(response)
        return HTTPResponse(
            statusCode: .ok,
            headers: [.contentType: "application/json"],
            body: data
        )
    }
}
