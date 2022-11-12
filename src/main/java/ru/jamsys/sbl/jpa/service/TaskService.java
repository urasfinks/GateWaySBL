package ru.jamsys.sbl.jpa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.Util;
import ru.jamsys.sbl.jpa.dto.TaskDTO;
import ru.jamsys.sbl.jpa.repo.TaskRepo;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.message.MessageImpl;

import java.sql.Timestamp;
import java.util.List;

@Service
public class TaskService {

    TaskRepo taskRepo;

    @Autowired
    public void setTaskRepo(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }

    @Transactional
    public Message execOneTask() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        List<TaskDTO> listTask = taskRepo.getAlready(timestamp);
        //Util.logConsole(Thread.currentThread(), "::execOneTask count: "+listTask.size());
        if (listTask.size() > 0) {
            TaskDTO task = taskRepo.findOneForUpdate(listTask.get(0).getId());
            if (task != null) {
                //Util.logConsole(Thread.currentThread(), task.toString());
                task.setStatus(1);
                task.setDateUpdate(new Timestamp(System.currentTimeMillis()));
                taskRepo.save(task);
                return new MessageImpl();
            }
        }
        return null;
    }

}
