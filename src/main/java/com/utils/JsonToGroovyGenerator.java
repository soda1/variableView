package com.utils;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonToGroovyGenerator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<String, LinkedHashMap<String, String>> classMap = new LinkedHashMap<>();

    public static void parseContextObjToClass(Object context) throws IOException {
        Path output = Paths.get("D:\\My\\code\\demo\\netty\\src\\main\\java\\com\\eric\\groovy\\ContextRoot.groovy");
        String jsonContent = JSON.toJSONString(context);
        JsonNode json = mapper.readTree(jsonContent);

        processJsonNode(json, "ContextRoot");

        StringBuilder sb = new StringBuilder();
        sb.append("package com.eric.groovy\n\n");
        sb.append("import groovy.transform.Canonical\n\n");

        for (Map.Entry<String, LinkedHashMap<String, String>> entry : classMap.entrySet()) {
            String className = entry.getKey();
            LinkedHashMap<String, String> props = entry.getValue();
            sb.append("@Canonical\n");
            sb.append("class ").append(className).append(" {\n");
            for (Map.Entry<String, String> prop : props.entrySet()) {
                sb.append("    ").append(prop.getValue()).append(" ").append(prop.getKey()).append("\n");
            }
            sb.append("}\n\n");
        }

        // Java 8 写入文件
        Files.write(output, sb.toString().getBytes(StandardCharsets.UTF_8));

        System.out.println("✅ Generated: " + output.toAbsolutePath());
    }
    public static void main(String[] args) throws Exception {

        Path input = Paths.get("D:\\My\\code\\demo\\netty\\src\\main\\java\\com\\eric\\_context.json");
        String rootName = sanitizeClassName("ContextRoot");
        Path output = Paths.get("D:\\My\\code\\demo\\netty\\src\\main\\java\\com\\eric\\groovy\\ContextRoot.groovy");

        // Java 8 读取文件
        String jsonContent = new String(Files.readAllBytes(input), StandardCharsets.UTF_8);

        JsonNode json = mapper.readTree(jsonContent);

        processJsonNode(json, rootName);

        StringBuilder sb = new StringBuilder();
        sb.append("package com.eric.groovy\n\n");
        sb.append("import groovy.transform.Canonical\n\n");

        for (Map.Entry<String, LinkedHashMap<String, String>> entry : classMap.entrySet()) {
            String className = entry.getKey();
            LinkedHashMap<String, String> props = entry.getValue();
            sb.append("@Canonical\n");
            sb.append("class ").append(className).append(" {\n");
            for (Map.Entry<String, String> prop : props.entrySet()) {
                sb.append("    ").append(prop.getValue()).append(" ").append(prop.getKey()).append("\n");
            }
            sb.append("}\n\n");
        }

        // Java 8 写入文件
        Files.write(output, sb.toString().getBytes(StandardCharsets.UTF_8));

        System.out.println("✅ Generated: " + output.toAbsolutePath());
    }

    private static void processJsonNode(JsonNode node, String className) {
        if (node.isObject()) {
            LinkedHashMap<String, String> props = classMap.computeIfAbsent(className, k -> new LinkedHashMap<>());
            node.fields().forEachRemaining(entry -> {
                String key = sanitizePropName(entry.getKey());
                JsonNode value = entry.getValue();
                String type = inferType(value, capitalize(key));
                props.put(key, type);
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                processJsonNode(child, className);
            }
        }
    }

    private static String inferType(JsonNode node, String classHint) {
        if (node.isNull()) return "Object";
        if (node.isBoolean()) return "boolean";
        if (node.isInt()) return "int";
        if (node.isLong()) return "long";
        if (node.isDouble() || node.isFloat() || node.isBigDecimal()) return "double";
        if (node.isTextual()) return "String";

        if (node.isArray()) {
            if (!node.elements().hasNext()) return "List<Object>";
            JsonNode first = null;
            for (JsonNode item : node) {
                if (!item.isNull()) { first = item; break; }
            }
            if (first == null) return "List<Object>";
            String elementType = inferType(first, singularize(classHint));
            return "List<" + elementType + ">";
        }

        if (node.isObject()) {
            String nestedClass = sanitizeClassName(classHint);
            processJsonNode(node, nestedClass);
            return nestedClass;
        }

        return "Object";
    }

    private static String sanitizeClassName(String name) {
        String clean = name.replaceAll("[^A-Za-z0-9_]", "");
        if (clean.isEmpty()) clean = "ClassName";
        clean = Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
        return clean;
    }

    private static String sanitizePropName(String name) {
        String clean = name.replaceAll("[^A-Za-z0-9_]", "_");
        if (Character.isDigit(clean.charAt(0))) clean = "_" + clean;
        return clean;
    }

    private static String singularize(String s) {
        if (s.endsWith("ies")) return s.substring(0, s.length() - 3) + "y";
        if (s.endsWith("s") && s.length() > 1) return s.substring(0, s.length() - 1);
        return s;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
