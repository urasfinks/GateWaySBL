package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.jamsys.sbl.jpa.dto.TaskDTO;

import javax.persistence.LockModeType;
import java.sql.Timestamp;
import java.util.List;

public interface TaskRepo extends CrudRepository<TaskDTO, Long> {
//    @Query("from TaskDTO t where t.dateExecute < :time and t.status = 0")
//    List<TaskDTO> getExecute(@Param("time") Timestamp time);

    @Query("select t from TaskDTO t where t.dateExecute < :time and t.status = 0")
    List<TaskDTO> getAlready(@Param("time") Timestamp time);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TaskDTO t where t.id = :id_task")
    TaskDTO findOneForUpdate(@Param("id_task") Long time);
}
