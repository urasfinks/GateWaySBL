package ru.jamsys.sbl.jpa.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.sbl.Util;

import java.util.*;

class TaskServiceTest {
    @Test
    void genPass() {
        int count = 1000000;
        Set<String> set = new HashSet<>();
        for (int i = 0; i < count; i++) {
            set.add(Util.genPassword());
        }
        Assertions.assertEquals(count, set.size(), "Получены дубликаты");
    }

    @Test
    void genUser() {
        int count = 100;
        Set<String> set = new HashSet<>();
        for (int i = 0; i < count; i++) {
            set.add(Util.genUser());
        }
        Assertions.assertEquals(count, set.size(), "Получены дубликаты");
    }
}