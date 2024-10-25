package cn.cat.middleware.dynamic.thread.pool.sdk.config;

import cn.cat.middleware.dynamic.thread.pool.sdk.domain.DynamicThreadPoolService;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Cat
 * @description 动态线程池自动配置
 * @date 2024-10-25 14:37
 */
@Configuration
public class DynamicThreadPoolAutoConfig {
    private static final Logger logger = LoggerFactory.getLogger(DynamicThreadPoolAutoConfig.class);

    @Bean(name = "dynamicThreadPoolService")
    public DynamicThreadPoolService dynamicThreadPoolService(ApplicationContext applicationContext, Map<String, ThreadPoolExecutor> threadPoolExecutors) {
        String applicationName = applicationContext.getEnvironment().getProperty("spring.application.name");

        if (StringUtils.isBlank(applicationName)) {
            applicationName = "unknown";
            logger.warn("动态线程池启动提示 ==> SpringBoot 应用未配置 spring.application.name，使用默认值 'unknown'");
        }

        logger.info("线程池信息：{}", JSON.toJSONString(threadPoolExecutors.keySet()));
        return new DynamicThreadPoolService(applicationName, threadPoolExecutors);
    }
}
