package ADG.Utils;

import ADG.UUID;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;

import java.util.Collection;
import java.util.Date;

public class Cookie {

    private static final String PLAYERID  = "playerid";
    private static final String USERNAME  = "username";
    private static final String LANGUAGE  = "language";

    private static final long MILLIS_400_DAYS = 400L * 24L * 60L * 60L * 1000L;

    public static String getPlayerId(){
        createPlayerIdCookie();
        return Cookies.getCookie(PLAYERID);
    }

    public static void createPlayerIdCookie(){
        Collection<String> cookieNames = Cookies.getCookieNames();
        if(!cookieNames.contains(PLAYERID)){
            Cookies.setCookie(PLAYERID, UUID.get(), longTermExpiry(), null, "/", isSecure());
        }
    }

    public static String getUsername() {
        String value = Cookies.getCookie(USERNAME);
        return value != null ? value : "";
    }

    public static void setUsername(String username) {
        Cookies.setCookie(USERNAME, username, longTermExpiry(), null, "/", isSecure());
    }

    public static Language getLanguage() {
        String value = Cookies.getCookie(LANGUAGE);
        if (value != null) {
            try { return Language.valueOf(value); } catch (IllegalArgumentException ignored) {}
        }
        return Language.en;
    }

    public static void createLanguageCookie() {
        Collection<String> cookieNames = Cookies.getCookieNames();
        if (!cookieNames.contains(LANGUAGE)) {
            Cookies.setCookie(LANGUAGE, Language.en.name(), longTermExpiry(), null, "/", isSecure());
        }
    }

    public static void setLanguage(Language language) {
        Cookies.setCookie(LANGUAGE, language.name(), longTermExpiry(), null, "/", isSecure());
    }

    public static void changeLanguage(Language language) {
        Cookies.setCookie(LANGUAGE, language.name(), longTermExpiry(), null, "/", isSecure());
        reloadWithLocale(language.name());
    }

    // Returns true if a navigation was triggered (caller should return immediately).
    public static boolean syncGwtLocale() {
        Language lang = getLanguage();
        String urlLocale = Window.Location.getParameter("locale");
        if (!lang.name().equals(urlLocale)) {
            reloadWithLocale(lang.name());
            return true;
        }
        return false;
    }

    private static void reloadWithLocale(String locale) {
        String hash = Window.Location.getHash();
        Window.Location.replace(Window.Location.getPath() + "?locale=" + locale + hash);
    }

    private static Date longTermExpiry() {
        return new Date(new Date().getTime() + MILLIS_400_DAYS);
    }

    private static boolean isSecure() {
        return Window.Location.getProtocol().startsWith("https");
    }
}
