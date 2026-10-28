package com.rounak.productsecureapi.aspect;

import com.rounak.productsecureapi.annotation.Auditable;
import com.rounak.productsecureapi.serviceImpl.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log =
            LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogService auditLogService;

    public AuditAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(com.rounak.productsecureapi.annotation.Auditable)")
    public Object auditOperation(ProceedingJoinPoint joinPoint)
            throws Throwable {

        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();

        Method method = signature.getMethod();

        Auditable auditable = method.getAnnotation(Auditable.class);

        String username = getCurrentUsername();
        String entityId = null;

        try {
            Object result = joinPoint.proceed();

            entityId = extractEntityId(result, joinPoint.getArgs());

            saveAuditSafely(
                    username,
                    auditable.action(),
                    auditable.entityType(),
                    entityId,
                    "SUCCESS",
                    "Method " + method.getName() + " completed successfully"
            );

            return result;

        } catch (Throwable exception) {

            saveAuditSafely(
                    username,
                    auditable.action(),
                    auditable.entityType(),
                    entityId,
                    "FAILURE",
                    "Method " + method.getName()
                            + " failed: "
                            + exception.getClass().getSimpleName()
            );

            throw exception;
        }
    }

    private String getCurrentUsername() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "ANONYMOUS";
        }

        return authentication.getName();
    }


    private String extractEntityId(Object result, Object[] args) {

        // Handle ProductResponse and other Java records.
        if (result instanceof com.rounak.productsecureapi.dto.product.ProductResponse response
                && response.id() != null) {
            return response.id().toString();
        }

        // Fallback for regular objects exposing getId().
        if (result != null) {
            try {
                Method getId = result.getClass().getMethod("getId");
                Object id = getId.invoke(result);

                if (id != null) {
                    return id.toString();
                }
            } catch (ReflectiveOperationException ignored) {
                // No public getId() method.
            }
        }

        // Fallback for methods receiving an ID directly.
        if (args != null && args.length > 0) {
            Object firstArg = args[0];

            if (firstArg instanceof Number || firstArg instanceof String) {
                return firstArg.toString();
            }
        }

        return null;
    }

    private void saveAuditSafely(
            String username,
            String action,
            String entityType,
            String entityId,
            String status,
            String details) {

        try {
            auditLogService.saveAuditLog(
                    username,
                    action,
                    entityType,
                    entityId,
                    status,
                    details
            );
        } catch (Exception auditException) {
            // Do not replace the business result or original exception
            // with an audit persistence exception.
            log.error(
                    "Failed to persist audit record for action {}",
                    action,
                    auditException
            );
        }
    }
}