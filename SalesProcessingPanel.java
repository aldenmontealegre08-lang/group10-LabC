package com.pos;
import javax.swing.*; import java.awt.*; import java.sql.*;

public class SalesProcessingPanel extends JPanel { 
 private final UserSession s; 
 private final QueryTableModel m = new QueryTableModel(); 
 private final JTable t = new JTable(m); 
 private final JTextField search = new JTextField(18);
 
 // --- OMNICHANNEL CONTROL FIELDS ---
 private final JTextField txtRiderName = new JTextField(12);
 private final JComboBox<String> cbFulfillmentStatus = new JComboBox<>(new String[]{
  "Preparing", "Packed", "Dispatched", "Out for Delivery", "Delivered", "Failed Delivery", "Returned"
 });
 private final JTextField txtProofCode = new JTextField(10);

 public SalesProcessingPanel(UserSession s) {
  super(new BorderLayout(8,8)); this.s = s;
  JPanel h = new JPanel(new BorderLayout());
  h.add(UIUtils.title("Omnichannel Order Processing & Fulfillment Hub"), BorderLayout.WEST);
  
  JPanel b = new JPanel();
  b.add(search);
  JButton find = new JButton("Search"), refresh = new JButton("Refresh"), 
          approve = new JButton("Approve"), reject = new JButton("Reject"), 
          fulfill = new JButton("Fulfill Paid Order"), cancel = new JButton("Cancel");
          
  for (JButton x : new JButton[]{find, refresh, approve, reject, fulfill, cancel}) b.add(x);
  h.add(b, BorderLayout.EAST);
  add(h, BorderLayout.NORTH);
  add(new JScrollPane(t), BorderLayout.CENTER);

  // Bottom action bar for Rider Assignments and Status Dispatches
  JPanel dispatchControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
  dispatchControl.setBorder(BorderFactory.createTitledBorder("Fulfillment Execution & Courier Proof Dispatch"));
  dispatchControl.add(new JLabel("Assign Rider:")); dispatchControl.add(txtRiderName);
  dispatchControl.add(new JLabel("Status:")); dispatchControl.add(cbFulfillmentStatus);
  dispatchControl.add(new JLabel("Proof/Confirmation Code:")); dispatchControl.add(txtProofCode);
  
  JButton btnUpdateFulfillment = new JButton("Update Fulfillment State");
  dispatchControl.add(btnUpdateFulfillment);
  add(dispatchControl, BorderLayout.SOUTH);

  find.addActionListener(e -> load(true));
  refresh.addActionListener(e -> load(false));
  approve.addActionListener(e -> status("APPROVED"));
  reject.addActionListener(e -> status("REJECTED"));
  cancel.addActionListener(e -> status("CANCELLED"));
  fulfill.addActionListener(e -> fulfillOrder());
  
  btnUpdateFulfillment.addActionListener(e -> updateFulfillmentState());
  load(false);
 }

 private int id() { 
  int r = t.getSelectedRow(); 
  return r < 0 ? 0 : Integer.parseInt(String.valueOf(t.getValueAt(t.convertRowIndexToModel(r), 0))); 
 }

 private void load(boolean searchOn) {
  try {
   // Enhanced query displaying the channel source, dynamic fees, delivery destination, or designated pickup time metrics
   String q = "SELECT o.order_id, o.order_no, u.full_name customer, " +
              "CASE o.channel_id WHEN 1 THEN 'Website' WHEN 2 THEN 'Social Media' WHEN 3 THEN 'Marketplace' ELSE 'POS Counter' END as order_channel, " +
              "o.total_amount, o.order_status, " +
              "COALESCE(d.delivery_status, p.pickup_status) as routing_status, " +
              "COALESCE(d.delivery_type, 'N/A') as speed, " +
              "COALESCE(d.assigned_rider, 'None') as rider, " +
              "COALESCE(d.delivery_address, p.claiming_date) as destination_or_date, " +
              "o.created_at " +
              "FROM sales_orders o " +
              "LEFT JOIN customer_profiles cp ON cp.customer_id = o.customer_id " +
              "LEFT JOIN users u ON u.user_id = cp.user_id " +
              "LEFT JOIN delivery_management d ON d.order_id = o.order_id " +
              "LEFT JOIN pickup_schedules p ON p.order_id = o.order_id " +
              "WHERE 1=1 " + 
              (searchOn ? " AND (o.order_no LIKE ? OR u.full_name LIKE ? OR o.order_status LIKE ?)" : "") +
              " ORDER BY o.order_id DESC";
   if (searchOn) m.load(q, "%" + search.getText() + "%", "%" + search.getText() + "%", "%" + search.getText() + "%");
   else m.load(q);
   t.setAutoCreateRowSorter(true);
  } catch (Exception e) { UIUtils.error(this, e); }
 }

