package com.pos;
import java.io.*; import java.sql.*; import java.util.*;
public final class DB {
 private static final Properties P=new Properties();
 static { try { File f=new File("config/db.properties"); if(f.exists()){ try(FileInputStream in=new FileInputStream(f)){ P.load(in); }} else { P.setProperty("db.url","jdbc:mysql://localhost:3306/point_of_sale_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Manila"); P.setProperty("db.user","root"); P.setProperty("db.password",""); } Class.forName("com.mysql.cj.jdbc.Driver"); } catch(Exception e){ System.err.println("Database setup: "+e.getMessage()); } }
 public static Connection getConnection() throws SQLException { return DriverManager.getConnection(P.getProperty("db.url"),P.getProperty("db.user"),P.getProperty("db.password","")); }
 public static boolean test(){ try(Connection c=getConnection()){ return c.isValid(3); } catch(Exception e){ return false; }} private DB(){}
}
