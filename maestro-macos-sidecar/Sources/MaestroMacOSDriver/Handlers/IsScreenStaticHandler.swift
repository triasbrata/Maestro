import AppKit
import CoreGraphics
import FlyingFox
import Foundation

struct IsScreenStaticHandler: HTTPHandler {
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        // Capture two screenshots 500ms apart and compare pixel data
        guard let firstCGImage = CGDisplayCreateImage(CGMainDisplayID()) else {
            let errorBody = ["error": "Failed to capture first screenshot"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .internalServerError,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        try await Task.sleep(nanoseconds: 500_000_000) // 500ms

        guard let secondCGImage = CGDisplayCreateImage(CGMainDisplayID()) else {
            let errorBody = ["error": "Failed to capture second screenshot"]
            let data = try JSONEncoder().encode(errorBody)
            return HTTPResponse(
                statusCode: .internalServerError,
                headers: [.contentType: "application/json"],
                body: data
            )
        }

        let isStatic = compareImages(lhs: firstCGImage, rhs: secondCGImage)

        let response = IsScreenStaticResponse(isStatic: isStatic)
        let data = try JSONEncoder().encode(response)
        return HTTPResponse(
            statusCode: .ok,
            headers: [.contentType: "application/json"],
            body: data
        )
    }

    /// Compares two CGImages by comparing their raw pixel data.
    private func compareImages(lhs: CGImage, rhs: CGImage) -> Bool {
        guard lhs.width == rhs.width, lhs.height == rhs.height else {
            return false
        }

        let lhsData = extractPixelData(from: lhs)
        let rhsData = extractPixelData(from: rhs)

        guard let lhsData = lhsData, let rhsData = rhsData else {
            return false
        }

        return lhsData == rhsData
    }

    /// Extracts raw RGBA pixel data from a CGImage.
    private func extractPixelData(from image: CGImage) -> Data? {
        let width = image.width
        let height = image.height
        let bytesPerPixel = 4
        let bytesPerRow = bytesPerPixel * width
        let totalBytes = bytesPerRow * height

        var pixelData = [UInt8](repeating: 0, count: totalBytes)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let bitmapInfo = CGImageAlphaInfo.premultipliedFirst.rawValue

        guard let context = CGContext(
            data: &pixelData,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: bytesPerRow,
            space: colorSpace,
            bitmapInfo: bitmapInfo
        ) else {
            return nil
        }

        context.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
        return Data(pixelData)
    }
}
