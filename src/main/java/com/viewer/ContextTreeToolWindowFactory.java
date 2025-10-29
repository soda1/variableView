package com.viewer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.viewer.ContextTreePanel;
import org.jetbrains.annotations.NotNull;

public class ContextTreeToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContextTreePanel panel = new ContextTreePanel(project);
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(panel.getMainPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
