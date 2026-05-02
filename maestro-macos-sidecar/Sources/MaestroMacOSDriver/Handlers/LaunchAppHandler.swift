import AppKit
import FlyingFox
import Foundation

struct LaunchAppHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        let launchRequest: LaunchAppRequest

        do {
            launchRequest = try decoder.decode(LaunchAppRequest.self, from: try await request.bodyData)
        } catch {
            let errorBody = ["error": "Invalid request body: \(error.localizedDescription)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .badRequest,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        guard let appURL = NSWorkspace.shared.urlForApplication(withBundleIdentifier: launchRequest.appId) else {
            let errorBody = ["error": "Application not found for bundle ID: \(launchRequest.appId)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .notFound,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        let config = NSWorkspace.OpenConfiguration()
        config.activates = true
        config.addsToRecentItems = true

        do {
            try await NSWorkspace.shared.openApplication(at: appURL, configuration: config)
        } catch {
            let errorBody = ["error": "Failed to launch application: \(error.localizedDescription)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .internalServerError,
                headers: [.contentType: "application/json"],
                body: data
            )
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
