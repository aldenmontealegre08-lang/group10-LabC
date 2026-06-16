package com.pos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class ProductBrowserPanel extends JPanel {
    private final UserSession s;
    private final QueryTableModel m = new QueryTableModel();
    private final JTable products = new JTable(m);
    private final DefaultTableModel cartM = new DefaultTableModel(new Object[]{"Product ID", "Product", "Qty", "Unit Price", "With Tax"}, 0);
    private final JTable cart = new JTable(cartM);
    
    private final JTextField search = new JTextField(14), qty = new JTextField("1", 4);
    
    // Fulfillment interactive fields
    private final JComboBox<String> channelBox = new JComboBox<>(new String[]{"WEBSITE", "SHOPEE", "LAZADA", "TIKTOK_SHOP"});
    private final JComboBox<String> fulfillmentBox = new JComboBox<>(new String[]{"INSTORE_PICKUP", "DELIVERY"});
    private final JComboBox<String> deliveryTierBox = new JComboBox<>(new String[]{"STANDARD", "SAME_DAY"});
    private final JComboBox<String> distanceBox = new JComboBox<>(new String[]{"0-5KM", "5-10KM", "10KM+"});
    private final JTextField addressField = new JTextField("Store Pickup", 18);
    private final JTextField dateField = new JTextField("YYYY-MM-DD", 8);
    private final JTextField slotField = new JTextField("10:00 AM - 02:00 PM", 12);

    public ProductBrowserPanel(UserSession s) {
        super(new BorderLayout(6, 6));
        this.s = s;
        buildLayout();
        loadCatalog();
    }

    private void buildLayout() {
        JPanel h = new JPanel(new BorderLayout());
        h.add(UIUtils.title("Omnichannel Product Browser"), BorderLayout.WEST);
        
        JPanel find = new JPanel();
        find.add(new JLabel("Search:")); find.add(search);
        JButton searchBtn = new JButton("Search"), refreshBtn = new JButton("Refresh");
        find.add(searchBtn); find.add(refreshBtn);
        h.add(find, BorderLayout.EAST);
        add(h, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(products), new JScrollPane(cart));
        split.setResizeWeight(0.5);
        add(split, BorderLayout.CENTER);

        // Control Center
        JPanel mainSouth = new JPanel(new BorderLayout());
        JPanel cartControl = new JPanel();
        cartControl.add(new JLabel("Qty:")); cartControl.add(qty);
        JButton addBtn = new JButton("Add To Order");
        JButton remBtn = new JButton("Remove Item");
        JButton submitBtn = new JButton("Finalize & Submit Channel Order");
        cartControl.add(addBtn); cartControl.add(remBtn); cartControl.add(submitBtn);
        mainSouth.add(cartControl, BorderLayout.NORTH);

        // Fulfillment Settings Pane
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        configPanel.setBorder(BorderFactory.createTitledBorder("Order Routing & Logistics Spec"));
        configPanel.add(new JLabel("Source:")); configPanel.add(channelBox);
        configPanel.add(new JLabel("Type:")); configPanel.add(fulfillmentBox);
        configPanel.add(new JLabel("Tier:")); configPanel.add(deliveryTierBox);
        configPanel.add(new JLabel("Distance:")); configPanel.add(distanceBox);
        configPanel.add(new JLabel("Address:")); configPanel.add(addressField);
        configPanel.add(new JLabel("Date:")); configPanel.add(dateField);
        configPanel.add(new JLabel("Slot:")); configPanel.add(slotField);
        
        mainSouth.add(configPanel, BorderLayout.SOUTH);
        add(mainSouth, BorderLayout.SOUTH);

        // UI Event Hooks
        searchBtn.addActionListener(e -> loadCatalog());
        refreshBtn.addActionListener(e -> { search.setText(""); loadCatalog(); });
        addBtn.addActionListener(e -> addItemToCart());
        remBtn.addActionListener(e -> {
            int row = cart.getSelectedRow();
            if(row >= 0) cartM.removeRow(row);
        });
        submitBtn.addActionListener(e -> dispatchOrder());
        
        fulfillmentBox.addActionListener(e -> {
            boolean isDelivery = "DELIVERY".equals(fulfillmentBox.getSelectedItem());
            addressField.setEditable(isDelivery);
            deliveryTierBox.setEnabled(isDelivery);
            distanceBox.setEnabled(isDelivery);
            if(!isDelivery) addressField.setText("Store Pickup");
        });
    }

    private void loadCatalog() {
        try {
            // Evaluates quantity_on_hand MINUS pending online reservations
            String sql = "SELECT p.product_id, p.sku, p.product_name, p.barcode, p.selling_price, " +
                         "(COALESCE((SELECT SUM(quantity_on_hand) FROM stock_balances WHERE product_id=p.product_id),0) - " +
                         " COALESCE((SELECT SUM(oi.quantity) FROM sales_order_items oi JOIN sales_orders o ON o.order_id=oi.order_id WHERE oi.product_id=p.product_id AND o.order_status='PENDING'),0)) as net_available " +
                         "FROM products p WHERE p.status='ACTIVE'";
            if (!search.getText().trim().isEmpty()) {
                sql += " AND (p.product_name LIKE ? OR p.sku LIKE ?)";
                m.load(sql, "%" + search.getText() + "%", "%" + search.getText() + "%");
            } else {
                m.load(sql);
            }
        } catch (Exception ex) { UIUtils.error(this, ex); }
    }

    private void addItemToCart() {
        int row = products.getSelectedRow();
        if (row < 0) return;
        try {
            row = products.convertRowIndexToModel(row);
            int id = Integer.parseInt(String.valueOf(m.getValueAt(row, 0)));
            String name = String.valueOf(m.getValueAt(row, 2));
            double price = Double.parseDouble(String.valueOf(m.getValueAt(row, 4)));
            double count = Double.parseDouble(qty.getText().trim());
            double stock = Double.parseDouble(String.valueOf(m.getValueAt(row, 5)));

            if (count <= 0 || count > stock) throw new Exception("Invalid requested quantity or stock is reserved.");
            cartM.addRow(new Object[]{id, name, count, price, count * price * (1 + AppConstants.TAX_RATE)});
        } catch (Exception ex) { UIUtils.error(this, ex); }
    }

    private void dispatchOrder() {
        if (cartM.getRowCount() == 0) { UIUtils.info(this, "Your order cart is empty."); return; }
        try {
            java.util.List<POSService.CartLine> targets = new ArrayList<>();
            for (int i = 0; i < cartM.getRowCount(); i++) {
                targets.add(new POSService.CartLine(
                    Integer.parseInt(String.valueOf(cartM.getValueAt(i, 0))),
                    String.valueOf(cartM.getValueAt(i, 1)),
                    Double.parseDouble(String.valueOf(cartM.getValueAt(i, 2))),
                    Double.parseDouble(String.valueOf(cartM.getValueAt(i, 3)))
                ));
            }
            
            Integer customerId = null;
            try { customerId = POSService.customerId(s.userId); } catch (Exception ignored) {}

            int orderId = POSService.createOnlineOrMarketplaceOrder(
                customerId, s.userId,
                String.valueOf(channelBox.getSelectedItem()),
                String.valueOf(fulfillmentBox.getSelectedItem()),
                "PENDING_GATEWAY",
                addressField.getText().trim(),
                String.valueOf(deliveryTierBox.getSelectedItem()),
                String.valueOf(distanceBox.getSelectedItem()),
                dateField.getText().replace("YYYY-MM-DD", ""),
                slotField.getText().trim(),
                targets
            );

            UIUtils.info(this, "Order successfully saved! System Tracking Reference ID: " + orderId);
            cartM.setRowCount(0);
            loadCatalog();
        } catch (Exception ex) { UIUtils.error(this, ex); }
    }
}