package com.example.pizza.service.logic;

import com.cloudinary.Cloudinary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadImpl implements FileUpload {

    private final Cloudinary cloudinary;

    @Override
    @Async("fileUploadTaskExecutor")
    public CompletableFuture<String> uploadFileAsync(MultipartFile multipartFile) {
        try {
            log.info("Async file upload started: filename={}, size={}",
                    multipartFile.getOriginalFilename(), multipartFile.getSize());

            Map<String, String> options = new HashMap<>();
            options.put("folder", "pizza");
            options.put("public_id", UUID.randomUUID().toString());

            String url = cloudinary.uploader()
                    .upload(multipartFile.getBytes(), options)
                    .get("url")
                    .toString();

            log.info("Async file upload completed: filename={}, url={}",
                    multipartFile.getOriginalFilename(), url);

            return CompletableFuture.completedFuture(url);

        } catch (Exception e) {
            log.error("Async file upload failed: filename={}, error={}",
                    multipartFile.getOriginalFilename(), e.getMessage(), e);
            return CompletableFuture.failedFuture(
                    new IOException("Cloudinary upload failed: " + e.getMessage(), e)
            );
        }
    }

    @Override
    public String uploadFile(MultipartFile multipartFile) throws IOException {
        try {
            log.info("Sync file upload started: filename={}", multipartFile.getOriginalFilename());

            Map<String, String> options = new HashMap<>();
            options.put("folder", "pizza");
            options.put("public_id", UUID.randomUUID().toString());

            String url = cloudinary.uploader()
                    .upload(multipartFile.getBytes(), options)
                    .get("url")
                    .toString();

            log.info("Sync file upload completed: url={}", url);
            return url;

        } catch (Exception e) {
            log.error("Sync file upload failed: error={}", e.getMessage(), e);
            throw new IOException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    @Async("fileUploadTaskExecutor")
    public CompletableFuture<String> deleteFileAsync(String imageURL) {
        try {
            log.info("Async file delete started: url={}", imageURL);

            String publicId = extractPublicId(imageURL);
            Map<String, String> options = new HashMap<>();
            options.put("invalidate", "true");

            cloudinary.uploader().destroy(publicId, options);

            String message = "File successfully deleted from Cloudinary: " + publicId;
            log.info(message);
            return CompletableFuture.completedFuture(message);

        } catch (Exception e) {
            log.error("Async file delete failed: url={}, error={}", imageURL, e.getMessage(), e);
            return CompletableFuture.failedFuture(
                    new IOException("Cloudinary delete failed: " + e.getMessage(), e)
            );
        }
    }

    @Override
    public String deleteFile(String imageURL) throws IOException {
        try {
            log.info("Sync file delete started: url={}", imageURL);

            String publicId = extractPublicId(imageURL);
            Map<String, String> options = new HashMap<>();
            options.put("invalidate", "true");

            cloudinary.uploader().destroy(publicId, options);

            String message = "File successfully deleted from Cloudinary: " + publicId;
            log.info(message);
            return message;

        } catch (Exception e) {
            log.error("Sync file delete failed: url={}, error={}", imageURL, e.getMessage(), e);
            throw new IOException("Cloudinary delete failed: " + e.getMessage(), e);
        }
    }

    private String extractPublicId(String imageURL) {
        int lastSlashIndex = imageURL.lastIndexOf("/");
        int lastDotIndex = imageURL.lastIndexOf(".");

        if (lastSlashIndex == -1 || lastDotIndex == -1 || lastDotIndex <= lastSlashIndex) {
            throw new IllegalArgumentException("Invalid Cloudinary URL: " + imageURL);
        }

        return "pizza/" + imageURL.substring(lastSlashIndex + 1, lastDotIndex);
    }
}