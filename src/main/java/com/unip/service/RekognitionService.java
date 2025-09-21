package com.unip.service;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

public class RekognitionService {

    private final RekognitionClient rekClient;
    private final String collectionId = "aps-biometrics"; // nome da coleção

    public RekognitionService() {
        rekClient = RekognitionClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    public void createCollection() {
        CreateCollectionRequest request = CreateCollectionRequest.builder()
                .collectionId(collectionId)
                .build();
        rekClient.createCollection(request);
    }

    public void registerFace(byte[] imageBytes, String externalId) {
        IndexFacesRequest request = IndexFacesRequest.builder()
                .collectionId(collectionId)
                .image(Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build())
                .externalImageId(externalId)
                .build();
        rekClient.indexFaces(request);
    }

    public SearchFacesByImageResponse findFace(byte[] imageBytes) {
        SearchFacesByImageRequest request = SearchFacesByImageRequest.builder()
                .collectionId(collectionId)
                .image(Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build())
                .build();
        return rekClient.searchFacesByImage(request);
    }
}
