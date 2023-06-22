package top.kelecc.winter.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

class YamlUtilsTest {

    @Test
    void loadYamlAsPlainMap() {
    }

    @Test
    public void loadYamlTest(){
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("application.yaml");
        System.out.println(map.get("kele.lover.name"));
    }
}
