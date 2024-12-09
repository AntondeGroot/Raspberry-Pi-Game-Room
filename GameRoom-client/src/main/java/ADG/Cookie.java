package ADG;

import com.google.gwt.user.client.Cookies;

import java.util.Collection;

public class Cookie {

    private static final String PLAYERID = "playerid";

    public static String getPlayerId(){
        createPlayerIdCookie();
        return Cookies.getCookie(PLAYERID);
    }

    public static void createPlayerIdCookie(){
        Collection<String> cookieNames = Cookies.getCookieNames();
        if(!cookieNames.contains(PLAYERID)){
            Cookies.setCookie(PLAYERID, uuid());
        }
    }

    private native static String uuid() /*-{
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g,
            function(c) {
                var r = Math.random() * 16 | 0, v = c == 'x' ? r
                    : (r & 0x3 | 0x8);
                return v.toString(16);
            });
    }-*/;
}
