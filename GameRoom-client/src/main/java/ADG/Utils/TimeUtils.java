package ADG.Utils;

import com.google.gwt.i18n.client.DateTimeFormat;

import java.util.Date;

public class TimeUtils {
    public static String getCurrentTime() {
        Date currentDate = new Date();
        DateTimeFormat timeFormat = DateTimeFormat.getFormat("HH:mm");
        return timeFormat.format(currentDate);
    }

    public static String convertUTCToLocal(String utcTime) {
        String[] parts = utcTime.split(":");
        int utcHours = Integer.parseInt(parts[0]);
        int utcMinutes = Integer.parseInt(parts[1]);
        // getTimezoneOffset() returns (UTC - local) in minutes
        int totalMinutes = utcHours * 60 + utcMinutes - getTimezoneOffset();
        totalMinutes = ((totalMinutes % 1440) + 1440) % 1440;
        return twoDigit(totalMinutes / 60) + ":" + twoDigit(totalMinutes % 60);
    }

    private static native int getTimezoneOffset() /*-{
        return new Date().getTimezoneOffset();
    }-*/;

    private static String twoDigit(int n) {
        return (n < 10 ? "0" : "") + n;
    }
}
