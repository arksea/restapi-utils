package net.arksea.restapi.utils.influx;

import net.arksea.restapi.RestException;
import net.arksea.restapi.RestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 *
 * Created by xiaohaixing on 2018/4/2.
 */
public class RequestLogFilter implements Filter {
    private static final Logger TRACE_LOGGER = LogManager.getLogger("net.arksea.restapi.logger.traceLogger");
    private static final Logger LOGGER = LogManager.getLogger("net.arksea.restapi.logger.InternalError");
    private static final Logger BADREQ_LOGGER = LogManager.getLogger("net.arksea.restapi.logger.BadRequest");

    private final IRequestLogger requestLogger;
    private final IRequestLogFilterConfig config;
    private final boolean delayCountRequest;

    public RequestLogFilter(IRequestLogFilterConfig config) {
        this(config, null);
    }
    public RequestLogFilter(IRequestLogFilterConfig config, IRequestLogger requestLogger) {
        this(config, requestLogger, true);
    }
    public RequestLogFilter(IRequestLogFilterConfig config, IRequestLogger requestLogger, boolean delayCountRequest) {
        this.config = config;
        this.requestLogger = requestLogger;
        this.delayCountRequest = delayCountRequest;
        if (requestLogger != null) {
            startWrite(requestLogger, this.LOGGER);
        }
    }

