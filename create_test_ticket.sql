-- Tạo ticket test cho event ID 1
INSERT INTO ticket_type (
    event_id,
    name,
    description,
    price,
    total_quantity,
    sold_quantity,
    start_sale_date,
    end_sale_date,
    sale
) VALUES (
    1, -- event_id
    'Vé Early Bird',
    'Vé ưu đãi cho những người đăng ký sớm',
    100000.00,
    100,
    0,
    NOW(),
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    10.00
);

-- Tạo thêm một ticket khác
INSERT INTO ticket_type (
    event_id,
    name,
    description,
    price,
    total_quantity,
    sold_quantity,
    start_sale_date,
    end_sale_date,
    sale
) VALUES (
    1, -- event_id
    'Vé VIP',
    'Vé VIP với nhiều ưu đãi đặc biệt',
    200000.00,
    50,
    0,
    NOW(),
    DATE_ADD(NOW(), INTERVAL 30 DAY),
    5.00
);
