package com.fsk.springbootpostgresql.service;

import com.fsk.springbootpostgresql.dto.requests.DeadlockTestRequest;
import com.fsk.springbootpostgresql.dto.requests.PlaceOrderRequest;
import com.fsk.springbootpostgresql.dto.requests.TransactionalStockAdjustRequest;
import com.fsk.springbootpostgresql.entity.ProductEntity;
import com.fsk.springbootpostgresql.exception.DeadlockDetectedException;
import com.fsk.springbootpostgresql.exception.ForcedRollbackException;
import com.fsk.springbootpostgresql.exception.ProductNotFoundException;
import com.fsk.springbootpostgresql.exception.StockBelowZeroException;
import com.fsk.springbootpostgresql.repository.ProductRepository;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class TransactionTestService {

    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final PlatformTransactionManager transactionManager;

    @Transactional
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
        return product;
    }

    @Transactional
    public void placeOrderAndForceRollback(PlaceOrderRequest request) {
        orderService.placeOrder(request);
        throw new ForcedRollbackException();
    }

    public void simulateDeadlock(DeadlockTestRequest request) {
        UUID first = request.firstProductId();
        UUID second = request.secondProductId();
        long delay = request.delayMs() == null ? 0 : request.delayMs();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        Callable<Void> task1 = () -> {
            runWithLocks(first, second, delay, barrier);
            return null;
        };

        Callable<Void> task2 = () -> {
            runWithLocks(second, first, delay, barrier);
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);

        try {
            f1.get();
            f2.get();
        } catch (ExecutionException ex) {
            if (isDeadlock(ex.getCause())) {
                throw new DeadlockDetectedException();
            }
            throw new RuntimeException(ex.getCause());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
    }

    private void runWithLocks(UUID first, UUID second, long delay, CyclicBarrier barrier) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> {
            ProductEntity firstProduct = productRepository.findByIdForUpdate(first);
            if (firstProduct == null) {
                throw new ProductNotFoundException();
            }
            awaitBarrier(barrier);
            sleepIfNeeded(delay);

            ProductEntity secondProduct = productRepository.findByIdForUpdate(second);
            if (secondProduct == null) {
                throw new ProductNotFoundException();
            }
        });
    }

    private void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // ignore barrier failures; deadlock test will fail fast
        }
    }

    private boolean isDeadlock(Throwable cause) {
        if (cause == null) {
            return false;
        }
        if (cause instanceof DeadlockDetectedException) {
            return true;
        }
        if (cause instanceof DataAccessException) {
            String message = cause.getMessage();
            return message != null && message.toLowerCase().contains("deadlock detected");
        }
        return isDeadlock(cause.getCause());
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
