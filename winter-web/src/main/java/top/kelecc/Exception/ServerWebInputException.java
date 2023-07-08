package top.kelecc.Exception;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/7/4 20:26
 */
public class ServerWebInputException extends ErrorResponseException {

    public ServerWebInputException() {
        super(400);
    }

    public ServerWebInputException(String message) {
        super(400, message);
    }

    public ServerWebInputException(Throwable cause) {
        super(400, cause);
    }

    public ServerWebInputException(String message, Throwable cause) {
        super(400, message, cause);
    }
}
