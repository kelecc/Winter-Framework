package top.kelecc.Exception;

/**
 * @author 可乐
 * @version 1.0
 * @description:
 * @date 2023/7/4 20:26
 */
public class ServerErrorException extends ErrorResponseException{
    public ServerErrorException() {
        super(500);
    }

    public ServerErrorException(String message) {
        super(500, message);
    }

    public ServerErrorException(Throwable cause) {
        super(500, cause);
    }

    public ServerErrorException(String message, Throwable cause) {
        super(500, message, cause);
    }
}
