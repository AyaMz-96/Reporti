package com.example.Reporti.controller;
import com.example.Reporti.dto.ReportResponse;
import com.example.Reporti.service.AgentNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(assignableTypes = ReportController.class)
public class AssignmentNotificationAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(AssignmentNotificationAdvice.class);

    private final AgentNotificationService notificationService;

    public AssignmentNotificationAdvice(AgentNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getMethod() != null && "assign".equals(returnType.getMethod().getName());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ReportResponse report) {
            try   {
                     notificationService.notifyAssigned(report.id());
                   }   catch (RuntimeException e) {
                log.warn("Assignment notification failed for report {}: {}", report.id(), e.getMessage());
                    }
                    }
        return body;
    }
}
