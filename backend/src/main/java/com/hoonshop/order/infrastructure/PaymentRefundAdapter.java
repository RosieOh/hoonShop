package com.hoonshop.order.infrastructure;

import com.hoonshop.common.domain.DomainException;
import com.hoonshop.order.application.port.RefundPort;
import com.hoonshop.payment.application.CancelPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PaymentRefundAdapter implements RefundPort {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundAdapter.class);

    private final CancelPaymentService cancelPaymentService;

    public PaymentRefundAdapter(CancelPaymentService cancelPaymentService) {
        this.cancelPaymentService = cancelPaymentService;
    }

    @Override
    public long refundIfPaid(String orderNumber, String reason) {
        try {
            var result = cancelPaymentService.cancel(orderNumber, null, reason);
            log.info("환불 완료 — order={} amount={}", orderNumber, result.cancelledAmount());
            return result.cancelledAmount();

        } catch (DomainException.NotFound e) {
            // 결제 기록이 없음 = 결제 전 취소. 정상 흐름입니다.
            return 0L;
        } catch (DomainException.Conflict e) {
            if ("NOT_APPROVED".equals(e.errorCode()) || "NOTHING_TO_CANCEL".equals(e.errorCode())) {
                return 0L;
            }
            throw e; // 환불 실패는 삼키지 않습니다 — 주문만 취소되면 고객 돈이 묶입니다.
        }
    }
}
