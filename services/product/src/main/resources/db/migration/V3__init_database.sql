-- Insert sample categories
insert into category (id, name, description)
values
    (nextval('category_seq'), 'Electronics', 'Devices and gadgets'),
    (nextval('category_seq'), 'Books', 'Various genres of books'),
    (nextval('category_seq'), 'Clothing', 'Apparel and accessories');

-- Retrieve category IDs for reference
-- (Assuming IDs are inserted in the order of sequence)
-- For simplicity, manually assigning IDs as if this is the first insert
-- Electronics = 1, Books = 51, Clothing = 101

-- Insert sample products
insert into product (id, name, description, available_quantity, price, category_id)
values
    (nextval('product_seq'), 'Smartphone', 'Latest model smartphone', 25, 699.99, 1),
    (nextval('product_seq'), 'Laptop', '15-inch display laptop', 10, 1199.49, 1),
    (nextval('product_seq'), 'Science Fiction Novel', 'Popular sci-fi book', 50, 15.99, 51),
    (nextval('product_seq'), 'History Textbook', 'Comprehensive history guide', 30, 45.00, 51),
    (nextval('product_seq'), 'T-shirt', 'Cotton t-shirt', 100, 9.99, 101),
    (nextval('product_seq'), 'Jeans', 'Slim fit jeans', 60, 39.95, 101);
