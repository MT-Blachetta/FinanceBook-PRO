package com.financebook.controller;

import com.financebook.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

/**
 * File upload controller.
 * Handles category icon and invoice document uploads.
 */
@Tag(name = "File Upload", description = "File upload and download endpoints")
@RestController
@RequiredArgsConstructor
public class FileUploadController {
    
    private final FileStorageService fileStorageService;
    
    @Operation(summary = "Upload icon", description = "Upload a category icon file")
    @PostMapping("/uploadicon")
    public ResponseEntity<Map<String, String>> uploadIcon(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        
        String filename = fileStorageService.uploadIcon(file);
        
        Map<String, String> response = new HashMap<>();
        response.put("filename", filename);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Download icon", description = "Download a category icon file")
    @GetMapping("/download_static/{filename}")
    public ResponseEntity<Resource> downloadIcon(@PathVariable String filename) throws MalformedURLException {
        Resource resource = fileStorageService.downloadIcon(filename);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
    
    @Operation(summary = "Upload invoice", description = "Upload an invoice file for a payment item")
    @PostMapping("/upload-invoice/{paymentItemId}")
    public ResponseEntity<Map<String, Object>> uploadInvoice(
            @PathVariable Long paymentItemId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        
        Map<String, Object> response = fileStorageService.uploadInvoice(
                paymentItemId, file, authentication.getName());
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Download invoice", description = "Download the invoice file for a payment item")
    @GetMapping("/download-invoice/{paymentItemId}")
    public ResponseEntity<Resource> downloadInvoice(
            @PathVariable Long paymentItemId,
            Authentication authentication) throws MalformedURLException {
        
        Resource resource = fileStorageService.downloadInvoice(paymentItemId, authentication.getName());
        
        String filename = "invoice_" + paymentItemId + "_" + resource.getFilename();
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }
    
    @Operation(summary = "Delete invoice", description = "Delete the invoice file for a payment item")
    @DeleteMapping("/invoice/{paymentItemId}")
    public ResponseEntity<Map<String, String>> deleteInvoice(
            @PathVariable Long paymentItemId,
            Authentication authentication) throws IOException {
        
        fileStorageService.deleteInvoice(paymentItemId, authentication.getName());
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Invoice deleted successfully");
        
        return ResponseEntity.ok(response);
    }
}
