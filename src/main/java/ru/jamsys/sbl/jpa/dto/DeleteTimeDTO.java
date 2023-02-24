package ru.jamsys.sbl.jpa.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;


@Data
@Entity
@Table(name = "deletetime")
public class DeleteTimeDTO {

    @Id
    @Column(name = "id_v_srv", nullable = false)
    private Long idSrv;

    @JsonFormat(pattern="dd.MM.yyyy HH:mm Z")
    @Column(name = "data_delete", nullable = false)
    private Timestamp dataDelete = new Timestamp(System.currentTimeMillis());

}
