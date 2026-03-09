# Eclipse AccessorJavadoc Plug-in

## Summary

An Eclipse plug-in that adds Javadoc comments to getters and setters of a Java class.
The generated Javadoc comments are copied from the corresponding field.

## Installation

* [Update site for latest](https://recyclebin5385.github.io/eclipse-accessorjavadoc/update/latest)
* [Update site for ver.0.1.2](https://recyclebin5385.github.io/eclipse-accessorjavadoc/update/releases/0.1.2)
* [Update site for ver.0.1.1](https://recyclebin5385.github.io/eclipse-accessorjavadoc/update/releases/0.1.1)
* [Update site for ver.0.1.0](https://recyclebin5385.github.io/eclipse-accessorjavadoc/update/releases/0.1.0)

Select **"Help -> Install New Software..."** from the Eclipse main menu to open the wizard.
Enter the update site URL above in the **"Work with:"** field. A feature named **"AccessorJavadoc"** will appear in the list.
Select the checkbox and click **Next**. Then follow the instructions in the wizard.

## Usage

Open a Java source file in the Eclipse Java Editor and place the cursor inside a class definition (an inner class can also be selected).

Select **"AccessorJavadoc -> Generate Getter/Setter Javadocs from Field Javadocs..."** from the Eclipse main menu or from the editor's context menu to open the dialog.
The shortcut key is **Ctrl+Shift+D**.

Select the getter and setter methods and click **OK** to add or overwrite their Javadoc comments.

If the tag `@accessorjavadoc.excluded` is present in the existing Javadoc of a getter or setter, the method is initially unselected when the dialog is displayed.
This is useful when you want to edit the Javadoc manually.

## Configuration

Select **"Window -> Preferences"** from the Eclipse main menu to open the Preferences dialog.
Select **AccessorJavadoc** in the left pane to open the configuration page.

**Field name regex** specifies a regular expression that matches field names.
By selecting a group with `()`, you can extract the variable name without a prefix and/or suffix.
Group #1 of the regular expression should match the field name with the prefix and/or suffix removed.

If the regular expression does not match the field name, the name is used as-is and no prefix or suffix is removed.

Example:

Specify `(?:[ms]?_)(.+)` and variable names are converted as follows:

```
m_foo -> foo
s_bar -> bar
_baz  -> baz
qux   -> not converted
```

**Getter summary template** and **Setter summary template** specify the templates used for the Javadoc summaries of getters and setters.

**@param NAME or @return template** specifies the template used for the parameter description of a setter or the return value description of a getter.

You can embed variables in these templates using the format **${variable_name}**.

Available variables:

* **label** – The "label" of the variable; the first sentence of the field's Javadoc with the final period removed.
* **label.capitalized** – The label with the first letter capitalized.
* **label.uncapitalized** – The label with the first letter in lowercase. (If there is a lowercase letter later in the first word, it is treated as an abbreviation and is not converted.)
* **label.toUpperCase** – The label converted to uppercase.
* **label.toLowerCase** – The label converted to lowercase.

**Period characters** specifies the characters that are treated as sentence-ending periods.

## License

This project is licensed under the BSD 2-Clause License.
See the LICENSE file for details.

## Version

Current version: **0.1.2**

## Changelog

### 0.1.2 - 2026-03-09

* Changed Java code parser version
* Modified UI texts

### 0.1.1 - 2017-04-30

* Fixed a message in the preference page

### 0.1.0 - 2017-04-02

* Initial release

## Copyright

Copyright (c) 2017 recyclebin5385
