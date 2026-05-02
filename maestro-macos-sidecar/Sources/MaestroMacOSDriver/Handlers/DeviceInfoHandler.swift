import AppKit
import FlyingFox
import Foundation

@MainActor
struct DeviceInfoHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        guard let screen = NSScreen.main else {
            return HTTPResponse(
                statusCode: .internalServerError,
                headers: [.contentType: "application/json"],
                body: Data("{\"error\":\"No main screen found\"}".utf8)
            )
        }

        let scale = screen.backingScaleFactor
        let info = DeviceInfo(
            width: Int(screen.frame.width * scale),
            height: Int(screen.frame.height * scale),
            osVersion: ProcessInfo.processInfo.operatingSystemVersionString,
            screenScale: Double(scale)
        )

        let data = try JSONEncoder().encode(info)
        return HTTPResponse(
            statusCode: .ok,
            headers: [.contentType: "application/json"],
            body: data
        )
    }
}
