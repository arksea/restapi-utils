package net.arksea.restapi.utils.influx;

import javax.servlet.ServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 *
 * Created by xiaohaixing on 2018/4/4.
 */
public class DefaultRequestLogFilterBuilder {
    private final List<String> includedUriPrefix;//包含这些前缀的请求才做记录
    private final List<String> ignoreUriPrefix; //忽略这些前缀的请求
    private final List<String> nameUriPrefix;   //带这些前缀的URI直接作为name
    private Pattern uriToNamePattern;
    private int namePatternMatchIndex;
    private String requestGroupHeaderName;
    private String requestIdHeaderName;
    private IRequestLogger requestLogger;
    private Function<ServletRequest,Boolean> traceDeterminer;

    public DefaultRequestLogFilterBuilder() {
        includedUriPrefix = new LinkedList<>();
        ignoreUriPrefix = new LinkedList<>();
        nameUriPrefix = new LinkedList<>();
        //name提取Url前3段
        uriToNamePattern = Pattern.compile("(/(?:(?:[^/?]+)(?:/[^/?]+)?(?:/[^/?]+)?))");
        namePatternMatchIndex = 1;
        requestGroupHeaderName = "x-request-group";
        requestIdHeaderName="x-requestid";
    }

    public DefaultRequestLogFilterBuilder addIncludedUriPrefix(String uri) {
        this.includedUriPrefix.add(uri);
        return this;
    }

    public DefaultRequestLogFilterBuilder addIgnoreUriPrefix(String uri) {
        this.ignoreUriPrefix.add(uri);
        return this;
    }

    public DefaultRequestLogFilterBuilder addNameUriPrefix(String prefix) {
        this.nameUriPrefix.add(prefix);
        return this;
    }

    public DefaultRequestLogFilterBuilder setRequestLogger(IRequestLogger logger) {
        this.requestLogger = logger;
        return this;
    }

    public DefaultRequestLogFilterBuilder setUriToNamePattern(String pattern, int nameGroupIndex) {
        this.uriToNamePattern = Pattern.compile(pattern);
        this.namePatternMatchIndex = nameGroupIndex;
        return this;
    }

    public DefaultRequestLogFilterBuilder setRequestIdHeaderName(String name) {
        this.requestIdHeaderName = name;
        return this;
    }

    public DefaultRequestLogFilterBuilder setRequestGroupHeaderName(String name) {
        this.requestGroupHeaderName = name;
        return this;
    }

    public DefaultRequestLogFilterBuilder setTraceDeterminer(Function<ServletRequest,Boolean> function) {
        this.traceDeterminer = function;
        return this;
    }


    public RequestLogFilter build() {
        if (this.traceDeterminer == null) {
            this.traceDeterminer = servletRequest -> false;
        }
        IRequestLogFilterConfig config = new DefaultRequestLogFilterConfig(
                includedUriPrefix,ignoreUriPrefix,nameUriPrefix,
                uriToNamePattern,namePatternMatchIndex,
                requestGroupHeaderName, requestIdHeaderName,
                traceDeterminer);
        return new RequestLogFilter(config, this.requestLogger);
    }

}
