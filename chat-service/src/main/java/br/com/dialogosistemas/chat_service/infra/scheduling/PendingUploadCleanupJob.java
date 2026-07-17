package br.com.dialogosistemas.chat_service.infra.scheduling;

import br.com.dialogosistemas.chat_service.domain.gateway.PendingUploadGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PendingUploadCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PendingUploadCleanupJob.class);
    private final PendingUploadGateway pendingUploadGateway;

    public PendingUploadCleanupJob(PendingUploadGateway pendingUploadGateway) {
        this.pendingUploadGateway = pendingUploadGateway;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredUploads() {
        log.info("Iniciando limpeza de pending uploads expirados");
        pendingUploadGateway.deleteExpired();
        log.info("Limpeza de pending uploads concluida");
    }
}
