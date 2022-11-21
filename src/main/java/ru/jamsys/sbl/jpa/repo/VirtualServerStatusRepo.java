package ru.jamsys.sbl.jpa.repo;

import org.hibernate.annotations.Immutable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.jpa.dto.VirtualServerStatusDTO;

@Repository
@Transactional
@Immutable
public interface VirtualServerStatusRepo extends CrudRepository<VirtualServerStatusDTO, Long> {
}
