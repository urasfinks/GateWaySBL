package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.repository.CrudRepository;
import ru.jamsys.sbl.jpa.dto.VirtualServerStatusDTO;

public interface VirtualServerStatusRepo extends CrudRepository<VirtualServerStatusDTO, Long> {
}
