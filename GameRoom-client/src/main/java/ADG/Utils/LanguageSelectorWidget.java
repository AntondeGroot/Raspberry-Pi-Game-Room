package ADG.Utils;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.FlowPanel;

public class LanguageSelectorWidget extends Composite {

    public LanguageSelectorWidget() {
        FlowPanel container = new FlowPanel();
        container.setStyleName("lang-selector-container");

        Label icon = new Label("🌐");
        icon.setStyleName("lang-selector-icon");

        ListBox listBox = new ListBox();
        listBox.setStyleName("lang-listbox");

        Language current = Cookie.getLanguage();
        for (Language lang : Language.values()) {
            listBox.addItem(lang.getDisplayName(), lang.name());
            if (lang == current) {
                listBox.setSelectedIndex(listBox.getItemCount() - 1);
            }
        }

        listBox.addChangeHandler(e -> {
            String selected = listBox.getValue(listBox.getSelectedIndex());
            Cookie.changeLanguage(Language.valueOf(selected));
        });

        container.add(icon);
        container.add(listBox);
        initWidget(container);
    }
}