package net.arksea.restapi.utils.influx.demo;

import net.arksea.restapi.utils.influx.IRequestLogProxyCreator;
import net.arksea.restapi.utils.influx.IRequestLogger;
import net.arksea.restapi.utils.influx.RequestLogProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Create by xiaohaixing on 2020/8/11
 */
@Component
public class DaoRequestLogProxyFactory {

    IRequestLogProxyCreator requestLogProxyCreator;

    @Autowired
    IRequestLogger requestLogger;

    @Autowired
    DemoEntityDao demoEntityDao;

    @PostConstruct
    void init() {
        requestLogProxyCreator = new RequestLogProxyCreator(requestLogger);
    }

    @Bean("demoEntityDaoProxy")
    DemoEntityDao creatDemoEntityDaoProxy() {
        return requestLogProxyCreator.newInstance(demoEntityDao, DemoEntityDao.class);
    }
}
