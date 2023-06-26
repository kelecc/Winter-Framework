package top.kelecc.winter.jdbc.tx;

import top.kelecc.winter.annotation.Transactional;
import top.kelecc.winter.aop.AnnotationProxyBeanPostProcessor;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/25 20:43
 */
public class TransactionalBeanPostProcessor extends AnnotationProxyBeanPostProcessor<Transactional> {
}
