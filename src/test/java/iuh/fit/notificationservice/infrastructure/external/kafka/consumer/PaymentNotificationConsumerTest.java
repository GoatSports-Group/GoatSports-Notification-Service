package iuh.fit.notificationservice.infrastructure.external.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationConsumerTest {
    @Mock NotificationService notificationService;
    @Mock NotificationRepository notificationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentNotificationConsumer consumer() {
        return new PaymentNotificationConsumer(objectMapper, notificationService, notificationRepository);
    }

    private String envelope(String eventType, UUID eventId, UUID payerId, UUID referenceId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "producer": "payment-service",
                  "payload": { "payerId": "%s", "referenceId": "%s", "referenceType": "BOOKING" }
                }
                """.formatted(eventId, eventType, payerId, referenceId);
    }

    @Test
    void paymentSucceededCreatesPaymentNotificationCarryingEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume(envelope("payment.succeeded.v1", eventId, payerId, referenceId));

        ArgumentCaptor<CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());

        CreateNotificationCommand command = captor.getValue();
        assertThat(command.eventId()).isEqualTo(eventId);
        assertThat(command.receiverId()).isEqualTo(payerId);
        assertThat(command.type()).isEqualTo("PAYMENT");
        assertThat(command.sourceService()).isEqualTo("PAYMENT");
        assertThat(command.referenceId()).isEqualTo(referenceId);
    }

    @Test
    void refundSucceededIsMappedToRefundNotification() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume(envelope("refund.succeeded.v1", eventId, UUID.randomUUID(), UUID.randomUUID()));

        ArgumentCaptor<CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("REFUND");
        assertThat(captor.getValue().referenceType()).isEqualTo("REFUND");
    }

    @Test
    void refundReminderTellsThePlayerTheAmountAndOpensTheRefund() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume(reminder(eventId, payerId, refundId, "AWAITING_BANK_ACCOUNT"));

        ArgumentCaptor<CreateNotificationCommand> captor =
                ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().receiverId()).isEqualTo(payerId);
        assertThat(captor.getValue().title()).isEqualTo("Liên kết ngân hàng để nhận 60.000 đ");
        assertThat(captor.getValue().referenceId()).isEqualTo(refundId);
    }

    @Test
    void cashRefundReminderIsLeftToVenueServiceToTellTheOwner() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume(reminder(eventId, UUID.randomUUID(), UUID.randomUUID(), "MANUAL_REVIEW"));

        verify(notificationService, never()).createNotification(any());
    }

    private String reminder(UUID eventId, UUID payerId, UUID refundId, String status) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "reminder.refund-pending.v1",
                  "payload": { "payerId": "%s", "refundId": "%s", "amount": 60000.0, "status": "%s" }
                }
                """.formatted(eventId, payerId, refundId, status);
    }

    @Test
    void duplicateEventIdIsSkipped() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(true);

        consumer().consume(envelope("payment.succeeded.v1", eventId, UUID.randomUUID(), UUID.randomUUID()));

        verify(notificationService, never()).createNotification(any());
    }

    @Test
    void unmappedEventTypeCreatesNoNotification() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume(envelope("payment.reconciled.v1", eventId, UUID.randomUUID(), UUID.randomUUID()));

        verify(notificationService, never()).createNotification(any());
    }

    @Test
    void eventWithoutEventIdIsIgnoredInsteadOfCreatingUndedupableNotification() throws Exception {
        String message = """
                { "eventType": "payment.succeeded.v1", "payload": { "payerId": "%s" } }
                """.formatted(UUID.randomUUID());

        consumer().consume(message);

        verify(notificationService, never()).createNotification(any());
    }

    /** payOS từ chối tài khoản nhận: người chơi phải được báo là chuyển tiền LỖI và vì sao, không phải lời nhắc chung chung. */
    @Test
    void failedPayoutTellsThePlayerWhatWentWrong() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID refundId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume("""
                {
                  "eventId": "%s",
                  "eventType": "refund.awaiting-bank-account.v1",
                  "payload": {
                    "refundId": "%s", "payerId": "%s", "status": "AWAITING_BANK_ACCOUNT",
                    "failureReason": "Số tài khoản ngân hàng không tồn tại hoặc không nhận được tiền (tài khoản •••• 6077). Kiểm tra lại số tài khoản và ngân hàng, hoặc chọn tài khoản khác."
                  }
                }
                """.formatted(eventId, refundId, payerId));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        CreateNotificationCommand command = captor.getValue();
        assertThat(command.title()).isEqualTo("Chuyển tiền hoàn chưa thành công");
        assertThat(command.content()).contains("•••• 6077").contains("không tồn tại").contains("Ngân hàng & hoàn tiền");
        assertThat(command.receiverId()).isEqualTo(payerId);
        assertThat(command.referenceId()).isEqualTo(refundId);
    }
}
