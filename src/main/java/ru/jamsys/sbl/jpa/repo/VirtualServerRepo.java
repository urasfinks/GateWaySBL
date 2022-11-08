package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.jamsys.sbl.jpa.dto.VirtualServerDTO;

import java.sql.Timestamp;
import java.util.List;

public interface VirtualServerRepo extends CrudRepository<VirtualServerDTO, Long> {
    @Query("from VirtualServerDTO vs1 where vs1.dateRemove < :time and vs1.status = 0")
    List<VirtualServerDTO> getRemove(@Param("time") Timestamp time);
}
