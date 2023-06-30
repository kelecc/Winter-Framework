package top.kelecc.winter.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author 可乐
 */
@FunctionalInterface
public interface InputStreamCallback<T> {

    T doWithInputStream(InputStream stream) throws IOException;
}
