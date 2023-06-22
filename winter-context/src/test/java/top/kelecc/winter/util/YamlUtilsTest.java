package top.kelecc.winter.util;

import org.junit.jupiter.api.Test;
import top.kelecc.util.YamlUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
