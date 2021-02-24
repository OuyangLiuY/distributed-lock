package com.only.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode()
@Accessors(chain = true)
@TableName("method_lock")
public class MethodLock {
    private static final long serialVersionUID = 1L;
    @TableField(value = "id")
    @TableId(type = IdType.AUTO)
    private Integer id;
    @TableField(value = "method_name")
    private String methodName;
    @TableField(value = "state")
    private Boolean state;
    @TableField(value = "update_time")
    private Date update_time;
    @TableField(value = "version")
    private Integer version;
    @TableField(value = "ip")
    private String ip;
    @TableField(value = "thread_id")
    private String threadId;
}
