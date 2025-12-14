package io.themselg.whisper_api.endpoints;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileEndpoint {

    // Carpeta donde se guardarán las fotos (en la raíz del proyecto)
    private final Path rootLocation = Paths.get("uploads");

    public FileEndpoint() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear carpeta uploads");
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), this.rootLocation.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

            // Devolvemos la URL relativa al endpoint de descarga
            Map<String, String> response = new HashMap<>();
            response.put("url", "/api/files/view/" + filename); 
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/view/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {            
            Path file = rootLocation.resolve(filename).normalize().toAbsolutePath();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                
                // Deteccionar el tipo de contenido
                String contentType = null;
                try {
                    contentType = Files.probeContentType(file);
                } catch (IOException e) {
                    // Si falla, no hacemos nada
                }

                // Si no se pudo detectar, usar un tipo genérico binario
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        // Aquí usamos el tipo real detectado (application/pdf, video/mp4, etc.)
                        .contentType(MediaType.parseMediaType(contentType))
                        // Header importante para que el navegador sepa el nombre real
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}