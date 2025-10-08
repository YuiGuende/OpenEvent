package com.group02.openevent.service.impl;


import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IRequestRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.RequestService;
import com.group02.openevent.util.CloudinaryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO;
import com.group02.openevent.dto.requestApproveEvent.CreateRequestDTO;
import com.group02.openevent.dto.requestApproveEvent.RequestDTO;
import com.group02.openevent.model.request.Request;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.model.request.RequestType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final IRequestRepo requestRepo;
    private final EventService eventService;
    private final CloudinaryUtil cloudinaryUtil;
    private final IAccountRepo  accountRepo;
    @Override
    @Transactional
    public Request approveRequest(Long requestId, String responseMessage) {
        Request request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request has already been processed");
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setResponseMessage(responseMessage);
        request.setUpdatedAt(LocalDateTime.now());

        if (request.getType() == RequestType.EVENT_APPROVAL && request.getEvent() != null) {
            eventService.updateEventStatus(request.getEvent().getId(), EventStatus.PUBLIC);
        }

        return requestRepo.save(request);
    }

    @Override
    @Transactional
    public Request rejectRequest(Long requestId, String responseMessage) {
        Request request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request has already been processed");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setResponseMessage(responseMessage);
        request.setUpdatedAt(LocalDateTime.now());

        return requestRepo.save(request);
    }

    @Override
    public Page<Request> getRequestsByReceiver(Long receiverAccountId, RequestStatus status, Pageable pageable) {
        if (status != null) {
            return requestRepo.findByReceiver_AccountIdAndStatus(receiverAccountId, status, pageable);
        } else {
            return requestRepo.findByReceiver_AccountId(receiverAccountId, pageable);
        }
    }

    @Override
    @Transactional
    public RequestDTO createRequest(CreateRequestDTO createRequestDTO) {
        Request request = Request.builder()
                .type(createRequestDTO.getType())
                .message(createRequestDTO.getMessage())
                .fileURL(createRequestDTO.getFileURL())
                .targetUrl(createRequestDTO.getTargetUrl())
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        //TODO
        // Set relationships (you'll need to fetch these from their respective services)
        // For now, we'll just set the IDs - you should inject AccountService and OrderService
        // and fetch the actual entities

        if (createRequestDTO.getEventId() != null) {
            Event event = eventService.getEventById(createRequestDTO.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            request.setEvent(event);
        }

        Request savedRequest = requestRepo.save(request);
        return convertToDTO(savedRequest);
    }

    @Override
    @Transactional
    public RequestDTO createRequestWithFile(CreateRequestDTO createRequestDTO, MultipartFile file) {
        String fileURL = null;

        // Upload file to Cloudinary if provided
        if (file != null && !file.isEmpty()) {
            try {
                fileURL = cloudinaryUtil.uploadFile(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file: " + e.getMessage());
            }
        }

        Request request = Request.builder()
                .type(createRequestDTO.getType())
                .sender(accountRepo.findById(createRequestDTO.getSenderId()).orElseThrow(() -> new RuntimeException("Sender not found")))
                .receiver(accountRepo.findById(createRequestDTO.getReceiverId()).orElseThrow(() -> new RuntimeException("Receiver not found")))
                .message(createRequestDTO.getMessage())
                .fileURL(fileURL) // Use uploaded file URL
                .targetUrl(createRequestDTO.getTargetUrl())
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        if (createRequestDTO.getEventId() != null) {
            Event event = eventService.getEventById(createRequestDTO.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            request.setEvent(event);
        }

        Request savedRequest = requestRepo.save(request);
        return convertToDTO(savedRequest);
    }

    @Override
    @Transactional
    public RequestDTO approveRequest(Long requestId, ApproveRequestDTO approveRequestDTO) {
        Request request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request has already been processed");
        }

        request.setStatus(RequestStatus.APPROVED);
        request.setResponseMessage(approveRequestDTO.getResponseMessage());
        request.setUpdatedAt(LocalDateTime.now());

        // If it's an event approval request, update the event status to PUBLIC
        if (request.getType() == RequestType.EVENT_APPROVAL && request.getEvent() != null) {
            eventService.updateEventStatus(request.getEvent().getId(), EventStatus.PUBLIC);
        }

        Request savedRequest = requestRepo.save(request);
        return convertToDTO(savedRequest);
    }

    @Override
    @Transactional
    public RequestDTO rejectRequest(Long requestId, ApproveRequestDTO approveRequestDTO) {
        Request request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Request has already been processed");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setResponseMessage(approveRequestDTO.getResponseMessage());
        request.setUpdatedAt(LocalDateTime.now());

        Request savedRequest = requestRepo.save(request);
        return convertToDTO(savedRequest);
    }

    @Override
    public Optional<RequestDTO> getRequestById(Long requestId) {
        return requestRepo.findById(requestId).map(this::convertToDTO);
    }

    @Override
    public List<RequestDTO> getAllRequests() {
        return requestRepo.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RequestDTO> getRequestsByStatus(RequestStatus status) {
        return requestRepo.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RequestDTO> getRequestsByType(RequestType type) {
        return requestRepo.findByType(type).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RequestDTO> getRequestsBySenderId(Long senderId) {
        return requestRepo.findBySenderAccountId(senderId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RequestDTO> getRequestsByReceiverId(Long receiverId) {
        return requestRepo.findByReceiver_AccountId(receiverId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RequestDTO> getRequestsByEventId(Long eventId) {
        return requestRepo.findByEvent_Id(eventId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<RequestDTO> listRequests(RequestStatus status, RequestType type, Pageable pageable) {
        Page<Request> requests;

        if (status != null && type != null) {
            requests = requestRepo.findByStatusAndType(status, type, pageable);
        } else if (status != null) {
            requests = requestRepo.findByStatus(status, pageable);
        } else if (type != null) {
            requests = requestRepo.findByType(type, pageable);
        } else {
            requests = requestRepo.findAll(pageable);
        }

        return requests.map(this::convertToDTO);
    }

    @Override
    public Page<RequestDTO> listRequestsByReceiver(Long receiverId, Pageable pageable) {
        return requestRepo.findByReceiver_AccountId(receiverId, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public RequestDTO convertToDTO(Request request) {
        return RequestDTO.builder()
                .requestId(request.getRequestId())
                .senderId(request.getSender() != null ? request.getSender().getAccountId() : null)
                .senderName(request.getSender() != null ? request.getSender().getEmail() : null)
                .receiverId(request.getReceiver() != null ? request.getReceiver().getAccountId() : null)
                .receiverName(request.getReceiver() != null ? request.getReceiver().getEmail() : null)
                .type(request.getType())
                .eventId(request.getEvent() != null ? request.getEvent().getId() : null)
                .eventTitle(request.getEvent() != null ? request.getEvent().getTitle() : null)
                .targetUrl(request.getTargetUrl())
                .orderId(request.getOrder() != null ? request.getOrder().getOrderId() : null)
                .message(request.getMessage())
                .fileURL(request.getFileURL())
                .responseMessage(request.getResponseMessage())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
