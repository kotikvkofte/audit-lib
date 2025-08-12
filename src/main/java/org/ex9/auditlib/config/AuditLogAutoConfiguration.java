package org.ex9.auditlib.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.ex9.auditlib.annotation.AuditLog;
import org.ex9.auditlib.aspect.AuditLogAspect;
import org.ex9.auditlib.filter.HttpLoggingFilter;
import org.ex9.auditlib.property.AuditLogProperties;
import org.ex9.auditlib.property.AuditKafkaProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Автоконфигурация стартера.
 * <p>
 * Регистрирует компоненты для логирования методов (через {@link AuditLogAspect}) и HTTP-запросов
 * (через {@link HttpLoggingFilter}). Активируется, если свойство <code>audit.logging.enabled=true</code>
 * указано в конфигурации приложения. Поддерживает настройку логирования через {@link AuditLogProperties}
 * и {@link AuditKafkaProperties}.
 * </p>
 * @author Крковцев Артём
 */
@Log4j2
@Configuration
@EnableConfigurationProperties({AuditLogProperties.class, AuditKafkaProperties.class})
@ConditionalOnClass({AuditLogProperties.class, AuditKafkaProperties.class})
@ConditionalOnProperty(prefix = "audit.logging", name = "enabled", havingValue = "true")
public class AuditLogAutoConfiguration {

    /**
     * Создаёт бин аспекта для обработки методов с аннотацией {@link AuditLog}.
     *
     * @return экземпляр {@link AuditLogAspect}
     */
    @Bean
    public AuditLogAspect auditLogAspect() {
        return new AuditLogAspect();
    }

    /**
     * Создаёт бин фильтра для логирования HTTP-запросов.
     *
     * @return экземпляр {@link HttpLoggingFilter}
     */
    @Bean
    public HttpLoggingFilter httpLoggingFilter() {
        return new HttpLoggingFilter();
    }

    /**
     * Регистрирует фильтр {@link HttpLoggingFilter} для обработки всех URL-шаблонов.
     *
     * @param filter фильтр для логирования HTTP-запросов
     * @return бин регистрации фильтра
     */
    @Bean
    public FilterRegistrationBean<HttpLoggingFilter> httpLoggingFilterRegistration(HttpLoggingFilter filter) {
        FilterRegistrationBean<HttpLoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(2);
        return registration;
    }

    /**
     * Конфигурацию и логирует её запуск конфигурации.
     */
    @PostConstruct
    public void init() {
        log.info("AuditLogAutoConfiguration init");
    }

}
