package com.pos;
import java.nio.charset.StandardCharsets; import java.security.MessageDigest;
public final class PasswordUtil { public static String hash(String s){ try { MessageDigest d=MessageDigest.getInstance("SHA-256"); byte[] b=d.digest(s.getBytes(StandardCharsets.UTF_8)); StringBuilder x=new StringBuilder(); for(byte v:b)x.append(String.format("%02x",v)); return x.toString(); } catch(Exception e){ throw new RuntimeException(e); }} private PasswordUtil(){} }
