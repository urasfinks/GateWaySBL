package ru.jamsys.sbl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpThreadStabilizer;
import ru.jamsys.sbl.component.CmpStatistic;

import java.util.stream.IntStream;

@SpringBootApplication
public class SblApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SblApplication.class, args);
        context.getBean(CmpStatistic.class).run();
        context.getBean(CmpThreadStabilizer.class).run();
    }


}
