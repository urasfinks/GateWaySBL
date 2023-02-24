package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.jamsys.sbl.jpa.dto.DeleteTimeDTO;

import java.util.List;

@Repository
public interface DeleteTimeRepo extends CrudRepository<DeleteTimeDTO, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select t from DeleteTimeDTO t where t.idSrv = :id_v_srv order by t.idSrv")
    List<DeleteTimeDTO> findAllByIdClient(@Param("id_v_srv") Long idSrv);
}
