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

    List<Request> findBySenderAccountId(Long senderId);

    List<Request> findByReceiver_AccountId(Long receiverId);

    List<Request> findByEvent_Id(Long eventId);

    // Pageable listing
    Page<Request> findAll(Pageable pageable);

    Page<Request> findByStatus(RequestStatus status, Pageable pageable);

    Page<Request> findByType(RequestType type, Pageable pageable);

    Page<Request> findByStatusAndType(RequestStatus status, RequestType type, Pageable pageable);

    Page<Request> findByReceiver_AccountId(Long receiverId, Pageable pageable);

    long countByReceiverAccountIdAndStatus(Long accountId, RequestStatus requestStatus);

    Page<Request> findByReceiver_AccountIdAndStatus(Long receiverAccountId, RequestStatus status, Pageable pageable);

    long countByReceiver_AccountIdAndStatus(Long receiverId, RequestStatus status);

    List<Request> findByReceiver_AccountIdAndStatusOrderByUpdatedAtDesc(Long receiverId, RequestStatus status);
}
