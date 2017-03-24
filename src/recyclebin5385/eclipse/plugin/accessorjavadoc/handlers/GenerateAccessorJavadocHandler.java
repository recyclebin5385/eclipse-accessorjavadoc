package recyclebin5385.eclipse.plugin.accessorjavadoc.handlers;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import recyclebin5385.eclipse.plugin.accessorjavadoc.Activator;
import recyclebin5385.eclipse.plugin.accessorjavadoc.preferences.PreferenceConstants;
import recyclebin5385.eclipse.plugin.accessorjavadoc.wizards.GenerateAccessorJavadocWizard;

/**
 * Javaのソースコードのgetter、setterのJavadocを生成するプラグインの {@link IHandler}。
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class GenerateAccessorJavadocHandler extends AbstractHandler {
    private static class FieldInfo {
        private String m_label;

        private String m_javadocTextBeforeSummary;

        private String m_javadocTextAfterSummary;
    }


    private static class AccessorInfo {
        private IMethod m_method;

        private String m_fieldName;

        private boolean m_getter;

        private Javadoc m_javadoc;
    }


    private static final Pattern ACCESSOR_PATTERN = Pattern.compile("(get|set|is)(.+)");

    private static final Pattern JAVADOC_SUMMARY_PARTTERN = Pattern.compile("\\s*\\*\\s*(.*?)\\s*");

    private static final Pattern PERIOD_PATTERN = Pattern.compile("\\s*(.+?)\\s*([.。]\\s*)?");

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    private static final Pattern JAVADOC_TAIL_PATTERN = Pattern.compile("[\\s\r\n\\*]*\\*/\\z");

    private static final Pattern NONSPACE_PATTERN = Pattern.compile("[^\\s]");

    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\r\n|\r|\n");


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        /*----------------------------------------------------------------
         * 設定値を取得する
         *----------------------------------------------------------------*/

        IPreferenceStore preferenceStore = Activator.getDefault().getPreferenceStore();

        String fieldNameRegex = preferenceStore.getString(PreferenceConstants.P_FIELD_NAME_REGEX);
        String getterJavadocSummaryTemplate = preferenceStore
                .getString(PreferenceConstants.P_GETTER_JAVADOC_SUMMARY_TEMPLATE);
        String setterJavadocSummaryTemplate = preferenceStore
                .getString(PreferenceConstants.P_SETTER_JAVADOC_SUMMARY_TEMPLATE);
        String getterJavadocSuffixTemplate = preferenceStore
                .getString(PreferenceConstants.P_GETTER_JAVADOC_SUFFIX_TEMPLATE);
        String setterJavadocSuffixTemplate = preferenceStore
                .getString(PreferenceConstants.P_SETTER_JAVADOC_SUFFIX_TEMPLATE);


        Pattern fieldNamePattern;
        try {
            fieldNamePattern = fieldNameRegex == null || fieldNameRegex.isEmpty() ? null
                    : Pattern.compile(fieldNameRegex);
        } catch (Exception exception) {
            return null;
        }


        /*----------------------------------------------------------------
         * アクティブなエディタを取得する
         *----------------------------------------------------------------*/

        ITextEditor editor;
        try {
            editor = (ITextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                    .getActiveEditor();
        } catch (Exception exception) {
            return null;
        }

        if (editor == null) {
            return null;
        }

        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());

        
        /*----------------------------------------------------------------
         * エディタの内容から型の情報を取得する
         *----------------------------------------------------------------*/

        ICompilationUnit compilationUnit;
        IType type;
        try {
            compilationUnit = (ICompilationUnit) JavaUI.getEditorInputJavaElement(editor.getEditorInput());
            ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
            IJavaElement selectedElement = compilationUnit.getElementAt(selection.getOffset());
            type = (IType) selectedElement.getAncestor(IJavaElement.TYPE);
        } catch (Exception exception) {
            return null;
        }

        if (type == null) {
            return null;
        }


        /*----------------------------------------------------------------
         * Javadocコメントをすべて取得する
         *----------------------------------------------------------------*/

        ASTParser parser = ASTParser.newParser(AST.JLS8);

        parser.setSource(compilationUnit);

        final TreeMap<Integer, Javadoc> javadocMap = new TreeMap<>();
        CompilationUnit unit = (CompilationUnit) parser.createAST(new NullProgressMonitor());

        unit.accept(new ASTVisitor() {
            @Override
            public boolean visit(Javadoc node) {
                javadocMap.put(node.getStartPosition(), node);
                return super.visit(node);
            }
        });


        /*----------------------------------------------------------------
         * メンバ変数およびそのJavadocを取得する
         *----------------------------------------------------------------*/

        Map<String, FieldInfo> fieldInfoMap = new HashMap<>();
        try {
            for (IField field : type.getFields()) {
                Javadoc fieldJavadoc = getJavadoc(field, javadocMap);
                if (fieldJavadoc == null) {
                    continue;
                }


                String fieldName = field.getElementName();
                if (fieldNamePattern != null) {
                    Matcher matcher = fieldNamePattern.matcher(fieldName);
                    if (matcher.matches() && matcher.groupCount() >= 1) {
                        fieldName = matcher.group(1);
                    }
                }

                fieldName = fieldName.toLowerCase();


                FieldInfo fieldInfo = new FieldInfo();

                String summary = null;
                for (Object oTag : fieldJavadoc.tags()) {
                    TagElement tag = (TagElement) oTag;
                    if (tag.getTagName() != null) {
                        continue;
                    }


                    String tagText = document.get(tag.getStartPosition(), tag.getLength());
                    Matcher matcher = JAVADOC_SUMMARY_PARTTERN.matcher(tagText);
                    if (matcher.matches()) {
                        tagText = matcher.group(1);
                    }

                    BreakIterator bi = BreakIterator.getSentenceInstance();
                    bi.setText(tagText);
                    int boundary = bi.next();
                    summary = boundary == BreakIterator.DONE ? tagText : tagText.substring(0, boundary);
                    summary = summary.trim();

                    fieldInfo.m_label = summary;
                    Matcher matcher2 = PERIOD_PATTERN.matcher(summary);
                    if (matcher2.matches()) {
                        fieldInfo.m_label = matcher2.group(1);
                    }

                    break;
                }

                if (summary == null || summary.isEmpty()) {
                    continue;
                }


                int fieldJavadocStart = fieldJavadoc.getStartPosition();
                int fieldJavadocLineOffset = document.getLineOffset(document.getLineOfOffset(fieldJavadocStart));
                String fieldJavadocIndent = NONSPACE_PATTERN
                        .matcher(document.get(fieldJavadocLineOffset, fieldJavadocStart - fieldJavadocLineOffset))
                        .replaceAll(" ");

                Pattern indentPattern = Pattern.compile("^" + Pattern.quote(fieldJavadocIndent), Pattern.MULTILINE);
                String javadocText = document.get(fieldJavadocStart, fieldJavadoc.getLength());
                javadocText = indentPattern.matcher(javadocText).replaceAll("");


                int summaryOffset = javadocText.indexOf(summary);
                if (summaryOffset == -1) {
                    continue;
                }
                fieldInfo.m_javadocTextBeforeSummary = javadocText.substring(0, summaryOffset);
                fieldInfo.m_javadocTextAfterSummary = javadocText.substring(summaryOffset + summary.length());


                fieldInfoMap.put(fieldName, fieldInfo);
            }
        } catch (JavaModelException | BadLocationException exception) {
            return null;
        }


        try {
            /*----------------------------------------------------------------
             * getter、setterの情報を収集する
             *----------------------------------------------------------------*/

            List<AccessorInfo> accessorInfoList = new ArrayList<>();

            for (IMethod method : type.getMethods()) {
                Matcher accesorNameMatcher = ACCESSOR_PATTERN.matcher(method.getElementName());
                if (!accesorNameMatcher.matches()) {
                    // getter、setter以外のメソッドは無視する
                    continue;
                }

                String fieldName = accesorNameMatcher.group(2).toLowerCase();
                FieldInfo fieldInfo = fieldInfoMap.get(fieldName);
                if (fieldInfo == null) {
                    // 対応する項目のJavadocがない場合は処理しない
                    continue;
                }


                AccessorInfo accessorInfo = new AccessorInfo();

                switch (accesorNameMatcher.group(1)) {
                case "set":
                    if (method.getNumberOfParameters() != 1) {
                        continue;
                    }

                    if (!method.getReturnType().equals("V")) {
                        continue;
                    }

                    accessorInfo.m_getter = false;

                    break;

                default:
                    if (method.getNumberOfParameters() != 0) {
                        continue;
                    }

                    accessorInfo.m_getter = true;

                    break;
                }

                accessorInfo.m_method = method;
                accessorInfo.m_fieldName = fieldName;
                accessorInfo.m_javadoc = getJavadoc(method, javadocMap);

                accessorInfoList.add(accessorInfo);
            }


            /*----------------------------------------------------------------
             * ダイアログを開く
             *----------------------------------------------------------------*/

            Map<IMethod, Boolean> methodSelectionMap = new LinkedHashMap<>();
            for (AccessorInfo accessorInfo : accessorInfoList) {
                methodSelectionMap.put(accessorInfo.m_method, Boolean.TRUE);
            }

            GenerateAccessorJavadocWizard wizard = new GenerateAccessorJavadocWizard(methodSelectionMap);
            WizardDialog dialog = new WizardDialog(null, wizard);
            if (dialog.open() != WizardDialog.OK) {
                return null;
            }


            /*----------------------------------------------------------------
             * getter、setterのJavadocを置換する
             *----------------------------------------------------------------*/

            int offset = 0;

            for (AccessorInfo accessorInfo : accessorInfoList) {
                if (!methodSelectionMap.get(accessorInfo.m_method)) {
                    continue;
                }

                FieldInfo fieldInfo = fieldInfoMap.get(accessorInfo.m_fieldName);


                // テンプレートおよびテンプレートの変数を求める

                Map<String, String> parameterMap = new HashMap<>();
                parameterMap.put("label", fieldInfo.m_label);

                String summaryTemplate;
                String suffixTemplate;

                if (accessorInfo.m_getter) {
                    summaryTemplate = getterJavadocSummaryTemplate;
                    suffixTemplate = getterJavadocSuffixTemplate;
                } else {
                    parameterMap.put("param", accessorInfo.m_method.getParameterNames()[0]);

                    summaryTemplate = setterJavadocSummaryTemplate;
                    suffixTemplate = setterJavadocSuffixTemplate;
                }


                // メンバ変数のJavadocを置換し、getterまたはsetterに設定する

                int start = offset + (accessorInfo.m_javadoc != null ? accessorInfo.m_javadoc.getStartPosition()
                        : accessorInfo.m_method.getSourceRange().getOffset());
                int lineOffset = document.getLineOffset(document.getLineOfOffset(start));
                String indent = NONSPACE_PATTERN.matcher(document.get(lineOffset, start - lineOffset)).replaceAll(" ");


                int oldLength = accessorInfo.m_javadoc != null ? accessorInfo.m_javadoc.getLength() : 0;


                String newSummary = replaceVariables(summaryTemplate, parameterMap);
                String newSuffix = replaceVariables(suffixTemplate, parameterMap);


                String newMethodJavadocText = fieldInfo.m_javadocTextBeforeSummary + newSummary
                        + fieldInfo.m_javadocTextAfterSummary;
                newMethodJavadocText = JAVADOC_TAIL_PATTERN.matcher(newMethodJavadocText)
                        .replaceFirst("\n" + newSuffix);
                if (accessorInfo.m_javadoc == null) {
                    newMethodJavadocText += "\n";
                }
                newMethodJavadocText = NEWLINE_PATTERN.matcher(newMethodJavadocText).replaceAll("$0" + indent);

                String oldMethodJavadocText = document.get(start, oldLength);

                if (!oldMethodJavadocText.equals(newMethodJavadocText)) {
                    document.replace(start, oldLength, newMethodJavadocText);

                    offset += newMethodJavadocText.length() - oldLength;
                }
            }
        } catch (JavaModelException | BadLocationException exception) {
            return null;
        }


        return null;
    }


    private static Javadoc getJavadoc(ISourceReference sourceReference, TreeMap<Integer, Javadoc> javadocMap)
            throws JavaModelException {
        SortedMap<Integer, Javadoc> tmpMap = javadocMap.subMap(sourceReference.getSourceRange().getOffset(),
                sourceReference.getSourceRange().getOffset() + sourceReference.getSourceRange().getLength());

        if (tmpMap.isEmpty()) {
            return null;
        }

        return tmpMap.values().iterator().next();
    }

    private static String replaceVariables(String s, Map<String, String> parameterMap) {
        StringBuffer buffer = new StringBuffer();
        Matcher variableMatcher = VARIABLE_PATTERN.matcher(s);
        while (variableMatcher.find()) {
            variableMatcher.appendReplacement(buffer, parameterMap.get(variableMatcher.group(1)));
        }
        variableMatcher.appendTail(buffer);
        return buffer.toString();
    }

}
