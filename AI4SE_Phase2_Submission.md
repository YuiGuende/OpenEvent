[//]: # (# AI4SE Phase 2 Submission: BDD Test Case Generation)

[//]: # ()
[//]: # (## Method Analysis)

[//]: # (**Class:** `PaymentController`  )

[//]: # (**Method:** `createPaymentForOrder`  )

[//]: # (**Purpose:** Creates a payment link for a specific order with authentication and authorization checks)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (### Input Prompt)

[//]: # ()
[//]: # (```)

[//]: # (Role: Senior Quality Assurance Engineer)

[//]: # ()
[//]: # (Task: Analyze the provided Spring Boot PaymentController.createPaymentForOrder method and generate comprehensive BDD &#40;Given/When/Then&#41; test cases.)

[//]: # ()
[//]: # (Method Details:)

[//]: # (- Class: PaymentController)

[//]: # (- Method: createPaymentForOrder&#40;@PathVariable Long orderId, HttpServletRequest request&#41;)

[//]: # (- Endpoint: POST /api/payments/create-for-order/{orderId})

[//]: # (- Returns: ResponseEntity<Map<String, Object>>)

[//]: # ()
[//]: # (Business Logic:)

[//]: # (1. Validates user authentication &#40;currentUserId from request attribute&#41;)

[//]: # (2. Retrieves order by ID from OrderService)

[//]: # (3. Validates order ownership &#40;order belongs to current user&#41;)

[//]: # (4. Checks for existing pending payment)

[//]: # (5. Creates new payment link if no existing payment)

[//]: # (6. Returns payment details &#40;success, orderId, paymentId, checkoutUrl, qrCode, amount&#41;)

[//]: # ()
[//]: # (Frameworks: JUnit 5, Mockito, and MockMvc &#40;for controllers&#41;.)

[//]: # ()
[//]: # (Generate comprehensive test cases covering:)

[//]: # (1. Happy Path scenarios)

[//]: # (2. Error Handling &#40;validation, exceptions&#41;)

[//]: # (3. Security/Authentication scenarios)

[//]: # (4. Creative/Edge cases)

[//]: # ()
[//]: # (Format each test case as a BDD table with Given/When/Then structure.)

[//]: # (```)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (### AI Output)

[//]: # ()
[//]: # (| Test Case ID | Scenario | Given | When | Then |)

[//]: # (|--------------|----------|-------|------|------|)

[//]: # (| **HAPPY PATH SCENARIOS** |)

[//]: # (| TC001 | Create new payment for valid order | • User is authenticated &#40;currentUserId = 1&#41;<br>• Order exists and belongs to user<br>• No existing payment for order | POST /api/payments/create-for-order/1 with valid orderId | • Status: 200 OK<br>• Response contains: success=true, orderId=1, paymentId, checkoutUrl, qrCode, amount |)

[//]: # (| TC002 | Return existing pending payment | • User is authenticated &#40;currentUserId = 1&#41;<br>• Order exists and belongs to user<br>• Existing payment with PENDING status exists | POST /api/payments/create-for-order/1 | • Status: 200 OK<br>• Response contains existing payment details<br>• No new payment created |)

[//]: # (| **AUTHENTICATION & AUTHORIZATION** |)

[//]: # (| TC003 | Unauthenticated user access | • No currentUserId in request attributes<br>• Order exists in system | POST /api/payments/create-for-order/1 | • Status: 400 Bad Request<br>• Response: {"success": false, "message": "User not logged in"} |)

[//]: # (| TC004 | Order belongs to different user | • User authenticated &#40;currentUserId = 1&#41;<br>• Order exists but belongs to user 2 | POST /api/payments/create-for-order/1 | • Status: 400 Bad Request<br>• Response: {"success": false, "message": "Order does not belong to current user"} |)

[//]: # (| **ERROR HANDLING** |)

[//]: # (| TC005 | Order not found | • User is authenticated<br>• Order with given ID does not exist | POST /api/payments/create-for-order/999 | • Status: 400 Bad Request<br>• Response: {"success": false, "message": "Order not found"} |)

[//]: # (| TC006 | Invalid order ID format | • User is authenticated<br>• Invalid order ID provided | POST /api/payments/create-for-order/abc | • Status: 400 Bad Request<br>• Spring validation error |)

[//]: # (| TC007 | PaymentService throws exception | • User is authenticated<br>• Order exists and belongs to user<br>• PaymentService.createPaymentLinkForOrder throws RuntimeException | POST /api/payments/create-for-order/1 | • Status: 400 Bad Request<br>• Response: {"success": false, "message": "Exception message"} |)

[//]: # (| **EDGE CASES** |)

[//]: # (| TC008 | Null orderId parameter | • User is authenticated<br>• Null orderId in path | POST /api/payments/create-for-order/null | • Status: 400 Bad Request<br>• Spring validation error |)

[//]: # (| TC009 | Negative orderId | • User is authenticated<br>• Negative orderId provided | POST /api/payments/create-for-order/-1 | • Status: 400 Bad Request<br>• Order not found error |)

[//]: # (| TC010 | Zero orderId | • User is authenticated<br>• Zero orderId provided | POST /api/payments/create-for-order/0 | • Status: 400 Bad Request<br>• Order not found error |)

[//]: # (| TC011 | Very large orderId | • User is authenticated<br>• Very large orderId &#40;Long.MAX_VALUE&#41; | POST /api/payments/create-for-order/9223372036854775807 | • Status: 400 Bad Request<br>• Order not found error |)

[//]: # (| TC012 | Order with null customer | • User is authenticated<br>• Order exists but customer is null | POST /api/payments/create-for-order/1 | • Status: 500 Internal Server Error<br>• NullPointerException |)

[//]: # (| TC013 | Order with null account | • User is authenticated<br>• Order exists, customer exists, but account is null | POST /api/payments/create-for-order/1 | • Status: 500 Internal Server Error<br>• NullPointerException |)

[//]: # (| **CONCURRENT ACCESS** |)

[//]: # (| TC014 | Multiple requests for same order | • User is authenticated<br>• Order exists and belongs to user<br>• Multiple concurrent requests for same order | Multiple POST requests simultaneously | • All requests return same payment details<br>• No duplicate payments created |)

[//]: # (| **PAYMENT STATUS VARIATIONS** |)

[//]: # (| TC015 | Existing payment with PAID status | • User is authenticated<br>• Order exists and belongs to user<br>• Existing payment with PAID status | POST /api/payments/create-for-order/1 | • Status: 200 OK<br>• New payment created &#40;ignores existing PAID payment&#41; |)

[//]: # (| TC016 | Existing payment with CANCELLED status | • User is authenticated<br>• Order exists and belongs to user<br>• Existing payment with CANCELLED status | POST /api/payments/create-for-order/1 | • Status: 200 OK<br>• New payment created &#40;ignores existing CANCELLED payment&#41; |)

[//]: # (| **SECURITY SCENARIOS** |)

[//]: # (| TC017 | SQL Injection attempt in orderId | • User is authenticated<br>• Malicious orderId with SQL injection | POST /api/payments/create-for-order/1'; DROP TABLE orders; -- | • Status: 400 Bad Request<br>• Order not found &#40;properly handled&#41; |)

[//]: # (| TC018 | XSS attempt in orderId | • User is authenticated<br>• Malicious orderId with XSS payload | POST /api/payments/create-for-order/<script>alert&#40;'xss'&#41;</script> | • Status: 400 Bad Request<br>• Order not found &#40;properly handled&#41; |)

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## Code Context Citations)

[//]: # ()
[//]: # (**Method Definition:** [cite: 124-193]  )

[//]: # (**Authentication Check:** [cite: 128-134]  )

[//]: # (**Order Retrieval:** [cite: 137-143]  )

[//]: # (**Authorization Check:** [cite: 145-151]  )

[//]: # (**Existing Payment Check:** [cite: 154-168]  )

[//]: # (**New Payment Creation:** [cite: 170-185]  )

[//]: # (**Exception Handling:** [cite: 187-192])

[//]: # ()
[//]: # (---)

[//]: # ()
[//]: # (## Test Implementation Notes)

[//]: # ()
[//]: # (### Mock Setup Required:)

[//]: # (```java)

[//]: # (@Mock private OrderService orderService;)

[//]: # (@Mock private PaymentService paymentService;)

[//]: # (@Mock private PayOS payOS;)

[//]: # (```)

[//]: # ()
[//]: # (### Key Test Data:)

[//]: # (- Valid Order ID: 1L)

[//]: # (- Valid User ID: 1L)

[//]: # (- Invalid Order ID: 999L)

[//]: # (- Mock Payment with PENDING status)

[//]: # (- Mock Order with Customer and Account)

[//]: # ()
[//]: # (### Assertion Patterns:)

[//]: # (- Status code validation: `.andExpect&#40;status&#40;&#41;.isOk&#40;&#41;&#41;`)

[//]: # (- JSON response validation: `.andExpect&#40;jsonPath&#40;"$.success"&#41;.value&#40;true&#41;&#41;`)

[//]: # (- Error message validation: `.andExpect&#40;jsonPath&#40;"$.message"&#41;.value&#40;"Expected message"&#41;&#41;`)

[//]: # ()
[//]: # (### Security Considerations:)

[//]: # (- Authentication bypass attempts)

[//]: # (- Authorization boundary testing)

[//]: # (- Input validation testing)

[//]: # (- SQL injection prevention)

[//]: # (- XSS prevention)


