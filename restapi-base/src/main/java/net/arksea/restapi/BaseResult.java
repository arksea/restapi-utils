package net.arksea.restapi;

/**
 *
 * Created by xiaohaixing on 2019/5/23.
 */
public class BaseResult {
    public final int code;
    public final String reqid;

    public BaseResult(int code, String reqid) {
        this.code = code;
        this.reqid = reqid;
    }
}
