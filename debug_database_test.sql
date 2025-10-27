-- ========================================
-- 🔍 DEBUG DATABASE TEST SCRIPT
-- ========================================

-- 1. Check if customers table has data
SELECT 'CUSTOMERS' as table_name, COUNT(*) as count FROM customers;
SELECT * FROM customers LIMIT 5;

-- 2. Check if accounts table has data  
SELECT 'ACCOUNTS' as table_name, COUNT(*) as count FROM accounts;
SELECT * FROM accounts LIMIT 5;

-- 3. Check if events table has data
SELECT 'EVENTS' as table_name, COUNT(*) as count FROM events;
SELECT id, title, status FROM events LIMIT 5;

-- 4. Check if ticket_type table has data
SELECT 'TICKET_TYPE' as table_name, COUNT(*) as count FROM ticket_type;
SELECT ticket_type_id, name, final_price, available_quantity, event_id FROM ticket_type LIMIT 5;

-- 5. Check if orders table has data
SELECT 'ORDERS' as table_name, COUNT(*) as count FROM orders;
SELECT * FROM orders LIMIT 5;

-- 6. Check if payments table has data
SELECT 'PAYMENTS' as table_name, COUNT(*) as count FROM payments;
SELECT * FROM payments LIMIT 5;

-- 7. Find specific event "Music Night"
SELECT 'MUSIC NIGHT EVENT' as search_type;
SELECT id, title, status FROM events WHERE title LIKE '%Music Night%';

-- 8. Find ticket types for Music Night event
SELECT 'MUSIC NIGHT TICKETS' as search_type;
SELECT tt.ticket_type_id, tt.name, tt.final_price, tt.available_quantity, e.title as event_title
FROM ticket_type tt 
JOIN events e ON tt.event_id = e.id 
WHERE e.title LIKE '%Music Night%';

-- 9. Check customer with userId = 1
SELECT 'CUSTOMER USERID 1' as search_type;
SELECT c.customer_id, a.account_id, a.email, a.role 
FROM customers c 
JOIN accounts a ON c.account_id = a.account_id 
WHERE a.account_id = 1;

-- 10. Test insert order (if needed)
-- INSERT INTO orders (customer_id, event_id, status, total_amount, created_at) 
-- VALUES (1, 67, 'PENDING', 500000, NOW());
-- SELECT 'TEST INSERT' as result, LAST_INSERT_ID() as new_order_id;
































