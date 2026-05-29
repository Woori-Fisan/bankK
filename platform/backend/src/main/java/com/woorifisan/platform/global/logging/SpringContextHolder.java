package com.woorifisan.platform.global.logging;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Logback Appender 등 Spring 컨텍스트 외부에서 Bean을 조회할 수 있도록
 * {@link ApplicationContext}를 정적으로 보관하는 홀더.
 *
 * <p>Spring이 초기화되기 전에 호출되면 {@code null}을 반환하므로,
 * 호출자는 반환값 null 여부를 반드시 확인해야 한다.</p>
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static volatile ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        SpringContextHolder.applicationContext = ctx;
    }

    /**
     * 지정한 타입의 Bean을 반환한다.
     * Spring 컨텍스트가 준비되지 않았거나 Bean이 존재하지 않으면 {@code null}을 반환한다.
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (applicationContext == null) return null;
        try {
            return applicationContext.getBean(beanClass);
        } catch (Exception e) {
            return null;
        }
    }
}
