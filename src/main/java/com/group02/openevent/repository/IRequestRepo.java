package com.group02.openevent.repository;


import com.group02.openevent.model.request.Request;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.model.request.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IRequestRepo extends JpaRepository<Request, Long> {
    List<Request> findByStatus(RequestStatus status);

    List<Request> findByType(RequestType type);

    List<Request> findBySenderUserId(Long senderId);

    List<Request> findByReceiverUserId(Long receiverId);

    List<Request> findByEvent_Id(Long eventId);

    // Pageable listing
    Page<Request> findAll(Pageable pageable);

    Page<Request> findByStatus(RequestStatus status, Pageable pageable);

    Page<Request> findByType(RequestType type, Pageable pageable);

    Page<Request> findByStatusAndType(RequestStatus status, RequestType type, Pageable pageable);

    Page<Request> findByReceiverUserId(Long receiverId, Pageable pageable);

    long countByReceiverUserIdAndStatus(Long receiverId, RequestStatus requestStatus);

    Page<Request> findByReceiverUserIdAndStatus(Long receiverId, RequestStatus status, Pageable pageable);

    List<Request> findByReceiverUserIdAndStatusOrderByUpdatedAtDesc(Long receiverId, RequestStatus status);
}
