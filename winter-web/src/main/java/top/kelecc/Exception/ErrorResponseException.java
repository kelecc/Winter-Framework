package top.kelecc.Exception;

import top.kelecc.winter.exception.NestedRuntimeException;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/7/4 20:27
 */
public class ErrorResponseException extends NestedRuntimeException {
    public final int statusCode;
    public ErrorResponseException(int statusCode) {
        this.statusCode = statusCode;
    }

    public ErrorResponseException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ErrorResponseException(int statusCode, Throwable cause) {
        super(cause);
        this.statusCode = statusCode;
    }

    public ErrorResponseException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
