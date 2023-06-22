package top.kelecc.component;

import top.kelecc.winter.annotation.Autowired;
import top.kelecc.winter.annotation.Component;

@Component
public class InjectProxyOnConstructorBean {
    public final OriginBean injected;

    public InjectProxyOnConstructorBean(@Autowired OriginBean injected) {
        this.injected = injected;
    }
}
