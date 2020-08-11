package net.arksea.restapi.utils.influx.demo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Create by xiaohaixing on 2020/8/11
 */
@Component
public class Demo {
    private static Logger logger = LogManager.getLogger(Demo.class);

    @Resource(name = "demoEntityDaoProxy")
    DemoEntityDao demoEntityDao;

    public void exec() throws Exception {
        logger.info("Demo.exec()");
        DemoEntity e = demoEntityDao.findOneByName("e1");
        Thread.sleep(100);
        if (e != null) {
            demoEntityDao.delete(e);
        }
        Thread.sleep(100);
        DemoEntity e1 = new DemoEntity();
        e1.setName("e1");
        demoEntityDao.save(e1);
    }
}
