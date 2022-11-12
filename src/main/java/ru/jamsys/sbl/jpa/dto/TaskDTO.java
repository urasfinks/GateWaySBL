package ru.jamsys.sbl.jpa.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "task")
@JsonIgnoreProperties
public class TaskDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sbl_seq")
    @Column(name = "id_task", nullable = false)
    private Long id;

    @Column(name = "data_task", nullable = false)
    private String task;

    @Column(name = "status_task", insertable = false)
    private int status;

    @Column(name = "date_add_task", insertable = false) //now()
    private Timestamp dateAdd;

    @JsonFormat(pattern="dd.MM.yyyy HH:mm Z")
    @Column(name = "date_execute_task", nullable = false)
    private Timestamp dateExecute = new Timestamp(System.currentTimeMillis());

    @Column(name = "id_client")
    private Long idClient;

    @Column(name = "date_update_task", insertable = false) //now()
    private Timestamp dateUpdate;

    @Column(name = "result_task", insertable = false)
    private String result;

    @Column(name = "retry_task", insertable = false)
    private Integer retry;

}
