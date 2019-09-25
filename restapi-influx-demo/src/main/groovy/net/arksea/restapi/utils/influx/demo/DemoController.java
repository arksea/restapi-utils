package net.arksea.restapi.utils.influx.demo;

import akka.actor.ActorSystem;
import akka.dispatch.Futures;
import net.arksea.restapi.RestException;
import net.arksea.restapi.RestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletResponse;

/**
 *
 * Created by xiaohaixing on 2017/7/25.
 */
@RestController
@RequestMapping(value = "/api/demo")
public class DemoController {
    private static final String MEDIA_TYPE = "application/json; charset=UTF-8";
    private Logger logger = LogManager.getLogger(DemoController.class);

    @Autowired
    ActorSystem system;

    /**
     * 同步正常返回
     * @param httpResonpse
     * @return
     */
    @RequestMapping(value = "sync-ok", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public String getSyncOk(final HttpServletResponse httpResonpse) {
        return RestUtils.createResult(0,"hello world", "1234");
    }

    /**
     * 同步返回错误
     * @param httpResonpse
     * @return
     */
    @RequestMapping(value = "sync-err", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public String getSyncErr(final HttpServletResponse httpResonpse) {
        httpResonpse.setStatus(507);
        return RestUtils.createResult(1,"hello world", "1234");
    }

    /**
     * 同步抛异常
     * @param httpResonpse
     * @return
     */
    @RequestMapping(value = "sync-throw", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public String getSyncThrow(final HttpServletResponse httpResonpse) {
        throw new RestException(HttpStatus.INTERNAL_SERVER_ERROR,"test error");
    }

    /**
     * 异步正常返回
     * @param httpResonpse
     * @return
     */
    @RequestMapping(value = "async-ok", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public DeferredResult<String> getAsyncOk(final HttpServletResponse httpResonpse) {
        DeferredResult<String> result = new DeferredResult<String>();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String str = RestUtils.createResult(1,"hello world", "1234");
        result.setResult(str);
        return result;
    }

    /**
     * 异步返回错误
     * @param httpResonpse
     * @return
     */
    @RequestMapping(value = "async-err", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public DeferredResult<String> getAsyncErr(final HttpServletResponse httpResonpse) {
        DeferredResult<String> result = new DeferredResult<String>();
        httpResonpse.setStatus(508);
        result.setResult(RestUtils.createMsgResult(1,new RestException(HttpStatus.BAD_REQUEST,"async-error"),"1234353"));
        return result;
    }

    /**
     * 异步返回错误
     * @param httpResonpse
     * @return
     */
    @RequestMapping(value = "async-setex", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public DeferredResult<String> getAsyncEx(final HttpServletResponse httpResonpse) {
        DeferredResult<String> result = new DeferredResult<String>();
        result.setErrorResult(new RestException(HttpStatus.valueOf(501),"async-setex"));
        return result;
    }

    /**
     * 异步Controller抛异常
     * @param httpResonpse
     * @return
     */
    @RequestMapping(value = "async-throw", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public DeferredResult<String> getAsyncThrow(final HttpServletResponse httpResonpse) {
        DeferredResult<String> result = new DeferredResult<String>();
        throw new RestException(HttpStatus.BAD_GATEWAY,"async-throw");
    }

    /**
     * 异步正常返回
     * @param httpResonpse
     * @return
     */
    @RequestMapping(value = "async-f-ok", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public DeferredResult<String> getAsyncFutureOk(final HttpServletResponse httpResonpse) {
        DeferredResult<String> result = new DeferredResult<String>();
        Futures.future(() -> {
            Thread.sleep(100);
            result.setResult(RestUtils.createMsgResult(0,"hello world","1234354"));
            return 1;
        },system.dispatcher());
        return result;
    }

    /**
     * 异步Future返回错误
     * @param httpResonpse
     * @return
     */
    @RequestMapping(value = "async-f-err", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public DeferredResult<String> getAsyncFutureErr(final HttpServletResponse httpResonpse) {
        DeferredResult<String> result = new DeferredResult<String>();
        Futures.future(() -> {
            Thread.sleep(100);
            httpResonpse.setStatus(509);
            result.setResult(RestUtils.createMsgResult(1,"async-f-err","1234353"));
            return 1;
        },system.dispatcher());
        return result;
    }

    /**
     * 异步Future中throw
     * @param httpResonpse
     * @return
     */
    @RequestMapping(value = "async-f-throw", method = RequestMethod.GET, produces = MEDIA_TYPE)
    public DeferredResult<String> getAsyncFutureThrow(final HttpServletResponse httpResonpse) {
        DeferredResult<String> result = new DeferredResult<String>();
        Futures.future(() -> {
            throw new RestException(HttpStatus.INTERNAL_SERVER_ERROR,"async-f-throw");
        },system.dispatcher());
        return result;
    }
}
