package com.fsk.springbootredis.service;

import com.fsk.springbootredis.dto.requests.DeadlockTestRequest;
import com.fsk.springbootredis.dto.requests.PlaceOrderRequest;
import com.fsk.springbootredis.dto.requests.TransactionalStockAdjustRequest;
import com.fsk.springbootredis.entity.ProductEntity;
import com.fsk.springbootredis.exception.ForcedRollbackException;
import com.fsk.springbootredis.exception.ProductNotFoundException;
import com.fsk.springbootredis.exception.StockBelowZeroException;
import com.fsk.springbootredis.repository.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionTestService {

    private final ProductRepository productRepository;
    private final OrderService orderService;

    public ProductEntity adjustStockWithDelay(TransactionalStockAdjustRequest request) {
        UUID productId = request.productId();
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        int updated = product.getStockQuantity() + request.delta();
        if (updated < 0) {
            throw new StockBelowZeroException();
        }

        sleepIfNeeded(request.delayMs());
        product.setStockQuantity(updated);
        return productRepository.save(product);
    }

    public void placeOrderAndForceRollback(PlaceOrderRequest request) {
        orderService.placeOrder(request);
        throw new ForcedRollbackException();
    }

    public void simulateDeadlock(DeadlockTestRequest request) {
        sleepIfNeeded(request.delayMs());
    }

    private void sleepIfNeeded(Long delayMs) {
        if (delayMs == null || delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
