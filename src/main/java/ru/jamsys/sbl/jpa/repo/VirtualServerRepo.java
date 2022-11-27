package ru.jamsys.sbl.jpa.repo;

import org.hibernate.annotations.Immutable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.jpa.dto.VirtualServerDTO;

import java.util.List;

@Repository
public interface VirtualServerRepo extends CrudRepository<VirtualServerDTO, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select t from VirtualServerDTO t where t.idSrv = :idServer order by t.portLocal desc ")
    List<VirtualServerDTO> getPortServer(@Param("idServer") Long idServer);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select vs from VirtualServerDTO vs inner join ServerDTO s on s.id = vs.idSrv where s.idRouter = :idRouter order by vs.portRouter desc ")
    List<VirtualServerDTO> getPortRouter(@Param("idRouter") Long idRouter);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select vs from VirtualServerDTO vs where vs.status > 0")
    List<VirtualServerDTO> getNormal();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select vs from VirtualServerDTO vs where vs.status = -1") //-1: Bad request; -2: Remove
    List<VirtualServerDTO> getBad();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select vs from VirtualServerDTO vs where vs.status = 0")
    List<VirtualServerDTO> getPrepare();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    List<VirtualServerDTO> findAllByIdSrv(Long idSrv);

}
