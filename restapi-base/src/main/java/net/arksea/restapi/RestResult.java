package net.arksea.restapi;

/**
 *
 * Created by xiaohaixing on 2018/5/24.
 */
public class RestResult<T> extends BaseResult {
    public final T result;

    public RestResult(int code, T result, String reqid) {
        super(code, reqid);
        this.result = result;
    }
}
