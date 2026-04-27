package ADG.i18n;

import com.google.gwt.core.client.GWT;

public class I18n {

    private static final AppConstants CONSTANTS = GWT.create(AppConstants.class);
    private static final AppMessages  MESSAGES  = GWT.create(AppMessages.class);

    private I18n() {}

    public static AppConstants c() { return CONSTANTS; }
    public static AppMessages  m() { return MESSAGES; }
}