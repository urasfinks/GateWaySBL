package ru.jamsys.sbl.jpa.dto;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "task_status")
public class TaskStatusDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sbl_seq")
    @Column(name = "id_task_status", nullable = false)
    private Long id;

    @Column(name = "id_task", nullable = false)
    private Long idTask;

    @Column(name = "data_task_status", nullable = false)
    private String data;

    @Column(name = "date_add_task_status", insertable = false)
    private Timestamp dateAdd = new Timestamp(System.currentTimeMillis());

    @Column(name = "level_task_status")
    private String level;

    @Column(name = "seq_task_status")
    private Long seq;

}
