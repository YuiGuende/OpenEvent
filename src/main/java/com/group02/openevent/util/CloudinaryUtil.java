/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.group02.openevent.util;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.servlet.http.Part;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 *
 * @author LEGION
 */
@Component
public class CloudinaryUtil {

    private Cloudinary cloudinary = new Cloudinary();

    public CloudinaryUtil() {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dszkninft",
                "api_key", "825471649162573",
                "api_secret", "9bu_Mv1MTSl1ZLaXNu1b8i8dEnY"
        ));
    }

    public String upload(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFile); // MultipartFile -> File

        Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.emptyMap());

        tempFile.delete();
        return (String) uploadResult.get("secure_url");
    }

    // Upload video
    public String uploadVideo(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
        file.transferTo(tempFile);

        Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap(
                "resource_type", "video"
        ));

        tempFile.delete();
        return (String) uploadResult.get("secure_url");
    }

    public String uploadFile(MultipartFile file) throws IOException {
        // Fix 2: Đảm bảo file tạm luôn được xóa bằng try-finally
        File tempFile = null;
        try {
            // Tạo file tạm thời
            tempFile = File.createTempFile("upload-", file.getOriginalFilename());
            // Chuyển dữ liệu từ MultipartFile sang File
            file.transferTo(tempFile);

            // Upload file lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap(
                    "resource_type", "auto" // "auto" cho phép Cloudinary tự nhận diện
            ));

            // Trả về URL an toàn
            return (String) uploadResult.get("secure_url");
        }
        finally {
            // Luôn luôn xóa file tạm sau khi hoàn tất (dù thành công hay thất bại)
            // để tránh rò rỉ bộ nhớ (disk space leak).
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

}
