package com.example.payment.audit;

import com.example.payment.config.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditRepository repo;

    /** Joins the caller's txn so the audit row commits atomically with the business change. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void record(String actorType, String actorId, String action, String aggregateType,
                       UUID aggregateId, UUID orderId, String reason) {
        AuditLog a = new AuditLog();
        a.setActorType(actorType); a.setActorId(actorId); a.setAction(action);
        a.setAggregateType(aggregateType); a.setAggregateId(aggregateId.toString());
        a.setOrderId(orderId); a.setReason(reason == null ? null : reason.substring(0, Math.min(500, reason.length())));
        a.setCorrelationId(MDC.get(CorrelationFilter.MDC_KEY));
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        if (ra instanceof ServletRequestAttributes sra) {
            HttpServletRequest r = sra.getRequest();
            String xff = r.getHeader("X-Forwarded-For");
            a.setSourceIp(xff != null ? xff.split(",")[0].trim() : r.getRemoteAddr());
        }
        repo.save(a);
    }
}