package net.arksea.restapi.utils.influx;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * Created by xiaohaixing on 2018/4/4.
 */
public class DefaultHttpRequestGroup implements IRequestGroup {
    @Override
    public String getName(ServletRequest request) {
        return ((HttpServletRequest) request).getRequestURI();
    }

    @Override
    public String getGroup(ServletRequest request) {
        return ((HttpServletRequest) request).getHeader("group");
    }

    @Override
    public boolean ignore(ServletRequest request) {
        return false;
    }

    @Override
    public String getRequestId(ServletRequest request) {
        return (String) request.getAttribute("restapi-requestid");
    }
}
