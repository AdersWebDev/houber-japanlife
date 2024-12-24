package com.lee.osakacity.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.lee.osakacity.infra.entity.File;
import com.lee.osakacity.infra.repository.S3FileRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    @Value("${aws.s3.bucket-name}")
    private String bucketName;
    private final AmazonS3 amazonS3;
    private final S3FileRepo s3FileRepo;

    /**
     * 파일 업로드 메서드
     * @param file 업로드할 파일
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadFile(MultipartFile file) {
        try {
            // 파일 이름 설정 (UUID 활용)
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

            // 메타데이터 설정
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            // S3에 파일 업로드
            amazonS3.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata));
            // 업로드된 파일의 URL 반환
            String fileUrl = amazonS3.getUrl(bucketName, fileName).toString();
            File fileEntity = File.builder()
                    .createDate(LocalDateTime.now())
                    .modifiedDate(LocalDateTime.now())
                    .fileUrl(fileUrl)
                    .fileName(fileName)
                    .build();

            s3FileRepo.save(fileEntity);

            return fileUrl;

        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    public void deleteFile(String fileName) {
        amazonS3.deleteObject(bucketName, fileName);
    }
}
