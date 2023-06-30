package top.kelecc.winter.util;

import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.Map;

class YamlUtilsTest {

    @Test
    void loadYamlAsPlainMap() {
    }

    @Test
    public void loadYamlTest() throws FileNotFoundException {
        Map<String, Object> map = YamlUtils.loadYamlAsPlainMap("application.yaml");
        System.out.println(map.get("kele.lover.name"));
    }
}
