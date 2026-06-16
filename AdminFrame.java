package com.pos;
import javax.swing.*;

public class AdminFrame extends JFrame { 
    private final UserSession s; 

    public AdminFrame(UserSession s) {
        super(AppConstants.APP_NAME + " - Admin / Cashier Portal");
        this.s = s;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1550, 870);
        setLocationRelativeTo(null);
        build();
    }

    private CrudPanel crud(String title, String table, String id, String[] fields, String select, String search) {
        return new CrudPanel(new CrudConfig(title, table, id, fields, select, search), s);
    }

    private void build() {
        JTabbedPane t = new JTabbedPane(); 
        t.addTab("Dashboard", new DashboardPanel(s));
        t.addTab("POS Checkout", new PosCheckoutPanel(s));
        t.addTab("Customer Orders", new SalesProcessingPanel(s));
        t.addTab("Payments", new PaymentReviewPanel(s));
        t.addTab("Logistics & Marketplace", new AdminLogisticsPanel(s)); 
        t.addTab("Payments", new PaymentReviewPanel(s));
        t.addTab("Returns / Refunds", new ReturnRefundPanel(s));
        t.addTab("Returns / Refunds", new ReturnRefundPanel(s));
        t.addTab("Receive Stock", new ReceiveStockPanel(s));
        t.addTab("Stock Operations", new StockOperationsPanel(s));
        t.addTab("Cashier Shifts", new CashierShiftPanel(s));
        t.addTab("Accounts", new AccountManagementPanel(s));

        t.addTab("Customers", crud("Customer Profiles", "customer_profiles", "customer_id", new String[]{"user_id", "customer_code", "birth_date", "loyalty_number", "loyalty_points", "credit_limit", "status"}, "SELECT * FROM customer_profiles", "customer_code"));
        t.addTab("Employees", crud("Employee Profiles", "employee_profiles", "employee_id", new String[]{"user_id", "employee_code", "branch_id", "position", "hired_date", "status"}, "SELECT * FROM employee_profiles", "employee_code"));
        t.addTab("Branches", crud("Branches", "branches", "branch_id", new String[]{"branch_code", "branch_name", "address", "phone", "status"}, "SELECT * FROM branches", "branch_name"));
        t.addTab("Terminals", crud("Register Terminals", "register_terminals", "terminal_id", new String[]{"branch_id", "terminal_code", "terminal_name", "status"}, "SELECT * FROM register_terminals", "terminal_code"));
        t.addTab("Warehouses", crud("Warehouses", "warehouses", "warehouse_id", new String[]{"branch_id", "warehouse_code", "warehouse_name", "status"}, "SELECT * FROM warehouses", "warehouse_name"));
        t.addTab("Locations", crud("Storage Locations", "storage_locations", "location_id", new String[]{"warehouse_id", "location_code", "description", "status"}, "SELECT * FROM storage_locations", "location_code"));
        t.addTab("Categories", crud("Product Categories", "product_categories", "category_id", new String[]{"category_name", "description", "status"}, "SELECT * FROM product_categories", "category_name"));
        t.addTab("Brands", crud("Brands", "brands", "brand_id", new String[]{"brand_name", "description", "status"}, "SELECT * FROM brands", "brand_name"));
        t.addTab("Units", crud("Units of Measure", "units", "unit_id", new String[]{"unit_name", "abbreviation", "status"}, "SELECT * FROM units", "unit_name"));
        t.addTab("Products", crud("Product Master File", "products", "product_id", new String[]{"sku", "category_id", "brand_id", "unit_id", "product_name", "description", "cost_price", "selling_price", "taxable", "reorder_level", "status"}, "SELECT * FROM products", "product_name"));
        t.addTab("Barcodes", crud("Product Barcodes", "product_barcodes", "barcode_id", new String[]{"product_id", "barcode", "is_primary", "status"}, "SELECT * FROM product_barcodes", "barcode"));
        t.addTab("Prices", crud("Product Price History", "product_prices", "price_id", new String[]{"product_id", "price_type", "price_amount", "effective_from", "effective_to", "status"}, "SELECT * FROM product_prices", "product_id"));
        t.addTab("Suppliers", crud("Suppliers", "suppliers", "supplier_id", new String[]{"supplier_name", "contact_person", "phone", "email", "address", "status"}, "SELECT * FROM suppliers", "supplier_name"));
        t.addTab("Purchases", crud("Purchase Orders", "purchase_orders", "purchase_id", new String[]{"purchase_no", "supplier_id", "branch_id", "order_date", "expected_date", "total_amount", "status", "created_by", "notes"}, "SELECT * FROM purchase_orders", "purchase_no"));
        t.addTab("Purchase Items", crud("Purchase Order Items", "purchase_order_items", "purchase_item_id", new String[]{"purchase_id", "product_id", "quantity", "unit_cost", "line_total", "received_quantity"}, "SELECT * FROM purchase_order_items", "purchase_id"));
        t.addTab("Batches", crud("Product Batches", "product_batches", "batch_id", new String[]{"product_id", "location_id", "batch_number", "expiry_date", "quantity_received", "quantity_available", "unit_cost", "status"}, "SELECT * FROM product_batches", "batch_number"));
        t.addTab("Transfers", crud("Stock Transfers", "stock_transfers", "transfer_id", new String[]{"transfer_no", "from_location_id", "to_location_id", "transfer_date", "status", "created_by", "notes"}, "SELECT * FROM stock_transfers", "transfer_no"));
        t.addTab("Adjustments", crud("Stock Adjustments", "stock_adjustments", "adjustment_id", new String[]{"adjustment_no", "location_id", "adjustment_date", "reason", "status", "created_by"}, "SELECT * FROM stock_adjustments", "adjustment_no"));
        t.addTab("Balances", crud("Stock Balances", "stock_balances", "balance_id", new String[]{"product_id", "location_id", "quantity_on_hand", "reserved_quantity", "reorder_level"}, "SELECT * FROM stock_balances", "product_id"));
        t.addTab("Movements", crud("Stock Movements", "stock_movements", "movement_id", new String[]{"product_id", "movement_type", "reference_table", "reference_id", "quantity_change", "remarks", "created_by"}, "SELECT * FROM stock_movements", "movement_type"));
        t.addTab("Order Items", crud("Sales Order Items", "sales_order_items", "order_item_id", new String[]{"order_id", "product_id", "quantity", "unit_price", "discount_amount", "tax_amount", "line_total"}, "SELECT * FROM sales_order_items", "order_id"));
        t.addTab("Receipts", crud("Sales Receipts", "sales_receipts", "receipt_id", new String[]{"order_id", "receipt_no", "printed_at", "printed_by"}, "SELECT * FROM sales_receipts", "receipt_no"));
        t.addTab("Promotions", crud("Promotions", "promotions", "promotion_id", new String[]{"promo_code", "promo_name", "discount_type", "discount_value", "start_date", "end_date", "status"}, "SELECT * FROM promotions", "promo_name"));
        t.addTab("Promotion Items", crud("Promotion Products", "promotion_products", "promotion_product_id", new String[]{"promotion_id", "product_id"}, "SELECT * FROM promotion_products", "promotion_id"));
        t.addTab("Tax Settings", crud("Tax Settings", "tax_settings", "tax_id", new String[]{"tax_name", "tax_rate", "effective_from", "status"}, "SELECT * FROM tax_settings", "tax_name"));
        t.addTab("Drawer Movements", crud("Cash Drawer Movements", "drawer_movement_id", new String[]{"shift_id", "movement_type", "amount", "reference_table", "reference_id", "remarks", "created_by"}, "SELECT * FROM cash_drawer_movements", "movement_type"));
        t.addTab("Gift Cards", crud("Gift Cards", "gift_cards", "gift_card_id", new String[]{"gift_card_code", "customer_id", "original_amount", "remaining_amount", "expiry_date", "status"}, "SELECT * FROM gift_cards", "gift_card_code"));
        t.addTab("Customer Credit", crud("Customer Credit", "customer_credit", "credit_id", new String[]{"customer_id", "credit_limit", "outstanding_balance", "status"}, "SELECT * FROM customer_credit", "customer_id"));
        t.addTab("Inventory Counts", crud("Inventory Counts", "inventory_counts", "count_id", new String[]{"count_no", "location_id", "count_date", "status", "conducted_by", "remarks"}, "SELECT * FROM inventory_counts", "count_no"));
        t.addTab("Count Items", crud("Inventory Count Items", "inventory_count_items", "count_item_id", new String[]{"count_id", "product_id", "system_quantity", "actual_quantity", "variance", "adjustment_posted"}, "SELECT * FROM inventory_count_items", "count_id"));
        t.addTab("Expenses", crud("Operating Expenses", "expenses", "expense_id", new String[]{"branch_id", "expense_date", "category", "description", "amount", "recorded_by"}, "SELECT * FROM expenses", "category"));
        t.addTab("Announcements", crud("Announcements", "announcements", "announcement_id", new String[]{"title", "body", "target_role", "status", "created_by"}, "SELECT * FROM announcements", "title"));
        t.addTab("Messages", crud("Messages", "messages", "message_id", new String[]{"sender_id", "receiver_id", "subject", "body", "reply_text", "status"}, "SELECT * FROM messages", "subject"));
        t.addTab("Notifications", crud("Notifications", "notifications", "notification_id", new String[]{"user_id", "title", "body", "status"}, "SELECT * FROM notifications", "title"));
        t.addTab("Feedback", crud("Feedback", "feedback", "feedback_id", new String[]{"customer_id", "order_id", "rating", "comments", "status"}, "SELECT * FROM feedback", "comments"));
        t.addTab("Audit Logs", crud("Audit Logs", "audit_logs", "log_id", new String[]{"user_id", "action", "details"}, "SELECT * FROM audit_logs", "action"));
        
        // =========================================================================
        // INTEGRATED FOR POINT 1: OMNICHANNEL SELLING, LOGISTICS & MARKETPLACE MODULING
        // =========================================================================
        t.addTab("Marketplace Channels", crud("Marketplace & Online Channels", "order_channels", "channel_id", new String[]{"channel_name"}, "SELECT * FROM order_channels", "channel_name"));
        t.addTab("Logistics & Deliveries", crud("Fulfillment & Delivery Management", "delivery_management", "delivery_id", new String[]{"order_id", "delivery_address", "delivery_type", "distance_bracket", "delivery_fee", "rider_assigned", "delivery_status", "receiving_name", "delivery_date", "photo_reference", "confirmation_code"}, "SELECT * FROM delivery_management", "delivery_status"));
        t.addTab("Pickup Schedules", crud("Customer Pickup Schedules", "pickup_schedules", "pickup_id", new String[]{"order_id", "claiming_date", "time_slot", "pickup_status"}, "SELECT * FROM pickup_schedules", "pickup_status"));

        t.addTab("Reports", new ReportPanel());
        add(t);

        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("Session");
        JMenuItem out = new JMenuItem("Logout");
        out.addActionListener(e -> {
            Audit.log(s, "LOGOUT", "User logged out");
            dispose();
            new AuthFrame().setVisible(true);
        });
        menu.add(out);
        mb.add(menu);
        setJMenuBar(mb);
    }
}