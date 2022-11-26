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
@Transactional
@Immutable
public interface ServerRepo extends CrudRepository<ServerDTO, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select t from ServerDTO t where t.status = 0 and t.pingStatus = 1 order by t.id asc")
    List<ServerDTO> getAlready();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ServerDTO t where t.id = :id_server")
    ServerDTO findOneForUpdate(@Param("id_server") Long idServer);

    
}
