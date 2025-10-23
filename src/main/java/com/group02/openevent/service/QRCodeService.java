package com.group02.openevent.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class QRCodeService {
    
    /**
     * Generate QR code image as byte array
     * @param content The content to encode in QR code (usually a URL)
     * @param width Width of QR code image
     * @param height Height of QR code image
     * @return QR code image as byte array (PNG format)
     */
    public byte[] generateQRCodeImage(String content, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        return outputStream.toByteArray();
    }
    
    /**
     * Generate QR code with default size (300x300)
     */
    public byte[] generateQRCodeImage(String content) throws WriterException, IOException {
        return generateQRCodeImage(content, 300, 300);
    }
}



