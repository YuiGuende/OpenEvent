package com.group02.openevent.model.user;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "guest")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guest_id")
    private Integer id;

    private String name;

    @Column(unique = true)
    private String email;

    @OneToMany(mappedBy = "guest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GuestTicket> guestTickets;

    // Getters, Setters
}
