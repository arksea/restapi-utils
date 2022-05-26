package net.arksea.restapi.utils.influx.demo;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Create by xiaohaixing on 2020/6/5
 */
@Entity
@Table(name = "demo_entity")
public class DemoEntity extends IdEntity {
    private String name;
    private Timestamp createTime; //创建时间

    @Column(unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(nullable = false, columnDefinition = ("TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"))
    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }
}
