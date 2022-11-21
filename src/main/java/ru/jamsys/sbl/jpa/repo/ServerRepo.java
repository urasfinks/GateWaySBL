package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.jamsys.sbl.jpa.dto.ServerDTO;
import ru.jamsys.sbl.jpa.dto.custom.ServerStatistic;

import javax.persistence.LockModeType;
import java.util.List;

public interface ServerRepo extends CrudRepository<ServerDTO, Long> {

    @Query("select t from ServerDTO t where t.status = 0 and t.pingStatus = 1 order by t.id asc")
    List<ServerDTO> getAlready();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ServerDTO t where t.id = :id_server")
    ServerDTO findOneForUpdate(@Param("id_server") Long idServer);

    @Query(value = "select " +
            " new ServerStatistic(s1.name, count(vs1.id)) " +
            "from VirtualServerDTO vs1\n" +
            "inner join ServerDTO s1 on s1.id = vs1.idSrv\n" +
            "where vs1.status = 1\n" +
            "group by s1.id")
    List<ServerStatistic> getStatistic();
}
