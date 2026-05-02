import AppKit
import FlyingFox
import Foundation

struct RunningAppHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        let runningAppRequest: RunningAppRequest

        do {
            runningAppRequest = try decoder.decode(RunningAppRequest.self, from: try await request.bodyData)
        } catch {
            let errorBody = ["error": "Invalid request body: \(error.localizedDescription)"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .badRequest,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        let foregroundBundleId = NSWorkspace.shared.frontmostApplication?.bundleIdentifier

        if let appId = foregroundBundleId, runningAppRequest.appIds.contains(appId) {
            let response = RunningAppResponse(appId: appId)
            let data = try JSONEncoder().encode(response)
            return HTTPResponse(
                statusCode: .ok,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        let response = RunningAppResponse(appId: "")
        let data = try JSONEncoder().encode(response)
        return HTTPResponse(
            statusCode: .ok,
            headers: [.contentType: "application/json"],
            body: data
        )
    }
}
