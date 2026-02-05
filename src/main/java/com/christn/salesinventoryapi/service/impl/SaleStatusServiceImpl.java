package com.christn.salesinventoryapi.service.impl;

import com.christn.salesinventoryapi.model.Sale;
import com.christn.salesinventoryapi.model.SaleStatus;
import com.christn.salesinventoryapi.model.SaleStatusHistory;
import com.christn.salesinventoryapi.repository.SaleStatusHistoryRepository;
import com.christn.salesinventoryapi.service.SaleStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SaleStatusServiceImpl implements SaleStatusService {

    private final SaleStatusHistoryRepository historyRepository;

    @Override
    public void recordStatusChange(Sale sale, SaleStatus toStatus, Long userId, String reason) {
        SaleStatus fromStatus = sale.getStatus();
        if (sale.getStatus() == toStatus) return;

        SaleStatusHistory h = new SaleStatusHistory();
        h.setSale(sale);
        h.setFromStatus(fromStatus);
        h.setToStatus(toStatus);
        h.setChangedAt(sale.getUpdatedAt());
        h.setChangedByUserId(userId);
        h.setReason(reason);

        historyRepository.save(h);

        sale.setStatus(toStatus);
    }
}
