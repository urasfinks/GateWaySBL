package ru.jamsys.sbl.jpa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.jamsys.sbl.component.CmpStatistic;
import ru.jamsys.sbl.jpa.dto.custom.ServerStatistic;
import ru.jamsys.sbl.jpa.repo.RouterRepo;
import ru.jamsys.sbl.jpa.repo.ServerRepo;
import ru.jamsys.sbl.jpa.repo.TaskRepo;
import ru.jamsys.sbl.jpa.repo.VirtualServerRepo;
import ru.jamsys.sbl.message.Message;

import java.util.List;

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
        for (ServerStatistic ss : statistic) {
            cmpStatistic.shareStatistic("countVMSrv" + ss.getName(), ss.getCount());
        }

        return null;
    }
}