 private void status(String status) {
  int x = id(); if (x == 0) return;
  try (Connection c = DB.getConnection(); 
       PreparedStatement p = c.prepareStatement("UPDATE sales_orders SET order_status=? WHERE order_id=? AND order_status IN ('PENDING','APPROVED')")) {
   p.setString(1, status); p.setInt(2, x); p.executeUpdate();
   Audit.log(s, "ORDER_STATUS_UPDATE", "Order ID " + x + " changed to " + status);
   load(false);
  } catch (Exception e) { UIUtils.error(this, e); }
 }

 private void fulfillOrder() {
  int x = id(); if (x == 0) return;
  try (Connection c = DB.getConnection()) {
   c.setAutoCommit(false);
   
   // Check if order has active stock reservations that need to be cleared/deducted permanently
   try (PreparedStatement p1 = c.prepareStatement("SELECT is_stock_reserved FROM sales_orders WHERE order_id = ?")) {
    p1.setInt(1, x);
    try (ResultSet r = p1.executeQuery()) {
     if (r.next() && r.getBoolean("is_stock_reserved")) {
      // Clear the temporary reservation and apply permanent inventory deduction
      try (PreparedStatement p2 = c.prepareStatement(
            "UPDATE stock_balances sb JOIN sales_order_items soi ON sb.product_id = soi.product_id " +
            "SET sb.reserved_quantity = sb.reserved_quantity - soi.quantity " +
            "WHERE soi.order_id = ?")) {
       p2.setInt(1, x); p2.executeUpdate();
      }
     }
    }
   }

   try (PreparedStatement p3 = c.prepareStatement("UPDATE sales_orders SET order_status='COMPLETED' WHERE order_id=?")) {
    p3.setInt(1, x); p3.executeUpdate();
   }
   
   c.commit();
   Audit.log(s, "OMNICHANNEL_ORDER_FULFILL", "Order ID: " + x);
   load(false);
   UIUtils.info(this, "Order marked as permanent sale. Active inventory reservations cleared cleanly.");
  } catch (Exception e) { UIUtils.error(this, e); }
 }

 private void updateFulfillmentState() {
  int orderId = id(); if (orderId == 0) { UIUtils.info(this, "Please select an order from the tracking grid."); return; }
  String rider = txtRiderName.getText().trim();
  String fulfillmentStatus = cbFulfillmentStatus.getSelectedItem().toString();
  String proof = txtProofCode.getText().trim();

  try (Connection c = DB.getConnection()) {
   boolean foundDelivery = false;
   
   // 1. Attempt updates to courier shipping pipelines
   String upDelivery = "UPDATE delivery_management SET assigned_rider=?, delivery_status=?, proof_confirmation_code=?, actual_delivery_date=CASE WHEN ?='Delivered' THEN NOW() ELSE null END WHERE order_id=?";
   try (PreparedStatement ps = c.prepareStatement(upDelivery)) {
    ps.setString(1, rider.isEmpty() ? "Unassigned Courier" : rider);
    ps.setString(2, fulfillmentStatus);
    ps.setString(3, proof.isEmpty() ? "NONE" : proof);
    ps.setString(4, fulfillmentStatus);
    ps.setInt(5, orderId);
    int affected = ps.executeUpdate();
    if (affected > 0) foundDelivery = true;
   }

   // 2. Fallback updates to user scheduled collection pickups
   if (!foundDelivery) {
    String upPickup = "UPDATE pickup_schedules SET pickup_status=? WHERE order_id=?";
    try (PreparedStatement ps = c.prepareStatement(upPickup)) {
     ps.setString(1, fulfillmentStatus);
     ps.setInt(2, orderId);
     ps.executeUpdate();
    }
   }

   Audit.log(s, "FULFILLMENT_STATE_CHANGE", "Order ID: " + orderId + " set to " + fulfillmentStatus);
   load(false);
   UIUtils.info(this, "Fulfillment milestones and rider credentials synchronized successfully.");
  } catch (Exception e) { UIUtils.error(this, e); }
 }
}