package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.jamsys.sbl.jpa.dto.VirtualServerDTO;

import java.util.List;

public interface VirtualServerRepo extends CrudRepository<VirtualServerDTO, Long> {

    @Query("select t from VirtualServerDTO t where t.idSrv = :idServer order by t.portLocal desc ")
    List<VirtualServerDTO> getPortServer(@Param("idServer") Long idServer);

    @Query("select t from VirtualServerDTO t where t.idRouter = :idRouter order by t.portRouter desc ")
    List<VirtualServerDTO> getPortRouter(@Param("idRouter") Long idRouter);

}
