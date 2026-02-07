package com.christn.salesinventoryapi.controller;

import com.christn.salesinventoryapi.dto.request.CreatePaymentRequest;
import com.christn.salesinventoryapi.dto.response.PaymentResponse;
import com.christn.salesinventoryapi.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales/{saleId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Registrar pago a venta", description = "Registra un pago parcial o total para una venta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pago registrado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Error de validación"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada"),
            @ApiResponse(responseCode = "409", description = "Pago excede el saldo pendiente o venta no ACTIVE")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> addPayment(
            @PathVariable Long saleId,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.addPayment(saleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
