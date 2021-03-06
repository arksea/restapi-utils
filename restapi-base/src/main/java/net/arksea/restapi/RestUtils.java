package net.arksea.restapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Iterator;


/**
 *
 * Created by xiaohaixing_dian91 on 2016/11/25.
 */
public final class RestUtils {
    public static ObjectMapper objectMapper = new ObjectMapper();

    private RestUtils() {
    }

    public static String createResult(int code, String reqid) {
        return "{\"code\":"+code+",\"reqid\":\""+reqid+"\"}";
    }

    public static <T> String createResult(int code, T value, String reqid) {
        try {
            final RestResult<T> result = new RestResult<>(code, value, reqid);
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new RestException("Object result to json failed", ex);
        }
    }

    public static <T> String createJsonResult(int code, String json, String reqid) {
        return "{\"code\":"+code+",\"result\":"+json+",\"reqid\":\""+reqid+"\"}";
    }

    public static String createError(int code, String error, String reqid) {
        try {
            String errorJson = objectMapper.writeValueAsString(error);
            return "{\"code\":"+code+",\"error\":"+errorJson+",\"reqid\":\""+reqid+"\"}";
        } catch (JsonProcessingException ex) {
            throw new RestException("Object result to json failed", ex);
        }
    }

    @Deprecated
    public static String createMsgResult(int code, String msg, String reqid) {
        return "{\"code\":"+code+",\"msg\":\""+msg+"\",\"reqid\":\""+reqid+"\"}";
    }

    @Deprecated
    public static String createMsgResult(int errCode, Throwable ex, String reqid) {
        String msg = ex.getMessage();
        if (!StringUtils.isEmpty(msg)) {
            msg = msg.replace("\"", "'").replace("\n", "\\n");
        }
        return "{\"code\":"+errCode+", \"msg\":\""+msg+"\",\"reqid\":\""+reqid+"\"}";
    }
    //--------------------------------------------------------------------------
    public static String getRequestLogInfo(final WebRequest req) {
        final StringBuilder sb = new StringBuilder();
        fillRequestLogInfo(sb, req);
        return sb.toString();
    }
    public static String getRequestLogInfo(final HttpServletRequest req) {
        final StringBuilder sb = new StringBuilder();
        fillRequestLogInfo(sb,req);
        return sb.toString();
    }

    public static String getRequestLogInfo(final Throwable ex, final HttpStatus status, final HttpServletRequest request,
                                           final String extDetail) {
        final StringBuilder sb = new StringBuilder();
        if (StringUtils.isEmpty(ex.getMessage())) {
            sb.append(ex.getClass().getSimpleName());
        } else {
            sb.append(ex.getMessage());
        }
        sb.append("\nstatus: ").append(status.toString());
        if (!StringUtils.isEmpty(extDetail)) {
            sb.append("\n").append(extDetail);
        }
        sb.append("\n");
        RestUtils.fillRequestLogInfo(sb, request);
        return sb.toString();
    }

    public static String getRequestLogInfo(final Throwable ex, final HttpStatus status, final WebRequest request,
                                           final String extDetail) {
        final StringBuilder sb = new StringBuilder();
        if (StringUtils.isEmpty(ex.getMessage())) {
            sb.append(ex.getClass().getSimpleName());
        } else {
            sb.append(ex.getMessage());
        }
        sb.append("\nstatus: ").append(status.toString());
        if (!StringUtils.isEmpty(extDetail)) {
            sb.append("\n").append(extDetail);
        }
        sb.append("\n");
        RestUtils.fillRequestLogInfo(sb, request);
        return sb.toString();
    }

    public static void fillRequestLogInfo(final StringBuilder sb, final WebRequest req) {
        sb.append("request: ").append(req.getDescription(true));
        sb.append("\nparams: \n");
        Iterator<String> it = req.getParameterNames();
        while (it.hasNext()) {
            String name = it.next();
            String value = req.getParameter(name);
            sb.append("  ").append(name).append(": ").append(value).append("\n");
        }
        sb.append("headers: \n");
        it = req.getHeaderNames();
        while(it.hasNext()) {
            String name = it.next();
            String value = req.getHeader(name);
            sb.append("  ").append(name).append(": ").append(value).append("\n");
        }
    }
    public static void fillRequestLogInfo(final StringBuilder sb, final HttpServletRequest req) {
        sb.append("request: uri=").append(req.getRequestURI())
          .append(";client=").append(req.getRemoteAddr());
        sb.append("\nparams: \n");
        Enumeration<String> it = req.getParameterNames();
        while (it.hasMoreElements()) {
            String name = it.nextElement();
            String value = req.getParameter(name);
            sb.append("  ").append(name).append(": ").append(value).append("\n");
        }
        sb.append("headers: \n");
        it = req.getHeaderNames();
        while(it.hasMoreElements()) {
            String name = it.nextElement();
            String value = req.getHeader(name);
            sb.append("  ").append(name).append(": ").append(value).append("\n");
        }
    }
}
