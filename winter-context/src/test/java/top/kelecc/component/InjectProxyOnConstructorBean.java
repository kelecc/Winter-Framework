package top.kelecc.component;

import top.kelecc.annotation.Autowired;
import top.kelecc.annotation.Component;

@Component
public class InjectProxyOnConstructorBean {
    public final OriginBean injected;

    public InjectProxyOnConstructorBean(@Autowired OriginBean injected) {
        this.injected = injected;
    }
}
