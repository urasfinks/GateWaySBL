package ru.jamsys.sbl.message;

import ru.jamsys.sbl.consumer.SBLConsumer;

import java.sql.Date;
import java.sql.Timestamp;

public interface Message {

    void onHandle(MessageHandle handleState, SBLConsumer service);

    String getBody();
    String getCorrelation();

    default String convertTimestamp(long timestamp) {
        Timestamp stamp = new Timestamp(timestamp);
        Date date = new Date(stamp.getTime());
        return date.toString();
    }

    void setError(Exception e);
}
