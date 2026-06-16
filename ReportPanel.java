package com.pos;
import javax.swing.*; import java.awt.*;

public class ReportPanel extends JPanel { 
 private final QueryTableModel m = new QueryTableModel();
 private final JTable t = new JTable(m);
 
 // --- EXTENDED JCOMBOBOX MATRIX OPTIONS WITH THE TWO REQUIRED SYSTEM REPORTS ---
 private final JComboBox<String> reports = new JComboBox<>(new String[]{
  "Daily Sales", "Product Sales", "Current Stock", "Low Stock", "Payment Collection", 
  "Returns and Refunds", "Cashier Shift Summary", "Loyalty Summary", "Purchase Receiving", 
  "Inventory Valuation", "Omnichannel Channel Performance", "Courier Delivery Performance"
 });

 public ReportPanel() {
  super(new BorderLayout(8,8));
  JPanel h = new JPanel();
  h.add(UIUtils.title("Operational Reports & Omnichannel Analytics"));
  h.add(reports);
  
  JButton run = new JButton("Generate"), csv = new JButton("Export CSV");
  h.add(run); h.add(csv);
  add(h, BorderLayout.NORTH);
  add(new JScrollPane(t), BorderLayout.CENTER);
  
  run.addActionListener(e -> run());
  csv.addActionListener(e -> CsvExporter.exportTable(this, t, String.valueOf(reports.getSelectedItem()).replace(' ', '_') + ".csv"));
  run();
 }

 private void run() {
  // SQL evaluation string indexing scripts matrix mapping sequence arrays
  String[] sql = {
   "SELECT * FROM v_daily_sales ORDER BY sale_date DESC",
   "SELECT * FROM v_product_sales ORDER BY quantity_sold DESC",
   "SELECT * FROM v_current_stock ORDER BY product_name",
   "SELECT * FROM v_low_stock ORDER BY product_name",
   "SELECT * FROM v_payment_collection ORDER BY payment_date DESC",
   "SELECT * FROM v_returns_refunds ORDER BY return_date DESC",
   "SELECT * FROM v_cashier_shift_summary ORDER BY shift_date DESC",
   "SELECT * FROM v_loyalty_summary ORDER BY loyalty_points DESC",
   "SELECT * FROM v_purchase_receiving ORDER BY received_date DESC",
   "SELECT * FROM v_inventory_valuation ORDER BY inventory_value DESC",
   
   // INDEX 10: Omnichannel Channel Revenue Performance Report Statement
   "SELECT " +
   "  CASE o.channel_id WHEN 1 THEN 'Website Storefront' WHEN 2 THEN 'Social Media Page' WHEN 3 THEN 'Third-Party Marketplace' ELSE 'Direct POS Station' END as channel_source, " +
   "  COUNT(o.order_id) as gross_orders_processed, " +
   "  SUM(CASE WHEN o.order_status='COMPLETED' THEN 1 ELSE 0 END) as completed_transactions, " +
   "  SUM(CASE WHEN o.order_status='CANCELLED' OR o.order_status='REJECTED' THEN 1 ELSE 0 END) as failed_bounces, " +
   "  FORMAT(SUM(o.total_amount), 2) as aggregate_gross_revenue, " +
   "  FORMAT(SUM(CASE WHEN o.order_status='COMPLETED' THEN o.total_amount ELSE 0 END), 2) as actual_retained_earnings " +
   "FROM sales_orders o GROUP BY o.channel_id",
   
   // INDEX 11: Logistics Dispatch Execution, Fulfillment, and Courier Performance Report Statement
   "SELECT " +
   "  assigned_rider as courier_personnel, " +
   "  COUNT(delivery_id) as total_assigned_shipments, " +
   "  SUM(CASE WHEN delivery_status='Delivered' THEN 1 ELSE 0 END) as successful_drops, " +
   "  SUM(CASE WHEN delivery_status='Failed Delivery' THEN 1 ELSE 0 END) as failed_shipments, " +
   "  SUM(CASE WHEN delivery_status='Returned' THEN 1 ELSE 0 END) as returned_incidents, " +
   "  FORMAT(SUM(delivery_fee), 2) as total_delivery_fees_collected " +
   "FROM delivery_management GROUP BY assigned_rider"
  };

  try {
   m.load(sql[reports.getSelectedIndex()]);
  } catch (Exception e) { 
   UIUtils.error(this, e); 
  }
 }
}