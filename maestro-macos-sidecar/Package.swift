// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "MaestroMacOSDriver",
    platforms: [.macOS(.v13)],
    dependencies: [
        .package(url: "https://github.com/swhitty/FlyingFox.git", exact: "0.20.0"),
    ],
    targets: [
        .executableTarget(
            name: "MaestroMacOSDriver",
            dependencies: ["FlyingFox"],
            path: "Sources/MaestroMacOSDriver"
        ),
    ]
)
