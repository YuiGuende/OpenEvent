package com.group02.openevent.controller.event;

import com.group02.openevent.model.event.Speaker;
import com.group02.openevent.service.IImageService;
import com.group02.openevent.service.impl.SpeakerServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/speakers")
public class SpeakerController {
    @Autowired
    private SpeakerServiceImpl speakerService;
    
    @Autowired
    private IImageService imageService;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpeaker(@PathVariable Long id) {
        speakerService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Speaker> updateSpeaker(@PathVariable Long id,
                                                 @RequestBody Speaker request) {
        Speaker updatedSpeaker = speakerService.update(id, request);
        return ResponseEntity.ok(updatedSpeaker);
    }
    
    @PostMapping
    @ResponseBody
    public ResponseEntity<?> createSpeaker(@RequestBody Speaker speaker) {
        try {
            // Validate required fields
            if (speaker.getName() == null || speaker.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name is required"));
            }
            
            // Set default role if not provided
            if (speaker.getDefaultRole() == null) {
                speaker.setDefaultRole(com.group02.openevent.model.enums.SpeakerRole.SPEAKER);
            }
            
            Speaker createdSpeaker = speakerService.create(speaker);
            return ResponseEntity.ok(createdSpeaker);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create speaker: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{speakerId}/events/{eventId}")
    @ResponseBody
    public ResponseEntity<Speaker> addSpeakerToEvent(@PathVariable Long speakerId, @PathVariable Long eventId) {
        Speaker speaker = speakerService.addToEvent(speakerId, eventId);
        return ResponseEntity.ok(speaker);
    }
    
    @PostMapping("/upload/image")
    @ResponseBody
    public ResponseEntity<Map<String, String>> uploadSpeakerImage(
            @RequestParam("file") MultipartFile file) throws IOException {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        String imageUrl = imageService.saveImage(file);
        
        Map<String, String> response = new HashMap<>();
        response.put("imageUrl", imageUrl);
        response.put("message", "Image uploaded successfully");
        
        return ResponseEntity.ok(response);
    }
}
