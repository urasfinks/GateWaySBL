package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.jamsys.sbl.jpa.dto.VirtualServerDto;

import java.sql.Timestamp;
import java.util.List;

public interface VirtualServerRepo extends CrudRepository<VirtualServerDto, Long> {
    @Query("from VirtualServerDto vs1 where vs1.dateRemove < :time and vs1.status = 0")
    List<VirtualServerDto> getRemove(@Param("time") Timestamp time);
}
