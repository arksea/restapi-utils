package net.arksea.restapi.utils.influx;

@Deprecated //已改名为HttpRequestLogFilter
public class RequestLogFilter extends HttpRequestLogFilter {
    public RequestLogFilter(IHttpRequestLogFilterConfig config) {
        super(config);
    }

    public RequestLogFilter(IHttpRequestLogFilterConfig config, IRequestLogger requestLogger) {
        super(config, requestLogger);
    }

    public RequestLogFilter(IHttpRequestLogFilterConfig config, IRequestLogger requestLogger, boolean delayCountRequest) {
        super(config, requestLogger, delayCountRequest);
    }
}
