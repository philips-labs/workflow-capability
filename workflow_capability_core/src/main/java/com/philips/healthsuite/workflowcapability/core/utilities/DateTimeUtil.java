package com.philips.healthsuite.workflowcapability.core.utilities;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DateTimeUtil {

    @NotNull
    public static Date getCurrentDateWithTimezone() {
        Date date = new Date();
        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}