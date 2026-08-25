TRUNCATE TABLE
    station_opening_period,
    station_current_product_price,
    station
RESTART IDENTITY CASCADE;

INSERT INTO station (
    id,
    external_id,
    country,
    brand,
    street,
    postal_code,
    locality,
    municipality,
    province,
    location,
    created_at,
    updated_at
)
VALUES
(
    '11111111-1111-1111-1111-111111111111',
    'S1',
    'ES',
    'REPSOL',
    'Street 1',
    '29550',
    'ALMANSA',
    'Almansa',
    'ARDALES',
    ST_GeogFromText('POINT(-4.844639 36.878694)'),
    NOW(),
    NOW()
),
(
    '22222222-2222-2222-2222-222222222222',
    'S2',
    'ES',
    'CEPSA',
    'Street 2',
    '29550',
    'ALMANSA',
    'Almansa',
    'ARDALES',
    ST_GeogFromText('POINT(-4.845300 36.879000)'),
    NOW(),
    NOW()
),
(
    '33333333-3333-3333-3333-333333333333',
    'S3',
    'ES',
    'BP',
    'Street 3',
    '29550',
    'ALMANSA',
    'Almansa',
    'ARDALES',
    ST_GeogFromText('POINT(-4.846000 36.879400)'),
    NOW(),
    NOW()
),
(
    '44444444-4444-4444-4444-444444444444',
    'S4',
    'ES',
    'SHELL',
    'Street 4',
    '29550',
    'ALMANSA',
    'Almansa',
    'ARDALES',
    ST_GeogFromText('POINT(-4.900000 36.900000)'),
    NOW(),
    NOW()
),
(
    '55555555-5555-5555-5555-555555555555',
    'S5',
    'ES',
    'GALP',
    'Street 5',
    '29550',
    'ALMANSA',
    'Almansa',
    'ARDALES',
    ST_GeogFromText('POINT(-4.844900 36.878900)'),
    NOW(),
    NOW()
),
(
    '66666666-6666-6666-6666-666666666666',
    'S6',
    'ES',
    'AVIA',
    'Street 6',
    '29550',
    'ALMANSA',
    'Almansa',
    'ARDALES',
    ST_GeogFromText('POINT(-4.847000 36.879800)'),
    NOW(),
    NOW()
);

INSERT INTO station_current_product_price (
    id,
    station_id,
    product_type,
    price,
    updated_at
)
VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '11111111-1111-1111-1111-111111111111', 'DIESEL_A', 1.400, NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '11111111-1111-1111-1111-111111111111', 'GASOLINE_95_E5', 1.600, NOW()),

('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '22222222-2222-2222-2222-222222222222', 'DIESEL_A', 1.700, NOW()),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '22222222-2222-2222-2222-222222222222', 'GASOLINE_95_E5', 1.500, NOW()),

('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5', '33333333-3333-3333-3333-333333333333', 'GASOLINE_95_E5', 1.300, NOW()),

('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', '44444444-4444-4444-4444-444444444444', 'DIESEL_A', 1.100, NOW()),

('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa7', '66666666-6666-6666-6666-666666666666', 'DIESEL_A', 1.550, NOW());

INSERT INTO station_opening_period (
    id,
    station_id,
    day_of_week,
    open_time,
    close_time
)
VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '11111111-1111-1111-1111-111111111111', 'MON', '08:00', '22:00'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '11111111-1111-1111-1111-111111111111', 'TUE', '08:00', '22:00'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3', '11111111-1111-1111-1111-111111111111', 'WED', '08:00', '22:00'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa4', '11111111-1111-1111-1111-111111111111', 'SAT', '09:00', '20:00'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa5', '22222222-2222-2222-2222-222222222222', 'MON', '07:00', '23:00'),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa6', '33333333-3333-3333-3333-333333333333', 'SUN', '09:00', '21:00');