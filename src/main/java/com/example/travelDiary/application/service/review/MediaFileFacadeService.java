package com.example.travelDiary.application.service.review;

import com.example.travelDiary.application.S3.S3Service;
import com.example.travelDiary.domain.model.review.MediaFile;
import com.example.travelDiary.domain.model.review.MediaFileStatus;
import com.example.travelDiary.presentation.dto.request.s3.FinishUploadRequest;
import com.example.travelDiary.presentation.dto.request.s3.PreSignedUploadInitiateRequest;
import com.example.travelDiary.presentation.dto.request.s3.PresignedUrlAbortRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;

import java.net.URL;

import static com.example.travelDiary.application.service.review.MediaFileUtils.guessContentTypeFromName;

@Slf4j
@Service
public class MediaFileFacadeService {
    private final S3Service s3Service;
    private final MediaFileService mediaFileService;
    private final MediaFileDataService mediaFileDataService;
    private final MediaFileRedisService redisService;

    @Autowired
    public MediaFileFacadeService(S3Service s3Service,
                                  MediaFileService mediaFileService, MediaFileDataService mediaFileDataService,
                                  MediaFileRedisService redisService) {
        this.s3Service = s3Service;
        this.mediaFileService = mediaFileService;
        this.mediaFileDataService = mediaFileDataService;
        this.redisService = redisService;
    }


    @Transactional
    public URL getMediaFile(String s3Key) {
        return s3Service.createPresignedUrlForGet(s3Key);
    }

    public URL uploadMediaFile(PreSignedUploadInitiateRequest request) {
        String key = mediaFileDataService.saveMediaFile(request);
        log.info("media file key on upload {}", key);
        return s3Service.createPresignedUrlForPut(key, guessContentTypeFromName(request), request.getFileSize());
    }

    public DeleteObjectResponse deleteMediaFile(String objectKey) {
        DeleteObjectResponse deleteObjectResponse = s3Service.deleteS3Object(objectKey);
        mediaFileDataService.deleteMediaMetadata(objectKey);
        return deleteObjectResponse;
    }

    @Transactional
    public URL uploadMediaFileMultipart(PreSignedUploadInitiateRequest request) {
        String key = mediaFileDataService.saveMediaFile(request);
        return s3Service.createMultipartUploadRequest(key, guessContentTypeFromName(request));
    }

    @Transactional
    public String uploadMediaFileMultipartComplete(FinishUploadRequest finishUploadRequest) {
        s3Service.completeMultipartUpload(finishUploadRequest.getObjectKey(), finishUploadRequest);
        markMediaUploadFinished(finishUploadRequest.getObjectKey());
        return "completed";
    }

    @Transactional
    public String abortUpload(PresignedUrlAbortRequest request) {
        s3Service.abortUpload(request.getObjectKey(), request);
        redisService.deleteMediaFile(request.getObjectKey());
        return "aborted";
    }

    @Transactional
    public MediaFile reconstructObjectFromKey(String objectKey) {
        String[] parts = objectKey.split("/");
        if (parts.length < 7) {
            throw new IllegalArgumentException("Invalid key format");
        }
        String fileSize = parts[parts.length - 3];
        String contentType = parts[parts.length - 4];
        String sanitizedFileName = parts[parts.length - 5];

        MediaFile mediaFile = MediaFile.builder()
                .mediaFileStatus(MediaFileStatus.UPLOADED)
                .originalFileName(sanitizedFileName)
                .sizeBytes(Long.valueOf(fileSize))
                .s3Key(objectKey)
                .contentType(contentType)
                .build();

        return mediaFile;
    }

    @Transactional
    public void markMediaUploadFinished(String objectKey) {
        log.info("mark media upload finished {}", objectKey);
        MediaFile mediaFile = redisService.getMediaFile(objectKey);

        if (mediaFile == null) {
            mediaFile = mediaFileService.findByS3Key(objectKey);
            if (mediaFile == null) {
                MediaFile reconstructed = reconstructObjectFromKey(objectKey);
                log.info("reconstructed mediaFile from ObjectKey {} giving entity {}", objectKey, reconstructed);
                mediaFileService.saveMediaFile(reconstructed);
                return;
            }
        } else {
            redisService.deleteMediaFile(objectKey);
        }

        mediaFile.setMediaFileStatus(MediaFileStatus.UPLOADED);
        mediaFileService.saveMediaFile(mediaFile);
    }

    @Transactional
    public MediaFileStatus getUploadStatus(String key) {
        MediaFile mediaFile = redisService.getMediaFile(key);
        if (mediaFile != null) {
            return mediaFile.getMediaFileStatus();
        }

        mediaFile = mediaFileService.findByS3Key(key);
        if (mediaFile != null) {
            return mediaFile.getMediaFileStatus();
        }

        return MediaFileStatus.FAILED;
    }
}
