package com.viewer;// src/main/java/com/yourcompany/jsonstructureviewer/toolwindow/JsonToolWindowFactory.java

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
import com.viewer.JsonTreeBuilder;
import com.viewer.JsonViewerSettingsState;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

public class JsonToolWindowFactory implements ToolWindowFactory {

    private Tree tree;
    private final JsonTreeBuilder treeBuilder = new JsonTreeBuilder();
    private Project project; // Store the project instance

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project; // Keep a reference to the project
        JPanel toolWindowContent = new JPanel();
        toolWindowContent.setLayout(new BoxLayout(toolWindowContent, BoxLayout.Y_AXIS));

        tree = new Tree();
        JBScrollPane scrollPane = new JBScrollPane(tree);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadAndParseJsonWithBackgroundTask());

        toolWindowContent.add(refreshButton);
        toolWindowContent.add(scrollPane);

        loadAndParseJsonWithBackgroundTask();

        tree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertTextIntoEditor(project);
                }
            }
        });

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(toolWindowContent, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private void loadAndParseJsonWithBackgroundTask() {
        String filePath = JsonViewerSettingsState.getInstance().jsonFilePath;
        if (filePath == null || filePath.trim().isEmpty()) {
            tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("JSON file not configured")));
            return;
        }

        // Practical check: Warn user if file is huge
        try {
            File file = new File(filePath);
            long fileSizeMB = file.length() / (1024 * 1024);
            if (fileSizeMB > 50) { // Warn for files larger than 50MB
                int result = Messages.showOkCancelDialog(
                        project,
                        "The selected file is very large (" + fileSizeMB + " MB). Parsing may take a while and consume significant memory. Do you want to continue?",
                        "Large File Warning",
                        "Continue", "Cancel", Messages.getWarningIcon());
                if (result != Messages.OK) {
                    return; // User cancelled
                }
            }
        } catch (Exception e) {
            // Ignore if file doesn't exist yet
        }


        // Use Task.Backgroundable to run the heavy work on a background thread
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Parsing JSON File", true) {
            private TreeModel newTreeModel;
            private String errorMessage;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                // This part runs on a BACKGROUND THREAD
                indicator.setIndeterminate(true);
                indicator.setText("Reading and parsing " + new File(filePath).getName());

                try {
                    String content = new String(Files.readAllBytes(Paths.get(filePath)));
                    // Check for cancellation before heavy parsing
                    indicator.checkCanceled();
                    newTreeModel = treeBuilder.buildTreeModel(content);
                } catch (IOException | InvalidPathException ex) {
                    errorMessage = "Error reading file: " + ex.getMessage();
                } catch (Exception ex) {
                    errorMessage = "An unexpected error occurred: " + ex.getMessage();
                }
            }

            @Override
            public void onSuccess() {
                // This part runs on the UI THREAD after the background task is successful
                if (errorMessage != null) {
                    tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode(errorMessage)));
                } else if (newTreeModel != null) {
                    tree.setModel(newTreeModel);
                }
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                // This part runs on the UI THREAD if an exception occurs
                tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Failed to parse JSON: " + error.getMessage())));
            }
        });
    }

    private void insertTextIntoEditor(Project project) {
        // ... (This method remains the same as before)
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JsonTreeBuilder.NodeInfo)) {
            return;
        }

        JsonTreeBuilder.NodeInfo nodeInfo = (JsonTreeBuilder.NodeInfo) selectedNode.getUserObject();

        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();
        if (parentNode != null && parentNode.getUserObject() instanceof JsonTreeBuilder.NodeInfo) {
            JsonTreeBuilder.NodeInfo parentInfo = (JsonTreeBuilder.NodeInfo) parentNode.getUserObject();
            if ("array".equals(parentInfo.type)) {
                return;
            }
        }

        String textToInsert = nodeInfo.getPath();
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return;
        }

        final Document document = editor.getDocument();
        final int offset = editor.getCaretModel().getOffset();

        WriteCommandAction.runWriteCommandAction(project, () ->
                document.insertString(offset, textToInsert)
        );
    }
}