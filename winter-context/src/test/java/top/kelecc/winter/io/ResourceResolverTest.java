package top.kelecc.winter.io;

import org.junit.jupiter.api.Test;
import top.kelecc.io.ResourceResolver;

import java.util.List;

/**
 * Unit test for simple App.
 */
public class ResourceResolverTest {

    /**
     * 扫描类名测试
     */
    @Test
    public void scanTest() {
        ResourceResolver rr = new ResourceResolver("");
        List<String> classList = rr.scan(res -> {
            String name = res.getName(); // 资源名称"org/example/Hello.class"
            if (name.endsWith(".class")) { // 如果以.class结尾
                // 把"org/example/Hello.class"变为"org.example.Hello":
                return name.substring(0, name.length() - 6).replace("/", ".").replace("\\", ".");
            }
            // 否则返回null表示不是有效的Class Name:
            return null;
        });
        System.out.println(classList);
    }

}
