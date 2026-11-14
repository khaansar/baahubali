-- Free status is decided per series: align every test with its series price (0 = free).
UPDATE mock_tests m
JOIN test_series s ON s.id = m.series_id
SET m.is_free = (s.base_price = 0)
WHERE m.is_free <> (s.base_price = 0);
