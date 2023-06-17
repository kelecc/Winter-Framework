package top.kelecc.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * @author 可乐
 * @version 1.0
 * @description: 资源解析器
 * @date 2023/6/17 20:35
 */

public class ResourceResolver {
    Logger logger = LoggerFactory.getLogger(getClass());

    String basePackage;

    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
    }

    public <R> List<R> scan(Function<Resource, R> mapper) {
        String path = basePackage.replace(".", "/");

        List<R> result = new ArrayList<>();
        try {
            scan0(path, result, mapper);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private <R> void scan0(String path, List<R> result, Function<Resource, R> mapper) throws IOException, URISyntaxException {
        logger.debug("扫描：{}", path);
        Enumeration<URL> resources = getContextClassLoader().getResources(path);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            URI uri = url.toURI();
            //去掉尾部的斜杠和反斜杠，统一格式
            String uriStr = removeTrailingSlash(uri.toString());
            //获取根路径
            String baseUriStr = uriStr.substring(0, uriStr.length() - path.length());
            if (uriStr.startsWith("file:")) {
                baseUriStr = baseUriStr.substring(5);
            }
            if (uriStr.startsWith("jar:")) {
                scanFile(true, baseUriStr, jarUritoPath(uri, baseUriStr), result, mapper);
            } else {
                scanFile(false, baseUriStr, Paths.get(uri), result, mapper);
            }
        }
    }

    private <R> void scanFile(boolean isJar, String baseUriStr, Path root, List<R> result, Function<Resource, R> mapper) throws IOException {
        String baseDir = removeTrailingSlash(baseUriStr);
        Files.walk(root)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    Resource res = null;
                    if (isJar) {
                        res = new Resource(baseDir, removeLeadingSlash(file.toString()));
                    } else {
                        String path = file.toString();
                        String name = removeLeadingSlash(path.substring(baseDir.length()));
                        res = new Resource("file" + path, name);
                    }
                    logger.debug("扫描到资源：{}", res.getName());
                    R apply = mapper.apply(res);
                    if (!Objects.isNull(apply)){
                        result.add(apply);
                    }
                });
    }

    private Path jarUritoPath(URI jarUri, String baseUriStr) throws IOException {
        final Map<String, ?> map = new HashMap<>();
        return FileSystems.newFileSystem(jarUri, map).getPath(jarUri.toString());
    }


    /**
     * 去除结尾的斜杠和反斜杠
     *
     * @param str
     * @return
     */
    private String removeTrailingSlash(String str) {
        if (str.endsWith("\\") || str.endsWith("/")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * 去除开头的斜杠和反斜杠
     *
     * @param str
     * @return 去除后的串
     */
    private String removeLeadingSlash(String str) {
        if (str.startsWith("\\") || str.startsWith("/")) {
            str = str.substring(1);
        }
        return str;
    }


    /**
     * 获取ClassLoader
     *
     * @return ClassLoader
     */
    private ClassLoader getContextClassLoader() {
        ClassLoader c1 = null;
        c1 = Thread.currentThread().getContextClassLoader();
        if (Objects.isNull(c1)) {
            c1 = getClass().getClassLoader();
        }
        return c1;
    }


}
