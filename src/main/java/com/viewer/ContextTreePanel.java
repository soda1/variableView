package com.viewer;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ContextTreePanel {
    private final JPanel mainPanel;
    private final JTree tree;
    private final Project project;

    public ContextTreePanel(Project project) {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());
        this.tree = new JTree(new DefaultMutableTreeNode("context"));
        this.mainPanel.add(new JScrollPane(tree), BorderLayout.CENTER);
        JButton refreshButton = new JButton("Refresh Context");
        refreshButton.addActionListener(e -> {
            ContextValueParser.parseContext(project);
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("context");
            tree.setModel(new DefaultTreeModel(root));
        });
        mainPanel.add(refreshButton, BorderLayout.NORTH);
        setupListeners();
    }

    private void setupListeners() {
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Object[] pathNodes = tree.getSelectionPath().getPath();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < pathNodes.length; i++) {
                        if (i > 0) sb.append(".");
                        sb.append(pathNodes[i].toString());
                    }
                    insertTextToEditor(sb.toString());
                }
            }
        });
    }

    private void insertTextToEditor(String text) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;

        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();
        WriteCommandAction.runWriteCommandAction(project, () ->
                document.insertString(offset, text)
        );
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }
}
