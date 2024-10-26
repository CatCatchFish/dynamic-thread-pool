package cn.cat.middleware.dynamic.thread.pool.sdk.trigger.listener;

import cn.cat.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import cn.cat.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import cn.cat.middleware.dynamic.thread.pool.sdk.registry.IRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.listener.MessageListener;

import java.util.List;

@Slf4j
@AllArgsConstructor
public class ThreadPoolConfigAdjustListener implements MessageListener<ThreadPoolConfigEntity> {
    private final IDynamicThreadPoolService dynamicThreadPoolService;
    private final IRegistry registry;

    @Override
    public void onMessage(CharSequence charSequence, ThreadPoolConfigEntity threadPoolConfigEntity) {
        dynamicThreadPoolService.updateThreadPoolConfig(threadPoolConfigEntity);
        // 更新完即要上报
        List<ThreadPoolConfigEntity> threadPoolConfigEntities = dynamicThreadPoolService.queryThreadPoolList();
        // 再上报应用中所有线程池
        registry.reportThreadPool(threadPoolConfigEntities);
        // 再上报线程池配置参数
        ThreadPoolConfigEntity threadPoolConfig =
                dynamicThreadPoolService.queryThreadPoolConfigByName(threadPoolConfigEntity.getThreadPoolName());
        registry.reportThreadPoolConfigParameter(threadPoolConfig);
        log.info("线程池配置更新成功，线程池名称：{}，线程池配置：{}", threadPoolConfig.getThreadPoolName(), threadPoolConfig);
    }
}
