package top.kelecc.component;

import top.kelecc.annotation.Component;
import top.kelecc.annotation.Value;

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
