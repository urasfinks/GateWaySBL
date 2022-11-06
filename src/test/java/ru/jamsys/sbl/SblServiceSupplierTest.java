package ru.jamsys.sbl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.sbl.component.CmpService;
import ru.jamsys.sbl.message.MessageImpl;
import ru.jamsys.sbl.service.SblServiceSupplier;
import ru.jamsys.sbl.service.SblService;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.function.Consumer;

class SblServiceSupplierTest {

    static ConfigurableApplicationContext context;

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        context = SpringApplication.run(SblApplication.class, args);
        SblApplication.initContext(context, true);
    }

    @Test
    void overclocking() {
        run(1, 5, 6000L, 10, 5, clone -> {
            Assertions.assertTrue(clone.getTpsInput() >= 5 && clone.getTpsInput() < 8, "Должен выдавать минимум 5 tpsInput");
            Assertions.assertTrue(clone.getThreadCount() >= 4, "Должен был разогнаться минимум в 4 потока");
        });
    }

    @Test
    void overTps() {
        run(1, 20, 6000L, 15, 5, clone ->
                Assertions.assertTrue(clone.getTpsOutput() >= 5, "Выходящих тпс должно быть больше либо равно 5"));
    }

    @Test
    void testThreadPark() {
        run(1, 250, 3000L, 20, 250, clone -> {
            Assertions.assertTrue(clone.getTpsInput() > 240, "getTpsInput Должно быть более 240 тпс");
            Assertions.assertTrue(clone.getTpsInput() < 260, "getTpsInput Должно быть меньше 260 тпс");
            Assertions.assertTrue(clone.getTpsOutput() > 240, "getTpsOutput Должно быть более 240 тпс");
            Assertions.assertTrue(clone.getTpsOutput() < 260, "getTpsOutput Должно быть меньше 260 тпс");
            Assertions.assertTrue(clone.getThreadCountPark() > 1 && clone.getThreadCountPark() < 5, "На парковке должно быть от 1 до 5 потоков");
        });
    }

    void run(int countThreadMin, int countThreadMax, long keepAlive, int sleep, int maxTps, Consumer<SblServiceStatistic> fnExpected) {
        Util.logConsole(Thread.currentThread(), "Start test");
        SblService test = context.getBean(CmpService.class).instance("Test", countThreadMin, countThreadMax, keepAlive, 333, () -> {
            Util.sleepMillis(500);
            return new MessageImpl();
        }, message -> {
        });
        test.setDebug(false);
        test.setTpsInputMax(maxTps);
        UtilTest.sleepSec(sleep);
        SblServiceStatistic clone = test.getStatClone();
        Util.logConsole(Thread.currentThread(), "LAST STAT: " + clone);
        if (clone != null) {
            fnExpected.accept(clone);
        }
        context.getBean(CmpService.class).shutdown("Test");
    }

