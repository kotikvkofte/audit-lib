package org.ex9.auditlib.layout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.ex9.auditlib.dto.LogDto;

import java.nio.charset.StandardCharsets;

/**
 * Кастомный layout для Console и File appender.
 *
 * @author Краковцев Артём
 */
@Plugin(name = "AuditJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class AuditJsonLayout extends AbstractStringLayout {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    protected AuditJsonLayout() {
        super(StandardCharsets.UTF_8);
    }

    @PluginFactory
    public static AuditJsonLayout createLayout() {
        return new AuditJsonLayout();
    }

    @Override
    public String toSerializable(LogEvent event) {
        Object[] params = event.getMessage().getParameters();
        if (params != null && params.length > 0) {
            Object obj = params[0];
            if (obj instanceof LogDto) {
                try {
                    return objectMapper.writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                    return "{\"error\":\"serialization failed\"}";
                }
            }
        }

        return event.getMessage().getFormattedMessage();
    }

}
