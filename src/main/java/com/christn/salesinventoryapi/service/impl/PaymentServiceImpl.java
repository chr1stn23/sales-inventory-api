package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.auth.AuthUserDetails;
import com.christn.salesinventoryapi.dto.mapper.PaymentMapper;
import com.christn.salesinventoryapi.dto.request.CreatePaymentRequest;
import com.christn.salesinventoryapi.dto.response.PaymentResponse;
import com.christn.salesinventoryapi.model.*;
import com.christn.salesinventoryapi.repository.PaymentRepository;
import com.christn.salesinventoryapi.repository.SaleRepository;
import com.christn.salesinventoryapi.service.PaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentResponse addPayment(Long saleId, CreatePaymentRequest req) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + saleId));

        if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new IllegalStateException("Solo se puede agregar un pago a una venta en estado ACTIVE");
        }

        BigDecimal amount = req.amount().setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal paid = paymentRepository.sumPostedBySaleId(saleId);
        if (paid == null) paid = BigDecimal.ZERO;
        BigDecimal remaining = sale.getTotalAmount().subtract(paid).setScale(2, RoundingMode.UNNECESSARY);

        boolean isCash = req.method() == PaymentMethod.CASH;

        if (!isCash && amount.compareTo(remaining) > 0) {
            throw new IllegalStateException("El monto excede el saldo pendiente: " + remaining);
        }

        BigDecimal change = BigDecimal.ZERO;
        if (isCash && amount.compareTo(remaining) > 0) {
            change = amount.subtract(remaining).setScale(2, RoundingMode.UNNECESSARY);
        }

        AuthUserDetails user = currentUser();

        Payment p = new Payment();
        p.setSale(sale);
        p.setAmount(amount);
        p.setMethod(req.method());
        p.setChange(change);
        p.setPaidAt(LocalDateTime.now());
        p.setReference(req.reference());
        p.setCreatedByUser(userRef(user.getId()));

        paymentRepository.save(p);

        return PaymentMapper.toResponse(p);
    }

    private AuthUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUserDetails user)) {
            throw new IllegalStateException("El usuario no está autenticado");
        }
        return user;
    }

    private User userRef(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }
}
