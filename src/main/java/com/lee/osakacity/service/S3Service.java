package com.lee.osakacity.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.lee.osakacity.dto.restful.ImgResponse;
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
    public ImgResponse uploadFile(MultipartFile file) {
        try {
            // 파일 이름 설정 (UUID 활용)
            String fileName =file.getOriginalFilename() + "-" + UUID.randomUUID();

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
                    .fileUrl(fileUrl)
                    .fileName(fileName)
                    .alt(this.getOriginFileNameWithOutType(file.getOriginalFilename()))
                    .isUsed(false)
                    .build();

            s3FileRepo.save(fileEntity);

            return new ImgResponse(fileEntity);

        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }
    private String getOriginFileNameWithOutType(String originalFilename){
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) { // 확장자가 존재할 경우
            return originalFilename.substring(0, dotIndex);
        } else {
            return originalFilename; // 확장자가 없을 경우 전체 이름 반환
        }
    }
    public void deleteFile(String fileName) {
        amazonS3.deleteObject(bucketName, fileName);
    }
}
