package ru.jamsys.sbl.jpa.dto;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "srv")
public class ServerDTO implements WebPatch<ServerDTO> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sbl_seq")
    @Column(name = "id_srv", nullable = false)
    private Long id;

    @Column(name = "name_srv", nullable = false)
    private String name;

    @Column(name = "date_add_srv", insertable = false)
    private Timestamp dateAdd = new Timestamp(System.currentTimeMillis());

    @Column(name = "ip_srv")
    private String ip;

    @Column(name = "status_srv", nullable = false, insertable = false)
    private Integer status = 0;

    @Column(name = "ping_date_srv", insertable = false)
    private Timestamp datePing;

    public void patch(ServerDTO foreign) {
        if (foreign.getName() != null) {
            this.setName(foreign.getName());
        }
        if (foreign.getIp() != null) {
            this.setIp(foreign.getIp());
        }
        if (foreign.getStatus() != null) {
            this.setStatus(foreign.getStatus());
        }
        if (foreign.getDatePing() != null) {
            this.setDatePing(foreign.getDatePing());
        }
    }

}
