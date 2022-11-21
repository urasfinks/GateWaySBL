package ru.jamsys.sbl.jpa.service;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.SblApplication;
import ru.jamsys.sbl.jpa.dto.ServerDTO;
import ru.jamsys.sbl.jpa.repo.ServerRepo;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.web.GreetingClient;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.Map;

@Service
public class PingService {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    protected <T> T saveWithoutCache(CrudRepository<T, Long> crudRepository, T entity) {
        //Это самое больше зло, с чем я встречался
        T ret = crudRepository.save(entity);
        try {
            em.flush();
        } catch (Exception e) {
        }
        return ret;
    }

    @Autowired
    public void setServerRepo(ServerRepo serverRepo) {
        this.serverRepo = serverRepo;
    }

    ServerRepo serverRepo;

    @Autowired
    public void setGreetingClient(GreetingClient greetingClient) {
        this.greetingClient = greetingClient;
    }

    GreetingClient greetingClient;

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    public Message exec() {
        Message ret = null;
        synchronized (SblApplication.class) {
            Iterable<ServerDTO> list = serverRepo.findAll();
            for (ServerDTO serverDTO : list) {
                boolean success = true;
                try {
                    String x = greetingClient.nettyRequestGet(
                            "http://" + serverDTO.getIp() + ":3000",
                            "/HealthCheck",
                            5
                    ).block();
                    Map<String, Object> parse = new Gson().fromJson(x, Map.class);
                    String status = (String) parse.get("status");
                    if (!status.equals("OK")) {
                        success = false;
                    }
                } catch (Exception e) {
                    success = false;
                }
                if (success == true) {
                    serverDTO.setPingDate(new Timestamp(System.currentTimeMillis()));
                    serverDTO.setPingStatus(1);
                } else {
                    serverDTO.setPingStatus(-1);
                }
                saveWithoutCache(serverRepo, serverDTO);
            }
        }
        return ret;
    }
}
