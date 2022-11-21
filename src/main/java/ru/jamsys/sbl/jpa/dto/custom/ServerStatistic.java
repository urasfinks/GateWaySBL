package ru.jamsys.sbl.jpa.dto.custom;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class ServerStatistic {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sbl_seq")
    private Long id;

    private String name;

    private Long count;

    public ServerStatistic(String name, Long count) {
        this.name = name;
        this.count = count;
    }

    public ServerStatistic() {

    }
}
