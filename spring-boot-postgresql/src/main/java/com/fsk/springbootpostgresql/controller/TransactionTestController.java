package com.fsk.springbootpostgresql.controller;

import com.fsk.springbootpostgresql.api.ApiResponse;
import com.fsk.springbootpostgresql.dto.requests.DeadlockTestRequest;
import com.fsk.springbootpostgresql.dto.requests.PlaceOrderRequest;
import com.fsk.springbootpostgresql.dto.requests.TransactionalStockAdjustRequest;
import com.fsk.springbootpostgresql.entity.ProductEntity;
import com.fsk.springbootpostgresql.service.TransactionTestService;
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
        return ResponseEntity.ok(ApiResponse.success("Deadlock test completed"));
    }
}
