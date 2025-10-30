package com.viewer;// src/main/java/com/yourcompany/jsonstructureviewer/toolwindow/JsonTreeBuilder.java

import com.google.gson.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.util.Map;

public class JsonTreeBuilder {

    public TreeModel buildTreeModel(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return new DefaultTreeModel(new DefaultMutableTreeNode("JSON not loaded or empty"));
        }

        try {
            JsonElement rootElement = JsonParser.parseString(jsonContent);
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("root");
            buildNode(rootNode, "root", rootElement);
            return new DefaultTreeModel(rootNode.getFirstChild());
        } catch (JsonSyntaxException e) {
            return new DefaultTreeModel(new DefaultMutableTreeNode("Error parsing JSON: " + e.getMessage()));
        }
    }

    private void buildNode(DefaultMutableTreeNode parentNode, String key, JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            DefaultMutableTreeNode objectNode = new DefaultMutableTreeNode(new NodeInfo(key, "object", parentNode));
            parentNode.add(objectNode);
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                buildNode(objectNode, entry.getKey(), entry.getValue());
            }
        } else if (jsonElement.isJsonArray()) {
            DefaultMutableTreeNode arrayNode = new DefaultMutableTreeNode(new NodeInfo(key, "array", parentNode));
            parentNode.add(arrayNode);
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            if (!jsonArray.isEmpty()) {
                // We only build the structure from the first element as per the requirement
                buildNode(arrayNode, "[array_item]", jsonArray.get(0));
            }
        } else if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
            String type;
            if (primitive.isString()) {
                type = "string";
            } else if (primitive.isNumber()) {
                type = "number";
            } else if (primitive.isBoolean()) {
                type = "boolean";
            } else {
                type = "primitive";
            }
            DefaultMutableTreeNode primitiveNode = new DefaultMutableTreeNode(new NodeInfo(key, type, parentNode));
            parentNode.add(primitiveNode);
        } else if (jsonElement.isJsonNull()) {
            DefaultMutableTreeNode nullNode = new DefaultMutableTreeNode(new NodeInfo(key, "null", parentNode));
            parentNode.add(nullNode);
        }
    }

    // Helper class to store node information and build path
    public static class NodeInfo {
        public final String key;
        public final String type;
        private final DefaultMutableTreeNode parent; // To build the path

        public NodeInfo(String key, String type, DefaultMutableTreeNode parent) {
            this.key = key;
            this.type = type;
            this.parent = parent;
        }

        public String getPath() {
            if (parent == null || "root".equals(((NodeInfo) parent.getUserObject()).key)) {
                return key;
            }
            // Recursively build path, but stop if we hit an array item
            if ("[array_item]".equals(key)) {
                return ((NodeInfo) parent.getUserObject()).getPath();
            }
            return ((NodeInfo) parent.getUserObject()).getPath() + "." + key;
        }

        @Override
        public String toString() {
            // How the node is displayed in the tree
            return key + ": " + type;
        }
    }
}