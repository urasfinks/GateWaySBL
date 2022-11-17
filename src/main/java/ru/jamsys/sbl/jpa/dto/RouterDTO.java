package ru.jamsys.sbl.jpa.dto;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "router")
public class RouterDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sbl_seq")
    @Column(name = "id_router", nullable = false)
    private Long id;

    @Column(name = "ip_router", nullable = false)
    private String ip;

}
