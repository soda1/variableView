package com.eric;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 调试时导出对象为 JSON 文件
 */
public class DebugUtil {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    /**
     * 将对象序列化为 JSON 文件，自动加时间戳命名
     *
     * @param obj 要导出的对象
     * @param filePath 输出文件路径，例如 "C:/temp/debug.json"
     */
    public static void dump(Object obj, String filePath) {
        try {
            File file = new File(filePath);

            // 自动创建父目录
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // 自动加时间戳防止覆盖
            int dotIndex = filePath.lastIndexOf('.');
            String timestamp = "_" + LocalDateTime.now().format(TIME_FORMAT);
            String finalPath = (dotIndex > 0)
                    ? filePath.substring(0, dotIndex) + timestamp + filePath.substring(dotIndex)
                    : filePath + timestamp + ".json";

            // 写入 JSON
            try (FileWriter writer = new FileWriter(finalPath)) {
                writer.write(JSON.toJSONString(
                        obj,
                        SerializerFeature.PrettyFormat,
                        SerializerFeature.WriteDateUseDateFormat,
                        SerializerFeature.DisableCircularReferenceDetect
                ));
            }

            System.out.println("[DEBUG] JSON dumped to: " + finalPath);
        } catch (IOException e) {
            System.err.println("[DEBUG] Dump failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Map<String, Object> context = new HashMap<>();
        context.put("a", "123213");
        context.put("hah",new person("heloo", 13));
        System.out.println("hdfaf");
    }
}
