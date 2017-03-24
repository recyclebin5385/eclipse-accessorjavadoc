package recyclebin5385.eclipse.plugin.accessorjavadoc.wizards;

import java.util.Map;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.wizard.Wizard;

public class GenerateAccessorJavadocWizard extends Wizard {
    private final GenerateAccessorJavadocWizardPage m_page;

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
