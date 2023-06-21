package top.kelecc.exception;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/6/19 15:39
 */
public class BeanDefinitionException extends BeansException {
    public BeanDefinitionException() {
    }

    public BeanDefinitionException(String message) {
        super(message);
    }

    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanDefinitionException(Throwable cause) {
        super(cause);
    }
}
