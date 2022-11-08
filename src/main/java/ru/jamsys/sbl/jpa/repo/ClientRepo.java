package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.repository.CrudRepository;
import ru.jamsys.sbl.jpa.dto.ClientDTO;

public interface ClientRepo extends CrudRepository<ClientDTO, Long> {
}
