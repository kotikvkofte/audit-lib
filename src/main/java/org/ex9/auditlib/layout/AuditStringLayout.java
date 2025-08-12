package org.ex9.auditlib.layout;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.ex9.auditlib.dto.LogDto;

import java.nio.charset.StandardCharsets;

/**
 * Кастомный layout для kafka appender.
 *
 * @author Краковцев Артём
 */
@Plugin(name = "AuditStringLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class AuditStringLayout extends AbstractStringLayout {

    protected AuditStringLayout() {
        super(StandardCharsets.UTF_8);
    }

    @PluginFactory
    public static AuditStringLayout createLayout() {
        return new AuditStringLayout();
    }

    @Override
    public String toSerializable(LogEvent event) {
        Object[] params = event.getMessage().getParameters();
        if (params != null && params.length > 0) {
            Object obj = params[0];

            if (obj instanceof LogDto logDto) {
                return logDto.getLog();
            }
        }

        return event.getMessage().getFormattedMessage();
    }

}
