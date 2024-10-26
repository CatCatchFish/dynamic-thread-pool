package cn.cat.middleware.dynamic.thread.pool.sdk.domain;

import cn.cat.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Cat
 * @description 动态线程池服务
 */
@Slf4j
@Service
public class DynamicThreadPoolService implements IDynamicThreadPoolService {
    private final String applicationName;
    private final Map<String, ThreadPoolExecutor> threadPoolExecutorMap;

    public DynamicThreadPoolService(String applicationName, Map<String, ThreadPoolExecutor> threadPoolExecutorMap) {
        this.applicationName = applicationName;
        this.threadPoolExecutorMap = threadPoolExecutorMap;
    }

    @Override
    public List<ThreadPoolConfigEntity> queryThreadPoolList() {
        Set<String> threadPoolBeanNames = threadPoolExecutorMap.keySet();
        List<ThreadPoolConfigEntity> threadPoolVOS = new ArrayList<>(threadPoolBeanNames.size());
        for (String threadPoolBeanName : threadPoolBeanNames) {
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolBeanName);
            ThreadPoolConfigEntity threadPoolConfigVO = convertToVO(threadPoolBeanName, threadPoolExecutor);
            threadPoolVOS.add(threadPoolConfigVO);
        }
        return threadPoolVOS;
    }

    @Override
    public ThreadPoolConfigEntity queryThreadPoolConfigByName(String threadPoolName) {
        ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolName);
        if (threadPoolExecutor == null) {
            return new ThreadPoolConfigEntity(applicationName, threadPoolName);
        }
        return convertToVO(threadPoolName, threadPoolExecutor);
    }

    @Override
    public void updateThreadPoolConfig(ThreadPoolConfigEntity threadPoolConfigEntity) {
        if (threadPoolConfigEntity == null || !applicationName.equals(threadPoolConfigEntity.getAppName())) return;
        ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolConfigEntity.getThreadPoolName());
        if (threadPoolExecutor == null) return;
        // 更新核心线程数和最大线程数
        threadPoolExecutor.setCorePoolSize(threadPoolConfigEntity.getCorePoolSize());
        threadPoolExecutor.setMaximumPoolSize(threadPoolConfigEntity.getMaximumPoolSize());
    }

    private ThreadPoolConfigEntity convertToVO(String threadPoolBeanName, ThreadPoolExecutor threadPoolExecutor) {
        ThreadPoolConfigEntity threadPoolConfigVO = new ThreadPoolConfigEntity(applicationName, threadPoolBeanName);
        threadPoolConfigVO.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
        threadPoolConfigVO.setMaximumPoolSize(threadPoolExecutor.getMaximumPoolSize());
        threadPoolConfigVO.setPoolSize(threadPoolExecutor.getPoolSize());
        threadPoolConfigVO.setActiveCount(threadPoolExecutor.getActiveCount());
        threadPoolConfigVO.setQueueSize(threadPoolExecutor.getQueue().size());
        threadPoolConfigVO.setQueueType(threadPoolExecutor.getQueue().getClass().getName());
        threadPoolConfigVO.setRemainingCapacity(threadPoolExecutor.getQueue().remainingCapacity());
        return threadPoolConfigVO;
    }
}
