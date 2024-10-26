package cn.cat.middleware.dynamic.thread.pool.sdk.config;

import cn.cat.middleware.dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import cn.cat.middleware.dynamic.thread.pool.sdk.domain.IDynamicThreadPoolService;
import cn.cat.middleware.dynamic.thread.pool.sdk.domain.model.entity.ThreadPoolConfigEntity;
import cn.cat.middleware.dynamic.thread.pool.sdk.domain.model.valobj.RegistryEnumVO;
import cn.cat.middleware.dynamic.thread.pool.sdk.registry.IRegistry;
import cn.cat.middleware.dynamic.thread.pool.sdk.registry.redis.RedisRegistry;
import cn.cat.middleware.dynamic.thread.pool.sdk.trigger.job.ThreadPoolDataReportJob;
import cn.cat.middleware.dynamic.thread.pool.sdk.trigger.listener.ThreadPoolConfigAdjustListener;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Cat
 * @description 动态线程池自动配置
 * @date 2024-10-25 14:37
 */
@Slf4j
@Configuration
@EnableScheduling
@EnableConfigurationProperties(DynamicThreadPoolAutoProperties.class)
public class DynamicThreadPoolAutoConfig {
    private String applicationName;

    @Bean("dynamicThreadRedissonClient")
    public RedissonClient redissonClient(DynamicThreadPoolAutoProperties properties) {
        Config config = new Config();
        // 根据需要可以设定编解码器；https://github.com/redisson/redisson/wiki/4.-%E6%95%B0%E6%8D%AE%E5%BA%8F%E5%88%97%E5%8C%96
        config.setCodec(JsonJacksonCodec.INSTANCE);

        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setPassword(properties.getPassword())
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive())
        ;

        RedissonClient redissonClient = Redisson.create(config);

        log.info("动态线程池，注册器（redis）链接初始化完成。{} {} {}", properties.getHost(), properties.getPoolSize(), !redissonClient.isShutdown());

        return redissonClient;
    }

    @Bean
    public IRegistry redisRegistry(RedissonClient dynamicThreadRedissonClient) {
        return new RedisRegistry(dynamicThreadRedissonClient);
    }

    @Bean(name = "dynamicThreadPoolService")
    public DynamicThreadPoolService dynamicThreadPoolService(ApplicationContext applicationContext, Map<String, ThreadPoolExecutor> threadPoolExecutors, RedissonClient redissonClient) {
        applicationName = applicationContext.getEnvironment().getProperty("spring.application.name");

        if (StringUtils.isBlank(applicationName)) {
            applicationName = "unknown";
            log.warn("动态线程池启动提示 ==> SpringBoot 应用未配置 spring.application.name，使用默认值 'unknown'");
        }

        // 调整为缓存中的线程池配置
        for (String threadPoolName : threadPoolExecutors.keySet()) {
            String threadPoolKey = RegistryEnumVO.THREAD_POOL_CONFIG_LIST_KEY.getKey() + "_" + applicationName + "_" + threadPoolName;
            ThreadPoolConfigEntity threadPoolConfig = redissonClient.<ThreadPoolConfigEntity>getBucket(threadPoolKey).get();
            if (threadPoolConfig == null) continue;
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutors.get(threadPoolName);
            threadPoolExecutor.setCorePoolSize(threadPoolConfig.getCorePoolSize());
            threadPoolExecutor.setMaximumPoolSize(threadPoolConfig.getMaximumPoolSize());
        }

        log.info("线程池信息：{}", JSON.toJSONString(threadPoolExecutors.keySet()));
        return new DynamicThreadPoolService(applicationName, threadPoolExecutors);
    }

    @Bean
    public ThreadPoolDataReportJob threadPoolDataReportJob(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        return new ThreadPoolDataReportJob(dynamicThreadPoolService, registry);
    }

    @Bean
    public ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener(IDynamicThreadPoolService dynamicThreadPoolService, IRegistry registry) {
        return new ThreadPoolConfigAdjustListener(dynamicThreadPoolService, registry);
    }

    @Bean(name = "dynamicThreadPoolRedisTopic")
    public RTopic dynamicThreadPoolRedisTopic(RedissonClient redissonClient, ThreadPoolConfigAdjustListener threadPoolConfigAdjustListener) {
        RTopic rTopic = redissonClient.getTopic(RegistryEnumVO.DYNAMIC_THREAD_POOL_REDIS_TOPIC.getKey() + "_" + applicationName);
        rTopic.addListener(ThreadPoolConfigEntity.class, threadPoolConfigAdjustListener);
        return rTopic;
    }
}
