package com.viewer;// src/main/java/com/yourcompany/jsonstructureviewer/settings/JsonViewerConfigurable.java


import com.intellij.openapi.options.Configurable;
import com.viewer.JsonViewerSettingsComponent;
import com.viewer.JsonViewerSettingsState;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;

public class JsonViewerConfigurable implements Configurable {

    private JsonViewerSettingsComponent mySettingsComponent;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "JSON Structure Viewer";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getPreferredFocusedComponent();
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        mySettingsComponent = new JsonViewerSettingsComponent();
        return mySettingsComponent.getPanel();
    }

    @Override
    public boolean isModified() {
        JsonViewerSettingsState settings = JsonViewerSettingsState.getInstance();
        return !mySettingsComponent.getJsonFilePath().equals(settings.jsonFilePath);
    }

    @Override
    public void apply() {
        JsonViewerSettingsState settings = JsonViewerSettingsState.getInstance();
        settings.jsonFilePath = mySettingsComponent.getJsonFilePath();
    }

    @Override
    public void reset() {
        JsonViewerSettingsState settings = JsonViewerSettingsState.getInstance();
        mySettingsComponent.setJsonFilePath(settings.jsonFilePath);
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}