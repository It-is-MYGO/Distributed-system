CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  nick_name VARCHAR(100),
  introduce_sign VARCHAR(255),
  address VARCHAR(255),
  avatar_url VARCHAR(500),
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  locked_flag TINYINT DEFAULT 0,
  is_deleted TINYINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS goods_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_level TINYINT NOT NULL,
  parent_id BIGINT NOT NULL DEFAULT 0,
  name VARCHAR(100) NOT NULL,
  category_rank INT NOT NULL DEFAULT 0,
  is_deleted TINYINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  category_id BIGINT,
  description TEXT,
  cover_image VARCHAR(255),
  carousel_images TEXT,
  tag VARCHAR(50),
  sales_count INT NOT NULL DEFAULT 0,
  original_price DECIMAL(10,2),
  seckill_price DECIMAL(10,2),
  seckill_start_at TIMESTAMP NULL,
  seckill_end_at TIMESTAMP NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES goods_category(id)
);

CREATE TABLE IF NOT EXISTS product_detail_image (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  image_url VARCHAR(500) NOT NULL,
  image_type VARCHAR(20) NOT NULL DEFAULT 'GALLERY',
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_detail_image_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS product_review (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  username VARCHAR(100) NOT NULL,
  avatar_url VARCHAR(500),
  rating TINYINT NOT NULL DEFAULT 5,
  content VARCHAR(500) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_product_review_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS inventory (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL UNIQUE,
  stock INT NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(30) NOT NULL,
  receiver_name VARCHAR(100),
  receiver_phone VARCHAR(30),
  user_address VARCHAR(255),
  paid_at TIMESTAMP NULL,
  carrier VARCHAR(80),
  tracking_no VARCHAR(80),
  logistics_status VARCHAR(30),
  delivered_at TIMESTAMP NULL,
  completed_at TIMESTAMP NULL,
  reviewed TINYINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);

CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(120),
  product_cover_image VARCHAR(255),
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS shopping_cart_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  is_deleted TINYINT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE IF NOT EXISTS coupon (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(40) NOT NULL UNIQUE,
  title VARCHAR(120) NOT NULL,
  threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  discount_amount DECIMAL(10,2) NOT NULL,
  category_id BIGINT,
  product_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  coupon_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
  issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  used_at TIMESTAMP NULL,
  CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon(id)
);

CREATE TABLE IF NOT EXISTS seckill_transaction_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  request_id VARCHAR(80) NOT NULL UNIQUE,
  username VARCHAR(100) NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  redis_deducted TINYINT NOT NULL DEFAULT 0,
  db_deducted TINYINT NOT NULL DEFAULT 0,
  order_no VARCHAR(64),
  error_message VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS governance_config (
  config_key VARCHAR(100) PRIMARY KEY,
  config_value VARCHAR(500) NOT NULL,
  description VARCHAR(255),
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
