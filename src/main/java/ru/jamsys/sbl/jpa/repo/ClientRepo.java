package ru.jamsys.sbl.jpa.repo;

import org.hibernate.annotations.Immutable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.jpa.dto.ClientDTO;
@Repository
@Transactional
@Immutable
public interface ClientRepo extends CrudRepository<ClientDTO, Long> {
}
