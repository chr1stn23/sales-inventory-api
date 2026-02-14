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
import org.jspecify.annotations.NonNull;
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
        // 1. Traer venta con lock para evitar pagos concurrentes que pasen la
        // validación
        Sale sale = saleRepository.findByIdForUpdate(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada: " + saleId));

        // 2. Validar estado
        if (sale.getStatus() != SaleStatus.ACTIVE) {
            throw new IllegalStateException("Solo se puede agregar un pago a una venta en estado ACTIVE");
        }

        // 3. Normalizar montos
        BigDecimal saleTotal = sale.getTotalAmount().setScale(2, RoundingMode.UNNECESSARY);

        BigDecimal alreadyPaid = paymentRepository.sumPostedBySaleId(saleId);
        if (alreadyPaid == null) alreadyPaid = BigDecimal.ZERO;
        alreadyPaid = alreadyPaid.setScale(2, RoundingMode.UNNECESSARY);

        // 4. Si ya está pagada (o sobre pagada), bloquear cualquier nuevo pago
        if (alreadyPaid.compareTo(saleTotal) >= 0) {
            throw new IllegalStateException(
                    "La venta ya está pagada. Total: " + saleTotal + " | Pagado: " + alreadyPaid
            );
        }

        BigDecimal paymentAmount = req.amount().setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal remaining = saleTotal.subtract(alreadyPaid).setScale(2, RoundingMode.UNNECESSARY);

        BigDecimal change = getChange(req, paymentAmount, remaining);

        // 7. Crear y guardar pago
        AuthUserDetails user = currentUser();

        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setAmount(paymentAmount);
        payment.setMethod(req.method());
        payment.setChange(change);
        payment.setPaidAt(LocalDateTime.now());
        payment.setReference(req.reference());
        payment.setCreatedByUserId(user.getId());

        paymentRepository.save(payment);

        return PaymentMapper.toResponse(payment);
    }

    private static @NonNull BigDecimal getChange(CreatePaymentRequest req, BigDecimal paymentAmount,
            BigDecimal remaining) {
        boolean isCash = req.method() == PaymentMethod.CASH;

        // 5. Si NO es cash, no permitir exceder el saldo
        if (!isCash && paymentAmount.compareTo(remaining) > 0) {
            throw new IllegalStateException("El monto excede el saldo pendiente: " + remaining);
        }

        // 6. Si es cash, permitir exceder y calcular vuelto
        BigDecimal change = BigDecimal.ZERO;
        if (isCash && paymentAmount.compareTo(remaining) > 0) {
            change = paymentAmount.subtract(remaining).setScale(2, RoundingMode.UNNECESSARY);
        }
        return change;
    }

    private AuthUserDetails currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUserDetails user)) {
            throw new IllegalStateException("El usuario no está autenticado");
        }
        return user;
    }
}
