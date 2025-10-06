package com.group02.openevent.service;


import com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO;
import com.group02.openevent.dto.requestApproveEvent.CreateRequestDTO;
import com.group02.openevent.dto.requestApproveEvent.RequestDTO;
import com.group02.openevent.model.request.Request;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.model.request.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface RequestService {
    
    // Create request
    RequestDTO createRequest(CreateRequestDTO createRequestDTO);
    RequestDTO createRequestWithFile(CreateRequestDTO createRequestDTO, MultipartFile file);
    
    // Approve/Reject request
    RequestDTO approveRequest(Long requestId, ApproveRequestDTO approveRequestDTO);
    RequestDTO rejectRequest(Long requestId, ApproveRequestDTO approveRequestDTO);
    
    // Get requests
    Optional<RequestDTO> getRequestById(Long requestId);
    List<RequestDTO> getAllRequests();
    List<RequestDTO> getRequestsByStatus(RequestStatus status);
    List<RequestDTO> getRequestsByType(RequestType type);
    List<RequestDTO> getRequestsBySenderId(Long senderId);
    List<RequestDTO> getRequestsByReceiverId(Long receiverId);
    List<RequestDTO> getRequestsByEventId(Long eventId);
    
    // Pageable
    Page<RequestDTO> listRequests(RequestStatus status, RequestType type, Pageable pageable);
    Page<RequestDTO> listRequestsByReceiver(Long receiverId, Pageable pageable);
    
    // Convert to DTO
    RequestDTO convertToDTO(Request request);
}
