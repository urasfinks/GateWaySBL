package ru.jamsys.sbl.jpa.service;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.SblApplication;
import ru.jamsys.sbl.jpa.dto.ServerDTO;
import ru.jamsys.sbl.jpa.dto.VirtualServerDTO;
import ru.jamsys.sbl.jpa.repo.ServerRepo;
import ru.jamsys.sbl.jpa.repo.VirtualServerRepo;
import ru.jamsys.sbl.message.Message;
import ru.jamsys.sbl.web.GreetingClient;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PingService {

    private VirtualServerRepo virtualServerRepo;

    @Autowired
    public void setVirtualServerRepo(VirtualServerRepo virtualServerRepo) {
        this.virtualServerRepo = virtualServerRepo;
    }

    @PersistenceContext
    private EntityManager em;

    protected <T> T saveWithoutCache(CrudRepository<T, Long> crudRepository, T entity) {
        return SblApplication.saveWithoutCache(em, crudRepository, entity);
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

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Message exec() {
        Message ret = null;
        synchronized (SblApplication.class) {
            Iterable<ServerDTO> list = serverRepo.findAll();
            for (ServerDTO serverDTO : list) {
                boolean success = true;
                try {
                    String x = greetingClient.nettyRequestGet(
                            "http://" + serverDTO.getIp() + ":" + serverDTO.getPort(),
                            "/HealthCheck",
                            5
                    ).block();
                    /*{
                          "status": "OK",
                          "data": [
                            {
                              "name": "win_16252",
                              "status": 2,
                              "statusDesc": "powered off",
                              "date": "2022-11-23T20:45:16.511000000"
                            },
                            {
                              "name": "win_16253",
                              "status": 3,
                              "statusDesc": "running",
                              "date": "2022-11-23T20:49:46.787000000"
                            },
                            {
                              "name": "win_16254",
                              "status": 3,
                              "statusDesc": "running",
                              "date": "2022-11-23T20:53:34.081000000"
                            },
                            {
                              "name": "win_16452",
                              "status": 3,
                              "statusDesc": "running",
                              "date": "2022-11-23T21:35:02.944000000"
                            }
                          ]
                        }
                    */
                    List<Long> marked = new ArrayList<>();
                    Map<String, Object> parse = new Gson().fromJson(x, Map.class);
                    String status = (String) parse.get("status");
                    if (!status.equals("OK")) {
                        success = false;
                    } else {
                        List<Map<String, Object>> listVM = (List<Map<String, Object>>) parse.get("data");
                        Iterable<VirtualServerDTO> listVirtualServer = virtualServerRepo.findAll();
                        for (Map<String, Object> vm : listVM) {
                            try {
                                String nameVM = (String) vm.get("name");
                                int statusVM = ((Double) vm.get("status")).intValue();
                                if (nameVM != null) {
                                    String[] split = nameVM.split("_");
                                    if (split.length == 2) {
                                        Long idVSrv = Long.parseLong(split[1]);
                                        marked.add(idVSrv);
                                        //if (idVSrv != null) {
                                            for (VirtualServerDTO virtualServerDTO : listVirtualServer) {
                                                if (virtualServerDTO.getId().equals(idVSrv)) {
                                                    if (virtualServerDTO.getStatus() != -2) { //Если сервер помечен как удалён, не надо ему ничего менять
                                                        virtualServerDTO.setStatus(statusVM);
                                                    } else {
                                                        System.out.println("VirtualServer " + virtualServerDTO.getId() + " marked status -2, ping skip");
                                                    }
                                                    virtualServerDTO.setVmStatusDate((String) vm.get("date"));
                                                    saveWithoutCache(virtualServerRepo, virtualServerDTO);
                                                    break;
                                                }
                                            }
                                        //}
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        //Проверим все оставшиеся VM если по ним вдруг не пришёл статус
                        List<VirtualServerDTO> all = virtualServerRepo.findAllByIdSrv(serverDTO.getId());
                        for (VirtualServerDTO virtualServerDTO : all) {
                            if (!marked.contains(virtualServerDTO.getId()) && virtualServerDTO.getStatus() > 0) {
                                virtualServerDTO.setStatus(-2);
                                virtualServerDTO.setVmStatusDate(LocalDateTime.now().toString());
                                saveWithoutCache(virtualServerRepo, virtualServerDTO);
                            }
                        }
                    }
                } catch (Exception e) {
                    success = false;
                }
                if (success == true) {
                    serverDTO.setPingDate(new Timestamp(System.currentTimeMillis()));
                    serverDTO.setTryPingDate(new Timestamp(System.currentTimeMillis()));
                    serverDTO.setPingStatus(1);
                } else {
                    serverDTO.setPingStatus(-1);
                    serverDTO.setTryPingDate(new Timestamp(System.currentTimeMillis()));
                }
                saveWithoutCache(serverRepo, serverDTO);
            }
        }
        return ret;
    }
}
