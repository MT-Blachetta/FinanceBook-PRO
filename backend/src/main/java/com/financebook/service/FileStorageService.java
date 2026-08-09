package com.financebook.service;

import com.financebook.entity.PaymentItem;
import com.financebook.entity.User;
import com.financebook.exception.ResourceNotFoundException;
import com.financebook.exception.UnauthorizedException;
import com.financebook.exception.ValidationException;
import com.financebook.repository.PaymentItemRepository;
import com.financebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * File storage service for category icons and invoice documents.
 * Mirrors Python's file upload/download endpoints from main.py.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    
    private final PaymentItemRepository paymentItemRepository;
    private final UserRepository userRepository;
    
    @Value("${file.upload.icons-dir:./icons}")
    private String iconsDir;
    
    @Value("${file.upload.invoices-dir:./app/invoices}")
    private String invoicesDir;
    
    private static final Set<String> ALLOWED_ICON_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/bmp", "image/svg+xml"
    );
    
    private static final Set<String> ALLOWED_INVOICE_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/tiff"
    );
    
    private static final Map<String, String> FILE_EXTENSIONS = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/gif", ".gif",
            "image/bmp", ".bmp",
            "image/svg+xml", ".svg",
            "application/pdf", ".pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx",
            "application/msword", ".doc",
            "image/tiff", ".tiff"
    );
    
    /**
     * Upload a category icon.
     * Mirrors Python's upload_icon endpoint.
     */
    public String uploadIcon(MultipartFile file) throws IOException {
        // Validate file type
        if (!ALLOWED_ICON_TYPES.contains(file.getContentType())) {
            throw new ValidationException("File type " + file.getContentType() +
                    " not allowed. Supported types: PNG, JPEG, GIF, BMP, SVG");
        }

        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(iconsDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Sanitize the client-supplied name: strip any directory components and
        // any characters outside a safe set so it cannot escape the icons dir.
        String filename = sanitizeFilename(file.getOriginalFilename());
        Path filePath = resolveWithin(uploadPath, filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filename;
    }

    /**
     * Download a category icon.
     * Mirrors Python's download_icon endpoint.
     */
    public Resource downloadIcon(String filename) throws MalformedURLException {
        Path filePath = resolveWithin(Paths.get(iconsDir), filename);
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            throw new ResourceNotFoundException("File not found");
        }

        return resource;
    }

    /**
     * Strip directory components and unsafe characters from a client-supplied
     * file name. Keeps the extension so browsers can infer the content type.
     */
    static String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ValidationException("File name is missing");
        }
        // Take only the last path segment (defends against "../../x" and "C:\x")
        String name = originalFilename.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        // Whitelist safe characters
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (name.isBlank() || name.equals(".") || name.equals("..") || name.chars().allMatch(c -> c == '.')) {
            throw new ValidationException("Invalid file name");
        }
        return name;
    }

    /**
     * Resolve a file name against a base directory and verify the result stays
     * inside that directory (path traversal guard).
     */
    private static Path resolveWithin(Path baseDir, String filename) {
        Path base = baseDir.toAbsolutePath().normalize();
        Path resolved = base.resolve(filename).normalize();
        if (!resolved.startsWith(base) || resolved.equals(base)) {
            throw new ValidationException("Invalid file name");
        }
        return resolved;
    }
    
    /**
     * Upload an invoice for a payment item.
     * Mirrors Python's upload_invoice endpoint.
     */
    @Transactional
    public Map<String, Object> uploadInvoice(
            Long paymentItemId,
            MultipartFile file,
            String username) throws IOException {
        
        User user = getUserByUsername(username);
        
        // Validate payment item exists and belongs to user
        PaymentItem paymentItem = paymentItemRepository.findById(paymentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment item not found"));
        
        if (!paymentItem.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to upload invoices for this item");
        }
        
        // Validate file type
        if (!ALLOWED_INVOICE_TYPES.contains(file.getContentType())) {
            throw new ValidationException("File type " + file.getContentType() + 
                    " not allowed. Supported types: PDF, DOCX, DOC, JPEG, PNG, GIF, BMP, TIFF");
        }
        
        // Validate file size
        if (file.getSize() > com.financebook.util.Constants.MAX_INVOICE_SIZE) {
            throw new ValidationException("File size exceeds 25MB limit");
        }
        
        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(invoicesDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Delete existing invoice if it exists
        if (paymentItem.getInvoicePath() != null) {
            Path oldFilePath = uploadPath.resolve(paymentItem.getInvoicePath());
            if (Files.exists(oldFilePath)) {
                Files.delete(oldFilePath);
            }
        }
        
        // Generate unique filename
        String extension = FILE_EXTENSIONS.getOrDefault(file.getContentType(), "");
        String uniqueFilename = paymentItemId + "_" + UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(uniqueFilename);
        
        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Update payment item
        paymentItem.setInvoicePath(uniqueFilename);
        paymentItemRepository.save(paymentItem);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Invoice uploaded successfully");
        response.put("filename", uniqueFilename);
        response.put("payment_item_id", paymentItemId);
        
        return response;
    }
    
    /**
     * Download an invoice for a payment item.
     * Mirrors Python's download_invoice endpoint.
     */
    @Transactional(readOnly = true)
    public Resource downloadInvoice(Long paymentItemId, String username) throws MalformedURLException {
        User user = getUserByUsername(username);
        
        PaymentItem paymentItem = paymentItemRepository.findById(paymentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment item not found"));
        
        if (!paymentItem.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to access this invoice");
        }
        
        if (paymentItem.getInvoicePath() == null) {
            throw new ResourceNotFoundException("No invoice file found for this payment item");
        }
        
        Path filePath = Paths.get(invoicesDir).resolve(paymentItem.getInvoicePath());
        Resource resource = new UrlResource(filePath.toUri());
        
        if (!resource.exists()) {
            throw new ResourceNotFoundException("Invoice file not found on disk");
        }
        
        return resource;
    }
    
    /**
     * Delete an invoice for a payment item.
     * Mirrors Python's delete_invoice endpoint.
     */
    @Transactional
    public void deleteInvoice(Long paymentItemId, String username) throws IOException {
        User user = getUserByUsername(username);
        
        PaymentItem paymentItem = paymentItemRepository.findById(paymentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment item not found"));
        
        if (!paymentItem.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Not authorized to delete this invoice");
        }
        
        if (paymentItem.getInvoicePath() == null) {
            throw new ResourceNotFoundException("No invoice file found for this payment item");
        }
        
        // Delete file from disk
        Path filePath = Paths.get(invoicesDir).resolve(paymentItem.getInvoicePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        
        // Clear invoice path from database
        paymentItem.setInvoicePath(null);
        paymentItemRepository.save(paymentItem);
    }

    /**
     * Best-effort deletion of an invoice file from the configured invoices
     * directory. Used when a payment item is deleted; a failure to remove the
     * file must not abort the surrounding transaction.
     */
    public void deleteInvoiceFileQuietly(String invoicePath) {
        try {
            Path filePath = resolveWithin(Paths.get(invoicesDir), invoicePath);
            if (Files.deleteIfExists(filePath)) {
                log.info("Deleted invoice file {}", filePath);
            }
        } catch (Exception e) {
            log.warn("Failed to delete invoice file '{}': {}", invoicePath, e.getMessage());
        }
    }

    /**
     * Get user by username.
     */
    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
