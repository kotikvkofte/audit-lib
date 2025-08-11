package org.ex9.auditlib.config;

import org.ex9.auditlib.interceptor.OutgoingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация RestTemplate для логирования исходящих HTTP-запросов.
 * <p>
 * Добавляет {@link OutgoingInterceptor} для перехвата и логирования исходящих запросов.
 * </p>
 * @author Краковцев Артём
 */
@Configuration
public class RestClientConfig {

    /**
     * Создаёт RestTemplate с перехватчиком для логирования исходящих запросов.
     *
     * @param outgoingInterceptor перехватчик для логирования
     * @return экземпляр {@link RestTemplate}
     */
    @Bean
    public RestTemplate restTemplate(OutgoingInterceptor outgoingInterceptor) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(outgoingInterceptor);
        return restTemplate;
    }

}
