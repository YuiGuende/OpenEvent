package com.group02.openevent.controller;

import com.group02.openevent.model.payment.WalletTransaction;
import com.group02.openevent.model.user.HostWallet;
import com.group02.openevent.service.IHostWalletService;
import com.group02.openevent.repository.IWalletTransactionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final IHostWalletService hostWalletService;
    private final IWalletTransactionRepository transactionRepository;
    private final com.group02.openevent.service.CustomerService customerService;

    public WalletController(IHostWalletService hostWalletService,
                           IWalletTransactionRepository transactionRepository,
                           com.group02.openevent.service.CustomerService customerService) {
        this.hostWalletService = hostWalletService;
        this.transactionRepository = transactionRepository;
        this.customerService = customerService;
    }

    private Long getHostIdFromSession(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return null;
        }
        try {
            com.group02.openevent.model.user.Customer customer = customerService.getCustomerByAccountId(accountId);
            if (customer != null && customer.getHost() != null) {
                return customer.getHost().getId();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(HttpSession session) {
        try {
            Long hostId = getHostIdFromSession(session);
            if (hostId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Host not found or not logged in"
                ));
            }

            HostWallet wallet = hostWalletService.getWalletByHostId(hostId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("hostId", hostId);
            // Convert BigDecimal to String to avoid precision issues in JSON
            response.put("balance", wallet.getBalance().toString());
            response.put("availableBalance", wallet.getAvailableBalance().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam(defaultValue = "all") String filter,
            HttpSession session) {
        try {
            Long hostId = getHostIdFromSession(session);
            if (hostId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Host not found or not logged in"
                ));
            }

            HostWallet wallet = hostWalletService.getWalletByHostId(hostId);
            List<WalletTransaction> transactions = transactionRepository.findByWalletAndFilterType(wallet, filter);
            
            List<Map<String, Object>> transactionData = transactions.stream()
                .map(txn -> {
                    Map<String, Object> txnMap = new HashMap<>();
                    txnMap.put("id", txn.getId());
                    txnMap.put("transactionType", txn.getTransactionType().name());
                    txnMap.put("amount", txn.getAmount().toString()); // Convert BigDecimal to String
                    txnMap.put("status", txn.getStatus().name());
                    txnMap.put("description", txn.getDescription() != null ? txn.getDescription() : "");
                    txnMap.put("createdAt", txn.getCreatedAt().toString());
                    txnMap.put("referenceId", txn.getReferenceId() != null ? txn.getReferenceId() : "");
                    return txnMap;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("transactions", transactionData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
}

