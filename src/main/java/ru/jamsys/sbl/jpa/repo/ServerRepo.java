package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.repository.CrudRepository;
import ru.jamsys.sbl.jpa.dto.ServerDto;

public interface ServerRepo extends CrudRepository<ServerDto, Long> {

}
