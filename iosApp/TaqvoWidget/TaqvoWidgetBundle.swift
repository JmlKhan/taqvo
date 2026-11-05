import WidgetKit
import SwiftUI

@main
struct TaqvoWidgetBundle: WidgetBundle {
    var body: some Widget {
        TaqvoHomeWidget()
        TaqvoLockScreenWidget()
        TaqvoAyahWidget()
        TaqvoDuaWidget()
    }
}
