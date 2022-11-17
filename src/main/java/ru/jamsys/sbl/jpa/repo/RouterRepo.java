package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.repository.CrudRepository;
import ru.jamsys.sbl.jpa.dto.RouterDTO;

public interface RouterRepo extends CrudRepository<RouterDTO, Long> {
}
