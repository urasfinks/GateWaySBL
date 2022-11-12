package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.repository.CrudRepository;
import ru.jamsys.sbl.jpa.dto.VirtualServerDTO;

public interface VirtualServerRepo extends CrudRepository<VirtualServerDTO, Long> {

}
