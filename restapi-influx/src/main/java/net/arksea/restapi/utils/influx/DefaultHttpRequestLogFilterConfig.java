package net.arksea.restapi.utils.influx;

import net.arksea.restapi.RestExceptionHandler;
import net.arksea.restapi.RestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultHttpRequestLogFilterConfig implements IHttpRequestLogFilterConfig {
    public static final String DEFAULT_URI_TO_NAME_PATTERN = "(/(?:(?:[^/?]+)(?:/[^/?]+)?(?:/[^/?]+)?))";
    private final List<String> includedUriPrefix;//包含这些前缀的请求才做记录
    private final List<String> ignoreUriPrefix;  //忽略这些前缀的请求
    private final List<String> nameUriPrefix;    //带这些前缀的URI直接作为name
    private final Pattern uriToNamePattern;
    private final int namePatternMatchIndex;
    private final String requestGroupHeaderName;
    private final String requestIdHeaderName;
    private final boolean alwaysWrapRequest;

    public DefaultHttpRequestLogFilterConfig(final List<String> includedUriPrefix,
                                             final List<String> ignoreUriPrefix,
                                             final List<String> nameUriPrefix,
                                             final String requestGroupHeaderName,
                                             final String requestIdHeaderName,
                                             final boolean alwaysWrapRequest) {
        this(includedUriPrefix,
                ignoreUriPrefix,
                nameUriPrefix,
                Pattern.compile(DefaultHttpRequestLogFilterConfig.DEFAULT_URI_TO_NAME_PATTERN),
                1,
                requestGroupHeaderName,
                requestIdHeaderName,
                alwaysWrapRequest);
    }

    public DefaultHttpRequestLogFilterConfig(final List<String> includedUriPrefix,
                                             final List<String> ignoreUriPrefix,
                                             final List<String> nameUriPrefix,
                                             final Pattern uriToNamePattern,
                                             final int namePatternMatchIndex,
                                             final String requestGroupHeaderName,
                                             final String requestIdHeaderName,
                                             final boolean alwaysWrapRequest) {
        this.includedUriPrefix = includedUriPrefix;
        this.ignoreUriPrefix = ignoreUriPrefix;
        this.nameUriPrefix = nameUriPrefix;
        this.uriToNamePattern = uriToNamePattern;
        this.namePatternMatchIndex = namePatternMatchIndex;
        this.requestGroupHeaderName = requestGroupHeaderName;
        this.requestIdHeaderName = requestIdHeaderName;
        this.alwaysWrapRequest = alwaysWrapRequest;
    }

    //除了指定的路径，默认提取Path前3段

    protected String getName(HttpServletRequest req) {
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


    protected String getGroup(HttpServletRequest req) {
        String group = req.getHeader(requestGroupHeaderName);
        return group == null ? "" : group;
    }

    @Override
    public boolean ignore(HttpServletRequest req) {
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
    public String makeExceptionResult(HttpServletRequest req, HttpStatus status, Throwable ex) {
        String error = ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
        String reqid = req.getHeader(requestIdHeaderName);
        if (reqid == null) {
            return RestUtils.createError(1, error);
        } else {
            return RestUtils.createError(1, error, reqid);
        }
    }
    @Override
    public boolean logDebugLevel(HttpStatus status, Throwable ex) {
        return RestExceptionHandler.logDebugLevel(status, ex);
    }

    @Override
    public boolean isAlwaysWrapRequest() {
        return alwaysWrapRequest;
    }

    @Override
    public HttpTraceInfo makeRootTraceInfo(HttpServletRequest req, RequestWrapper requestWrapper) {
        HttpTraceInfo info = new HttpTraceInfo();
        String traceId = makeSpanId();
        info.setTraceId(traceId);
        info.setSpanId(traceId);
        info.setSampled("1");
        info.setRequestName(getName(req));
        info.setRequestGroup(getGroup(req));
        return info;
    }

    @Override
    public boolean deferDetermineSampled(HttpServletRequest req, HttpServletResponse response,
                                         RequestWrapper requestWrapper, RespondWrapper respondWrapper,
                                         HttpTraceInfo traceInfo) {
        String detail = makeRequestDetail(req, response, requestWrapper, respondWrapper);
        traceInfo.getTags().put("request-detail", detail);
        return true;
    }

    @Override
    public HttpTraceInfo tracePrejudgement(HttpServletRequest req, RequestWrapper requestWrapper) {
        //优先按B3规范获取
        String traceId =  req.getHeader("X-B3-TraceId");
        String parentId = req.getHeader("X-B3-ParentSpanId");
        String spanId = req.getHeader("X-B3-SpanId");
        String sampled = req.getHeader("X-B3-Sampled");
        String flags = req.getHeader("X-B3-Flags");
        HttpTraceInfo info;
        //没有值，说明是root节点，自行生成
        if (traceId == null) {
            info = makeRootTraceInfo(req,requestWrapper);
        } else {
            info = new HttpTraceInfo();
            info.setTraceId(traceId);
            info.setParentSpanId(parentId);
            info.setSpanId(spanId);
            info.setSampled(sampled);
            info.setFlags(flags);
            info.setRequestName(getName(req));
            info.setRequestGroup(getGroup(req));
        }
        Map<String,String> tags = new HashMap<>();
        info.setTags(tags);
        return info;
    }

    protected String makeRequestDetail(HttpServletRequest request, HttpServletResponse response,
                                                   RequestWrapper requestWrapper, RespondWrapper respondWrapper) {
        final StringBuilder sb = new StringBuilder();
        RestUtils.fillRequestLogInfo(sb, request);
        sb.append("--- request body:\n");
        sb.append(requestWrapper.getBody());
        RestUtils.fillResponseLogInfo(sb, response);
        sb.append("\n--- response body:\n");
        sb.append(respondWrapper.getRespondBody());
        return sb.toString();
    }
    @Override
    public String makeSpanId() {
        return UUID.randomUUID().toString().replaceAll("-","").substring(0,16);
    }
}
