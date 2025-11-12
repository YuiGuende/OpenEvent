package com.group02.openevent.service.impl;


import com.group02.openevent.dto.notification.RequestFormDTO;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IDepartmentRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IRequestRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.RequestService;
import com.group02.openevent.service.UserService;
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
    private final IEventRepo eventRepo;
    private final CloudinaryUtil cloudinaryUtil;
    private final IAccountRepo accountRepo;
    private final IDepartmentRepo departmentRepository;
    private final UserService userService;

    public RequestFormDTO getRequestFormData(Long eventId) throws Exception {
        List<Department> departments = departmentRepository.findAll();

        List<RequestFormDTO.DepartmentDTO> departmentDTOs = departments.stream()
                .map(dept -> {
                    // Use user_id (userId) instead of account_id because Request entity uses User (user_id)
                    Long userId = null;
                    if (dept.getUser() != null) {
                        userId = dept.getUser().getUserId();
                    }
                    return RequestFormDTO.DepartmentDTO.builder()
                            .id(userId) // Use user_id, not account_id
                            .name(dept.getDepartmentName())
                            .build();
                })
                .collect(Collectors.toList());
        Optional<Event> event = eventService.getEventById(eventId);
        if (event.isEmpty()) {
            throw new Exception("event not found!");
        }

        return RequestFormDTO.builder()
                .eventId(eventId)
                .eventName(event.get().getTitle())
                .departments(departmentDTOs)
                .build();
    }

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
            return requestRepo.findByReceiverUserIdAndStatus(receiverAccountId, status, pageable);
        } else {
            return requestRepo.findByReceiverUserId(receiverAccountId, pageable);
        }
    }

    @Override
    @Transactional
    public RequestDTO createRequest(CreateRequestDTO createRequestDTO) {
        // Validate: Check if there's already a PENDING or APPROVED request for this event
        if (createRequestDTO.getEventId() != null) {
            List<RequestStatus> blockingStatuses = List.of(RequestStatus.PENDING, RequestStatus.APPROVED);
            List<Request> existingRequests = requestRepo.findByEvent_IdAndStatusIn(
                    createRequestDTO.getEventId(), blockingStatuses);
            
            if (!existingRequests.isEmpty()) {
                Request existingRequest = existingRequests.get(0);
                if (existingRequest.getStatus() == RequestStatus.APPROVED) {
                    throw new RuntimeException("Cannot send request: Event has already been approved. Request ID: " + existingRequest.getRequestId());
                } else if (existingRequest.getStatus() == RequestStatus.PENDING) {
                    throw new RuntimeException("Cannot send request: There is already a pending request for this event. Request ID: " + existingRequest.getRequestId());
                }
            }
        }
        
        Request request = Request.builder()
                .type(createRequestDTO.getType())
                .message(createRequestDTO.getMessage())
                .sender(userService.getUserById(createRequestDTO.getSenderId()))
                .receiver(userService.getUserById(createRequestDTO.getReceiverId()))
                .fileURL(createRequestDTO.getFileURL())
                .targetUrl(createRequestDTO.getTargetUrl())
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        if (createRequestDTO.getEventId() != null) {
            // Use findByIdWithHostAccount to eagerly fetch Host relationship
            Event event = eventRepo.findByIdWithHostAccount(createRequestDTO.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            request.setEvent(event);
            
            // Set host from event (required by database constraint)
            if (event.getHost() == null) {
                throw new RuntimeException("Event has no host assigned");
            }
            request.setHost(event.getHost());
        } else {
            throw new RuntimeException("Event ID is required for creating request");
        }

        Request savedRequest = requestRepo.save(request);
        return convertToDTO(savedRequest);
    }

    @Override
    @Transactional
    public RequestDTO createRequestWithFile(CreateRequestDTO createRequestDTO, MultipartFile file) {
        // Validate: Check if there's already a PENDING or APPROVED request for this event
        if (createRequestDTO.getEventId() != null) {
            List<RequestStatus> blockingStatuses = List.of(RequestStatus.PENDING, RequestStatus.APPROVED);
            List<Request> existingRequests = requestRepo.findByEvent_IdAndStatusIn(
                    createRequestDTO.getEventId(), blockingStatuses);
            
            if (!existingRequests.isEmpty()) {
                Request existingRequest = existingRequests.get(0);
                if (existingRequest.getStatus() == RequestStatus.APPROVED) {
                    throw new RuntimeException("Cannot send request: Event has already been approved. Request ID: " + existingRequest.getRequestId());
                } else if (existingRequest.getStatus() == RequestStatus.PENDING) {
                    throw new RuntimeException("Cannot send request: There is already a pending request for this event. Request ID: " + existingRequest.getRequestId());
                }
            }
        }
        
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
                .sender(userService.getUserById(createRequestDTO.getSenderId()))
                .receiver(userService.getUserById(createRequestDTO.getReceiverId()))
                .message(createRequestDTO.getMessage())
                .fileURL(fileURL) // Use uploaded file URL
                .targetUrl(createRequestDTO.getTargetUrl())
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        if (createRequestDTO.getEventId() != null) {
            Event event = eventRepo.findByIdWithHostAccount(createRequestDTO.getEventId())
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            request.setEvent(event);
            
            // Set host from event (required by database constraint)
            if (event.getHost() == null) {
                throw new RuntimeException("Event has no host assigned");
            }
            request.setHost(event.getHost());
        } else {
            throw new RuntimeException("Event ID is required for creating request");
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
        return requestRepo.findBySenderUserId(senderId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RequestDTO> getRequestsByReceiverId(Long receiverId) {
        return requestRepo.findByReceiverUserId(receiverId).stream()
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
        return requestRepo.findByReceiverUserId(receiverId, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public RequestDTO convertToDTO(Request request) {
        return RequestDTO.builder()
                .requestId(request.getRequestId())
                .senderId(request.getSender() != null ? request.getSender().getUserId() : null)
                .senderName(request.getSender() != null ? request.getSender().getAccount().getEmail() : null)
                .receiverId(request.getReceiver() != null ? request.getReceiver().getUserId() : null)
                .receiverName(request.getReceiver() != null ? request.getReceiver().getAccount().getEmail() : null)
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
    
    @Override
    public List<RequestDTO> getRequestsByHostId(Long hostId) {
        return requestRepo.findByHost_Id(hostId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}
