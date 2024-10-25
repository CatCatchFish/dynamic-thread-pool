package cn.cat.middleware.dynamic.thread.pool.sdk.trigger.job;

import cn.cat.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import cn.cat.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import cn.cat.middleware.dynamic.thread.pool.sdk.registry.IRegistry;
import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class ThreadPoolDataReportJob {
    private final IDynamicThreadPoolService dynamicThreadPoolService;
    private final IRegistry registry;

    @Scheduled(cron = "0/20 * * * * ?")
    public void execReportThreadPoolList() {
        List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.queryThreadPoolList();
        registry.reportThreadPool(threadPoolConfigEntities);
        log.info("动态线程池，上报线程池信息：{}", JSON.toJSONString(threadPoolConfigEntities));

        for (ThreadPoolConfigEntity threadPoolConfigEntity : threadPoolConfigEntities) {
            registry.reportThreadPoolConfigParameter(threadPoolConfigEntity);
            log.info("动态线程池，上报线程池参数：{}", JSON.toJSONString(threadPoolConfigEntity));
        }
    }
}