    private void startWrite(IRequestLogger requestLogger, Logger logger) {
        final Thread writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(true) {
                        Thread.sleep(10000);
                        requestLogger.writeLogs();
                    }
                } catch (InterruptedException ex) {
                    logger.error("Request log write thread interrputed", ex);
                }
            }
        });
        writeThread.setDaemon(true);
        writeThread.start();
    }

    @Override
    public void destroy() {
        //do nothing
    }

    //filter需要配置两个dispatcher：
    // <dispatcher>REQUEST</dispatcher>
    // <dispatcher>ASYNC</dispatcher>
    //一个在收到request时调用，另一个在获得respond结果时调用
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest req = (HttpServletRequest) request;
        final HttpServletResponse resp = (HttpServletResponse) response;
        if (config.ignore(request)) {
            chain.doFilter(req, resp);
            return;
        }
        if (req.getAttribute("-restapi-start-time") == null) { //同步调用， 防止配置有误，多次调用此filter
            getGroupNameAndDoFilter(req, resp, chain);
        } else { //异步调用
            //1、当Controller为异步方法，在设置respond结果后，无论是正确的结果还是错误的结果，ASYNC的doFilter将被调用，逻辑将走到此处
            //2、当Controller为异步方法，在Controller超时没有设置结果时，ASYNC的doFilter会被调用，逻辑将走到此处
            Boolean needTrace = (Boolean)req.getAttribute("-restapi-need-trace");
            if (needTrace!=null && needTrace && TRACE_LOGGER.isInfoEnabled()) {
                RespondWrapper respWrapper = (RespondWrapper)request.getAttribute("__respWrapper");
                chain.doFilter(req, respWrapper);
                try {
                    //如果ContentType不为null，说明Controller为同步模式，此时已返回结果，
                    //如果ContentType为null，说明Controller为异步模式，此时Controller还未实际执行
                    String respBody = respWrapper.getRespondBody();
                    final StringBuilder sb = new StringBuilder();
                    RestUtils.fillRequestLogInfo(sb, req);
                    sb.append("--- request body:\n");
                    RequestWrapper reqWrapper = (RequestWrapper) request.getAttribute("__reqWrapper");
                    sb.append(reqWrapper.getBody());
                    RestUtils.fillResponseLogInfo(sb, resp);
                    sb.append("\n--- response body:\n");
                    sb.append(respBody);
                    TRACE_LOGGER.info(sb.toString());
                } catch (Exception ex) {
                    TRACE_LOGGER.error("log trace info failed",ex);
                }
            } else {
                chain.doFilter(req, resp);
            }
            if (requestLogger != null) {
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
                Long requestStartTime = (Long) req.getAttribute("-restapi-start-time");
                String name = (String) req.getAttribute("-restapi-name");
                String group = (String) req.getAttribute("-restapi-group");
                long respondTime = System.currentTimeMillis() - requestStartTime;
                requestLogger.respond(name, group, status, respondTime);
                if (delayCountRequest) {
                    requestLogger.request(name, group);
                }
            }
        }
    }

    private void getGroupNameAndDoFilter(final HttpServletRequest req,HttpServletResponse resp,final FilterChain chain) throws IOException {
        long startTime = System.currentTimeMillis();
        req.setAttribute("-restapi-start-time", startTime);
        boolean needTrace = config.needTrace(req);
        req.setAttribute("-restapi-need-trace", needTrace); //用于保证每次请求只调用一次config.needTrace(req)
        String name = "unknown";
        String group = "unknown";
        try {
            name = config.getName(req);
            group = config.getGroup(req);
        } catch (Exception ex) {
            handleException(ex, name, group, startTime,req,resp);
            return;
        }
        req.setAttribute("-restapi-name", name);
        req.setAttribute("-restapi-group", group);
        if (!delayCountRequest) {
            if (requestLogger != null) {
                requestLogger.request(name, group);
            }
        }
        try {
            if (needTrace && TRACE_LOGGER.isInfoEnabled()) {
                RequestWrapper reqWrapper = new RequestWrapper(req);
                RespondWrapper respWrapper = new RespondWrapper(resp);
                req.setAttribute("__reqWrapper", reqWrapper); //异步调用时需要暂时先存下来
                req.setAttribute("__respWrapper", respWrapper); //异步调用时需要暂时先存下来
                chain.doFilter(reqWrapper, respWrapper);
                //如果ContentType不为null，说明Controller为同步模式，此时已返回结果，
                //如果ContentType为null，说明Controller为异步模式，此时Controller还未实际执行
                if (respWrapper.getContentType() != null) {
                    if (TRACE_LOGGER.isInfoEnabled()) {
                        try {
                            String respBody = respWrapper.getRespondBody();
                            final StringBuilder sb = new StringBuilder();
                            RestUtils.fillRequestLogInfo(sb, req);
                            sb.append("--- request body:\n");
                            sb.append(reqWrapper.getBody());
                            RestUtils.fillResponseLogInfo(sb, resp);
                            sb.append("\n--- response body:\n");
                            sb.append(respBody);
                            TRACE_LOGGER.info(sb.toString());
                        } catch (Exception ex) {
                            TRACE_LOGGER.error("log trace info failed",ex);
                        }
                    }
                }
            } else {
                chain.doFilter(req, resp);
            }
            //当Controller同步返回结果时，ContentType不为null
            //当Controller为异步方法，此时返回respond还未设置结果
            //如果配置了ExceptionHandler，Controller抛出的异常会被Handler拦截，逻辑也会走到这里，
            //    Handler中 return new ResponseEntity<Object>(body, headers, status), status可以在此处取到
            if (requestLogger != null && resp.getContentType() != null) {
                int status = resp.getStatus();
                long respondTime = System.currentTimeMillis() - startTime;
                requestLogger.respond(name, group, status, respondTime);
                if (delayCountRequest) {
                    requestLogger.request(name, group);
                }
            }
        } catch (Exception ex) {
            //1、如果没有配置ExceptionHandler，Controller抛出的异常将在这里被catch; 2、本Filter之后的Filter中抛出的异常也将在这里catch
            handleException(ex, name, group, startTime,req,resp);
        }
    }

    private void handleException(RestException ex, String name, String group, long startTime,
                                 HttpServletRequest req,HttpServletResponse resp) throws IOException {
        HttpStatus retStatus = ex.getStatus();
        if (config.logDebugLevel(retStatus, ex)) {
            BADREQ_LOGGER.debug(()->RestUtils.getRequestLogInfo(ex, retStatus, req, ""), ex);
        } else {
            LOGGER.warn(()->RestUtils.getRequestLogInfo(ex, retStatus, req, ""), ex);
        }
        if (requestLogger != null) {
            requestLogger.respond(name, group, ex.getStatus().value(), System.currentTimeMillis() - startTime);
            if (delayCountRequest) {
                requestLogger.request(name, group);
            }
        }
        resultError(ex.getStatus(), ex, req, resp);
    }

    private void handleException(Throwable ex, String name, String group, long startTime,
                                 HttpServletRequest req,HttpServletResponse resp) throws IOException {
        if (config.logDebugLevel(HttpStatus.INTERNAL_SERVER_ERROR, ex)) {
            BADREQ_LOGGER.debug(()->RestUtils.getRequestLogInfo(ex, HttpStatus.INTERNAL_SERVER_ERROR, req, ""), ex);
        } else {
            LOGGER.warn(()->RestUtils.getRequestLogInfo(ex, HttpStatus.INTERNAL_SERVER_ERROR, req, ""), ex);
        }
        if (requestLogger != null) {
            requestLogger.respond(name, group, HttpStatus.INTERNAL_SERVER_ERROR.value(), System.currentTimeMillis() - startTime);
            if (delayCountRequest) {
                requestLogger.request(name, group);
            }
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
