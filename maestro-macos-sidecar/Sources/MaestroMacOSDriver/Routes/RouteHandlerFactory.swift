import Foundation
import FlyingFox

class RouteHandlerFactory {
    @MainActor
    class func createRouteHandler(route: Route) -> HTTPHandler {
        switch route {
        case .status:
            return StatusHandler()
        case .deviceInfo:
            return DeviceInfoHandler()
        case .runningApp:
            return RunningAppHandler()
        case .screenshot:
            return ScreenshotHandler()
        case .launchApp:
            return LaunchAppHandler()
        case .terminateApp:
            return TerminateAppHandler()
        case .viewHierarchy:
            return ViewHierarchyHandler()
        case .touch:
            return TouchHandler()
        case .swipe:
            return SwipeHandler()
        case .inputText:
            return InputTextHandler()
        case .pressKey:
            return PressKeyHandler()
        case .eraseText:
            return EraseTextHandler()
        case .isScreenStatic:
            return IsScreenStaticHandler()
        case .setPermissions:
            return SetPermissionsHandler()
        }
    }
}
