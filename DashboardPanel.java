package com.pos;
import javax.swing.*; import java.awt.*; import java.sql.*;

public class DashboardPanel extends JPanel { 
    private final UserSession session; 
    private final JPanel cards = new JPanel(new GridLayout(3, 4, 12, 12)); // Increased grid size to accommodate new cards
    private final String[][] metrics = {
        {"Today's Sales", "SELECT CONCAT('PHP ',FORMAT(COALESCE(SUM(total_amount),0),2)) FROM sales_orders WHERE DATE(created_at)=CURDATE() AND order_status='COMPLETED'"},
        {"Open Shifts", "SELECT COUNT(*) FROM cashier_shifts WHERE status='OPEN'"},
        {"Products", "SELECT COUNT(*) FROM products WHERE status='ACTIVE'"},
        {"Low Stock", "SELECT COUNT(*) FROM v_low_stock"},
        {"Pending Orders", "SELECT COUNT(*) FROM sales_orders WHERE order_status='PENDING'"},
        {"Pending Payments", "SELECT COUNT(*) FROM sales_payments WHERE status='PENDING'"},
        {"Returns Today", "SELECT COUNT(*) FROM sales_returns WHERE return_date=CURDATE()"},
        {"Customers", "SELECT COUNT(*) FROM customer_profiles"},
        // --- ADDED FOR POINT 1: OMNICHANNEL LOGISTICS & MARKETPLACE SUMMARY METRICS ---
        {"Pending Deliveries", "SELECT COUNT(*) FROM delivery_management WHERE delivery_status IN ('Preparing','Packed','Dispatched','Out for Delivery')"},
        {"Pickups Scheduled Today", "SELECT COUNT(*) FROM pickup_schedules WHERE claiming_date=CURDATE() AND pickup_status='Scheduled'"},
        {"Marketplace Orders Volume", "SELECT COUNT(*) FROM sales_orders WHERE channel_id IN (SELECT channel_id FROM order_channels WHERE channel_name <> 'POS')"},
        {"Failed / Returned Delivery", "SELECT COUNT(*) FROM delivery_management WHERE delivery_status IN ('Failed Delivery','Returned')"}
    };

    public DashboardPanel(UserSession s) {
        super(new BorderLayout(12, 12));
        session = s;
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel head = new JPanel(new BorderLayout());
        head.add(UIUtils.title("Point of Sale Dashboard"), BorderLayout.WEST);
        JButton reload = new JButton("Refresh Dashboard");
        head.add(reload, BorderLayout.EAST);
        add(head, BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
        reload.addActionListener(e -> load());
        load();
    }

    private void load() {
        cards.removeAll();
        try (Connection c = DB.getConnection()) {
            for (String[] m : metrics) {
                String value = "0";
                try (PreparedStatement p = c.prepareStatement(m[1]); ResultSet r = p.executeQuery()) {
                    if (r.next()) {
                        Object valObj = r.getObject(1);
                        value = valObj != null ? String.valueOf(valObj) : "0";
                    }
                } catch (Exception e) {
                    value = "0"; // Graceful fallback if tables are empty/uninitialized
                }
                JPanel card = new JPanel(new BorderLayout());
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEtchedBorder(), 
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)
                ));
                JLabel name = new JLabel(m[0]);
                JLabel val = new JLabel(value);
                val.setFont(val.getFont().deriveFont(Font.BOLD, 24f));
                card.add(name, BorderLayout.NORTH);
                card.add(val, BorderLayout.CENTER);
                cards.add(card);
            }
        } catch (Exception e) {
            UIUtils.error(this, e);
        }
        cards.revalidate();
        cards.repaint();
    }
}