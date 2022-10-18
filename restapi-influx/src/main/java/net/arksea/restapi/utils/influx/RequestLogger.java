package net.arksea.restapi.utils.influx;

import net.arksea.httpclient.asker.FuturedHttpClient;

@Deprecated //已改名为HttpRequestLogger
public class RequestLogger extends HttpRequestLogger {
    @Deprecated //要求提供表名参数
    public RequestLogger(String dbUrl, FuturedHttpClient futuredHttpClient) {
        super(dbUrl, futuredHttpClient);
    }

    public RequestLogger(String tableName, String dbUrl, FuturedHttpClient futuredHttpClient) {
        super(tableName, dbUrl, futuredHttpClient);
    }
}
