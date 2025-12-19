package com.example.pizza.service.logic;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface FileUpload {

    String uploadFile(MultipartFile multipartFile) throws IOException;
    CompletableFuture<String> uploadFileAsync(MultipartFile multipartFile);
    String deleteFile(String imageURL) throws IOException;
    CompletableFuture<String> deleteFileAsync(String imageURL);
}