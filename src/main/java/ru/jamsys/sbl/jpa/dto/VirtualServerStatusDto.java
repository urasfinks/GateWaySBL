package ru.jamsys.sbl.jpa.dto;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "v_srv_status")
public class VirtualServerStatusDto {

    @Id
    @GeneratedValue(strategy= GenerationType.SEQUENCE, generator = "sbl_seq")
    @Column(name="id_v_srv_status", nullable = false)
    private Long id;

    @Column(name="id_v_srv", nullable = false)
    private Long idVSrv;

    @Column(name="data_v_srv_status", nullable = false)
    private String data;

    @Column(name="date_add_v_srv_status", insertable = false)
    private Timestamp dateAdd;

}
