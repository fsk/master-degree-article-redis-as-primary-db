package com.fsk.springbootredis.controller;

import com.fsk.springbootredis.api.ApiResponse;
import com.fsk.springbootredis.dto.requests.DeadlockTestRequest;
import com.fsk.springbootredis.dto.requests.PlaceOrderRequest;
import com.fsk.springbootredis.dto.requests.TransactionalStockAdjustRequest;
import com.fsk.springbootredis.entity.ProductEntity;
import com.fsk.springbootredis.service.TransactionTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tx-tests")
public class TransactionTestController {

    private final TransactionTestService transactionTestService;

    @PostMapping("/stock-adjust")
    public ResponseEntity<ApiResponse<ProductEntity>> adjustStock(@RequestBody TransactionalStockAdjustRequest request) {
        return ResponseEntity.ok(ApiResponse.success(transactionTestService.adjustStockWithDelay(request)));
    }

    @PostMapping("/order-rollback")
    public ResponseEntity<ApiResponse<Void>> forceRollback(@RequestBody PlaceOrderRequest request) {
        transactionTestService.placeOrderAndForceRollback(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/deadlock")
    public ResponseEntity<ApiResponse<String>> simulateDeadlock(@RequestBody DeadlockTestRequest request) {
        transactionTestService.simulateDeadlock(request);
        return ResponseEntity.ok(ApiResponse.success("Deadlock not applicable for Redis"));
    }
}
