package recyclebin5385.eclipse.plugin.accessorjavadoc.handlers;

import java.util.List;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;


/**
 * {@link GenerateAccessorJavadocHandler}によって処理されるgetter、setterを選択するダイアログ。
 * 
 * @author owner
 *
 */
public class AccessorSelectionDialog extends Dialog {
    /**
     * メソッドの状態。
     * 
     * @author owner
     *
     */
    public static class MethodState {
        /**
         * メソッド
         */
        private IMethod m_method;

        /**
         * 選択されているかどうか
         */
        private boolean m_selected;

        /**
         * Javadocが存在するかどうか
         */
        private boolean m_documented;

        /**
         * タグにより除外対象と指定されているかどうか
         */
        private boolean m_taggedAsExcluded;

        /**
         * メソッドを取得する。
         *
         * @return メソッド
         */
        public IMethod getMethod() {
            return m_method;
        }

        /**
         * メソッドを設定する。
         *
         * @param method
         *            メソッド
         */
        public void setMethod(IMethod method) {
            m_method = method;
        }

        /**
         * 選択されているかどうかを取得する。
         *
         * @return 選択されているかどうか
         */
        public boolean isSelected() {
            return m_selected;
        }

        /**
         * 選択されているかどうかを設定する。
         *
         * @param selected
         *            選択されているかどうか
         */
        public void setSelected(boolean selected) {
            m_selected = selected;
        }

        /**
         * Javadocが存在するかどうかを取得する。
         *
         * @return Javadocが存在するかどうか
         */
        public boolean isDocumented() {
            return m_documented;
        }

        /**
         * Javadocが存在するかどうかを設定する。
         *
         * @param documented
         *            Javadocが存在するかどうか
         */
        public void setDocumented(boolean documented) {
            m_documented = documented;
        }

        /**
         * タグにより除外対象と指定されているかどうかを取得する。
         *
         * @return タグにより除外対象と指定されているかどうか
         */
        public boolean isTaggedAsExcluded() {
            return m_taggedAsExcluded;
        }

        /**
         * タグにより除外対象と指定されているかどうかを設定する。
         *
         * @param taggedAsExcluded
         *            タグにより除外対象と指定されているかどうか
         */
        public void setTaggedAsExcluded(boolean taggedAsExcluded) {
            m_taggedAsExcluded = taggedAsExcluded;
        }
    }

    private final List<MethodState> m_methodStateList;

    private Table m_table;

    /**
     * コンストラクタ。
     * 
     * @param parentShell
     *            親のシェル
     * @param methodStateList
     *            メソッド状態のリスト
     */
    public AccessorSelectionDialog(Shell parentShell, List<MethodState> methodStateList) {
        super(parentShell);
        m_methodStateList = methodStateList;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite ret = (Composite) super.createDialogArea(parent);

        ret.setLayout(new GridLayout(2, false));

        Label tableLabel = new Label(ret, SWT.NONE);
        tableLabel.setText("Select methods to create getters/setters:");
        GridData tableLabelGridData = new GridData(GridData.FILL_HORIZONTAL);
        tableLabelGridData.horizontalSpan = 2;
        tableLabel.setLayoutData(tableLabelGridData);

        m_table = new Table(ret, SWT.BORDER | SWT.CHECK | SWT.H_SCROLL | SWT.V_SCROLL);

        m_table.setLayoutData(new GridData(GridData.FILL_BOTH));
        for (MethodState methodState : m_methodStateList) {
            TableItem item = new TableItem(m_table, SWT.NONE);
            item.setText(methodState.m_method.getElementName());
            item.setData(methodState);
            item.setChecked(methodState.isSelected());
        }

        m_table.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateButtonStatus();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                updateButtonStatus();
            }
        });


        Composite buttonPane = new Composite(ret, SWT.NONE);
        buttonPane.setLayout(new RowLayout(SWT.VERTICAL));
        buttonPane.setLayoutData(new GridData(SWT.NONE, SWT.BEGINNING, false, false));

        Button selectAllButton = new Button(buttonPane, SWT.NONE);
        selectAllButton.setText("Select &All");
        selectAllButton.addSelectionListener(createButtonSelectionListener(true, true));

        Button deselectAllButton = new Button(buttonPane, SWT.NONE);
        deselectAllButton.setText("&Deselect All");
        deselectAllButton.addSelectionListener(createButtonSelectionListener(false, false));

        Button selectGettersButton = new Button(buttonPane, SWT.NONE);
        selectGettersButton.setText("Select &Getters");
        selectGettersButton.addSelectionListener(createButtonSelectionListener(true, false));

        Button selectSettersButton = new Button(buttonPane, SWT.NONE);
        selectSettersButton.setText("Select &Setters");
        selectSettersButton.addSelectionListener(createButtonSelectionListener(false, true));


        return ret;
    }

    private SelectionListener createButtonSelectionListener(boolean getterSelected, boolean setterSelected) {
        return new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                execute();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                execute();
            }

            private void execute() {
                for (TableItem item : m_table.getItems()) {
                    MethodState methodState = (MethodState) item.getData();
                    try {
                        if (methodState.m_method.getReturnType().equals("V")) {
                            // setter
                            item.setChecked(setterSelected);
                        } else {
                            // getter
                            item.setChecked(getterSelected);
                        }
                    } catch (JavaModelException e) {
                        // NOTE 何もしない
                    }
                }

                updateButtonStatus();
            }
        };
    }

    private void updateButtonStatus() {
        Button okButton = getButton(IDialogConstants.OK_ID);

        if (okButton != null) {
            boolean okEnabled = false;
            if (m_table != null) {
                for (TableItem item : m_table.getItems()) {
                    if (item.getChecked()) {
                        okEnabled = true;
                        break;
                    }
                }
            }

            okButton.setEnabled(okEnabled);
        }
    }

    private void updateMethodSelectionMap() {
        if (m_table != null) {
            for (TableItem item : m_table.getItems()) {
                ((MethodState) item.getData()).m_selected = item.getChecked();
            }
        }
    }

    @Override
    protected Point getInitialSize() {
        return new Point(480, 300);
    }

    @Override
    protected void okPressed() {
        updateMethodSelectionMap();
        super.okPressed();
    }

    @Override
    public void create() {
        super.create();
        updateButtonStatus();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Generate Getter/Setter Javadocs from Field Javadocs");
    }
}
