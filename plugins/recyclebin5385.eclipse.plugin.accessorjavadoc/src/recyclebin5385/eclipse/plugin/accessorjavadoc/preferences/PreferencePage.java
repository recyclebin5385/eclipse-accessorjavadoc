package recyclebin5385.eclipse.plugin.accessorjavadoc.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import recyclebin5385.eclipse.plugin.accessorjavadoc.Activator;

/**
 * プラグインの設定ページ。
 * 
 * @author owner
 *
 */
public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * コンストラクタ。
     */
    public PreferencePage() {
        super(GRID);
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("AccessorJavadoc preference page");
    }

    @Override
    public void createFieldEditors() {
        addField(new StringFieldEditor(PreferenceConstants.P_FIELD_NAME_REGEX, "&Field name regex:",
                getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.P_GETTER_JAVADOC_SUMMARY_TEMPLATE,
                "&Getter summary template:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.P_SETTER_JAVADOC_SUMMARY_TEMPLATE,
                "&Setter summary template:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.P_PARAM_OR_RETURN_TEMPLATE,
                "@param NAME or @&return template:", getFieldEditorParent()));
        addField(new StringFieldEditor(PreferenceConstants.P_PERIOD_CHARACTERS, "&Period characters:",
                getFieldEditorParent()));
    }

    @Override
    public void init(IWorkbench workbench) {
        // NOTE 何もしない
    }
}