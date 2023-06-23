package top.kelecc.winter.aop;

import top.kelecc.winter.annotation.Component;
import top.kelecc.winter.annotation.Enhanced;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/22 19:50
 */
@Component
public class EnhancedProxyBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Enhanced> {
}
