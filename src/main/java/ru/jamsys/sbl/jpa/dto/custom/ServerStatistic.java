package ru.jamsys.sbl.jpa.dto.custom;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class ServerStatistic {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sbl_seq")
    private Long idSrv;

    private Long count;

    private Integer statusVSrv;

    public ServerStatistic(Long idSrv, Long count, Integer statusVSrv) {
        this.idSrv = idSrv;
        this.count = count;
        this.statusVSrv = statusVSrv;
    }

    public ServerStatistic() {

    }
}
