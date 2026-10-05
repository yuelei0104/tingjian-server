package com.tingjian.server.service;

import com.tingjian.server.common.BusinessException;
import com.tingjian.server.dao.VerificationCodeDao;
import com.tingjian.server.entity.VerificationCodeEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationCodeServiceTests {
    private final VerificationCodeDao dao = mock(VerificationCodeDao.class);
    private final VerificationDeliveryGateway gateway = mock(VerificationDeliveryGateway.class);
    private final AuthRateLimitService limiter = mock(AuthRateLimitService.class);
    private final VerificationCodeService service = new VerificationCodeService(
            dao, gateway, limiter, "unit-test-pepper", 10);

    @Test
    void issuedCodeCanBeConsumedOnceForTheMatchingEmailAndPurpose() {
        ArgumentCaptor<VerificationCodeEntity> entity =
                ArgumentCaptor.forClass(VerificationCodeEntity.class);
        ArgumentCaptor<String> deliveredCode = ArgumentCaptor.forClass(String.class);

        var response = service.issueEmail(
                " USER@example.com ", VerificationCodeService.REGISTER, "127.0.0.1", true);

        verify(dao).create(entity.capture());
        verify(gateway).send(
                eq("EMAIL"), eq("user@example.com"), eq(VerificationCodeService.REGISTER),
                deliveredCode.capture(), eq(10));
        when(dao.findForUpdate(response.verificationId()))
                .thenReturn(Optional.of(entity.getValue()));
        when(dao.consume(eq(response.verificationId()), any())).thenReturn(1);

        service.verifyAndConsume(
                response.verificationId(), deliveredCode.getValue(), "user@example.com",
                VerificationCodeService.REGISTER);

        verify(dao).consume(eq(response.verificationId()), any());
    }

    @Test
    void mismatchedDestinationCountsAsFailure() {
        ArgumentCaptor<VerificationCodeEntity> entity =
                ArgumentCaptor.forClass(VerificationCodeEntity.class);
        var response = service.issueEmail(
                "user@example.com", VerificationCodeService.RESET_PASSWORD, "127.0.0.1", false);
        verify(dao).create(entity.capture());
        when(dao.findForUpdate(response.verificationId()))
                .thenReturn(Optional.of(entity.getValue()));

        assertThrows(BusinessException.class, () -> service.verifyAndConsume(
                response.verificationId(), "000000", "other@example.com",
                VerificationCodeService.RESET_PASSWORD));

        verify(dao).incrementFailedAttempts(response.verificationId());
    }
}
