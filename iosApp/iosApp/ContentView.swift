import SwiftUI
import UIKit
import ComposeApp

// Arayüzün tamamı Kotlin'de (composeApp/src/commonMain); burada ekrana yerleştirilir ve kamera verilir
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(cameraFactory: PoseCameraFactoryImpl())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .preferredColorScheme(.dark)
    }
}
