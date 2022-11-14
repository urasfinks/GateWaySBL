package ru.jamsys.sbl.jpa.repo;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.jpa.dto.TaskDTO;

import javax.persistence.LockModeType;
import java.sql.Timestamp;
import java.util.List;

@Immutable
public interface TaskRepo extends CrudRepository<TaskDTO, Long> {
//    @Query("from TaskDTO t where t.dateExecute < :time and t.status = 0")
//    List<TaskDTO> getExecute(@Param("time") Timestamp time);

    @Query("select t from TaskDTO t where t.dateExecute < :time and t.status = 0")
    List<TaskDTO> getAlready(@Param("time") Timestamp time);

    //@Lock(LockModeType.PESSIMISTIC_WRITE)
    //@Query("select t.id from TaskDTO t where t.id = :id_task")
    //@Modifying(flushAutomatically = true, clearAutomatically = true)
    //@Transactional
    @Query(nativeQuery = true, value = "select * from task where id_task = :id_task for update OF task SKIP LOCKED")
    TaskDTO test(@Param("id_task") Long id_task);
}
