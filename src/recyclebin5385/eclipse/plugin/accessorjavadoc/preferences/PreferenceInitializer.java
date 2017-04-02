package recyclebin5385.eclipse.plugin.accessorjavadoc.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import recyclebin5385.eclipse.plugin.accessorjavadoc.Activator;

/**
 * 設定を初期化するクラス。
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        store.setDefault(PreferenceConstants.P_FIELD_NAME_REGEX, "(?:[ms]?_)(.+)");
        store.setDefault(PreferenceConstants.P_GETTER_JAVADOC_SUMMARY_TEMPLATE, "Gets ${label.uncapitalized}.");
        store.setDefault(PreferenceConstants.P_SETTER_JAVADOC_SUMMARY_TEMPLATE, "Sets ${label.uncapitalized}.");
        store.setDefault(PreferenceConstants.P_PARAM_OR_RETURN_TEMPLATE, "${label.uncapitalized}");
        store.setDefault(PreferenceConstants.P_PERIOD_CHARACTERS, ".。");
    }
}
