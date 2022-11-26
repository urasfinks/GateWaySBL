package ru.jamsys.sbl.jpa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.jpa.dto.ServerDTO;
import ru.jamsys.sbl.jpa.dto.custom.ServerStatistic;
import ru.jamsys.sbl.jpa.repo.RouterRepo;
import ru.jamsys.sbl.jpa.repo.ServerRepo;
import ru.jamsys.sbl.jpa.repo.TaskRepo;
import ru.jamsys.sbl.jpa.repo.VirtualServerRepo;
import ru.jamsys.sbl.message.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticService {

    CmpStatistic cmpStatistic;
    RouterRepo routerRepo;
    ServerRepo serverRepo;
    VirtualServerRepo virtualServerRepo;

    TaskRepo taskRepo;

    @Autowired
    public void setTaskRepo(TaskRepo taskRepo) {
        this.taskRepo = taskRepo;
    }

    @Autowired
    public void setVirtualServerRepo(VirtualServerRepo virtualServerRepo) {
        this.virtualServerRepo = virtualServerRepo;
    }

    @Autowired
    public void setServerRepo(ServerRepo serverRepo) {
        this.serverRepo = serverRepo;
    }

    @Autowired
    public void setRouterRepo(RouterRepo routerRepo) {
        this.routerRepo = routerRepo;
    }

    @Autowired
    public void setCmpStatistic(CmpStatistic cmpStatistic) {
        this.cmpStatistic = cmpStatistic;
    }


    public Message exec() {

        cmpStatistic.shareStatistic("countRouter", routerRepo.count());
        cmpStatistic.shareStatistic("countServer", serverRepo.count());
        cmpStatistic.shareStatistic("countServerActive", serverRepo.getAlready().size());

        cmpStatistic.shareStatistic("countVMNormal", virtualServerRepo.getNormal().size());
        cmpStatistic.shareStatistic("countVMBad", virtualServerRepo.getBad().size());
        cmpStatistic.shareStatistic("countVMPrepare", virtualServerRepo.getPrepare().size());

        cmpStatistic.shareStatistic("countTaskNormal", taskRepo.getNormal().size());
        cmpStatistic.shareStatistic("countTaskBad", taskRepo.getBad().size());
        cmpStatistic.shareStatistic("countTaskPrepare", taskRepo.getPrepare().size());

        List<ServerStatistic> statistic = serverRepo.getStatistic();
        List<ServerDTO> srv = (List<ServerDTO>) serverRepo.findAll();
        Map<String, Integer> map = new HashMap<>();
        for (ServerDTO serverDTO : srv) {
            map.put("countSrv" + serverDTO.getName() + "_3", 0);
            map.put("countSrv" + serverDTO.getName() + "_2", 0);
            map.put("countSrv" + serverDTO.getName() + "_1", 0);
            map.put("countSrv" + serverDTO.getName() + "_0", 0);
            map.put("countSrv" + serverDTO.getName() + "_-1", 0);
            map.put("countSrv" + serverDTO.getName() + "_-2", 0);
        }
        for (ServerStatistic ss : statistic) {
            ServerDTO serverDTO = serverRepo.findById(ss.getIdSrv()).orElse(null);
            String srvName = serverDTO.getName();
            if (serverDTO != null) {
                map.put("countSrv" + srvName + "_" + ss.getStatusVSrv(), ss.getCount().intValue());
            }
        }
        //System.out.println(map);
        for (String key : map.keySet()) {
            cmpStatistic.shareStatistic(key, map.get(key));
        }
        return null;
    }
}
