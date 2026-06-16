package com.pos;
import java.sql.*; import java.util.*;

public final class POSService {
 public static String code(String prefix){return prefix+"-"+System.currentTimeMillis();}
 
 public static int customerId(int userId)throws SQLException{try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement("SELECT customer_id FROM customer_profiles WHERE user_id=?")){p.setInt(1,userId);ResultSet r=p.executeQuery();if(!r.next())throw new SQLException("Customer profile not found.");return r.getInt(1);}}
 
 public static int openShift(int employeeId,int terminalId,double openingCash)throws SQLException{try(Connection c=DB.getConnection()){PreparedStatement v=c.prepareStatement("SELECT shift_id FROM cashier_shifts WHERE employee_id=? AND status='OPEN'");v.setInt(1,employeeId);if(v.executeQuery().next())throw new SQLException("This cashier already has an open shift.");PreparedStatement p=c.prepareStatement("INSERT INTO cashier_shifts(terminal_id,employee_id,shift_date,opened_at,opening_cash,status) VALUES(?,?,CURDATE(),NOW(),?,'OPEN')",Statement.RETURN_GENERATED_KEYS);p.setInt(1,terminalId);p.setInt(2,employeeId);p.setDouble(3,openingCash);p.executeUpdate();ResultSet k=p.getGeneratedKeys();k.next();return k.getInt(1);}}
 
 public static void closeShift(int shiftId,double actualCash,int userId)throws SQLException{try(Connection c=DB.getConnection();PreparedStatement p=c.prepareStatement("UPDATE cashier_shifts SET closed_at=NOW(),actual_cash=?,expected_cash=opening_cash+(SELECT COALESCE(SUM(total_amount),0) FROM sales_orders WHERE shift_id=? AND order_status='COMPLETED'),status='CLOSED' WHERE shift_id=?")){p.setDouble(1,actualCash);p.setInt(2,shiftId);p.setInt(3,shiftId);p.executeUpdate();Audit.log(new UserSession(userId,"","",""),"CLOSE_SHIFT","shift="+shiftId);}}

 // =========================================================================
 // UPDATED FOR POINT 1: OMNICHANNEL COMPATIBLE ORDER INTAKE TRANSACTION EXECUTION
 // =========================================================================
 public static int createSale(Integer customerId, int userId, Integer terminalId, Integer shiftId, String status, String paymentMethod, double tendered, java.util.List<CartLine> items, int channelId, boolean isStockReserved, double deliveryFee) throws SQLException {
  Connection c = DB.getConnection();
  try {
   c.setAutoCommit(false);
   double subtotal = 0, tax = 0;
   for (CartLine i : items) {
    subtotal += i.quantity * i.unitPrice;
    tax += i.quantity * i.unitPrice * AppConstants.TAX_RATE;
   }
   double gross = subtotal + tax + deliveryFee;

   // Save Order along with Channel Source Identity mapping specifications
   String oq = "INSERT INTO sales_orders(order_no,customer_id,user_id,terminal_id,shift_id,total_amount,order_status,channel_id,is_stock_reserved) VALUES(?,?,?,?,?,?,?,?,?)";
   PreparedStatement op = c.prepareStatement(oq, Statement.RETURN_GENERATED_KEYS);
   op.setString(1, code("ORD"));
   if (customerId == null) op.setNull(2, Types.INTEGER); else op.setInt(2, customerId);
   op.setInt(3, userId);
   if (terminalId == null) op.setNull(4, Types.INTEGER); else op.setInt(4, terminalId);
   if (shiftId == null) op.setNull(5, Types.INTEGER); else op.setInt(5, shiftId);
   op.setDouble(6, gross);
   op.setString(7, status);
   op.setInt(8, channelId);
   op.setBoolean(9, isStockReserved);
   op.executeUpdate();
   
   ResultSet rk = op.getGeneratedKeys();
   rk.next();
   int oid = rk.getInt(1);

   // Save Items & Execute Immediate Automatic Isolation stock counts to avoid over-selling scenarios
   PreparedStatement ip = c.prepareStatement("INSERT INTO sales_order_items(order_id,product_id,quantity,unit_price,discount_amount,tax_amount,line_total) VALUES(?,?,?,0,?,?)");
   PreparedStatement sp = c.prepareStatement(isStockReserved ? 
     "UPDATE stock_balances SET reserved_quantity = reserved_quantity + ?, quantity_on_hand = quantity_on_hand - ? WHERE product_id = ? LIMIT 1" :
     "UPDATE stock_balances SET quantity_on_hand = quantity_on_hand - ? WHERE product_id = ? LIMIT 1");
     
   for (CartLine i : items) {
    double itemTax = i.quantity * i.unitPrice * AppConstants.TAX_RATE;
    double lineTotal = (i.quantity * i.unitPrice) + itemTax;
    
    ip.setInt(1, oid); ip.setInt(2, i.productId); ip.setDouble(3, i.quantity);
    ip.setDouble(4, i.unitPrice); ip.setDouble(5, itemTax); ip.setDouble(6, lineTotal);
    ip.addBatch();

    if (isStockReserved) {
     sp.setDouble(1, i.quantity); sp.setDouble(2, i.quantity); sp.setInt(3, i.productId);
    } else {
     sp.setDouble(1, i.quantity); sp.setInt(2, i.productId);
    }
    sp.addBatch();
    
    PreparedStatement mv = c.prepareStatement("INSERT INTO stock_movements(product_id,movement_type,reference_table,reference_id,quantity_change,remarks,created_by) VALUES(?,'SALE','sales_orders',?,-?,'Omnichannel Order Allocation Check',?)");
    mv.setInt(1, i.productId); mv.setInt(2, oid); mv.setDouble(3, i.quantity); mv.setInt(4, userId);
    mv.executeUpdate();
   }
   ip.executeBatch();
   sp.executeBatch();

   if (tendered > 0 || !"PENDING".equals(status)) {
    PreparedStatement pp = c.prepareStatement("INSERT INTO sales_payments(order_id,amount,payment_method,reference_no,status,submitted_at) VALUES(?,?,?,'POS_AUTO','COMPLETED',NOW())");
    pp.setInt(1, oid); pp.setDouble(2, gross); pp.setString(3, paymentMethod);
    pp.executeUpdate();
   }
   c.commit();
   return oid;
  } catch (Exception e) {
   c.rollback();
   throw e instanceof SQLException ? (SQLException) e : new SQLException(e);
  } finally {
   c.close();
  }
 }

 // Retro-compatibility signature mapping interface hook for standard cash register terminal execution
 public static int createSale(Integer customerId, int userId, Integer terminalId, Integer shiftId, String status, String paymentMethod, double tendered, java.util.List<CartLine> items) throws SQLException {
  return createSale(customerId, userId, terminalId, shiftId, status, paymentMethod, tendered, items, 4, false, 0.00);
 }

 public static void receiveStock(int poId, int productId, int locationId, String batchNo, String expiry, double qty, double cost, int actor) throws SQLException {
  Connection c = DB.getConnection();
  try {
   c.setAutoCommit(false);
   PreparedStatement r = c.prepareStatement("INSERT INTO goods_receipts(receipt_no,purchase_id,received_date,received_by) VALUES(?,?,CURDATE(),?)", Statement.RETURN_GENERATED_KEYS);
   r.setString(1, code("GRN")); r.setInt(2, poId); r.setInt(3, actor); r.executeUpdate();
   ResultSet rk = r.getGeneratedKeys(); rk.next(); int rid = rk.getInt(1);
   PreparedStatement ri = c.prepareStatement("INSERT INTO goods_receipt_items(receipt_id,product_id,quantity_received,unit_cost) VALUES(?,?,?,?)");
   ri.setInt(1, rid); ri.setInt(2, productId); ri.setDouble(3, qty); ri.setDouble(4, cost); ri.executeUpdate();
   PreparedStatement pb = c.prepareStatement("INSERT INTO product_batches(product_id,location_id,batch_number,expiry_date,quantity_received,quantity_available,unit_cost,status) VALUES(?,?,?,?,?,?,?,'ACTIVE')");
   pb.setInt(1, productId); pb.setInt(2, locationId); pb.setString(3, batchNo); pb.setString(4, expiry); pb.setDouble(5, qty); pb.setDouble(6, qty); pb.setDouble(7, cost); pb.executeUpdate();
   PreparedStatement sb = c.prepareStatement("INSERT INTO stock_balances(product_id,location_id,quantity_on_hand,reorder_level) VALUES(?,?,?,0) ON DUPLICATE KEY UPDATE quantity_on_hand=quantity_on_hand+VALUES(quantity_on_hand)");
   sb.setInt(1, productId); sb.setInt(2, locationId); sb.setDouble(3, qty); sb.executeUpdate();
   PreparedStatement mv = c.prepareStatement("INSERT INTO stock_movements(product_id,movement_type,reference_table,reference_id,quantity_change,remarks,created_by) VALUES(?,'RECEIPT','goods_receipts',?,?,?,?)");
   mv.setInt(1, productId); mv.setInt(2, rid); mv.setDouble(3, qty); mv.setString(4, "Batch " + batchNo); mv.setInt(5, actor); mv.executeUpdate();
   c.commit();
  } catch (Exception e) { c.rollback(); throw e instanceof SQLException ? (SQLException) e : new SQLException(e); } finally { c.close(); }
 }

 public static void transferStock(int productId, int fromLocation, int toLocation, double quantity, String notes, int actor) throws SQLException {
  Connection c = DB.getConnection();
  try {
   c.setAutoCommit(false);
   PreparedStatement v = c.prepareStatement("SELECT quantity_on_hand FROM stock_balances WHERE product_id=? AND location_id=?");
   v.setInt(1, productId); v.setInt(2, fromLocation);
   ResultSet vr = v.executeQuery(); if (!vr.next() || vr.getDouble(1) < quantity) throw new SQLException("Insufficient stock in source location.");
   PreparedStatement t = c.prepareStatement("INSERT INTO stock_transfers(transfer_no,from_location_id,to_location_id,transfer_date,status,created_by,notes) VALUES(?,?,?,CURDATE(),'COMPLETED',?,?)", Statement.RETURN_GENERATED_KEYS);
   t.setString(1, code("TRF")); t.setInt(2, fromLocation); t.setInt(3, toLocation); t.setInt(4, actor); t.setString(5, notes); t.executeUpdate();
   ResultSet tk = t.getGeneratedKeys(); tk.next(); int tid = tk.getInt(1);
   PreparedStatement ti = c.prepareStatement("INSERT INTO stock_transfer_items(transfer_id,product_id,quantity) VALUES(?,?,?)");
   ti.setInt(1, tid); ti.setInt(2, productId); ti.setDouble(3, quantity); ti.executeUpdate();
   PreparedStatement out = c.prepareStatement("UPDATE stock_balances SET quantity_on_hand=quantity_on_hand-? WHERE product_id=? AND location_id=?");
   out.setDouble(1, quantity); out.setInt(2, productId); out.setInt(3, fromLocation); out.executeUpdate();
   PreparedStatement in = c.prepareStatement("INSERT INTO stock_balances(product_id,location_id,quantity_on_hand,reorder_level) VALUES(?,?,?,0) ON DUPLICATE KEY UPDATE quantity_on_hand=quantity_on_hand+VALUES(quantity_on_hand)");
   in.setInt(1, productId); in.setInt(2, toLocation); in.setDouble(3, quantity); in.executeUpdate();
   for (String type : new String[]{"TRANSFER_OUT", "TRANSFER_IN"}) {
    PreparedStatement mv = c.prepareStatement("INSERT INTO stock_movements(product_id,movement_type,reference_table,reference_id,quantity_change,remarks,created_by) VALUES(?,?,?,?,?,?,?)");
    mv.setInt(1, productId); mv.setString(2, type); mv.setString(3, "stock_transfers"); mv.setInt(4, tid); mv.setDouble(5, "TRANSFER_OUT".equals(type) ? -quantity : quantity); mv.setString(6, notes); mv.setInt(7, actor); mv.executeUpdate();
   }
   c.commit();
  } catch (Exception e) { c.rollback(); throw e instanceof SQLException ? (SQLException) e : new SQLException(e); } finally { c.close(); }
 }

 public static class CartLine { public final int productId; public final String productName; public final double quantity, unitPrice; public CartLine(int id, String n, double q, double p){productId=id;productName=n;quantity=q;unitPrice=p;} }
}
