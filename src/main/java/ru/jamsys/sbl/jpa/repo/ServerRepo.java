package ru.jamsys.sbl.jpa.repo;

import org.hibernate.annotations.Immutable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.jpa.dto.ServerDTO;
import ru.jamsys.sbl.jpa.dto.custom.ServerStatistic;

import javax.persistence.LockModeType;
import java.util.List;

@Repository
public interface ServerRepo extends CrudRepository<ServerDTO, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select t from ServerDTO t where t.status = 0 and t.pingStatus = 1 order by t.id asc")
    List<ServerDTO> getAlready();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ServerDTO t where t.id = :id_server")
    ServerDTO findOneForUpdate(@Param("id_server") Long idServer);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "SELECT s2.* FROM srv s2\n" +
            "INNER JOIN (\n" +
            "\tSELECT \n" +
            "\t\ts1.id_srv, \n" +
            "\t\t(s1.max_count_v_srv - count(vs1.*)) as diff\n" +
            "\tFROM srv s1\n" +
            "\tLEFT JOIN v_srv vs1 ON vs1.id_srv = s1.id_srv AND vs1.status_v_srv >= 0\n" +
            "\tGROUP BY s1.id_srv\n" +
            "\tORDER BY s1.id_srv\n" +
            ") AS sq1 ON sq1.id_srv = s2.id_srv AND sq1.diff > 0 AND s2.ping_status_srv = 1", nativeQuery = true)
    List<ServerDTO> getAvailable();

}
