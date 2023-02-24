package ru.jamsys.sbl.jpa.dto;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "billid")
public class BillidDTO {

    @Id
    @Column(name = "id_client", nullable = false)
    private Long idClient;

    @Column(name = "id_billid", nullable = false)
    private String idBill;

}
