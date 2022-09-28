package net.arksea.restapi.utils.influx;

import net.arksea.restapi.RestExceptionHandler;
import net.arksea.restapi.RestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultRequestLogFilterConfig implements IRequestLogFilterConfig {
    private final List<String> includedUriPrefix; //包含这些前缀的请求才做记录
    private final List<String> ignoreUriPrefix;//忽略这些前缀的请求
    private final List<String> nameUriPrefix;  //带这些前缀的URI直接作为name
    private final Pattern uriToNamePattern;
    private final int namePatternMatchIndex;
    private final String requestGroupHeaderName;
    private final String requestIdHeaderName;
    private final Function<ServletRequest,Boolean> traceDeterminer;

    public DefaultRequestLogFilterConfig(final List<String> includedUriPrefix,
                                         final List<String> ignoreUriPrefix,
                                         final List<String> nameUriPrefix,
                                         final Pattern uriToNamePattern,
                                         final int namePatternMatchIndex,
                                         final String requestGroupHeaderName,
                                         final String requestIdHeaderName,
                                         final Function<ServletRequest,Boolean> traceDeterminer) {
        this.includedUriPrefix = includedUriPrefix;
        this.ignoreUriPrefix = ignoreUriPrefix;
        this.nameUriPrefix = nameUriPrefix;
        this.uriToNamePattern = uriToNamePattern;
        this.namePatternMatchIndex = namePatternMatchIndex;
        this.requestGroupHeaderName = requestGroupHeaderName;
        this.requestIdHeaderName = requestIdHeaderName;
        this.traceDeterminer = traceDeterminer;
    }

    //除了指定的路径，默认提取Path前3段
    @Override
    public String getName(ServletRequest request) {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        for (String pre: nameUriPrefix) {
            if (uri.startsWith(pre)) {
                return pre;
            }
        }
        Matcher matcher = uriToNamePattern.matcher(uri);
        if (matcher.find()) {
            return matcher.group(namePatternMatchIndex);
        } else {
            return uri;
        }
    }

    @Override
    public String getGroup(ServletRequest request) {
        HttpServletRequest req = (HttpServletRequest) request;
        String group = req.getHeader(requestGroupHeaderName);
        return group == null ? "" : group;
    }

    @Override
    public boolean ignore(ServletRequest request) {
        HttpServletRequest req = (HttpServletRequest) request;
        if (req.getMethod().equals(RequestMethod.OPTIONS.name())) { //OPTION请求不做统计
            return true;
        }
        String uri = req.getRequestURI();
        boolean needLog = false;
        //必须至少包含一项比如“/api"，否则不做记录，主要是防止用在web应用上，意外的对资源文件请求进行记录
        if (includedUriPrefix.isEmpty()) {
            return true;
        } else {
            for (String pre: includedUriPrefix) {
                if (uri.startsWith(pre)) {
                    needLog = true;
                    break;
                }
            }
        }
        if (needLog) {
            for (String pre : ignoreUriPrefix) {
                if (uri.startsWith(pre)) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    @Override
    public String makeExceptionResult(ServletRequest request, HttpStatus status, Throwable ex) {
        String error = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            String reqid = req.getHeader(requestIdHeaderName);
            if (reqid == null) {
                return RestUtils.createError(1, error);
            } else {
                return RestUtils.createError(1, error, reqid);
            }
        } else {
            return RestUtils.createError(1, error);
        }
    }
    @Override
    public boolean logDebugLevel(HttpStatus status, Throwable ex) {
        return RestExceptionHandler.logDebugLevel(status, ex);
    }
    @Override
    public boolean needTrace(ServletRequest request) {
        return traceDeterminer.apply(request);
    }
}
