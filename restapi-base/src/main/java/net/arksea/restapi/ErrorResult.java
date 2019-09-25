package net.arksea.restapi;

/**
 *
 * Created by xiaohaixing on 2019/5/23.
 */
public class ErrorResult<T> extends BaseResult {
    public final String error;

    public ErrorResult(int code, String reqid, String error) {
        super(code, reqid);
        this.error = error;
    }
}
