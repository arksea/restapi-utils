package net.arksea.restapi.utils.influx;

import net.arksea.restapi.RestException;
import net.arksea.restapi.RestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import zipkin2.Endpoint;
import zipkin2.Span;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 *
 * Created by xiaohaixing on 2018/4/2.
 */
public class HttpRequestLogFilter implements Filter {
    private static final Logger LOGGER = LogManager.getLogger("net.arksea.restapi.logger.InternalError");
    private static final Logger BADREQ_LOGGER = LogManager.getLogger("net.arksea.restapi.logger.BadRequest");

    private final IRequestLogger requestLogger;
    private final IHttpRequestLogFilterConfig config;
    private final Object lock = new Object();

    public HttpRequestLogFilter(IHttpRequestLogFilterConfig config) {
        this(config, null);
    }

    public HttpRequestLogFilter(IHttpRequestLogFilterConfig config, IRequestLogger requestLogger) {
        this.config = config;
        this.requestLogger = requestLogger;
        if (requestLogger != null) {
            startWrite(requestLogger);
        }
    }

    private void startWrite(IRequestLogger requestLogger) {
        final Thread writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(true) {
                        synchronized (lock) {
                            lock.wait(10000);
                        }
                        requestLogger.writeLogs();
                    }
                } catch (InterruptedException ex) {
                    LOGGER.error("Request log write thread interrputed", ex);
                }
            }
        });
        writeThread.setDaemon(true);
        writeThread.start();
    }

    @Override
    public void destroy() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    //filter需要配置两个dispatcher：
    // <dispatcher>REQUEST</dispatcher>
    // <dispatcher>ASYNC</dispatcher>
    //一个在收到request时调用，另一个在获得respond结果时调用
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse resp = (HttpServletResponse) response;
        if (config.ignore(req)) {
            chain.doFilter(req, resp);
            return;
        }
        if (req.getAttribute("-restapi-start-time") == null) {
            //第一次dispatcher，针对web.xml的REQUEST配置:
            //<dispatcher>REQUEST</dispatcher>
            //Attribute "-restapi-start-time" 用于判断是否是第一次dispatcher
            //所以在onRequestDispatcher时必须设置此Attribute
            onRequestDispatcher(req, resp, chain);
        } else {
            //第二次dispatcher，针对web.xml的ASYNC配置：
            //<dispatcher>ASYNC</dispatcher>
            onAsyncDispatcher(req, resp, chain);
        }
    }

    private void onRequestDispatcher(final HttpServletRequest req, HttpServletResponse resp, final FilterChain chain) throws IOException, ServletException {
        long startTime = System.currentTimeMillis();
        req.setAttribute("-restapi-start-time", startTime);
        HttpTraceInfo traceInfo;
        RequestWrapper reqWrapper = null;
        try {
            if (config.isAlwaysWrapRequest()) {
                reqWrapper = new RequestWrapper(req);
                req.setAttribute("__reqWrapper", reqWrapper); //异步调用时需要,暂时先存下来
            }
            traceInfo = config.tracePrejudgement(req, reqWrapper);
            req.setAttribute("-restapi-trace-info", traceInfo);
        } catch (Exception ex) {
            chain.doFilter(req, resp);
            return;
        }
        try {
            if (traceInfo.needSampled()) {
                if (reqWrapper == null) {
                    reqWrapper = new RequestWrapper(req);
                    req.setAttribute("__reqWrapper", reqWrapper); //异步调用时需要,暂时先存下来
                }
                RespondWrapper respWrapper = new RespondWrapper(resp);
                req.setAttribute("__respWrapper", respWrapper); //异步调用时需要,暂时先存下来
                chain.doFilter(reqWrapper, respWrapper);
                //如果ContentType不为null，说明Controller为同步模式，此时已返回结果，
                //如果ContentType为null，说明Controller为异步模式，此时Controller还未实际执行
                if (respWrapper.getContentType() != null) {
                    long respondTime = System.currentTimeMillis() - startTime;
                    if (config.deferDetermineSampled(req, resp, reqWrapper, respWrapper, traceInfo)) {
                        reportTrace(req, traceInfo, startTime, respondTime);
                    }
                }
            } else {
                chain.doFilter(reqWrapper == null ? req : reqWrapper, resp);
            }
            //当Controller同步返回结果时，ContentType不为null
            //当Controller为异步方法，此时返回respond还未设置结果
            //如果配置了ExceptionHandler，Controller抛出的异常会被Handler拦截，逻辑也会走到这里，
            //    Handler中 return new ResponseEntity<Object>(body, headers, status), status可以在此处取到
            if (requestLogger != null && resp.getContentType() != null) {
                int status = resp.getStatus();
                long respondTime = System.currentTimeMillis() - startTime;
                requestLogger.monitor(traceInfo.getRequestName(), traceInfo.getRequestGroup(), status, respondTime);
            }
        } catch (Exception ex) {
            //1、如果没有配置ExceptionHandler，Controller抛出的异常将在这里被catch; 2、本Filter之后的Filter中抛出的异常也将在这里catch
            handleException(ex, traceInfo.getRequestName(), traceInfo.getRequestGroup(), startTime,req,resp);
        }
    }

    public void onAsyncDispatcher(final HttpServletRequest req, HttpServletResponse resp, final FilterChain chain) throws IOException, ServletException {
        Long requestStartTime = (Long) req.getAttribute("-restapi-start-time");
        //1、当Controller为异步方法，在设置respond结果后，无论是正确的结果还是错误的结果，ASYNC的doFilter将被调用，逻辑将走到此处
        //2、当Controller为异步方法，在Controller超时没有设置结果时，ASYNC的doFilter会被调用，逻辑将走到此处
        HttpTraceInfo traceInfo = (HttpTraceInfo)req.getAttribute("-restapi-trace-info");
        if (traceInfo!=null && traceInfo.needSampled()) {
            RequestWrapper reqWrapper = (RequestWrapper) req.getAttribute("__reqWrapper");
            RespondWrapper respWrapper = (RespondWrapper)req.getAttribute("__respWrapper");
            chain.doFilter(reqWrapper, respWrapper);
            //如果ContentType不为null，说明Controller为同步模式，此时已返回结果，
            //如果ContentType为null，说明Controller为异步模式，此时Controller还未实际执行
            long respondTime = System.currentTimeMillis() - requestStartTime;
            if (config.deferDetermineSampled(req, resp, reqWrapper, respWrapper, traceInfo)) {
                reportTrace(req, traceInfo, requestStartTime, respondTime);
            }
        } else {
            chain.doFilter(req, resp);
        }
        if (requestLogger != null && traceInfo != null) {
            int status = resp.getStatus();
            if (status >= 400 && status <= 600 && req.getAttribute("-restapi-error-logged") == null) { //RestExceptionHandler未记录此异常，时写一条日志
                //何时出现
                //1、没有配置RestExceptionHandler时
                //2、使用异步模式，DeferredResult.setResult一个status不为200的结果时，
                //   而非DeferredResult.setErrorResult一个异常(这种情况RestExceptionHandler将会拦截到这个异常并处理)
                HttpStatus retStatus = HttpStatus.valueOf(status);
                RestException ex = new RestException("error result");
                String alarmMsg = RestUtils.getRequestLogInfo(ex, retStatus, req, "");
                //外部错误日志用debug级别
                if (config.logDebugLevel(retStatus, ex)) {
                    BADREQ_LOGGER.debug(alarmMsg, ex);
                } else {
                    LOGGER.warn(alarmMsg, ex);
                }
            }
            long respondTime = System.currentTimeMillis() - requestStartTime;
            requestLogger.monitor(traceInfo.getRequestName(), traceInfo.getRequestGroup(), status, respondTime);
        }
    }

    private void reportTrace(final HttpServletRequest req,
                             HttpTraceInfo traceInfo,
                             long startTime, long duration) {
        try {
            Endpoint localEndpoint = Endpoint.newBuilder()
                    .serviceName(req.getServerName())
                    .ip(req.getLocalAddr())
                    .port(req.getLocalPort())
                    .build();
            Endpoint remoteEndpoint = Endpoint.newBuilder()
                    .ip(req.getRemoteHost())
                    .port(req.getRemotePort())
                    .build();
            Span.Builder sb = Span.newBuilder();
            Map<String, String> tags = traceInfo.getTags();
            if (tags != null) {
                tags.forEach(sb::putTag);
            }
            Span span = sb.traceId(traceInfo.getTraceId())
                    .parentId(traceInfo.getParentSpanId())
                    .id(config.makeSpanId())
                    .kind(Span.Kind.SERVER)
                    .name(req.getMethod() + " " + req.getRequestURI())
                    .timestamp(startTime)
                    .duration(duration)
                    .localEndpoint(localEndpoint)
                    .remoteEndpoint(remoteEndpoint)
                    .build();
            requestLogger.trace(span);
        } catch (Exception ex) {
            LOGGER.error("log trace info failed",ex);
        }
    }

    private void handleException(Throwable ex, String name, String group, long startTime,
                                 HttpServletRequest req,HttpServletResponse resp) throws IOException {
        if (config.logDebugLevel(HttpStatus.INTERNAL_SERVER_ERROR, ex)) {
            BADREQ_LOGGER.debug(()->RestUtils.getRequestLogInfo(ex, HttpStatus.INTERNAL_SERVER_ERROR, req, ""), ex);
        } else {
            LOGGER.warn(()->RestUtils.getRequestLogInfo(ex, HttpStatus.INTERNAL_SERVER_ERROR, req, ""), ex);
        }
        if (requestLogger != null) {
            requestLogger.monitor(name, group, HttpStatus.INTERNAL_SERVER_ERROR.value(), System.currentTimeMillis() - startTime);
        }
        resultError(HttpStatus.INTERNAL_SERVER_ERROR, ex, req, resp);
    }

    private void resultError(HttpStatus status, Throwable ex, HttpServletRequest request, HttpServletResponse httpResponse) throws IOException {
        PrintWriter out = null;
        try {
            httpResponse.setStatus(status.value());
            out = httpResponse.getWriter();
            String data = config.makeExceptionResult(request, status, ex);
            out.write(data);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    public void init(final FilterConfig config) throws ServletException {
        //do nothing
        LOGGER.info("RequestLogFilter init()");
    }
}
