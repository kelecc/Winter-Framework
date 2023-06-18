package top.kelecc.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

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

    /**
     * 扫描 basePackage
     *
     * @param mapper 映射函数
     * @param <R>    映射后的类型
     * @return List集合，集合中是映射后的类型
     */
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

    /**
     * 进一步操作basePackage并细分扫描类型
     */
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

    /**
     * 扫描资源
     *
     * @param isJar  是否是jar
     * @param result 最终返回的集合
     * @param mapper 映射函数
     * @param <R>    返回类型
     */
    private <R> void scanFile(boolean isJar, String baseUriStr, Path root, List<R> result, Function<Resource, R> mapper) throws IOException, URISyntaxException {
        String baseDir = removeTrailingSlash(baseUriStr);
        if (isJar) {
            scanJar(root, baseDir, result, mapper);
            return;
        }
        try (Stream<Path> pathStream = Files.walk(root)) {
            pathStream.filter(Files::isRegularFile)
                    .forEach(file -> {
                        String path = file.toString().replace('\\', '/');
                        String name = removeLeadingSlash(path.substring(baseDir.length())).replace('\\', '/');
                        Resource resource = new Resource("file:" + path, name);
                        logger.debug("扫描到资源：{}", resource.getName());
                        R apply = mapper.apply(resource);
                        if (!Objects.isNull(apply)) {
                            result.add(apply);
                        }
                    });
        }

    }

    /**
     * 扫描JAR包
     */
    private <R> void scanJar(Path root, String baseDir, List<R> result, Function<Resource, R> mapper) throws IOException, URISyntaxException {
        String jarPathStr = baseDir.substring(4, baseDir.lastIndexOf("!"));
        URL url = new URL(jarPathStr);
        // 获取 URL 对应的 URI
        URI uri = url.toURI();
        // 创建 Path 对象
        Path jarPath = Paths.get(uri);
        // 创建 JarFile 实例
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // 遍历 JAR 文件中的条目
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                String path = removeTrailingSlash(root.toAbsolutePath().toString()) + name;
                // 处理条目，例如输出条目名称
                Resource resource = new Resource(path, name);
                logger.debug("扫描到资源：{}", resource.getName());
                R apply = mapper.apply(resource);
                if (!Objects.isNull(apply)) {
                    result.add(apply);
                }
            }
        }
    }

    /**
     * 转换文件系统
     */
    private Path jarUritoPath(URI jarUri, String baseUriStr) throws IOException {
        final Map<String, ?> map = new HashMap<>();
        try (FileSystem fileSystem = FileSystems.newFileSystem(jarUri, map)) {
            return fileSystem.getPath(jarUri.toString());
        }
    }


    /**
     * 去除结尾的斜杠和反斜杠
     *
     * @param str 需要去除的串
     * @return 去除后的串
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
     * @param str 需要去除的串
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
        ClassLoader c1 = Thread.currentThread().getContextClassLoader();
        if (Objects.isNull(c1)) {
            c1 = getClass().getClassLoader();
        }
        return c1;
    }
}
