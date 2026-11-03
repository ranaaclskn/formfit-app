import AVFoundation
import ComposeApp
import MediaPipeTasksVision
import UIKit

/// Kotlin'deki PoseCameraFactory arayüzünün iOS uygulaması.
final class PoseCameraFactoryImpl: NSObject, PoseCameraFactory {
    func createCameraView(frontCamera: Bool, sink: PoseSink) -> UIView {
        let view = PoseCameraView(frontCamera: frontCamera, sink: sink)
        view.start()
        return view
    }

    func stopCameraView(view: UIView) {
        (view as? PoseCameraView)?.stop()
    }
}

/// Kamera önizlemesi + MediaPipe Pose Landmarker.
/// Kareler dik (portrait) ve ön kamerada aynalanmış gelir; böylece noktalar önizlemeyle örtüşür.
final class PoseCameraView: UIView, AVCaptureVideoDataOutputSampleBufferDelegate, PoseLandmarkerLiveStreamDelegate {
    private let session = AVCaptureSession()
    private let previewLayer: AVCaptureVideoPreviewLayer
    private let queue = DispatchQueue(label: "formfit.camera")
    private let frontCamera: Bool
    private let sink: PoseSink
    private var landmarker: PoseLandmarker?
    private var lastTimestamp = 0
    private var imageWidth = 0
    private var imageHeight = 0

    init(frontCamera: Bool, sink: PoseSink) {
        self.frontCamera = frontCamera
        self.sink = sink
        previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = .resizeAspectFill
        super.init(frame: .zero)
        backgroundColor = .black
        layer.addSublayer(previewLayer)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) kullanılmıyor") }

    override func layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds
    }

    func start() {
        UIApplication.shared.isIdleTimerDisabled = true
        AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
            guard granted, let self else { return }
            self.queue.async {
                self.configure()
                self.session.startRunning()
            }
        }
    }

    func stop() {
        UIApplication.shared.isIdleTimerDisabled = false
        queue.async { [session] in session.stopRunning() }
    }

    private func configure() {
        session.beginConfiguration()
        // 4:3; analiz edilen kare ile önizleme aynı oranda olmalı
        session.sessionPreset = .vga640x480
        let position: AVCaptureDevice.Position = frontCamera ? .front : .back
        guard
            let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position),
            let input = try? AVCaptureDeviceInput(device: device),
            session.canAddInput(input)
        else {
            session.commitConfiguration()
            return
        }
        session.addInput(input)

        let output = AVCaptureVideoDataOutput()
        output.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32BGRA]
        output.alwaysDiscardsLateVideoFrames = true
        output.setSampleBufferDelegate(self, queue: queue)
        guard session.canAddOutput(output) else {
            session.commitConfiguration()
            return
        }
        session.addOutput(output)
        if let connection = output.connection(with: .video) {
            if connection.isVideoOrientationSupported { connection.videoOrientation = .portrait }
            if frontCamera && connection.isVideoMirroringSupported { connection.isVideoMirrored = true }
        }
        session.commitConfiguration()

        guard let modelPath = Bundle.main.path(forResource: "pose_landmarker_full", ofType: "task") else {
            print("FormFit: pose_landmarker_full.task bulunamadı")
            return
        }
        let options = PoseLandmarkerOptions()
        options.baseOptions.modelAssetPath = modelPath
        options.runningMode = .liveStream
        options.numPoses = 1
        options.minPoseDetectionConfidence = 0.5
        options.minPosePresenceConfidence = 0.5
        options.minTrackingConfidence = 0.5
        options.poseLandmarkerLiveStreamDelegate = self
        landmarker = try? PoseLandmarker(options: options)
    }

    func captureOutput(_ output: AVCaptureOutput, didOutput sampleBuffer: CMSampleBuffer, from connection: AVCaptureConnection) {
        guard let landmarker, let image = try? MPImage(sampleBuffer: sampleBuffer) else { return }
        let seconds = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sampleBuffer))
        let timestamp = Int(seconds * 1000)
        // MediaPipe zaman damgalarının kesin artan olmasını ister
        guard timestamp > lastTimestamp else { return }
        lastTimestamp = timestamp
        if let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) {
            imageWidth = CVPixelBufferGetWidth(pixelBuffer)
            imageHeight = CVPixelBufferGetHeight(pixelBuffer)
        }
        try? landmarker.detectAsync(image: image, timestampInMilliseconds: timestamp)
    }

    func poseLandmarker(
        _ poseLandmarker: PoseLandmarker,
        didFinishDetection result: PoseLandmarkerResult?,
        timestampInMilliseconds: Int,
        error: Error?
    ) {
        let width = imageWidth
        let height = imageHeight
        guard let points = result?.landmarks.first, !points.isEmpty else {
            DispatchQueue.main.async { [sink] in sink.submitEmpty() }
            return
        }
        let values = KotlinFloatArray(size: Int32(points.count * 4))
        for (i, p) in points.enumerated() {
            let base = Int32(i * 4)
            values.set(index: base, value: p.x)
            values.set(index: base + 1, value: p.y)
            values.set(index: base + 2, value: p.z)
            values.set(index: base + 3, value: p.visibility?.floatValue ?? 0)
        }
        DispatchQueue.main.async { [sink] in
            sink.submit(values: values, imageWidth: Int32(width), imageHeight: Int32(height), timestampMs: Int64(timestampInMilliseconds))
        }
    }
}
