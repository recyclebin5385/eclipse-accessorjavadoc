package recyclebin5385.eclipse.plugin.accessorjavadoc.wizards;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * {@link GenerateAccessorJavadocWizard}のページ。
 * 
 * @author owner
 *
 */
public class GenerateAccessorJavadocWizardPage extends WizardPage {
    private final Map<IMethod, Boolean> m_methodSelectionMap;

    private Table m_table;

    /**
     * コンストラクタ。
     * 
     * @param methodSelectionMap
     *            メソッド→選択状態のマップ
     */
    public GenerateAccessorJavadocWizardPage(Map<IMethod, Boolean> methodSelectionMap) {
        super("Generate getter/setter Javadoc");
        m_methodSelectionMap = methodSelectionMap;
    }

    @Override
    public boolean isPageComplete() {
        if (m_table != null) {
            for (TableItem item : m_table.getItems()) {
                if (item.getChecked()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        setControl(composite);

        GridLayout layout = new GridLayout();
        composite.setLayout(layout);

        new Label(composite, SWT.NONE).setText("Methods:");

        m_table = new Table(composite, SWT.BORDER | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);
        m_table.setLayoutData(new GridData(GridData.FILL_BOTH));
        for (Entry<IMethod, Boolean> entry : m_methodSelectionMap.entrySet()) {
            TableItem item = new TableItem(m_table, SWT.NONE);
            item.setText(entry.getKey().getElementName());
            item.setData(entry.getKey());
            item.setChecked(entry.getValue());
        }

        m_table.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateWizardContainer();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                updateWizardContainer();
            }
        });


        updateWizardContainer();
    }

    private void updateWizardContainer() {
        getWizard().getContainer().updateButtons();
    }

    /**
     * メソッド→選択状態のマップを更新する。
     */
    public void updateMethodSelectionMap() {
        if (m_table != null) {
            for (TableItem item : m_table.getItems()) {
                m_methodSelectionMap.put((IMethod) item.getData(), item.getChecked());
            }
        }
    }
}
