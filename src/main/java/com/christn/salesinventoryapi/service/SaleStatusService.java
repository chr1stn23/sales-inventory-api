package com.christn.salesinventoryapi.service;

import com.christn.salesinventoryapi.model.Sale;
import com.christn.salesinventoryapi.model.SaleStatus;

public interface SaleStatusService {

    void recordStatusChange(Sale sale, SaleStatus toStatus, Long userId, String reason);
}
