package ru.jamsys.sbl.message;

import ru.jamsys.sbl.jpa.dto.ServerDto;
import ru.jamsys.sbl.service.SblService;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

public interface Message {

    void onHandle(MessageHandle handleState, SblService service);

    @SuppressWarnings("unused")
    String getBody();

    @SuppressWarnings("unused")
    String getCorrelation();

    default String convertTimestamp(long timestamp) {
        Timestamp stamp = new Timestamp(timestamp);
        Date date = new Date(stamp.getTime());
        return date.toString();
    }

    void setError(Exception e);

    <T> T getHeader(String name);
}
