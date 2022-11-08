package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.repository.CrudRepository;
import ru.jamsys.sbl.jpa.dto.ServerDTO;

public interface ServerRepo extends CrudRepository<ServerDTO, Long> {

}
