package com.unip.service;

import com.unip.model.Role;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiConsumer;

@Service
public class FaceService {
    private final FaceRecognizer faceRecognizer;
    private final Map<Integer, String> idToNameMap = new HashMap<>();
    private final Map<Integer, String> idToEmailMap = new HashMap<>();
    private final Map<Integer, Role> idToRoleMap = new HashMap<>();

    private final String FACES_DIR = "faces";
    private final String LABELS_FILE = FACES_DIR + "/labels.txt";

    private final Map<Integer, Integer> recognitionAttempts = new HashMap<>();
    private final Map<Integer, Integer> successfulRecognitions = new HashMap<>();

    private final FaceProcessingHelper helper;

    public FaceService() {
        this.faceRecognizer = LBPHFaceRecognizer.create();
        this.helper = new FaceProcessingHelper(FACES_DIR, LABELS_FILE, faceRecognizer,
                idToNameMap, idToEmailMap, idToRoleMap, recognitionAttempts, successfulRecognitions);
        helper.loadLabels();
        helper.retrainModel();
    }

    public void register(Mat face, String personName, String email, Role role, BiConsumer<String, Role> callback) {
        helper.register(face, personName, email, role, callback);
    }

    public void authenticate(Mat face, BiConsumer<String, Role> callback) {
        helper.authenticate(face, callback);
    }
}