package com.stableflow.blockchain.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stableflow.blockchain.entity.PaymentScanCursor;
import com.stableflow.blockchain.mapper.PaymentScanCursorMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentScanCursorService {

    private final PaymentScanCursorMapper paymentScanCursorMapper;

    public PaymentScanCursorService(PaymentScanCursorMapper paymentScanCursorMapper) {
        this.paymentScanCursorMapper = paymentScanCursorMapper;
    }

    public PaymentScanCursor getOrCreate(String recipientAddress) {
        PaymentScanCursor cursor = paymentScanCursorMapper.selectOne(
            new LambdaQueryWrapper<PaymentScanCursor>()
                .eq(PaymentScanCursor::getRecipientAddress, recipientAddress)
        );
        if (cursor != null) {
            return cursor;
        }

        PaymentScanCursor newCursor = new PaymentScanCursor();
        newCursor.setRecipientAddress(recipientAddress);
        paymentScanCursorMapper.insert(newCursor);
        return newCursor;
    }

    @Transactional
    public void updateCursor(String recipientAddress, String lastSeenSignature) {
        PaymentScanCursor cursor = getOrCreate(recipientAddress);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        cursor.setLastSeenSignature(lastSeenSignature);
        cursor.setLastScannedAt(now);
        cursor.setUpdatedAt(now);
        paymentScanCursorMapper.updateById(cursor);
    }
}
