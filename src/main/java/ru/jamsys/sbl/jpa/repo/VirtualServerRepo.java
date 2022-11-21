package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import ru.jamsys.sbl.jpa.dto.VirtualServerDTO;
import ru.jamsys.sbl.jpa.dto.custom.ServerStatistic;

import java.util.List;

public interface VirtualServerRepo extends CrudRepository<VirtualServerDTO, Long> {

    @Query("select t from VirtualServerDTO t where t.idSrv = :idServer order by t.portLocal desc ")
    List<VirtualServerDTO> getPortServer(@Param("idServer") Long idServer);

    @Query("select vs from VirtualServerDTO vs inner join ServerDTO s on s.id = vs.idSrv where s.idRouter = :idRouter order by vs.portRouter desc ")
    List<VirtualServerDTO> getPortRouter(@Param("idRouter") Long idRouter);

    @Query("select vs from VirtualServerDTO vs where vs.status = 1")
    List<VirtualServerDTO> getNormal();

    @Query("select vs from VirtualServerDTO vs where vs.status = -1")
    List<VirtualServerDTO> getBad();

    @Query("select vs from VirtualServerDTO vs where vs.status = 0")
    List<VirtualServerDTO> getPrepare();

}
