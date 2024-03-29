package ru.jamsys.sbl.jpa.dto;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "v_srv")
public class VirtualServerDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sbl_seq")
    @Column(name = "id_v_srv", nullable = false)
    private Long id;

    @Column(name = "id_srv", nullable = false)
    private Long idSrv;

    @Column(name = "id_client", nullable = false)
    private Long idClient;

    @Column(name = "iso_v_srv", nullable = false)
    private String iso;

    @Column(name = "port_local_v_srv", nullable = false)
    private Integer portLocal;

    @Column(name = "port_router_v_srv", nullable = false)
    private Integer portRouter;

    @Column(name = "date_add_v_srv", insertable = false)
    private Timestamp dateAdd = new Timestamp(System.currentTimeMillis());

    @Column(name = "login_v_srv", nullable = false)
    private String login;

    @Column(name = "password_v_srv", nullable = false)
    private String password;

    @Column(name = "status_v_srv", insertable = false)
    private Integer status = 0;

    @Column(name = "response_v_srv")
    private String response = "";

    @Column(name = "id_task")
    private Long idTask;

    @Column(name = "vm_status_date_v_srv", insertable = false)
    private String vmStatusDate;

    @Column(name = "rdp_info_v_srv")
    private String rdpInfo;

    @Column(name = "date_remove_v_srv")
    private Timestamp dateRemove;
}
