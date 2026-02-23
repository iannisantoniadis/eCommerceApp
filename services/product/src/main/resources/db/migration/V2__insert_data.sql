INSERT INTO category (name, description) VALUES
                                             ('Electronics', 'Gadgets, devices, and computing hardware'),
                                             ('Home & Kitchen', 'Appliances, furniture, and kitchenware'),
                                             ('Office Supplies', 'Stationery, paper, and desk accessories'),
                                             ('Fitness', 'Workout equipment and health tracking gear');

INSERT INTO product (name, description, available_quantity, price, category_id) VALUES
                                            ('Wireless Mouse', 'Ergonomic 2.4GHz optical mouse', 150, 25.99, 1),
                                            ('Mechanical Keyboard', 'RGB backlit with blue switches', 45, 89.50, 1),
                                            ('Air Fryer', '5.5L capacity with digital touchscreen', 30, 120.00, 2),
                                            ('Standing Desk', 'Adjustable height motorized desk', 12, 349.99, 3),
                                            ('Yoga Mat', 'Non-slip 6mm extra thick mat', 200, 19.95, 4),
                                            ('Coffee Maker', 'Programmable 12-cup drip brewer', 25, 45.00, 2),
                                            ('Notebook Set', 'Pack of 3 hardcover ruled notebooks', 500, 12.50, 3),
                                            ('Dumbbell Set', 'Adjustable weight set (up to 50 lbs)', 15, 199.00, 4);