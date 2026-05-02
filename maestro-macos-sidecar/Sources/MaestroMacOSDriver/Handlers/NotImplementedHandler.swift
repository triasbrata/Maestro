import FlyingFox
import Foundation

struct NotImplementedHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let responseBody = ["error": "Not implemented"]
        let data = try JSONEncoder().encode(responseBody)
        return HTTPResponse(
            statusCode: .notImplemented,
            headers: [.contentType: "application/json"],
            body: data
        )
    }
}
