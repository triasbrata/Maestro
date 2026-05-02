import AppKit
import CoreGraphics
import FlyingFox
import Foundation

@MainActor
struct ScreenshotHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let compressed = request.query["compressed"] == "true"

        guard let cgImage = CGDisplayCreateImage(CGMainDisplayID()) else {
            return HTTPResponse(
                statusCode: .internalServerError,
                headers: [.contentType: "application/json"],
                body: Data("{\"error\":\"Failed to capture screenshot. Grant Screen Recording permission in System Settings.\"}".utf8)
            )
        }

        let bitmapRep = NSBitmapImageRep(cgImage: cgImage)
        bitmapRep.size = NSSize(width: cgImage.width, height: cgImage.height)

        let imageData: Data?
        if compressed {
            imageData = bitmapRep.representation(
                using: .jpeg,
                properties: [.compressionFactor: 0.5]
            )
        } else {
            imageData = bitmapRep.representation(
                using: .png,
                properties: [:]
            )
        }

        guard let data = imageData else {
            return HTTPResponse(
                statusCode: .internalServerError,
                headers: [.contentType: "application/json"],
                body: Data("{\"error\":\"Failed to encode screenshot\"}".utf8)
            )
        }

        let contentType: String = compressed ? "image/jpeg" : "image/png"
        return HTTPResponse(
            statusCode: .ok,
            headers: [.contentType: contentType],
            body: data
        )
    }
}
