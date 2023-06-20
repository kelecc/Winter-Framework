package top.kelecc.context;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/20 13:43
 */
public interface BeanPostProcessor {

    /**
     * Invoked after new Bean().
     *
     * @param bean
     * @param beanName
     * @return
     */
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * Invoked after bean.init() called.
     *
     * @param bean
     * @param beanName
     * @return
     */
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    /**
     * Invoked before bean.setXyz() called.
     *
     * @param bean
     * @param beanName
     * @return
     */
    default Object postProcessOnSetProperty(Object bean, String beanName) {
        return bean;
    }
}
