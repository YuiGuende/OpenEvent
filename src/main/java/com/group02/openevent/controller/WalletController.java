package com.group02.openevent.controller;

import com.group02.openevent.exception.WalletException;
import com.group02.openevent.model.payment.WalletTransaction;
import com.group02.openevent.model.user.HostWallet;
import com.group02.openevent.model.user.User;
import com.group02.openevent.service.BankVerificationService;
import com.group02.openevent.service.HostService;
import com.group02.openevent.service.IHostWalletService;
import com.group02.openevent.service.UserService;
import com.group02.openevent.service.impl.BankVerificationServiceImpl;
import com.group02.openevent.repository.IWalletTransactionRepository;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final IHostWalletService hostWalletService;
    private final IWalletTransactionRepository transactionRepository;
    private final BankVerificationService bankVerificationService;
    private final HostService hostService;
    private final UserService userService;

    public WalletController(IHostWalletService hostWalletService,
                            IWalletTransactionRepository transactionRepository,
                            BankVerificationService bankVerificationService,
                            HostService hostService,
                            UserService userService) {
        this.hostWalletService = hostWalletService;
        this.transactionRepository = transactionRepository;
        this.bankVerificationService = bankVerificationService;
        this.hostService = hostService;
        this.userService = userService;
    }

    /**
     * GET /api/wallet/debug - Debug endpoint to check session
     */
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugSession(HttpSession session) {
        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("sessionExists", session != null);
        if (session != null) {
            debugInfo.put("sessionId", session.getId());
            debugInfo.put("ACCOUNT_ID", session.getAttribute("USER_ID"));
            debugInfo.put("USER_ID", session.getAttribute("USER_ID"));
            debugInfo.put("KYC_NAME", session.getAttribute("KYC_NAME"));
            java.util.Enumeration<String> attrNames = session.getAttributeNames();
            java.util.List<String> attributes = new java.util.ArrayList<>();
            while (attrNames.hasMoreElements()) {
                attributes.add(attrNames.nextElement());
            }
            debugInfo.put("allAttributes", attributes);
        }
        return ResponseEntity.ok(debugInfo);
    }

    /**
     * GET /api/wallet/info - Kiểm tra ví đã tồn tại chưa và trả về thông tin ví
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getWalletInfo(HttpSession session) {
        log.info("=== getWalletInfo called ===");
        try {
            if (session == null) {
                log.warn("Session is null in getWalletInfo - request may have been blocked by interceptor");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Session not found. Please login again."
                ));
            }
            
            // Log session info for debugging
            Long userId = (Long) session.getAttribute("USER_ID");
            log.info("getWalletInfo - session ID: {}, USER_ID: {}", 
                    session.getId(), userId);
            
            Long hostId = userService.getCurrentUser(session).getHost().getId();
            log.info("getWalletInfo - hostId: {}", hostId);
            
            if (hostId == null) {
                log.warn("Host ID is null - user may not be a host. userId: {}", userId);
                // Return 200 with exists=false instead of 401/403 to allow UI to handle gracefully
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("exists", false);
                response.put("isHost", false);
                response.put("message", "Bạn chưa phải là Host. Vui lòng đăng nhập với tài khoản Host.");
                String kycName = (String) session.getAttribute("KYC_NAME");
                response.put("kycName", kycName != null ? kycName : null);
                response.put("kycVerified", kycName != null);
                return ResponseEntity.ok(response);
            }

            HostWallet wallet = hostWalletService.getWalletByHostId(hostId);
            
            Map<String, Object> response = new HashMap<>();
            if (wallet == null) {
                // Ví chưa tồn tại
                response.put("success", true);
                response.put("exists", false);
                response.put("message", "Wallet does not exist");
                // Lấy KYC name từ session nếu có
                String kycName = (String) session.getAttribute("KYC_NAME");
                response.put("kycName", kycName != null ? kycName : null);
                response.put("kycVerified", kycName != null);
            } else {
                // Ví đã tồn tại
                response.put("success", true);
                response.put("exists", true);
                response.put("hostId", hostId);
                response.put("balance", wallet.getBalance().toString());
                response.put("availableBalance", wallet.getAvailableBalance().toString());
                response.put("bankAccountNumber", wallet.getBankAccountNumber());
                response.put("bankCode", wallet.getBankCode());
                response.put("accountHolderName", wallet.getAccountHolderName());
                response.put("kycName", wallet.getKycName());
                response.put("kycVerified", wallet.getKycVerified() != null && wallet.getKycVerified());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    /**
     * POST /api/wallet/create - Tạo ví với thông tin ngân hàng và KYC
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createWallet(@RequestBody Map<String, String> request,
                                                            HttpSession session) {
        // Biến hostId để dùng trong log lỗi
        Long hostId = null;
        try {
            hostId = userService.getCurrentUser(session).getHost().getId();
            if (hostId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Host not found or not logged in"
                ));
            }

            // BƯỚC 2: XÓA CHECK "walletExists" TẠI ĐÂY
            // if (hostWalletService.walletExists(hostId)) {
            //     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
            //         "success", false,
            //         "message", "Ví đã tồn tại. Không thể tạo mới."
            //     ));
            // }

            // ... (toàn bộ logic lấy kycName, bankAccountNumber, bankCode, verify bank... giữ nguyên) ...

            // Lấy KYC name từ session
            String kycName = (String) session.getAttribute("KYC_NAME");
            if (kycName == null || kycName.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false,
                        "message", "Chưa hoàn thành KYC. Vui lòng làm KYC trước khi tạo ví."
                ));
            }

            // Lấy thông tin từ request
            String bankAccountNumber = request.get("bankAccountNumber");
            String bankCode = request.get("bankCode");

            if (bankAccountNumber == null || bankAccountNumber.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false,
                        "message", "Số tài khoản ngân hàng không được để trống"
                ));
            }

            if (bankCode == null || bankCode.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false,
                        "message", "Mã ngân hàng không được để trống"
                ));
            }

            // Kiểm tra tên chủ tài khoản ngân hàng
            String accountHolderName;
            try {
                accountHolderName = bankVerificationService.verifyBankAccount(bankAccountNumber.trim(), bankCode.trim());
                if (accountHolderName == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                            "success", false,
                            "message", "Thông tin tài khoản ngân hàng không hợp lệ hoặc không tìm thấy"
                    ));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false,
                        "message", "Lỗi khi kiểm tra tài khoản ngân hàng: " + e.getMessage()
                ));
            }

            // So sánh tên chủ tài khoản với KYC name (chuẩn hóa)
            if (!BankVerificationServiceImpl.compareNames(accountHolderName, kycName)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false,
                        "message", "Tên chủ tài khoản ngân hàng không khớp với tên KYC. Tên KYC: " + kycName + ", Tên tài khoản: " + accountHolderName
                ));
            }

            // Tạo ví
            HostWallet wallet = hostWalletService.createWalletWithBankInfo(
                    hostId,
                    bankAccountNumber.trim(),
                    bankCode.trim(),
                    accountHolderName,
                    kycName.trim()
            );

            // ... (logic xóa session và tạo response giữ nguyên) ...
            session.removeAttribute("KYC_NAME");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            // ... (response data) ...

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

            // BƯỚC 3: THÊM KHỐI CATCH NÀY
        } catch (DataIntegrityViolationException e) {
            // Lỗi này xảy ra khi unique constraint (hostId) bị vi phạm
            log.warn("Data integrity violation for host {}: {}", hostId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", "Ví đã tồn tại. Không thể tạo mới." // Trả về thông báo thân thiện
            ));
        } catch (WalletException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            // Lỗi 500 (StaleObjectStateException) của bạn đã bị bắt ở đây
            log.error("Internal server error in createWallet for host {}: {}", hostId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Lỗi hệ thống: " + e.getMessage()
            ));
        }
    }
    /**
     * GET /api/wallet/balance - Lấy số dư
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(HttpSession session) {
        try {
            Long hostId = userService.getCurrentUser(session).getHost().getId();
            if (hostId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Host not found or not logged in"
                ));
            }

            HostWallet wallet = hostWalletService.getWalletByHostId(hostId);
            
            if (wallet == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Ví không tồn tại. Vui lòng tạo ví trước."
                ));
            }
            
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
            Long hostId = userService.getCurrentUser(session).getHost().getId();
            if (hostId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Host not found or not logged in"
                ));
            }

            HostWallet wallet = hostWalletService.getWalletByHostId(hostId);
            if (wallet == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Ví không tồn tại. Vui lòng tạo ví trước.",
                    "transactions", List.of()
                ));
            }
            
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

