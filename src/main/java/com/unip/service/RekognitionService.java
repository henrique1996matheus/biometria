package com.unip.service;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
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
                .credentialsProvider(ProfileCredentialsProvider.create("pessoal"))
                .build();
    }

    public void createCollection() {
        CreateCollectionRequest request = CreateCollectionRequest.builder()
                .collectionId(collectionId)
                .build();
        rekClient.createCollection(request);
    }

    public void registerFace(byte[] imageBytes, String externalId) {
        try {
            IndexFacesRequest request = IndexFacesRequest.builder()
                    .collectionId(collectionId)
                    .image(Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build())
                    .externalImageId(externalId)
                    .build();

            IndexFacesResponse response = rekClient.indexFaces(request);

            System.out.println("Faces cadastradas: " + response.faceRecords().size());
            for (FaceRecord record : response.faceRecords()) {
                System.out.println("FaceId: " + record.face().faceId());
                System.out.println("ExternalImageId: " + record.face().externalImageId());
                System.out.println("BoundingBox: " + record.face().boundingBox());
            }

            if (!response.unindexedFaces().isEmpty()) {
                System.out.println("Faces não indexadas:");
                response.unindexedFaces().forEach(face ->
                    System.out.println("Razão: " + face.reasons())
                );
            }

        } catch (RekognitionException e) {
            System.err.println("Erro ao cadastrar face: " + e.awsErrorDetails().errorMessage());
        }
    }

    public SearchFacesByImageResponse findFace(byte[] imageBytes) {
        SearchFacesByImageRequest request = SearchFacesByImageRequest.builder()
                .collectionId(collectionId)
                .image(Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build())
                .build();
        return rekClient.searchFacesByImage(request);
    }
}
