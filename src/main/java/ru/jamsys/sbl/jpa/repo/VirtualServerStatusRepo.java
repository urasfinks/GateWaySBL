package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.repository.CrudRepository;
import ru.jamsys.sbl.jpa.dto.VirtualServerStatusDto;

public interface VirtualServerStatusRepo extends CrudRepository<VirtualServerStatusDto, Long> {
}
