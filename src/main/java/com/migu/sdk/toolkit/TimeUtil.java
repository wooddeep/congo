package com.migu.sdk.toolkit;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by lihan on 2017/11/30.
 */
public class TimeUtil {

    private static Date getStartTime() {
        Calendar todayStart = Calendar.getInstance();
        todayStart.set(Calendar.HOUR, 0);
        todayStart.set(Calendar.MINUTE, 0);
        todayStart.set(Calendar.SECOND, 0);
        todayStart.set(Calendar.MILLISECOND, 0);
        return todayStart.getTime();
    }

    private static Date getEndTime() {
        Calendar todayEnd = Calendar.getInstance();
        todayEnd.set(Calendar.HOUR, 23);
        todayEnd.set(Calendar.MINUTE, 59);
        todayEnd.set(Calendar.SECOND, 59);
        todayEnd.set(Calendar.MILLISECOND, 999);
        return todayEnd.getTime();
    }

    public static long getSpareSecToday() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long end = getEndTime().getTime();
        long now = System.currentTimeMillis();
        System.out.println(end);
        System.out.println(now);
        return (end - now) / 1000;
    }

    public static String getPrevTimeAgo(int years, int months, int days, int minutes, int seconds) {
        return "";
    }

    public static String getPrevTimeAgo(int minutes) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        long now = System.currentTimeMillis();
        long prev = now - minutes * 60 * 1000; // 之前的毫秒数
        Timestamp ts = new Timestamp(prev);
        return sdf.format(ts);
    }

    public static long getPrevTimeAgoSec(int minutes) {
        long now = System.currentTimeMillis();
        long prev = now / 1000 - minutes * 60 ; // 之前的秒数
        return prev;
    }
}
