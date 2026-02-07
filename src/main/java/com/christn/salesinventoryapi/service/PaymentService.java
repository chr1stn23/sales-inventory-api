package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.dto.request.CreatePaymentRequest;
import com.christn.salesinventoryapi.dto.response.PaymentResponse;

public interface PaymentService {

    PaymentResponse addPayment(Long saleId, CreatePaymentRequest req);
}
