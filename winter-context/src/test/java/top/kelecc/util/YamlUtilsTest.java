package top.kelecc.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/18 20:35
 */
public class YamlUtilsTest {

    @Test
    public void loadYamlTest(){
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("application.yaml");
        System.out.println(map.get("kele.lover.name"));
    }

}
