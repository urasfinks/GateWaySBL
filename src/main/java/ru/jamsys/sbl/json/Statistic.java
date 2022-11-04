package ru.jamsys.sbl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import ru.jamsys.sbl.Util;

//@JsonAlias({"name", "wName"})
//@JsonIgnore
//@JsonProperty("cpu_load")

@Data
public class Statistic {

    @JsonProperty("timestamp")
    public long timestamp = System.currentTimeMillis();
    public int cpu;
}
