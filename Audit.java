package com.pos;
import java.sql.*;
public final class Audit { public static void log(UserSession s,String action,String detail){ if(s==null)return; try(Connection c=DB.getConnection(); PreparedStatement p=c.prepareStatement("INSERT INTO audit_logs(user_id,action,details) VALUES(?,?,?)")){p.setInt(1,s.userId);p.setString(2,action);p.setString(3,detail);p.executeUpdate();}catch(Exception ignored){} } private Audit(){} }
