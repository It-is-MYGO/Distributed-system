package com.example.distributedsystem.config;

import com.example.distributedsystem.dto.ProductCreateRequest;
import com.example.distributedsystem.entity.Category;
import com.example.distributedsystem.entity.Product;
import com.example.distributedsystem.entity.ProductDetailImage;
import com.example.distributedsystem.entity.ProductReview;
import com.example.distributedsystem.entity.User;
import com.example.distributedsystem.mapper.CategoryMapper;
import com.example.distributedsystem.mapper.ProductDetailImageMapper;
import com.example.distributedsystem.mapper.ProductMapper;
import com.example.distributedsystem.mapper.ProductReviewMapper;
import com.example.distributedsystem.mapper.UserMapper;
import com.example.distributedsystem.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class DemoDataInitializer implements CommandLineRunner {
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductDetailImageMapper productDetailImageMapper;
    private final ProductReviewMapper productReviewMapper;
    private final ProductService productService;
    private final PasswordEncoder passwordEncoder;
    private final String instanceId;
    private final JdbcTemplate jdbcTemplate;

    public DemoDataInitializer(
            UserMapper userMapper,
            CategoryMapper categoryMapper,
            ProductMapper productMapper,
            ProductDetailImageMapper productDetailImageMapper,
            ProductReviewMapper productReviewMapper,
            ProductService productService,
            PasswordEncoder passwordEncoder,
            @Value("${app.instance-id:local}") String instanceId,
            JdbcTemplate jdbcTemplate
    ) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.productDetailImageMapper = productDetailImageMapper;
        this.productReviewMapper = productReviewMapper;
        this.productService = productService;
        this.passwordEncoder = passwordEncoder;
        this.instanceId = instanceId;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        if (!"local".equals(instanceId) && !"app-1".equals(instanceId)) {
            return;
        }
        migrateColumns();
        seedUsers();
        backfillDemoAvatars();
        seedCategories();
        seedProducts();
        seedSupplementProducts();
        seedProductImages();
        seedProductReviews();
        ensureReviewMix();
        seedCoupons();
        backfillSales();
        seedSeckillWindows();
        seedGovernanceConfig();
    }

    private void migrateColumns() {
        addColumnIfMissing("orders", "receiver_name", "ALTER TABLE orders ADD COLUMN receiver_name VARCHAR(100)");
        addColumnIfMissing("orders", "receiver_phone", "ALTER TABLE orders ADD COLUMN receiver_phone VARCHAR(30)");
        addColumnIfMissing("orders", "paid_at", "ALTER TABLE orders ADD COLUMN paid_at TIMESTAMP NULL");
        addColumnIfMissing("orders", "carrier", "ALTER TABLE orders ADD COLUMN carrier VARCHAR(80)");
        addColumnIfMissing("orders", "tracking_no", "ALTER TABLE orders ADD COLUMN tracking_no VARCHAR(80)");
        addColumnIfMissing("orders", "logistics_status", "ALTER TABLE orders ADD COLUMN logistics_status VARCHAR(30)");
        addColumnIfMissing("orders", "delivered_at", "ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP NULL");
        addColumnIfMissing("orders", "completed_at", "ALTER TABLE orders ADD COLUMN completed_at TIMESTAMP NULL");
        addColumnIfMissing("orders", "reviewed", "ALTER TABLE orders ADD COLUMN reviewed TINYINT DEFAULT 0");
        addColumnIfMissing("user_account", "avatar_url", "ALTER TABLE user_account ADD COLUMN avatar_url LONGTEXT");
        jdbcTemplate.execute("ALTER TABLE user_account MODIFY COLUMN avatar_url LONGTEXT");
        addColumnIfMissing("user_account", "email", "ALTER TABLE user_account ADD COLUMN email VARCHAR(120)");
        addColumnIfMissing("user_account", "phone", "ALTER TABLE user_account ADD COLUMN phone VARCHAR(30)");
        addColumnIfMissing("product_review", "avatar_url", "ALTER TABLE product_review ADD COLUMN avatar_url VARCHAR(500)");
        addColumnIfMissing("product_review", "order_id", "ALTER TABLE product_review ADD COLUMN order_id BIGINT");
        addColumnIfMissing("product_review", "followup_content", "ALTER TABLE product_review ADD COLUMN followup_content VARCHAR(500)");
        addColumnIfMissing("product_review", "followup_at", "ALTER TABLE product_review ADD COLUMN followup_at TIMESTAMP NULL");
        addColumnIfMissing("product", "sales_count", "ALTER TABLE product ADD COLUMN sales_count INT NOT NULL DEFAULT 0");
        addColumnIfMissing("product", "original_price", "ALTER TABLE product ADD COLUMN original_price DECIMAL(10,2)");
        addColumnIfMissing("product", "seckill_price", "ALTER TABLE product ADD COLUMN seckill_price DECIMAL(10,2)");
        addColumnIfMissing("product", "seckill_start_at", "ALTER TABLE product ADD COLUMN seckill_start_at TIMESTAMP NULL");
        addColumnIfMissing("product", "seckill_end_at", "ALTER TABLE product ADD COLUMN seckill_end_at TIMESTAMP NULL");
        normalizeShoppingCartItems();
        jdbcTemplate.execute("""
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
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS governance_config (
                  config_key VARCHAR(100) PRIMARY KEY,
                  config_value VARCHAR(500) NOT NULL,
                  description VARCHAR(255),
                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
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
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS user_coupon (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id BIGINT NOT NULL,
                  coupon_id BIGINT NOT NULL,
                  status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
                  issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  used_at TIMESTAMP NULL
                )
                """);
    }

    private void normalizeShoppingCartItems() {
        jdbcTemplate.update("DELETE FROM shopping_cart_item WHERE is_deleted = 1");
        jdbcTemplate.update("""
                UPDATE shopping_cart_item target
                JOIN (
                  SELECT user_id, product_id, MIN(id) AS keep_id, SUM(quantity) AS total_quantity
                  FROM shopping_cart_item
                  GROUP BY user_id, product_id
                  HAVING COUNT(*) > 1
                ) merged ON target.id = merged.keep_id
                SET target.quantity = merged.total_quantity
                """);
        jdbcTemplate.update("""
                DELETE target FROM shopping_cart_item target
                JOIN (
                  SELECT user_id, product_id, MIN(id) AS keep_id
                  FROM shopping_cart_item
                  GROUP BY user_id, product_id
                  HAVING COUNT(*) > 1
                ) merged ON target.user_id = merged.user_id
                         AND target.product_id = merged.product_id
                         AND target.id <> merged.keep_id
                """);
        if (!hasIndex("shopping_cart_item", "uk_cart_user_product")) {
            jdbcTemplate.execute("ALTER TABLE shopping_cart_item ADD UNIQUE KEY uk_cart_user_product (user_id, product_id)");
        }
    }

    private boolean hasIndex(String tableName, String indexName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class,
                    tableName,
                    indexName
            );
            return count != null && count > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        if (!hasColumn(tableName, columnName)) {
            jdbcTemplate.execute(ddl);
        }
    }

    private boolean hasColumn(String tableName, String columnName) {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            if (columns.next()) {
                return true;
            }
        } catch (SQLException ignored) {
        }
        try (Connection connection = jdbcTemplate.getDataSource().getConnection();
             ResultSet columns = connection.getMetaData().getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            return columns.next();
        } catch (SQLException e) {
            throw new IllegalStateException("检查表字段失败：" + tableName + "." + columnName, e);
        }
    }

    private void seedUsers() {
        createUserIfAbsent("student", "123456", "USER", "普通用户", "北京市海淀区中关村软件园 1 号", demoAvatar("student"));
        createUserIfAbsent("admin", "admin123", "ADMIN", "管理员", "上海市浦东新区秒杀运营中心", demoAvatar("admin"));
    }

    private void createUserIfAbsent(String username, String password, String role, String nickName, String address, String avatarUrl) {
        if (userMapper.findByUsername(username) != null) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setNickName(nickName);
        user.setAddress(address);
        user.setAvatarUrl(avatarUrl);
        user.setLockedFlag(false);
        userMapper.insert(user);
    }

    private void backfillDemoAvatars() {
        jdbcTemplate.update("UPDATE user_account SET nick_name = '普通用户', avatar_url = ? WHERE username = ? AND (avatar_url IS NULL OR avatar_url = '' OR nick_name = '测试用户')", demoAvatar("student"), "student");
        jdbcTemplate.update("UPDATE user_account SET avatar_url = ? WHERE username = ? AND (avatar_url IS NULL OR avatar_url = '')", demoAvatar("admin"), "admin");
        jdbcTemplate.update("UPDATE product_review SET rating = 4 WHERE username = 'm***2'");
        jdbcTemplate.update("UPDATE product_review SET rating = 2 WHERE username = 'a***n'");
        jdbcTemplate.update("""
                UPDATE product_review
                SET avatar_url = CASE
                    WHEN username = 's***7' THEN ?
                    WHEN username = 'm***2' THEN ?
                    WHEN username = 'a***n' THEN ?
                    WHEN username = 'c***5' THEN ?
                    ELSE ?
                END
                WHERE avatar_url IS NULL OR avatar_url = ''
                """, demoAvatar("s7"), demoAvatar("m2"), demoAvatar("an"), demoAvatar("c5"), demoAvatar("buyer"));
    }

    private String demoAvatar(String seed) {
        return "https://api.dicebear.com/8.x/thumbs/svg?seed=" + seed;
    }

    private void seedCategories() {
        List<String> names = List.of(
                "数码办公", "智能家电", "美妆个护", "运动生活",
                "冰洗", "空调", "电视", "厨卫大电", "电脑", "办公", "文具用品",
                "手机", "运营商", "数码", "生活电器", "厨房小电", "个护健康",
                "食品", "酒类", "生鲜", "特产", "美妆", "个护清洁", "宠物",
                "元器件", "劳保物资", "五金机电", "家装", "建材", "家具",
                "家居日用", "厨具", "男鞋", "运动", "户外", "男装", "女装", "童装", "内衣"
        );
        for (int i = 0; i < names.size(); i++) {
            if (categoryIdByName(names.get(i)) != null) {
                continue;
            }
            Category category = new Category();
            category.setCategoryLevel(1);
            category.setParentId(0L);
            category.setName(names.get(i));
            category.setCategoryRank((i + 1) * 10);
            category.setIsDeleted(false);
            categoryMapper.insert(category);
        }
    }

    private void seedProducts() {
        if (!productMapper.findAll().isEmpty()) {
            return;
        }

        // Demo photos are direct Pexels image URLs collected from free stock photo pages.
        List<SeedProduct> products = List.of(
                new SeedProduct("旗舰降噪耳机 Pro", "数码秒杀爆款，蓝牙连接、主动降噪，适合通勤和学习。", "https://images.pexels.com/photos/17726845/pexels-photo-17726845.jpeg?auto=compress&cs=tinysrgb&w=900", "限时秒杀", 599, 1L, 80),
                new SeedProduct("轻薄办公笔记本 Air 14", "高色域屏幕、长续航键盘手感，适合课程设计和日常办公。", "https://images.pexels.com/photos/5584136/pexels-photo-5584136.jpeg?auto=compress&cs=tinysrgb&w=900", "新品", 4299, 1L, 35),
                new SeedProduct("双屏效率工作站套装", "显示器、键鼠、桌面设备组合，打造高效宿舍/办公室工位。", "https://images.pexels.com/photos/21325132/pexels-photo-21325132.jpeg?auto=compress&cs=tinysrgb&w=900", "热卖", 2699, 1L, 22),
                new SeedProduct("智能电饭煲 4L", "预约煮饭、多菜单模式，适合宿舍和家庭使用。", "https://images.pexels.com/photos/11133944/pexels-photo-11133944.jpeg?auto=compress&cs=tinysrgb&w=900", "家电秒杀", 299, 2L, 120),
                new SeedProduct("复古厨房电饭煲套装", "厨房场景精选款，库存充足，可演示高并发下单扣减。", "https://images.pexels.com/photos/31479830/pexels-photo-31479830.jpeg?auto=compress&cs=tinysrgb&w=900", "满减", 399, 2L, 66),
                new SeedProduct("意式咖啡机 Barista", "不锈钢机身、奶泡蒸汽管，适合咖啡爱好者。", "https://images.pexels.com/photos/5825346/pexels-photo-5825346.jpeg?auto=compress&cs=tinysrgb&w=900", "精选", 1299, 2L, 18),
                new SeedProduct("焕亮保湿精华套装", "温和补水、简洁包装，美妆个护频道秒杀款。", "https://images.pexels.com/photos/8100775/pexels-photo-8100775.jpeg?auto=compress&cs=tinysrgb&w=900", "个护", 169, 3L, 140),
                new SeedProduct("桌面手机平板支架", "金属折叠支架，适合直播、网课和桌面办公。", "https://images.pexels.com/photos/4405372/pexels-photo-4405372.jpeg?auto=compress&cs=tinysrgb&w=900", "低价", 49, 1L, 300)
        );

        products.forEach(product -> productService.create(toRequest(product)));
    }

    private void seedSupplementProducts() {
        List<SeedProduct> products = List.of(
                new SeedProduct("高清学习平板 11 英寸", "金属机身、护眼屏幕，适合网课和轻办公。", "https://images.pexels.com/photos/5082579/pexels-photo-5082579.jpeg?auto=compress&cs=tinysrgb&w=900", "数码热卖", 1599, 1L, 54),
                new SeedProduct("机械键盘青轴套装", "PBT 键帽、白色背光，桌面办公和游戏都适用。", "https://images.pexels.com/photos/2115257/pexels-photo-2115257.jpeg?auto=compress&cs=tinysrgb&w=900", "榜单", 229, 1L, 96),
                new SeedProduct("便携移动电源 20000mAh", "双向快充，适合通勤、旅行和露营。", "https://images.pexels.com/photos/4526407/pexels-photo-4526407.jpeg?auto=compress&cs=tinysrgb&w=900", "数码配件", 129, 1L, 180),
                new SeedProduct("智能扫地机器人", "激光导航、自动回充，适合家庭清洁。", "https://images.pexels.com/photos/4108715/pexels-photo-4108715.jpeg?auto=compress&cs=tinysrgb&w=900", "家电热卖", 1899, 2L, 38),
                new SeedProduct("小型空气炸锅 5L", "少油烹饪，多菜单模式，适合宿舍和家庭。", "https://images.pexels.com/photos/6996085/pexels-photo-6996085.jpeg?auto=compress&cs=tinysrgb&w=900", "厨房电器", 369, 2L, 74),
                new SeedProduct("恒温电热水壶", "快速烧水、保温模式，办公家用都方便。", "https://images.pexels.com/photos/4051401/pexels-photo-4051401.jpeg?auto=compress&cs=tinysrgb&w=900", "生活电器", 159, 2L, 130),
                new SeedProduct("氨基酸洁面套装", "温和清洁，适合日常护肤清洁。", "https://images.pexels.com/photos/6621462/pexels-photo-6621462.jpeg?auto=compress&cs=tinysrgb&w=900", "个护清洁", 89, 3L, 210),
                new SeedProduct("修护面霜 50g", "清爽质地，适合换季保湿修护。", "https://images.pexels.com/photos/7796509/pexels-photo-7796509.jpeg?auto=compress&cs=tinysrgb&w=900", "美妆精选", 139, 3L, 160),
                new SeedProduct("香氛沐浴露家庭装", "持久留香，大容量家庭装。", "https://images.pexels.com/photos/6621329/pexels-photo-6621329.jpeg?auto=compress&cs=tinysrgb&w=900", "身体护理", 79, 3L, 250),
                new SeedProduct("缓震跑步鞋", "轻量鞋面、缓震中底，适合日常跑步训练。", "https://images.pexels.com/photos/2529148/pexels-photo-2529148.jpeg?auto=compress&cs=tinysrgb&w=900", "运动热卖", 399, 4L, 88),
                new SeedProduct("瑜伽训练垫", "加厚防滑，适合居家训练。", "https://images.pexels.com/photos/4498151/pexels-photo-4498151.jpeg?auto=compress&cs=tinysrgb&w=900", "运动生活", 99, 4L, 170),
                new SeedProduct("户外保温杯 500ml", "不锈钢内胆，适合通勤和户外。", "https://images.pexels.com/photos/1188649/pexels-photo-1188649.jpeg?auto=compress&cs=tinysrgb&w=900", "户外精选", 69, 4L, 260)
        );
        products.forEach(product -> {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product WHERE name = ?", Integer.class, product.name());
            if (count == null || count == 0) {
                productService.create(toRequest(product));
            }
        });
        seedCategoryKeywordProducts();
    }

    private void seedCategoryKeywordProducts() {
        List<NamedSeedProduct> products = List.of(
                new NamedSeedProduct("冰洗", "一级能效冰箱 520L", "风冷无霜、变频保鲜，适合家庭囤货。", "https://images.pexels.com/photos/5825576/pexels-photo-5825576.jpeg?auto=compress&cs=tinysrgb&w=900", "冰洗补贴", 2899, 46),
                new NamedSeedProduct("空调", "新风变频空调 1.5 匹", "快速制冷制热，卧室客厅都适用。", "https://images.pexels.com/photos/7195853/pexels-photo-7195853.jpeg?auto=compress&cs=tinysrgb&w=900", "空调热卖", 2399, 58),
                new NamedSeedProduct("电视", "MiniLED 智能电视 65 英寸", "高刷大屏，适合影音娱乐和主机游戏。", "https://images.pexels.com/photos/6976094/pexels-photo-6976094.jpeg?auto=compress&cs=tinysrgb&w=900", "电视新品", 3299, 41),
                new NamedSeedProduct("厨卫大电", "嵌入式洗碗机 13 套", "高温除菌、节水洗涤，厨房升级精选。", "https://images.pexels.com/photos/1457841/pexels-photo-1457841.jpeg?auto=compress&cs=tinysrgb&w=900", "厨卫大电", 2599, 35),
                new NamedSeedProduct("电脑", "游戏台式主机 RTX 套装", "高性能显卡和高速固态，适合游戏与设计。", "https://images.pexels.com/photos/2582937/pexels-photo-2582937.jpeg?auto=compress&cs=tinysrgb&w=900", "电脑热卖", 5999, 28),
                new NamedSeedProduct("办公", "人体工学办公椅", "腰托可调，适合久坐学习办公。", "https://images.pexels.com/photos/1957478/pexels-photo-1957478.jpeg?auto=compress&cs=tinysrgb&w=900", "办公精选", 699, 120),
                new NamedSeedProduct("文具用品", "考试文具收纳套装", "中性笔、便签和文件夹组合装。", "https://images.pexels.com/photos/590493/pexels-photo-590493.jpeg?auto=compress&cs=tinysrgb&w=900", "文具用品", 39, 260),
                new NamedSeedProduct("手机", "轻薄影像手机 Pro", "高刷屏、长续航和夜景影像。", "https://images.pexels.com/photos/1092644/pexels-photo-1092644.jpeg?auto=compress&cs=tinysrgb&w=900", "手机秒杀", 3299, 64),
                new NamedSeedProduct("运营商", "校园流量卡套餐", "大流量月包，适合学生和通勤用户。", "https://images.pexels.com/photos/404280/pexels-photo-404280.jpeg?auto=compress&cs=tinysrgb&w=900", "运营商", 59, 500),
                new NamedSeedProduct("数码", "4K 运动相机", "防抖拍摄，适合旅行骑行记录。", "https://images.pexels.com/photos/1203808/pexels-photo-1203808.jpeg?auto=compress&cs=tinysrgb&w=900", "数码新品", 899, 90),
                new NamedSeedProduct("生活电器", "除螨无线吸尘器", "轻量机身，多刷头清洁宿舍和家庭。", "https://images.pexels.com/photos/4107120/pexels-photo-4107120.jpeg?auto=compress&cs=tinysrgb&w=900", "生活电器", 799, 88),
                new NamedSeedProduct("厨房小电", "多功能早餐机", "煎烤蒸煮一体，小户型厨房友好。", "https://images.pexels.com/photos/6287525/pexels-photo-6287525.jpeg?auto=compress&cs=tinysrgb&w=900", "厨房小电", 199, 150),
                new NamedSeedProduct("个护健康", "智能筋膜按摩仪", "多档力度，运动后放松肩颈腿部。", "https://images.pexels.com/photos/4498482/pexels-photo-4498482.jpeg?auto=compress&cs=tinysrgb&w=900", "个护健康", 299, 110),
                new NamedSeedProduct("食品", "坚果零食礼盒", "每日坚果、果干和饼干组合。", "https://images.pexels.com/photos/1295572/pexels-photo-1295572.jpeg?auto=compress&cs=tinysrgb&w=900", "食品热卖", 89, 320),
                new NamedSeedProduct("酒类", "低度果酒礼盒", "微醺口感，聚餐礼赠都合适。", "https://images.pexels.com/photos/1283219/pexels-photo-1283219.jpeg?auto=compress&cs=tinysrgb&w=900", "酒类精选", 129, 90),
                new NamedSeedProduct("生鲜", "冷链牛排组合", "原切牛排，冷链配送到家。", "https://images.pexels.com/photos/361184/asparagus-steak-veal-steak-veal-361184.jpeg?auto=compress&cs=tinysrgb&w=900", "生鲜冷链", 168, 70),
                new NamedSeedProduct("特产", "地方糕点特产礼盒", "传统点心，节日送礼更体面。", "https://images.pexels.com/photos/1126359/pexels-photo-1126359.jpeg?auto=compress&cs=tinysrgb&w=900", "特产礼盒", 98, 160),
                new NamedSeedProduct("美妆", "柔雾持妆粉底液", "自然遮瑕，通勤妆容持久。", "https://images.pexels.com/photos/3373746/pexels-photo-3373746.jpeg?auto=compress&cs=tinysrgb&w=900", "美妆精选", 159, 130),
                new NamedSeedProduct("个护清洁", "家庭清洁洗护套装", "洗衣液、消毒液和清洁喷雾组合。", "https://images.pexels.com/photos/4239031/pexels-photo-4239031.jpeg?auto=compress&cs=tinysrgb&w=900", "个护清洁", 79, 260),
                new NamedSeedProduct("宠物", "宠物自动喂食器", "定时定量投喂，适合猫狗家庭。", "https://images.pexels.com/photos/4587998/pexels-photo-4587998.jpeg?auto=compress&cs=tinysrgb&w=900", "宠物好物", 199, 95),
                new NamedSeedProduct("元器件", "开发板传感器套件", "适合课程实验和物联网开发。", "https://images.pexels.com/photos/163100/circuit-circuit-board-resistor-computer-163100.jpeg?auto=compress&cs=tinysrgb&w=900", "元器件", 129, 180),
                new NamedSeedProduct("劳保物资", "防护手套口罩套装", "车间实验室常备防护物资。", "https://images.pexels.com/photos/4483610/pexels-photo-4483610.jpeg?auto=compress&cs=tinysrgb&w=900", "劳保物资", 49, 400),
                new NamedSeedProduct("五金机电", "家用电钻工具箱", "多批头组合，家具安装维修常用。", "https://images.pexels.com/photos/162553/keys-workshop-mechanic-tools-162553.jpeg?auto=compress&cs=tinysrgb&w=900", "五金机电", 299, 75),
                new NamedSeedProduct("家装", "免打孔置物架套装", "浴室厨房通用，安装方便。", "https://images.pexels.com/photos/6585751/pexels-photo-6585751.jpeg?auto=compress&cs=tinysrgb&w=900", "家装精选", 69, 210),
                new NamedSeedProduct("建材", "环保乳胶漆 5L", "低气味墙面翻新，覆盖力强。", "https://images.pexels.com/photos/5691622/pexels-photo-5691622.jpeg?auto=compress&cs=tinysrgb&w=900", "建材", 239, 65),
                new NamedSeedProduct("家具", "北欧实木床头柜", "圆角设计，卧室收纳小家具。", "https://images.pexels.com/photos/1866149/pexels-photo-1866149.jpeg?auto=compress&cs=tinysrgb&w=900", "家具", 399, 48),
                new NamedSeedProduct("家居日用", "四件套床品套装", "亲肤面料，可机洗易打理。", "https://images.pexels.com/photos/6315806/pexels-photo-6315806.jpeg?auto=compress&cs=tinysrgb&w=900", "家居日用", 199, 130),
                new NamedSeedProduct("厨具", "不粘锅三件套", "煎炒炖多场景厨房套装。", "https://images.pexels.com/photos/6996095/pexels-photo-6996095.jpeg?auto=compress&cs=tinysrgb&w=900", "厨具", 259, 85),
                new NamedSeedProduct("男鞋", "轻便休闲男鞋", "透气鞋面，日常通勤舒适。", "https://images.pexels.com/photos/19090/pexels-photo.jpg?auto=compress&cs=tinysrgb&w=900", "男鞋", 199, 140),
                new NamedSeedProduct("运动", "速干运动 T 恤", "吸湿排汗，跑步健身适用。", "https://images.pexels.com/photos/3757376/pexels-photo-3757376.jpeg?auto=compress&cs=tinysrgb&w=900", "运动", 79, 300),
                new NamedSeedProduct("户外", "露营折叠椅", "轻量便携，户外野餐露营可用。", "https://images.pexels.com/photos/6271625/pexels-photo-6271625.jpeg?auto=compress&cs=tinysrgb&w=900", "户外", 119, 120),
                new NamedSeedProduct("男装", "基础款纯棉卫衣", "简洁版型，春秋通勤休闲。", "https://images.pexels.com/photos/6311390/pexels-photo-6311390.jpeg?auto=compress&cs=tinysrgb&w=900", "男装", 159, 190),
                new NamedSeedProduct("女装", "通勤针织开衫", "柔软亲肤，办公室和日常都适合。", "https://images.pexels.com/photos/6311397/pexels-photo-6311397.jpeg?auto=compress&cs=tinysrgb&w=900", "女装", 169, 170),
                new NamedSeedProduct("童装", "儿童连帽外套", "舒适保暖，校园日常穿搭。", "https://images.pexels.com/photos/5693891/pexels-photo-5693891.jpeg?auto=compress&cs=tinysrgb&w=900", "童装", 129, 130),
                new NamedSeedProduct("内衣", "无痕基础内衣套装", "柔软面料，日常舒适穿着。", "https://images.pexels.com/photos/6311615/pexels-photo-6311615.jpeg?auto=compress&cs=tinysrgb&w=900", "内衣", 99, 160)
        );
        products.forEach(product -> {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product WHERE name = ?", Integer.class, product.name());
            if (count != null && count > 0) {
                return;
            }
            Long categoryId = categoryIdByName(product.categoryName());
            if (categoryId == null) {
                return;
            }
            productService.create(toRequest(new SeedProduct(product.name(), product.description(), product.coverImage(), product.tag(), product.price(), categoryId, product.stock())));
        });
    }

    private Long categoryIdByName(String name) {
        return categoryMapper.findAll().stream()
                .filter(category -> name.equals(category.getName()))
                .map(Category::getId)
                .findFirst()
                .orElse(null);
    }

    private ProductCreateRequest toRequest(SeedProduct seed) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName(seed.name());
        request.setDescription(seed.description());
        request.setCoverImage(seed.coverImage());
        request.setTag(seed.tag());
        request.setPrice(BigDecimal.valueOf(seed.price()));
        request.setOriginalPrice(BigDecimal.valueOf(seed.price()));
        request.setCategoryId(seed.categoryId());
        request.setStock(seed.stock());
        request.setStatus("ACTIVE");
        return request;
    }

    private void seedProductImages() {
        List<String> galleryPool = List.of(
                "https://images.pexels.com/photos/3737594/pexels-photo-3737594.jpeg?auto=compress&cs=tinysrgb&w=900",
                "https://images.pexels.com/photos/5076516/pexels-photo-5076516.jpeg?auto=compress&cs=tinysrgb&w=900",
                "https://images.pexels.com/photos/5825346/pexels-photo-5825346.jpeg?auto=compress&cs=tinysrgb&w=900",
                "https://images.pexels.com/photos/8100775/pexels-photo-8100775.jpeg?auto=compress&cs=tinysrgb&w=900"
        );
        List<String> detailPool = List.of(
                "https://images.pexels.com/photos/7679862/pexels-photo-7679862.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/5797991/pexels-photo-5797991.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/5632381/pexels-photo-5632381.jpeg?auto=compress&cs=tinysrgb&w=1200",
                "https://images.pexels.com/photos/7319307/pexels-photo-7319307.jpeg?auto=compress&cs=tinysrgb&w=1200"
        );
        List<Product> products = productMapper.findAll();
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM product_detail_image WHERE product_id = ?",
                    Integer.class,
                    product.getId()
            );
            if (count != null && count > 0) {
                continue;
            }
            insertImage(product.getId(), product.getCoverImage(), "GALLERY", 0);
            insertImage(product.getId(), galleryPool.get(i % galleryPool.size()), "GALLERY", 1);
            insertImage(product.getId(), galleryPool.get((i + 1) % galleryPool.size()), "GALLERY", 2);
            insertImage(product.getId(), detailPool.get(i % detailPool.size()), "DETAIL", 10);
            insertImage(product.getId(), detailPool.get((i + 2) % detailPool.size()), "DETAIL", 11);
        }
    }

    private void insertImage(Long productId, String imageUrl, String imageType, int sortOrder) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        ProductDetailImage image = new ProductDetailImage();
        image.setProductId(productId);
        image.setImageUrl(imageUrl);
        image.setImageType(imageType);
        image.setSortOrder(sortOrder);
        productDetailImageMapper.insert(image);
    }

    private void seedProductReviews() {
        if (productReviewMapper.countAll() > 0) {
            return;
        }
        List<Product> products = productMapper.findAll();
        for (Product product : products) {
            insertReview(product.getId(), "s***7", 5, product.getName() + " 到货很快，包装完整，秒杀价很划算，整体好评。", demoAvatar("s7"));
            insertReview(product.getId(), "m***2", 4, "下单流程很顺，库存扣减能实时看到，适合演示商城和秒杀功能。", demoAvatar("m2"));
            insertReview(product.getId(), "c***5", 3, "商品整体符合预期，配送速度正常，后续再观察使用体验。", demoAvatar("c5"));
            insertReview(product.getId(), "a***n", 2, "包装有轻微压痕，但客服处理比较及时，系统保留差评比例演示。", demoAvatar("an"));
        }
    }

    private void ensureReviewMix() {
        List<Product> products = productMapper.findAll();
        for (Product product : products) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM product_review WHERE product_id = ? AND username = 'c***5'",
                    Integer.class,
                    product.getId()
            );
            if (count == null || count == 0) {
                insertReview(product.getId(), "c***5", 3, "商品整体符合预期，配送速度正常，后续再观察使用体验。", demoAvatar("c5"));
            }
        }
        jdbcTemplate.update("UPDATE product_review SET avatar_url = ? WHERE username = 'c***5' AND (avatar_url IS NULL OR avatar_url = '')", demoAvatar("c5"));
    }

    private void seedCoupons() {
        createCouponIfAbsent("SAVE20", "满199减20", 199, 20, null, null);
        createCouponIfAbsent("SAVE30", "满299减30", 299, 30, null, null);
        createCouponIfAbsent("LESS8", "无门槛立减8元", 0, 8, null, null);
        createCouponIfAbsent("DIGITAL50", "数码品类满500减50", 500, 50, 1L, null);
        createCouponIfAbsent("HOME25", "家电品类满299减25", 299, 25, 2L, null);
        createCouponIfAbsent("BEAUTY15", "美妆个护满129减15", 129, 15, 3L, null);
        createCouponIfAbsent("PHONE10", "桌面支架单品立减10", 0, 10, null, 8L);
        issueCouponIfAbsent("student", "SAVE20");
        issueCouponIfAbsent("student", "SAVE30");
        issueCouponIfAbsent("student", "LESS8");
        issueCouponIfAbsent("student", "DIGITAL50");
        issueCouponIfAbsent("student", "HOME25");
        issueCouponIfAbsent("student", "BEAUTY15");
        issueCouponIfAbsent("student", "PHONE10");
        issueCouponIfAbsent("admin", "SAVE20");
        issueCouponIfAbsent("admin", "LESS8");
        issueCouponIfAbsent("admin", "DIGITAL50");
    }

    private void createCouponIfAbsent(String code, String title, int threshold, int discount, Long categoryId, Long productId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM coupon WHERE code = ?", Integer.class, code);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO coupon(code, title, threshold_amount, discount_amount, category_id, product_id, status) VALUES(?, ?, ?, ?, ?, ?, 'ACTIVE')",
                code,
                title,
                BigDecimal.valueOf(threshold),
                BigDecimal.valueOf(discount),
                categoryId,
                productId
        );
    }

    private void issueCouponIfAbsent(String username, String code) {
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM user_account WHERE username = ?", Long.class, username);
        Long couponId = jdbcTemplate.queryForObject("SELECT id FROM coupon WHERE code = ?", Long.class, code);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_coupon WHERE user_id = ? AND coupon_id = ? AND status = 'UNUSED'",
                Integer.class,
                userId,
                couponId
        );
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO user_coupon(user_id, coupon_id, status) VALUES(?, ?, 'UNUSED')", userId, couponId);
        }
    }

    private void backfillSales() {
        List<Product> products = productMapper.findAll();
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            if (product.getSalesCount() == null || product.getSalesCount() <= 0) {
                jdbcTemplate.update("UPDATE product SET sales_count = ? WHERE id = ?", Math.max(120, 3200 - i * 173), product.getId());
            }
        }
    }

    private void seedGovernanceConfig() {
        upsertConfig("traffic.rate-limit.enabled", "true", "是否开启API限流");
        upsertConfig("traffic.rate-limit.permits-per-second", "35", "单实例每秒允许请求数");
        upsertConfig("traffic.circuit-breaker.enabled", "true", "是否开启熔断");
        upsertConfig("traffic.circuit-breaker.failure-threshold", "8", "滚动窗口失败次数阈值");
        upsertConfig("traffic.circuit-breaker.open-seconds", "20", "熔断打开时长");
        upsertConfig("traffic.degrade.enabled", "true", "是否开启降级响应");
        upsertConfig("traffic.degrade.path-prefix", "/api/product/search", "降级保护接口前缀");
        upsertConfig("nacos.server-addr", "http://nacos:8848", "Nacos服务地址");
    }

    private void upsertConfig(String key, String value, String description) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM governance_config WHERE config_key = ?", Integer.class, key);
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO governance_config(config_key, config_value, description) VALUES(?, ?, ?)", key, value, description);
        }
    }

    private void seedSeckillWindows() {
        jdbcTemplate.update("UPDATE product SET original_price = price WHERE original_price IS NULL");
        jdbcTemplate.update("""
                UPDATE product
                SET seckill_price = ROUND(original_price * 0.82, 2),
                    seckill_start_at = DATE_SUB(NOW(), INTERVAL 10 MINUTE),
                    seckill_end_at = DATE_ADD(NOW(), INTERVAL 36 HOUR),
                    tag = '正在秒杀'
                WHERE id IN (
                  SELECT id FROM (
                    SELECT id FROM product ORDER BY sales_count DESC LIMIT 4
                  ) AS active_seckill_products
                )
                """);
        jdbcTemplate.update("""
                UPDATE product
                SET seckill_price = ROUND(original_price * 0.86, 2),
                    seckill_start_at = DATE_SUB(NOW(), INTERVAL 30 MINUTE),
                    seckill_end_at = DATE_ADD(NOW(), INTERVAL 48 HOUR)
                WHERE seckill_price IS NULL
                  AND (
                    tag LIKE '%秒杀%'
                    OR id IN (
                      SELECT id FROM (
                        SELECT id FROM product ORDER BY sales_count DESC LIMIT 4
                      ) AS hot_products
                    )
                  )
                """);
    }

    private void insertReview(Long productId, String username, int rating, String content, String avatarUrl) {
        ProductReview review = new ProductReview();
        review.setProductId(productId);
        review.setUsername(username);
        review.setRating(rating);
        review.setContent(content);
        review.setAvatarUrl(avatarUrl);
        productReviewMapper.insert(review);
    }

    private record SeedProduct(
            String name,
            String description,
            String coverImage,
            String tag,
            int price,
            Long categoryId,
            int stock
    ) {
    }

    private record NamedSeedProduct(
            String categoryName,
            String name,
            String description,
            String coverImage,
            String tag,
            int price,
            int stock
    ) {
    }
}
