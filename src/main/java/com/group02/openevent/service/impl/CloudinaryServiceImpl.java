package com.group02.openevent.service.impl;

import com.cloudinary.Cloudinary;
import com.group02.openevent.service.IImageService;
import com.group02.openevent.util.CloudinaryUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
@Service
public class CloudinaryServiceImpl implements IImageService {
    private final CloudinaryUtil cloudinaryUtil;

    public CloudinaryServiceImpl(CloudinaryUtil cloudinaryUtil) {
        this.cloudinaryUtil = cloudinaryUtil;
    }

    @Override
    public String saveImage(MultipartFile file) {
        try {
            return cloudinaryUtil.upload(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }
}
