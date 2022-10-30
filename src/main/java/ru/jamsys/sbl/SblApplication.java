package ru.jamsys.sbl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.component.CmpHelper;
import ru.jamsys.sbl.component.CmpStatistic;

@SpringBootApplication
public class SblApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SblApplication.class, args);
        context.getBean(CmpStatistic.class).run();
        context.getBean(CmpHelper.class).run();
        context.getBean(CmpService.class).createConsumer("Test", 1, 10, 60000L, msg ->
            System.out.println(msg.getCorrelation())
        );
        //GreetingClient greetingClient = context.getBean(GreetingClient.class);
        // We need to block for the content here or the JVM might exit before the message is logged
        //System.out.println(">> message = " + greetingClient.getMessage().block());
//        LinkedBlockingQueue<Runnable> runnables = new LinkedBlockingQueue<>(10);
//        ThreadPoolExecutor executorService = new ThreadPoolExecutor(10, 10, 10L, TimeUnit.SECONDS, runnables);
//        executorService.allowCoreThreadTimeOut(true);
//        for (int i = 0; i < 15; i++) {
//            executorService.submit(new RTask());
//        }
//        System.out.println(runnables.stream().count());
        //executorService.shutdown();
//        ConsumerService test = new ConsumerService("Test", 1, 10, (msg) -> {
//            System.out.println(msg.getCorrelation());
//        });
//
//
//        Thread t1 = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true){
//                    for (int i = 0; i < 5; i++) {
//                        Message message = new MessageImpl();
//                        test.handle(message);
//                    }
//                    try{
//                        Thread.sleep(5000);
//                    }catch (Exception e){}
//                    //LockSupport.park();
//                }
//
//            }
//        });
//        t1.start();


//        ArrayList<String> strings = new ArrayList<>();
//        strings.add("Hello");
//        strings.add("world");
//        try {
//            Util.forEach(Util.copyToArrayString(strings), (item)->{
//                System.out.println(item);
//            });
//        }catch (Exception e){}

    }


}
