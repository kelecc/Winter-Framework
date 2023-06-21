package top.kelecc.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/18 20:23
 */
public class YamlUtils {
    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadYamlAsPlainMap(String path) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("application.yaml")) {

            LoaderOptions loaderOptions = new LoaderOptions();
            DumperOptions dumperOptions = new DumperOptions();
            Representer representer = new Representer(dumperOptions);
            NoImplicitResolver resolver = new NoImplicitResolver();
            // 读取 application.yaml 文件
            Yaml yaml = new Yaml(new Constructor(loaderOptions), representer, dumperOptions, loaderOptions, resolver);
            Map<String, Object> configMap = yaml.load(inputStream);
            // 遍历嵌套 Map，将嵌套层级为一级的数据提取到新的 Map 中
            Map<String, Object> flattenedMap = new HashMap<>();
            flattenMap("", configMap, flattenedMap);
            return flattenedMap;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 辅助方法，将嵌套 Map 展平为一级的 Map
     */
    @SuppressWarnings("unchecked")
    private static void flattenMap(String prefix, Map<String, Object> sourceMap, Map<String, Object> targetMap) {
        for (Map.Entry<String, Object> entry : sourceMap.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                // 递归处理嵌套的 Map
                flattenMap(key, (Map<String, Object>) value, targetMap);
            } else {
                // 将提取的 key-value 对放入一级的 Map 中
                targetMap.put(key, value);
            }
        }
    }
}

/**
 * Disable ALL implicit convert and treat all values as string.
 */
class NoImplicitResolver extends Resolver {
    public NoImplicitResolver() {
        super();
        super.yamlImplicitResolvers.clear();
    }
}
