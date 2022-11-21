package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.jpa.dto.ClientDTO;
@Repository
@Transactional
public interface ClientRepo extends CrudRepository<ClientDTO, Long> {
}
