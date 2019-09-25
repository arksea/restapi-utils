package net.arksea.restapi.utils.influx;

import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * Created by xiaohaixing on 2018/4/4.
 */
public class RequestLogFilterBuilder {
    private final List<String> ignoreUris;
    private final List<String> ignoreUriPrefix;
    private final List<String> nameUriPrefix;
    private Pattern uriToNamePattern;
    private int namePatternMatchIndex;
    private String requestGroupHeaderName;
    private IRequestLogger requestLogger;

    public RequestLogFilterBuilder() {
        ignoreUris = new LinkedList<>();
        ignoreUriPrefix = new LinkedList<>();
        nameUriPrefix = new LinkedList<>();
        //name提取Url前3段
        uriToNamePattern = Pattern.compile("(/(?:(?:[^/?]+)(?:/[^/?]+)?(?:/[^/?]+)?))");
        namePatternMatchIndex = 1;
        requestGroupHeaderName = "requestGroup";
    }

    public RequestLogFilterBuilder clearIgnoreUri(String uri) {
        this.ignoreUris.clear();
        return this;
    }

    public RequestLogFilterBuilder addIgnoreUri(String uri) {
        this.ignoreUris.add(uri);
        return this;
    }

    public RequestLogFilterBuilder addIgnoreUriPrefix(String uri) {
        this.ignoreUriPrefix.add(uri);
        return this;
    }

    public RequestLogFilterBuilder addNameUriPrefix(String prefix) {
        this.nameUriPrefix.add(prefix);
        return this;
    }

    public RequestLogFilterBuilder setRequestLogger(IRequestLogger logger) {
        this.requestLogger = logger;
        return this;
    }

    public RequestLogFilterBuilder setUriToNamePattern(String pattern, int nameGroupIndex) {
        this.uriToNamePattern = Pattern.compile(pattern);
        this.namePatternMatchIndex = nameGroupIndex;
        return this;
    }

    public RequestLogFilterBuilder setRequestGroupHeaderName(String name) {
        this.requestGroupHeaderName = name;
        return this;
    }

    public RequestLogFilter build() {
        IRequestGroup requestGroup = new IRequestGroup() {
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
                return group == null ? "null" : group;
            }

            @Override
            public boolean ignore(ServletRequest request) {
                HttpServletRequest req = (HttpServletRequest) request;
                if (req.getMethod().equals(RequestMethod.OPTIONS.name())) { //OPTION请求不做统计
                    return true;
                }
                String uri = req.getRequestURI();
                for (String ignore: ignoreUris) {
                    if (ignore.equals(uri)) {
                        return true;
                    }
                }
                for (String pre: ignoreUriPrefix) {
                    if (uri.startsWith(pre)) {
                        return true;
                    }
                }
                return false;
            }

            /**
             * 设置Attribute:请求ID
             * @param request
             * @return
             */
            @Override
            public String getRequestId(ServletRequest request) {
                HttpServletRequest req = (HttpServletRequest) request;
                String reqid = (String)req.getAttribute("restapi-requestid");
                if (reqid == null) {
                    reqid = req.getHeader("x-restapi-requestid");
                    if (reqid == null) {
                        reqid = UUID.randomUUID().toString();
                    }
                    req.setAttribute("restapi-requestid", reqid);
                }
                return reqid;
            }
        };
        return new RequestLogFilter(requestGroup, this.requestLogger);
    }

}
