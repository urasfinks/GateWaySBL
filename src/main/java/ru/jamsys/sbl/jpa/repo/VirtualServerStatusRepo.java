package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.jpa.dto.VirtualServerStatusDTO;

@Repository
@Transactional
public interface VirtualServerStatusRepo extends CrudRepository<VirtualServerStatusDTO, Long> {
}
