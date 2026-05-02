import Foundation
import FlyingFox

let port: UInt16 = ProcessInfo.processInfo.environment["PORT"].flatMap(UInt16.init) ?? 22088

let server = MacOSHTTPServer(port: port)
try await server.start()
