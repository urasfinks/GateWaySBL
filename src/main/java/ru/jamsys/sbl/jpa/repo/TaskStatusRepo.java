package ru.jamsys.sbl.jpa.repo;

import org.springframework.data.repository.CrudRepository;
import ru.jamsys.sbl.jpa.dto.TaskStatusDTO;

public interface TaskStatusRepo extends CrudRepository<TaskStatusDTO, Long> {
}
