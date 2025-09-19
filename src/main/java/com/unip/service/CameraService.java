package com.unip.service;

import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.util.function.Consumer;

public class CameraService {

    private VideoCapture videoCapture;
    private volatile boolean cameraAtiva = false;
    private Thread cameraThread;

    public void startCamera(Consumer<Mat> frameCallback) {
        stopCamera();
        cameraAtiva = true;
        cameraThread = new Thread(() -> {
            //videoCapture = new VideoCapture(0, opencv_videoio.CAP_DSHOW);
            videoCapture = new VideoCapture(0, opencv_videoio.CAP_V4L2);

            if (!videoCapture.isOpened()) {
                System.err.println("Não foi possível abrir a câmera!");
                return;
            }

            Mat frame = new Mat();
            while (cameraAtiva && videoCapture.read(frame)) {
                frameCallback.accept(frame);
                try { Thread.sleep(33); } catch (InterruptedException e) { break; }
            }
            videoCapture.release();
        });
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    public void stopCamera() {
        cameraAtiva = false;
        if (cameraThread != null) cameraThread.interrupt();
        if (videoCapture != null && videoCapture.isOpened()) videoCapture.release();
    }

    public boolean isCameraActive() {
        return cameraAtiva;
    }

    public Mat captureFrame() {
        if (videoCapture != null && videoCapture.isOpened()) {
            Mat frame = new Mat();
            videoCapture.read(frame);
            return frame;
        }
        return null;
    }
}
