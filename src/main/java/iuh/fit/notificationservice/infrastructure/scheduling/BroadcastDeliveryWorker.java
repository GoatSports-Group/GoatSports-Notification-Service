package iuh.fit.notificationservice.infrastructure.scheduling;

import iuh.fit.notificationservice.application.port.scheduling.BroadcastDeliveryScheduler;
import iuh.fit.notificationservice.application.service.BroadcastService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Gửi thông báo hệ thống ở nền: ngay sau khi admin bấm gửi (sau commit), và quét định kỳ để gửi tiếp các
 * thông báo dở dang (service khởi động lại giữa chừng, auth-service tạm lỗi). BroadcastService.claim đảm bảo
 * mỗi thông báo chỉ một tiến trình gửi tại một thời điểm, kể cả khi chạy nhiều instance.
 */
@Slf4j
@Component
public class BroadcastDeliveryWorker implements BroadcastDeliveryScheduler {
    // ObjectProvider: BroadcastService cần chính scheduler này, tiêm trực tiếp sẽ thành vòng phụ thuộc.
    private final ObjectProvider<BroadcastService> broadcastService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public BroadcastDeliveryWorker(ObjectProvider<BroadcastService> broadcastService) {
        this.broadcastService = broadcastService;
    }

    @Override
    public void schedule(UUID broadcastId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit(broadcastId);
                }
            });
        } else {
            submit(broadcastId);
        }
    }

    @Scheduled(fixedDelayString = "${notification.broadcast.resume-delay-ms:60000}", initialDelay = 30_000)
    public void resumeStalled() {
        broadcastService.getObject().findStalled().forEach(this::submit);
    }

    private void submit(UUID broadcastId) {
        executor.submit(() -> {
            try {
                broadcastService.getObject().deliver(broadcastId);
            } catch (RuntimeException exception) {
                log.warn("Gửi thông báo hệ thống {} lỗi, sẽ thử lại ở lần quét sau: {}", broadcastId, exception.getMessage());
            }
        });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
