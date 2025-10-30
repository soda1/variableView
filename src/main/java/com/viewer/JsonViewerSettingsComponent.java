package com.viewer;// src/main/java/com/yourcompany/jsonstructureviewer/settings/JsonViewerSettingsComponent.java

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;

import javax.swing.*;

public class JsonViewerSettingsComponent {

    private final JPanel myMainPanel;
    private final TextFieldWithBrowseButton myJsonFilePath = new TextFieldWithBrowseButton();

    public JsonViewerSettingsComponent() {
        myJsonFilePath.addBrowseFolderListener("Select JSON File", null, null,
                new FileChooserDescriptor(true, false, false, false, false, false));

        myMainPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("JSON file path: "), myJsonFilePath, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    public JPanel getPanel() {
        return myMainPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return myJsonFilePath;
    }

    public String getJsonFilePath() {
        return myJsonFilePath.getText();
    }

    public void setJsonFilePath(String newText) {
        myJsonFilePath.setText(newText);
    }
}