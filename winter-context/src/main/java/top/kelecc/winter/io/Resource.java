package top.kelecc.winter.io;

import java.util.Objects;

/**
 * @author 可乐
 * @version 1.0
 * @description: 文件类
 * @date 2023/6/17 20:35
 */
public class Resource {

    private final String path;
    private final String name;

    public Resource(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return this.path;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "path='" + path + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return Objects.equals(path, resource.path) &&
                Objects.equals(name, resource.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name);
    }
}
