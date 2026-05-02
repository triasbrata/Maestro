import FlyingFox

enum Route: String, CaseIterable {
    case status
    case deviceInfo
    case runningApp
    case screenshot
    case launchApp
    case terminateApp

    // Phase 3 stubs (registered but return 501 Not Implemented):
    case viewHierarchy
    case touch
    case swipe
    case inputText
    case pressKey
    case eraseText
    case isScreenStatic
    case setPermissions

    func toHTTPRoute() -> HTTPRoute {
        HTTPRoute(rawValue)
    }
}
