package com.viewer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "com.viewer.JsonViewerSettingsState",
        storages = @Storage("JsonViewerSettings.xml")
)
public class JsonViewerSettingsState implements PersistentStateComponent<JsonViewerSettingsState> {

    public String jsonFilePath = "";

    public static JsonViewerSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(JsonViewerSettingsState.class);
    }

    @Nullable
    @Override
    public JsonViewerSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull JsonViewerSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}