package com.group02.openevent.service.impl;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.Speaker;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.ISpeakerRepo;
import com.group02.openevent.service.SpeakerService;
 import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SpeakerServiceImpl implements SpeakerService {
    @Autowired
    ISpeakerRepo speakerRepo;
    @Autowired
    IEventRepo eventRepo;

    @Override
    public List<Speaker> findAllByEventId(Long id) {
        return speakerRepo.findSpeakerByEventId(id);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Speaker speakerToDelete = speakerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Speaker với ID: " + id));
        List<Event> eventsWithThisSpeaker = eventRepo.findBySpeakersContains(speakerToDelete);

        for (Event event : eventsWithThisSpeaker) {
            event.getSpeakers().remove(speakerToDelete);
            // Bạn không cần gọi eventRepo.save(event) ở đây
            // vì @Transactional sẽ tự động lưu các thay đổi.
        }

        speakerRepo.delete(speakerToDelete);
    }



    @Override
    @Transactional
    public Speaker update(Long id, Speaker speakerDetails) {
        // 1. Tìm speaker đã có trong database
        Speaker existingSpeaker = speakerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Speaker với ID: " + id));

        // 2. Cập nhật các trường của speaker đã có bằng dữ liệu mới
        // Giống như bạn đang sửa một tài liệu có sẵn
        existingSpeaker.setName(speakerDetails.getName());
        existingSpeaker.setProfile(speakerDetails.getProfile());
        existingSpeaker.setImageUrl(speakerDetails.getImageUrl());
        existingSpeaker.setDefaultRole(speakerDetails.getDefaultRole());
        // ... cập nhật các trường khác nếu có

        // 3. Lưu lại speaker đã được cập nhật.
        // Vì existingSpeaker là một đối tượng đã được JPA quản lý,
        // lệnh save này sẽ thực hiện một câu lệnh UPDATE trong SQL.
        return speakerRepo.save(existingSpeaker);
    }

    @Override
    @Transactional
    public Speaker create(Speaker request) {
        // Tạo speaker mới
        Speaker newSpeaker = new Speaker();
        newSpeaker.setName(request.getName());
        newSpeaker.setProfile(request.getProfile());
        newSpeaker.setImageUrl(request.getImageUrl());
        newSpeaker.setDefaultRole(request.getDefaultRole());
        
        // Lưu speaker vào database
        Speaker savedSpeaker = speakerRepo.save(newSpeaker);
        
        return savedSpeaker;
    }

    @Override
    @Transactional
    public Speaker addToEvent(Long speakerId, Long eventId) {
        // Tìm speaker và event
        Speaker speaker = speakerRepo.findById(speakerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Speaker với ID: " + speakerId));
        
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Event với ID: " + eventId));
        
        // Thêm event vào speaker's events list
        if (speaker.getEvents() == null) {
            speaker.setEvents(new ArrayList<>());
        }
        
        if (!speaker.getEvents().contains(event)) {
            speaker.getEvents().add(event);
            speakerRepo.save(speaker);
        }
        
        return speaker;
    }

}
