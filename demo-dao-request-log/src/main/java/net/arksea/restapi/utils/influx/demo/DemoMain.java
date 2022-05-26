package net.arksea.restapi.utils.influx.demo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * Created by xiaohaixing on 2020/5/11.
 */
public class DemoMain {
    private static final Logger logger = LogManager.getLogger(DemoMain.class);
    private DemoMain() {};

    /**
     * @param args command line args
     */
    public static void main(final String[] args) {
        try {
            final AbstractApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"application-context.xml" });
            logger.info("启动Demo",context.getApplicationName());
            Demo demo = context.getBean(Demo.class);
            demo.exec();
            Thread.sleep(120_000);
            context.close();
            logger.info("Demo已停止");
        } catch (Exception ex) {
            logger.error("启动Demo失败", ex);
        }
    }
}
