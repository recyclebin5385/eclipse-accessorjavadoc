package recyclebin5385.eclipse.plugin.accessorjavadoc.handlers;

import java.text.BreakIterator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import recyclebin5385.eclipse.plugin.accessorjavadoc.Activator;
import recyclebin5385.eclipse.plugin.accessorjavadoc.handlers.AccessorSelectionDialog.MethodState;
import recyclebin5385.eclipse.plugin.accessorjavadoc.preferences.PreferenceConstants;

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

        private MethodState m_methodState;
    }


    private static final Pattern ACCESSOR_PATTERN = Pattern.compile("(get|set|is)(.+)");

    private static final Pattern REMOVE_JAVADOC_HEADER_PARTTERN = Pattern
            .compile("^[ \t\\x0B\f]*\\*+[ \t\\x0B\f]*(.*?)[ \t\\x0B\f]*$", Pattern.MULTILINE);

    private static final Pattern TRIM_PATTERN = Pattern.compile("[\\x00-\\x20]*(.+?)[\\x00-\\x20]*", Pattern.DOTALL);

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    private static final Pattern JAVADOC_TAIL_PATTERN = Pattern.compile("[\\s\r\n\\*]*\\*/\\z");

    private static final Pattern NONSPACE_PATTERN = Pattern.compile("[^\\s]");

    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\r\n|\r|\n");

    private static final String DEFAULT_GETTER_JAVADOC_SUFFIX_TEMPLATE_FORMAT = " *\n * @return {0}\n */";

    private static final String DEFAULT_SETTER_JAVADOC_SUFFIX_TEMPLATE_FORMAT = " *\n * @param $'{'param'}'\n *            {0}\n */";

    private static final String TAG_NAME_EXCLUDED = "@accessorjavadoc.excluded";

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
        String getterJavadocSuffixTemplate = MessageFormat.format(DEFAULT_GETTER_JAVADOC_SUFFIX_TEMPLATE_FORMAT,
                preferenceStore.getString(PreferenceConstants.P_PARAM_OR_RETURN_TEMPLATE));
        String setterJavadocSuffixTemplate = MessageFormat.format(DEFAULT_SETTER_JAVADOC_SUFFIX_TEMPLATE_FORMAT,
                preferenceStore.getString(PreferenceConstants.P_PARAM_OR_RETURN_TEMPLATE));


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


                /*----------------------------------------------------------------
                 * Javadocから概要を切り出す
                 *----------------------------------------------------------------*/


                // タグの中での概要のオフセット
                int summaryStartOffset = 0;
                int summaryEndOffset = 0;

                for (Object oTag : fieldJavadoc.tags()) {
                    TagElement tag = (TagElement) oTag;
                    if (tag.getTagName() != null) {
                        continue;
                    }


                    // タグの文字列を切り出す

                    // Javadoc全体の中でのタグの開始位置のオフセット
                    int tagStartOffset = tag.getStartPosition() - fieldJavadoc.getStartPosition();

                    String tagText = document.get(tag.getStartPosition(), tag.getLength());


                    // タグの文字列の各行の行頭の*を削除する

                    // 行頭の*を除去した後の文字の位置→除去する前の文字の位置のマップ
                    TreeMap<Integer, Integer> tagTextPositionMap = new TreeMap<>();

                    Matcher removeJavadocHeaderMatcher = REMOVE_JAVADOC_HEADER_PARTTERN.matcher(tagText);
                    StringBuffer tagTextBuffer = new StringBuffer();
                    int lastMatchEnd = 0;
                    while (removeJavadocHeaderMatcher.find()) {
                        tagTextPositionMap.put(
                                tagTextBuffer.length() + removeJavadocHeaderMatcher.start() - lastMatchEnd,
                                removeJavadocHeaderMatcher.start(1));
                        removeJavadocHeaderMatcher.appendReplacement(tagTextBuffer, "$1");
                        tagTextPositionMap.put(tagTextBuffer.length(), removeJavadocHeaderMatcher.end());
                        lastMatchEnd = removeJavadocHeaderMatcher.end();
                    }
                    removeJavadocHeaderMatcher.appendTail(tagTextBuffer);

                    tagText = tagTextBuffer.toString();


                    // 概要となる最初の文を切り出す

                    BreakIterator bi = BreakIterator.getSentenceInstance();
                    bi.setText(tagText);
                    int sentenceBoundary = bi.next();
                    if (sentenceBoundary == BreakIterator.DONE) {
                        sentenceBoundary = tagText.length();
                    }
                    String firstSentence = tagText.substring(0, sentenceBoundary);


                    // 概要となる文の前後の非表示文字を除去する

                    Matcher firstSentenceMatcher = TRIM_PATTERN.matcher(firstSentence);
                    String summary;
                    int summaryStartOffsetInTag;
                    if (firstSentenceMatcher.matches()) {
                        summaryStartOffsetInTag = firstSentenceMatcher.start(1);
                        summary = firstSentenceMatcher.group(1);
                    } else {
                        summaryStartOffsetInTag = 0;
                        summary = firstSentence;
                    }


                    // 概要の位置を記憶する

                    summaryStartOffset = tagStartOffset + replaceIndex(summaryStartOffsetInTag, tagTextPositionMap);
                    summaryEndOffset = tagStartOffset
                            + replaceIndex(summaryStartOffsetInTag + summary.length(), tagTextPositionMap);


                    // 概要の句点を除去してラベルとする

                    Pattern removePeriodPattern = Pattern
                            .compile(
                                    MessageFormat.format("(.+?)\\s*(?:[{0}])?",
                                            Pattern.quote(preferenceStore
                                                    .getString(PreferenceConstants.P_PERIOD_CHARACTERS))),
                                    Pattern.DOTALL);
                    Matcher removePeriodMatcher = removePeriodPattern.matcher(summary);
                    if (removePeriodMatcher.matches()) {
                        fieldInfo.m_label = removePeriodMatcher.group(1);
                    } else {
                        fieldInfo.m_label = summary;
                    }

                    break;
                }

                if (fieldInfo.m_label == null || fieldInfo.m_label.isEmpty()) {
                    continue;
                }


                /*----------------------------------------------------------------
                 * 概要の前後のJavadocを求める
                 *----------------------------------------------------------------*/

                int fieldJavadocStart = fieldJavadoc.getStartPosition();
                int fieldJavadocLineOffset = document.getLineOffset(document.getLineOfOffset(fieldJavadocStart));
                String fieldJavadocIndent = NONSPACE_PATTERN
                        .matcher(document.get(fieldJavadocLineOffset, fieldJavadocStart - fieldJavadocLineOffset))
                        .replaceAll(" ");


                Pattern indentPattern = Pattern.compile("^" + Pattern.quote(fieldJavadocIndent), Pattern.MULTILINE);
                String javadocText = document.get(fieldJavadoc.getStartPosition(), fieldJavadoc.getLength());

                fieldInfo.m_javadocTextBeforeSummary = indentPattern
                        .matcher(javadocText.substring(0, summaryStartOffset)).replaceAll("");

                // NOTE 先頭の空白を必ず残すためにダミーのインデントを追加してから置換する
                fieldInfo.m_javadocTextAfterSummary = indentPattern
                        .matcher(fieldJavadocIndent + javadocText.substring(summaryEndOffset)).replaceAll("");


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

            List<MethodState> methodStateList = new ArrayList<>();
            for (AccessorInfo accessorInfo : accessorInfoList) {
                MethodState methodState = new MethodState();
                methodStateList.add(methodState);
                accessorInfo.m_methodState = methodState;


                methodState.setMethod(accessorInfo.m_method);
                if (accessorInfo.m_javadoc != null) {
                    methodState.setDocumented(true);

                    for (Object oTag : accessorInfo.m_javadoc.tags()) {
                        TagElement tag = (TagElement) oTag;
                        if (TAG_NAME_EXCLUDED.equals(tag.getTagName())) {
                            methodState.setTaggedAsExcluded(true);
                            break;
                        }
                    }
                }

                methodState.setSelected(!methodState.isTaggedAsExcluded());
            }

            AccessorSelectionDialog dialog = new AccessorSelectionDialog(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), methodStateList);
            if (dialog.open() != AccessorSelectionDialog.OK) {
                return null;
            }


            /*----------------------------------------------------------------
             * getter、setterのJavadocを置換する
             *----------------------------------------------------------------*/

            int offset = 0;

            for (AccessorInfo accessorInfo : accessorInfoList) {
                if (!accessorInfo.m_methodState.isSelected()) {
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


    private static int replaceIndex(int index, NavigableMap<Integer, Integer> indexMap) {
        Entry<Integer, Integer> entry = indexMap.floorEntry(index);
        if (entry == null) {
            return index;
        }

        return entry.getValue() + index - entry.getKey();
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
            String[] variableFields = variableMatcher.group(1).split("\\.");
            try {
                String parameterValue = parameterMap.get(variableFields[0]);

                if (variableFields.length > 1) {
                    switch (variableFields[1]) {
                    case "capitalized":
                        parameterValue = capitalize(parameterValue, true, false);
                        break;

                    case "uncapitalized":
                        parameterValue = capitalize(parameterValue, false, false);
                        break;

                    case "toUpperCase":
                        parameterValue = parameterValue.toUpperCase();
                        break;

                    case "toLowerCase":
                        parameterValue = parameterValue.toLowerCase();
                        break;

                    default:
                        break;
                    }
                }
                variableMatcher.appendReplacement(buffer, parameterValue);
            } catch (Exception exception) {
                variableMatcher.appendReplacement(buffer, variableMatcher.group());
            }
        }
        variableMatcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String capitalize(String s, boolean firstToUpper, boolean forced) {
        BreakIterator bi = BreakIterator.getWordInstance();
        bi.setText(s);
        int boundary = bi.next();
        String firstWord = boundary == BreakIterator.DONE ? s : s.substring(0, boundary);

        if (firstWord.isEmpty()) {
            return s;
        }

        char firstChar = firstWord.charAt(0);
        boolean firstUpperOrLowerCase = Character.isLowerCase(firstChar) || Character.isUpperCase(firstChar);

        boolean allLowerCase = true;
        for (int i = 1; i < firstWord.length(); i++) {
            char c = firstWord.charAt(i);
            if (!Character.isLowerCase(c)) {
                allLowerCase = false;
                break;
            }
        }

        // 略語かどうかを検出する
        boolean conversionRequired = forced || (firstUpperOrLowerCase && allLowerCase);

        if (!conversionRequired) {
            return s;
        }

        if (firstToUpper) {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        } else {
            return s.substring(0, 1).toLowerCase() + s.substring(1);
        }
    }
}
