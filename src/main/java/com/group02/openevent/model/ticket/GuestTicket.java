package com.group02.openevent.model.ticket;

import jakarta.persistence.*;

@Entity
@Table(
    name = "guest_ticket",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"guest_id", "ticket_type_id"})
    }
)
public class GuestTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guest_ticket_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    private Integer quantityBought = 0;   // số vé đã mua
    private Integer maxQuantity = 10;     // số vé tối đa được mua

    // Getters, Setters
}
