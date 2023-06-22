package top.kelecc.component;

import top.kelecc.winter.annotation.Component;
import top.kelecc.winter.annotation.Value;

@Component
public class OriginBean {
    @Value("${app.title}")
    public String name;

    @Value("${app.version}")
    public String version;

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
