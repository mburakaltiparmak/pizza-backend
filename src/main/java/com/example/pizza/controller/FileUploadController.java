package com.example.pizza.controller;

import com.example.pizza.service.logic.FileUpload;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/upload")
public class FileUploadController {
    private final FileUpload fileUpload;

    public FileUploadController(FileUpload fileUpload) {
        this.fileUpload = fileUpload;
    }

    @GetMapping("/add-product")
    public String addProduct() {
        return "add-product";
    }

    @PostMapping
    public String uploadFile(@RequestParam("image") MultipartFile multipartFile,
                             Model model) throws IOException {
        String imageURL = fileUpload.uploadFile(multipartFile);
        model.addAttribute("imageURL", imageURL);
        return "uploaded-product";
    }

    @GetMapping("/file")
    public String getFile(Model model) throws IOException {
        String imageURL = (String) model.getAttribute("imageURL");
        return imageURL;
    }
}
