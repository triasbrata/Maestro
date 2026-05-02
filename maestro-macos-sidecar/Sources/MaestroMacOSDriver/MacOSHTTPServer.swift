import FlyingFox
import Foundation
import os

struct MacOSHTTPServer {
    let port: UInt16

    private let logger = Logger(
        subsystem: "com.maestro.macos-driver",
        category: String(describing: Self.self)
    )

    func start() async throws {
        let server = HTTPServer(
            address: try .inet(ip4: "127.0.0.1", port: port),
            timeout: 30
        )

        for route in Route.allCases {
            let handler = await RouteHandlerFactory.createRouteHandler(route: route)
            await server.appendRoute(route.toHTTPRoute(), to: handler)
        }

        logger.log("Maestro macOS Driver starting on 127.0.0.1:\(port, privacy: .public)")
        try await server.run()
    }
}
