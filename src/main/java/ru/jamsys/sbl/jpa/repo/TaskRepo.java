package ru.jamsys.sbl.jpa.repo;

import org.hibernate.annotations.Immutable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.jpa.dto.TaskDTO;

import java.sql.Timestamp;
import java.util.List;

@Repository
@Transactional
@Immutable
public interface TaskRepo extends CrudRepository<TaskDTO, Long> {
//    @Query("from TaskDTO t where t.dateExecute < :time and t.status = 0")
//    List<TaskDTO> getExecute(@Param("time") Timestamp time);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select t from TaskDTO t where t.dateExecute < :time and t.status = 0 order by t.id")
    List<TaskDTO> getAlready(@Param("time") Timestamp time);

    //@Lock(LockModeType.PESSIMISTIC_WRITE)
    //@Query("select t.id from TaskDTO t where t.id = :id_task")
    //@Modifying(flushAutomatically = true, clearAutomatically = true)
    //@Transactional
    @Query(nativeQuery = true, value = "select * from task where id_task = :id_task for update OF task SKIP LOCKED")
    TaskDTO lock(@Param("id_task") Long id_task);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select t from TaskDTO t where t.status > 0")
    List<TaskDTO> getNormal();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select t from TaskDTO t where t.status < 0")
    List<TaskDTO> getBad();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select t from TaskDTO t where t.status = 0")
    List<TaskDTO> getPrepare();
}
