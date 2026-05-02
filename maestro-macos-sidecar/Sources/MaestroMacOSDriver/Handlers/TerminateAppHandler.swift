import AppKit
import FlyingFox
import Foundation

struct TerminateAppHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        let terminateRequest: TerminateAppRequest

        do {
            terminateRequest = try decoder.decode(TerminateAppRequest.self, from: try await request.bodyData)
        } catch {
            let errorBody = ["error": "Invalid request body: \(error.localizedDescription)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .badRequest,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        guard let app = NSWorkspace.shared.runningApplications.first(where: {
            $0.bundleIdentifier == terminateRequest.appId
        }) else {
            let errorBody = ["error": "Application not running: \(terminateRequest.appId)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .notFound,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        let success = app.terminate()
        guard success else {
            let errorBody = ["error": "Failed to terminate application: \(terminateRequest.appId)"]
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