//    @Test
//    void f() throws FileNotFoundException {
//        String crntImage = "MIIQ9AIBAzCCELoGCSqGSIb3DQEHAaCCEKsEghCnMIIQozCCCPcG\n" +
//                "CSqGSIb3DQEHBqCCCOgwggjkAgEAMIII3QYJKoZIhvcNAQcBMBwG\n" +
//                "CiqGSIb3DQEMAQMwDgQIPYSIuglAbtYCAggAgIIIsCVmIgTjNomJ\n" +
//                "JEXnc8jr89KBZHKNVtdPtotbaA/TwlQHb16WCYTLh1JddRvoLy0H\n" +
//                "pAidKEa3CmftOQB77QJYAZQ1zEC7+vGO2tUB4AA9b2DEStjEIrQc\n" +
//                "TOaAFzAHV1VT4udxeO0YbnEGoiTK9hfau1wBYmYnqoHrDXlDM9kv\n" +
//                "vQhc/8iCbJqjmbTdEN7aoFw8na1SmgUBgn3R2wHI71lc257UJ4ib\n" +
//                "xHBsyMmbQWL3SY9I/yYZe3LUrPWb4fDSqnUS0vm0fV0lLxs4meBs\n" +
//                "5ngHLzOXrsTZXIXEw5lb1qh/i1k6vjDzaLHczpjq5uNkM3cMqmjX\n" +
//                "Ltttd0NZ0MvqnU4P87ckRp8o/xUCBcB+V22D4pLES7g0qf7euHlk\n" +
//                "Y4DYwQRA7g6yULBgXdk6Z4JlzUDuGo0RJ/UPsyxggIFO58tkAYpn\n" +
//                "HHqIQDnAp6vw0dbpr7+BLz7UCct9nme92v3SN0LUUwjY2TtuBSpR\n" +
//                "ppG+KZshnJNoBbIppEVHDjVwrgsgTVK2mV5gbTX+stlTdLnZ961Y\n" +
//                "YHuG5nXuWr9/JexTDfZdp4fQ3fG8I3+n0ZA1+I9PSHBgB73Le+5c\n" +
//                "j2K+eEW9Lzw6pVBCbS8yq40EMwdTXCowSfO6KHrmptGOfE0Alnnv\n" +
//                "exZg1mVmmmEV1/qxyIOD6+AdGZ8mK89Hy8JwN9gat2SRNKLEw2ld\n" +
//                "N88DEoGXysEr9RX19lnqX+EmOTQ7TBF3NCrRm16ltiuvaBmQFw89\n" +
//                "YxhkL3ihUOgCYF3EVJyR84sXOxa4jM5eDSNA5jtxNRO8EI3FnNMA\n" +
//                "t3oYgg3A3T3ffalLUqJfUrLmovRl/hzszM+7kL3A4ERs0rEdd53B\n" +
//                "hKrLwBQF9lZEfq5GYjaariw/H92mIqWuN3VCQYua9vgGDT0Ukbz8\n" +
//                "kJS7nQi6Wv0GGQeOm/Fk6omHYkcybEwFZtkyVPcE7lVEz2Y6p/yC\n" +
//                "GX388Sf3QPdvu5dky1nyHIDm2NKbVBKcbS1MC6qUEGRlrnE6VPX2\n" +
//                "naYwTq1B3eZeYvJVcV+vArkdeo/yCkmj1PFknhbopdmp2D/Om8K/\n" +
//                "u2TovW+yAx3WHmuAsCSG+s9fGlYg+HRpIqgybiCLk0TVI1zI+IIZ\n" +
//                "aYbCYoMhW2KnYx8XbVgmZH5oaSlI5CUs+aj4zCyc/fYV3ZxbHl8Q\n" +
//                "fWJxWTX5v/b4HsT1ool7AAHOe2tDdTOWMzD3X6iV+Lr7De/+mkN1\n" +
//                "Sz3Yd/nP8KHxyqCX7Q3XXl00z2fqbsTmuNSfRqFMbGLzGPuRxflo\n" +
//                "NAbiAauoi+tJ39eSEOjaMoNr1iSIxKhSppZ4GEynYJUmeyc83Dk9\n" +
//                "/cPdepXMA1TUnFtoegxV903RIgiUCILJ0ZkHaiKkhrPyiQYS2Hos\n" +
//                "mHbblMZekWnZu/2rqvmTRxLmQkuN1RaYUSKCEc7CLHkXV+PRgvuk\n" +
//                "Zq16NOdBzSFfJnZLd/UYlowZll00+z2XodbfK+Elm5Ci3EQE9lxo\n" +
//                "shc1HUNJ4pz5ph9BytHL7Kazb/9Qr2uw0w0NbNRPSr/JVKX6QR4T\n" +
//                "w37gQcWtFBTIw2+XYjWI2EwY+G97Uy3KT70+hqjBNSXaOUVIsUAX\n" +
//                "sfQObsBxXOW4MaQJ7F8w55rQ2ck/jHm9RCIn55+tyeErkWQW8FtP\n" +
//                "lW9iMyqYtZUq004CrPL2VKuxaknV7zIxv3yR9J7YpDgnG5C1JoUU\n" +
//                "122teP5GotShjkywWguNopXOHdX3OWtipd151Pn2cKJvd4gaoKqo\n" +
//                "4uTMr+OvJhKncxUkl/VcgbIiiL6vOp7jjg+4aSLOxIaVKV0MJz9W\n" +
//                "1SkPdn+XWl1Z5HAbZLpH+OzjlO+5VJu/8jqZgKk1WbFTHoVLHSwB\n" +
//                "oav0dDJK/Rnc1oyhbz7FpTFbn7qILxta0MH+TNJD0ljNLnLoGqYU\n" +
//                "9D8/Uyz54+B2K5Ov9FimHKHNA1ah5CY/J0au0U8J9I0enboYjE8o\n" +
//                "aqkMK/3aEUGeRvclBl/Tq+H4XaFV0iNgJHN6aoAau52WUvkir/tx\n" +
//                "cs4kBbKUG+s29DypSZzd1vamnn86R42tg9zyYKGrKNmttHbCTnQF\n" +
//                "7TvqwRmVHklob4Fq+nh44v0xFa/Y1UvoIh2APa7kHUblbwctKoUd\n" +
//                "NR1ljslinq5LzkaZ3bqhVFGYzYaZ4gkvhDu3t4iV6Cujmt+NO8C1\n" +
//                "oVyPTCTT4aUquvep06IeMUHy8X2pevIuFNTM9POVSNTxbkzkclmB\n" +
//                "1alzziMP2m4oN5Qn0KknVWIxxHHQ22e1sn7J6Bi4aqTnGSLnHyp6\n" +
//                "pkMOmB027ZKAAeH6lPG4Zny7afnJ8ffGl6AZbYYq2wkZ8RKi8FAL\n" +
//                "7pmRGivP4tHlDurl8vBxBjbbnavyJ0t7DeydTY7glX1j6cVzlIWU\n" +
//                "/ymfwsbSV+fOsqHiGm9/t6H7PxL56XACMEjdwIFgTRtRQ8RXCTaT\n" +
//                "l55OBN2wg+EmyD0o8ItpxqXdiMQsxJIHywoRyfMkYhqHAcW2iLmY\n" +
//                "iac5dmZ0CyYBafQ7DRuAeBBOIYayAgUkPVbN0XmPDIkQM1VHzLiM\n" +
//                "YhVZw8rMIzJUUNV508hO/FVMwmZ3Ns3Ew2FyIEgJ5N1ba3PjZvFF\n" +
//                "YYB93BMdiGFFpnn3PzkXRo9jYUmOKMS6AocMYdw3jhD1njz/l3ba\n" +
//                "zXtbVsp1q0IrjOYBuhroo6e/ImFKrnfuivAhcb0PnoVGqDVdQr3Y\n" +
//                "syUGdIn8jQlk49q5YTn7o29A6YXfx/JHbhHx1/0n2TcZNlb9K9jT\n" +
//                "MT9YxmK5mhNiaxZbIR1JMI2W1GAKIY7mIyttFnqDX04uWKlQI9cQ\n" +
//                "gK1XNh4V7frfYOkXNNJfMD+/s5bk/AFqNrRbLFROYI1A+ebCptnA\n" +
//                "gwQhpZXSRDg7Y9nCm5RucpKnIxdzSLCq//f4FqYgHk49wo7nlBW9\n" +
//                "fzIW0pt1iV3YdgIHFKEvD35HPLVCPKqTTLy2FnhKuQC6gBJKbd0L\n" +
//                "ESWKtHaKXFJVcTSOzevNhBxASJp7AZ9UE5/6VuYC+y0wggekBgkq\n" +
//                "hkiG9w0BBwGgggeVBIIHkTCCB40wggeJBgsqhkiG9w0BDAoBAqCC\n" +
//                "By4wggcqMBwGCiqGSIb3DQEMAQMwDgQIITHpuq6mTqcCAggABIIH\n" +
//                "CCjWeNQDaQ6ZXJ/eslM4zgOhSpibOyfZiRqE0Vufq4e0Z5JGOqJT\n" +
//                "wzK1BRMQBOajt0sAes8F7cjvgjHfB6VRNz8/1l64CTaDbkEcwlvU\n" +
//                "JUvAHbX54HpLk1nJ1fIr5RuRXWfNThYj0uvXZtSEim/jUUXCRjhf\n" +
//                "hOxWHu/AXi6z0Z6edZ3WdUTVHYdu4fIZptCHjR/WP72UPv21XkzY\n" +
//                "bvF8k0La737xgToPdn7dYQxn0UFzGRp3LrpWGU7ErpHG+m8KLph9\n" +
//                "1R6/fLGN5cRVn3hnDM0Y9sPA9l02d07R5w508SWfvmS1Y8LhlWGB\n" +
//                "vfZCaFUJSbIlRporM+cd5S/WwKrUIt2A4lUMFIVLhKZ5NJvP/Ndv\n" +
//                "zmD5VGiBITapSuw6Cv51h65F4CY4mPemUcugcj59BFucebj7iPct\n" +
//                "hVrBO0820kd3BoFHi68KQCxaL+IG8dMeISqCPrkjmo03XJVEa+KC\n" +
//                "Y+c20/YmzDxhXf124pzyjAZ1GBNEu4zszRVcnYLWqXG//4eNTJq+\n" +
//                "xk6yRIWVtn3v9s9zw2ZvVPnGFe+wU7UPE8MU0MMVZgsgxj/thMg3\n" +
//                "/hreB8mhHF85H60J9lzc0aaYWYzstRczUun6hDnvfcrfMHBEsrL9\n" +
//                "EyV925tqrCbQWhEigsGqQQRy4W65QsKAImcTIA+uhHdj4Pyh+Koi\n" +
//                "QqOdS+kp87bldXHFbNFCSl+tGOWcPIMIo+Fc39BEl3ACgq2nd8PK\n" +
//                "gvU/7zwCUIteZ302bWMI7cW0f9/aaiX2YMXPhalFs5TaySJDF5rM\n" +
//                "bdRPO1jHp1mb05mvOBYhQb/U/JW57kSiZYjAJE7NEszywgcL5Bum\n" +
//                "qSjFWHm8Sw5VmmcoghmpaTG58xH8E/4+sNFTy7RSegrs/j5G3yCb\n" +
//                "3K7r9/LJ4m+GmQ6hXQr4o+hzQRwqZ7/IY7vHD/eNlELt7ubaiR5n\n" +
//                "lcEP4bW3LX44/uk6ECpgQxbMkdVKU3Z2PEepkXRgcsLVOdMClZuy\n" +
//                "pe+OWQkRgm09E8D4jBnUwxQfZkVW7PLn/hLvl0n81FU2hHDs+sG5\n" +
//                "z3GeaTEqTPN8VmBL/Qv2RHp1LFwDpxCxGedv1e26FXiWuvzMCGQM\n" +
//                "KCX8MjfzKmO0HQ4YmYfaSvHVfPStvje16Okf1kM5pNaIXUoj8Iz1\n" +
//                "yTPmVotb/a3FtrIqFwRN4jhC7vWMxlY3iI7spbDG+Wkx6fsAQOtL\n" +
//                "6y1ZaqCxQbq5v8dS+OUdxCodhnQr3P7kwHdnpeIWbW0GqNdhFK0A\n" +
//                "htLBalSBx/VYuZd0zAXKC4ExuAQJNudXHA40tVFNgi2FMeuOJ9ib\n" +
//                "OarrleQwuhGvKfFwtWSkoZmXbcB+5/stOcAoCisy0f1caBGgLPhN\n" +
//                "RJYUrpmaNL+vJCDGzpYTYchAxcE4lyIWf8OcfdocESPC6crdXuZM\n" +
//                "IWOIeNvSw7gH6Zk+soiRztgMQmk03JnRqPHA9+VIbMNpamtXhxPI\n" +
//                "lz+pwAvUnx2qNEu1dz2NfQKVT93q+JDPsI1mk29WxLK/672rKg0c\n" +
//                "SUv3XBl4msl2zyOHVl1KYuX+TpaTOVP1isyvE8OkqO5Ud/ylg68F\n" +
//                "jz3V/hGbElm3k2aIdjn1YRr39dM0R1ym+971AKI2tl8KVct2pFJl\n" +
//                "JlDzol90wKXT5hLQtoifzBJDU5TSI9PCaO/l2i1O+ACXQWrbksHf\n" +
//                "4//vQ2Kl5DlL3bs10QOfNiVU6gaZEnH6siy9VOlL3wVtsPHCoXUV\n" +
//                "LmwE/Mmm7xpKt8ae+wi7/W/MJlfSLjfhdc/Vgg30XVEvuuksOQQ6\n" +
//                "mzDDUWefz5IYj0EZFa5sZcl6nSLt8wv4dLuc+NGQaw44RGnLeru3\n" +
//                "yEqaEicQTKBFeO6W2OGrCX6b6RsaeGjpBq6B0ec8I6crbOQy97FC\n" +
//                "4P2MdS4M7N8aCdWVm2JuxbsyLvkGJrFmcVemJmg7scy9D6HnuYiA\n" +
//                "rzm8F/hLXyaOAFLbmUl4vo+32aPjZj8JcAKlPUzYoA0YbX678/uN\n" +
//                "3QvdghUq/jtS3a4HxeEBfHLK2gct/aPdIiu37iL1R+81XU2DGfha\n" +
//                "EIqsTjCpcezCWYQ2VdDRUiGti3QWKUNRTQfBZAwTPAklnwle5t4I\n" +
//                "0w/tsk0I64JXmsLBpfpdrhSrqoPtVzn88jlrSnrFSKO3VqaV/HH+\n" +
//                "p286/RxS4MnoHJ8Zs+8VACxhD05hc/DKmXRlOD8EhN/xPt/NdpUd\n" +
//                "phYJYbzFj7pG5BYFxx9pIcudc6Jwvyb5zVckuv+CVq0Gd7pAMCT1\n" +
//                "lSBH62ZkQkqNLudmQokzRqzGZXrosCtFnXuSnqJB++8YTNLkTj4u\n" +
//                "U/aV4DeZwwQ2QtdJr4sPuJj/qT2P7KX6Dzl+6/mKAiUlIdXJKOxe\n" +
//                "9Zx9Lo9L4Lssqv0mKc8jImTbNFX6toCnTGH8uMmwYNkdCEd2nwt0\n" +
//                "UxbzqduRGjFIMCEGCSqGSIb3DQEJFDEUHhIAdgBwAG4AYwBsAGkA\n" +
//                "ZQBuAHQwIwYJKoZIhvcNAQkVMRYEFHohcaiBgS+2sI7Xlg19b591\n" +
//                "beSfMDEwITAJBgUrDgMCGgUABBQGabRdCSEv1Mtu/fmokY7ZkSjH\n" +
//                "pQQI3k2/aeSvgjgCAggA\n";
//        byte[] data = Base64.getDecoder().decode(crntImage.replace("\n",""));
//        try (OutputStream stream = new FileOutputStream("output.p12")) {
//            stream.write(data);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    @Test
    void getNeedCountThread() {
        Assertions.assertEquals(125, SblServiceSupplier.getNeedCountThread(SblServiceStatistic.instanceSupplierTest(500, 100, 150, 1), 250, true), "#1");
        Assertions.assertEquals(63, SblServiceSupplier.getNeedCountThread(SblServiceStatistic.instanceSupplierTest(500, 100, 150, 125), 250, true), "#2");
        Assertions.assertEquals(0, SblServiceSupplier.getNeedCountThread(SblServiceStatistic.instanceSupplierTest(500, 100, 0, 1), 250, true), "#3");
        Assertions.assertEquals(10, SblServiceSupplier.getNeedCountThread(SblServiceStatistic.instanceSupplierTest(500, 50, 10, 1), 250, true), "#4");
        Assertions.assertEquals(10, SblServiceSupplier.getNeedCountThread(SblServiceStatistic.instanceSupplierTest(0, 50, 10, 1), 250, true), "#5");
    }

}