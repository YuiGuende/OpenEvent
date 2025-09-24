package com.group02.openevent.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


public interface IImageService {
    String saveImage(MultipartFile file);
}
