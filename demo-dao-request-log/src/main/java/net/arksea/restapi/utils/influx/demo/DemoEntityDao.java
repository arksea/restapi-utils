package net.arksea.restapi.utils.influx.demo;

import org.springframework.data.repository.CrudRepository;

/**
 *
 * Created by xiaohaixing on 2019/4/15.
 */
public interface DemoEntityDao extends CrudRepository<DemoEntity, Long> {
    DemoEntity findOneByName(String name);
}
