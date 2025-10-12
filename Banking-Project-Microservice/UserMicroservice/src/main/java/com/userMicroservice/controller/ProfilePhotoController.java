package com.userMicroservice.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/auth")
public class ProfilePhotoController {

    private final Path storageRoot = Paths.get("uploads", "profile").toAbsolutePath().normalize();

    @PostMapping("/user/{userId}/photo")
    @PreAuthorize("isAuthenticated() and #userId == authentication.principal.subject or hasRole('ADMIN')")
    public ResponseEntity<Void> uploadPhoto(@PathVariable String userId,
                                            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Files.createDirectories(storageRoot.resolve(userId));
            String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename());
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot > 0 && dot < original.length() - 1) {
                ext = original.substring(dot);
            }
            if (ext.length() > 8) ext = ""; // safety
            String targetName = "profile" + ext;
            Path target = storageRoot.resolve(Paths.get(userId, targetName)).normalize();
            if (!target.startsWith(storageRoot)) {
                return ResponseEntity.badRequest().build();
            }
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}/photo")
    @PreAuthorize("isAuthenticated() and #userId == authentication.principal.subject or hasRole('ADMIN')")
    public ResponseEntity<Resource> getPhoto(@PathVariable String userId) {
        try {
            Path userDir = storageRoot.resolve(userId).normalize();
            if (!Files.exists(userDir) || !Files.isDirectory(userDir)) {
                return ResponseEntity.notFound().build();
            }
            // Try known names: profile with common extensions
            String[] candidates = new String[] { "profile.png", "profile.jpg", "profile.jpeg", "profile.webp", "profile" };
            Path found = null;
            for (String c : candidates) {
                Path p = userDir.resolve(c);
                if (Files.exists(p) && Files.isReadable(p)) {
                    found = p; break;
                }
            }
            if (found == null) {
                // Fallback: find any file starting with 'profile'
                try (var stream = Files.list(userDir)) {
                    found = stream
                            .filter(p -> p.getFileName().toString().toLowerCase().startsWith("profile"))
                            .findFirst().orElse(null);
                }
            }
            if (found == null) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(found.toUri());
            String contentType = probeContentType(found, resource);
            // Inline so browser displays it
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String probeContentType(Path file, Resource resource) {
        try {
            String probed = Files.probeContentType(file);
            if (probed != null && !probed.isBlank()) return probed;
        } catch (Exception ignore) {}
        try {
            String byName = URLConnection.guessContentTypeFromName(resource.getFilename());
            if (byName != null && !byName.isBlank()) return byName;
        } catch (Exception ignore) {}
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
