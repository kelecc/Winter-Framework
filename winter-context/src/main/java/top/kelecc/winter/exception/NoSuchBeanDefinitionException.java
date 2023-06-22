package top.kelecc.winter.exception;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/20 21:53
 */
public class NoSuchBeanDefinitionException extends BeanDefinitionException {
    public NoSuchBeanDefinitionException() {
    }

    public NoSuchBeanDefinitionException(String message) {
        super(message);
    }
}
