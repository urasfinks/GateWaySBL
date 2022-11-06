package ru.jamsys.sbl.jpa.dto;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "srv")
public class ServerDto {
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE, generator = "sbl_seq")
    @Column(name="id_srv", nullable = false)
    private Long id;

    @Column(name="name_srv", nullable = false)
    private String name;

    @Column(name="date_add_srv", insertable = false)
    private Timestamp dateAdd;

    @Column(name="ip_srv")
    private String ip;

    @Column(name="status_srv", nullable = false, insertable = false)
    private Integer status;

}
