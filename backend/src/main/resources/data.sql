-- ============================================================================
-- SAMPLE DATA FOR RESTAURANT POS SYSTEM
-- ============================================================================

-- Sample menu items for testing
INSERT INTO menu_items (name, price, category, print_destination, available, created_at) VALUES 
-- ХРАНА (КУЈНА)
('Чизбургер', 280.00, 'ХРАНА', 'КУЈНА', true, CURRENT_TIMESTAMP),
('Пилешко филе', 320.00, 'ХРАНА', 'КУЈНА', true, CURRENT_TIMESTAMP),
('Грчка салата', 180.00, 'ХРАНА', 'КУЈНА', true, CURRENT_TIMESTAMP),
('Пица Маргарита', 250.00, 'ХРАНА', 'КУЈНА', true, CURRENT_TIMESTAMP),
('Паста Болоњезе', 220.00, 'ХРАНА', 'КУЈНА', true, CURRENT_TIMESTAMP),
('Риба со зеленчук', 380.00, 'ХРАНА', 'КУЈНА', true, CURRENT_TIMESTAMP),

-- ПИЈАЛОЦИ (БАР)
('Кока Кола', 60.00, 'ПИЈАЛОЦИ', 'БАР', true, CURRENT_TIMESTAMP),
('Минерална вода', 40.00, 'ПИЈАЛОЦИ', 'БАР', true, CURRENT_TIMESTAMP),
('Сок од портокал', 80.00, 'ПИЈАЛОЦИ', 'БАР', true, CURRENT_TIMESTAMP),
('Кафе еспресо', 50.00, 'ПИЈАЛОЦИ', 'БАР', true, CURRENT_TIMESTAMP),
('Капучино', 70.00, 'ПИЈАЛОЦИ', 'БАР', true, CURRENT_TIMESTAMP),
('Чај', 30.00, 'ПИЈАЛОЦИ', 'БАР', true, CURRENT_TIMESTAMP),

-- АЛКОХОЛ (БАР)
('Пиво Скопско', 90.00, 'АЛКОХОЛ', 'БАР', true, CURRENT_TIMESTAMP),
('Вино Тиквеш', 120.00, 'АЛКОХОЛ', 'БАР', true, CURRENT_TIMESTAMP),
('Ракија', 100.00, 'АЛКОХОЛ', 'БАР', true, CURRENT_TIMESTAMP),
('Виски', 150.00, 'АЛКОХОЛ', 'БАР', true, CURRENT_TIMESTAMP),

-- ДЕСЕРТИ (КУЈНА)
('Тирамису', 150.00, 'ДЕСЕРТИ', 'КУЈНА', true, CURRENT_TIMESTAMP),
('Чоколадно суфле', 180.00, 'ДЕСЕРТИ', 'КУЈНА', true, CURRENT_TIMESTAMP),
('Сладолед', 80.00, 'ДЕСЕРТИ', 'КУЈНА', true, CURRENT_TIMESTAMP),
('Палачинки', 120.00, 'ДЕСЕРТИ', 'КУЈНА', true, CURRENT_TIMESTAMP);
