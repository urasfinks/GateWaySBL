package ru.jamsys.sbl.jpa.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "client")
public class ClientDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sbl_seq")
    @Column(name = "id_client", nullable = false)
    private Long id;

    @Column(name = "mail_client", nullable = false)
    private String mail;

    @Column(name = "login_client", nullable = false)
    private String login;

    @JsonIgnore
    @Column(name = "password_client", nullable = false)
    private String password;

    @Column(name = "date_add_client", insertable = false)
    private Timestamp dateAdd = new Timestamp(System.currentTimeMillis());

}
