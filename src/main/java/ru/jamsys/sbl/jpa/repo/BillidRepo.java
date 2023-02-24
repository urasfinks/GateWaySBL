package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.jamsys.sbl.jpa.dto.BillidDTO;

import java.util.List;

@Repository
public interface BillidRepo extends CrudRepository<BillidDTO, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select t from BillidDTO t where t.idClient = :id_client order by t.idClient")
    List<BillidDTO> findAllByIdClient(@Param("id_client") Long idClient);
}
