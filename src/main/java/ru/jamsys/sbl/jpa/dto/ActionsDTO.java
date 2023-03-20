package ru.jamsys.sbl.jpa.dto;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "actions")
public class ActionsDTO {

    @Id
    @Column(name = "id_client", nullable = false)
    private Long idClient;

    @Column(name = "id_v_srv", nullable = false)
    private Long idSrv;

    @Column(name = "tarif", nullable = false)
    private String tarif;

    @Column(name = "days", nullable = false)
    private Long days;

}
