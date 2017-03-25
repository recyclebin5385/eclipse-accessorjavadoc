package recyclebin5385.eclipse.plugin.accessorjavadoc.wizards;

import java.util.Map;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.wizard.Wizard;

import recyclebin5385.eclipse.plugin.accessorjavadoc.handlers.GenerateAccessorJavadocHandler;

/**
 * {@link GenerateAccessorJavadocHandler}の動作設定を編集するウィザード。
 * 
 * @author owner
 *
 */
public class GenerateAccessorJavadocWizard extends Wizard {
    private final GenerateAccessorJavadocWizardPage m_page;

    /**
     * コンストラクタ。
     * 
     * @param methodSelectionMap
     *            メソッド→選択状態のマップ
     */
    public GenerateAccessorJavadocWizard(Map<IMethod, Boolean> methodSelectionMap) {
        m_page = new GenerateAccessorJavadocWizardPage(methodSelectionMap);

        setWindowTitle("Add Javadoc to getters/setters of the class");
        addPage(m_page);
    }

    @Override
    public boolean performFinish() {
        m_page.updateMethodSelectionMap();
        return true;
    }
}
