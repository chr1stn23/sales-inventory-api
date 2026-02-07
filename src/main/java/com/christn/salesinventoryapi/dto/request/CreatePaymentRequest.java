package com.christn.salesinventoryapi.dto.request;

import com.christn.salesinventoryapi.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePaymentRequest(

        @Schema(description = "Monto del pago", example = "10.50", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El monto del pago no puede ser nulo")
        @DecimalMin(value = "0.01", message = "El monto del pago debe ser mayor que 0")
        @Digits(integer = 8, fraction = 2, message = "El monto del pago debe tener máximo 2 decimales")
        BigDecimal amount,

        @Schema(description = "Método de pago", example = "CASH", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El método de pago no puede ser nulo")
        PaymentMethod method,

        @Schema(description = "Referencia del pago (voucher, operación, etc.)", example = "YAPE-4938274",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Size(max = 100, message = "La referencia del pago no puede superar los 100 caracteres")
        String reference
) {
}
