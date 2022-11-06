package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.jamsys.sbl.jpa.dto.ServerDto;
import ru.jamsys.sbl.jpa.dto.VirtualServerDto;

import java.util.List;

public interface VirtualServerRepo extends CrudRepository<VirtualServerDto, Long> {
    @Query("from VirtualServerDto vs1 where vs1.dateRemove < :time")
    List<ServerDto> getRemove(@Param("time") Integer time);
}
