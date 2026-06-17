package com.example.vulnerable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;

/**
 * WARNING: This file is INTENTIONALLY VULNERABLE for educational/security training.
 * DO NOT use any of this code in production. Each method demonstrates a common
 * vulnerability class (OWASP Top 10 / CWE examples).
 *
 * Compiles on JDK 17+ with no external dependencies.
 */
public class VulnerableApp {

  private static final Logger log = Logger.getLogger(VulnerableApp.class.getName());

  // [BAD] CWE-798: Hardcoded credentials
  private static final String DB_URL = "jdbc:mysql://localhost:3306/app";
  private static final String DB_USER = "admin";
  private static final String DB_PASSWORD = "P@ssw0rd123!";
  private static final String API_KEY = "sk_live_51HxYz9aBcDeFgHiJkLmNoPqRsTuVwXyZ";
  private static final String AWS_SECRET = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

  // [BAD] CWE-321: Hardcoded cryptographic key (must be 16 bytes for AES-128)
  private static final String ENCRYPTION_KEY = "1234567890123456";

  // [BAD] CWE-89: SQL Injection via string concatenation
  public User loginUser(String username, String password) throws SQLException {
    Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    String query = "SELECT * FROM users WHERE username = '" + username
        + "' AND password = '" + password + "'";
    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(query);
    if (rs.next()) {
      return new User(rs.getString("username"));
    }
    return null;
  }

  // [BAD] CWE-78: OS Command Injection
  public String pingHost(String host) throws IOException {
    Process p = Runtime.getRuntime().exec("ping -c 1 " + host);
    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line).append("\n");
    }
    return sb.toString();
  }

  // [BAD] CWE-22: Path Traversal
  public String readUserFile(String filename) throws IOException {
    File file = new File("/var/app/userfiles/" + filename);
    BufferedReader reader = new BufferedReader(new FileReader(file));
    StringBuilder content = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      content.append(line).append("\n");
    }
    reader.close();
    return content.toString();
  }

  // [BAD] CWE-79: Cross-Site Scripting (reflected XSS)
  // Returns raw HTML built from untrusted input.
  public String renderProfile(String nameFromRequest) {
    return "<html><body><h1>Hello " + nameFromRequest + "</h1></body></html>";
  }

  // [BAD] CWE-611: XML External Entity (XXE) - no protections enabled
  public void parseXml(String xml) throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    db.parse(new InputSource(new StringReader(xml)));
  }

  // [BAD] CWE-502: Insecure Deserialization
  public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
    return ois.readObject();
  }

  // [BAD] CWE-327 + CWE-328 + CWE-916: MD5, unsalted, for passwords
  public String hashPassword(String password) throws Exception {
    MessageDigest md = MessageDigest.getInstance("MD5");
    byte[] digest = md.digest(password.getBytes());
    StringBuilder hex = new StringBuilder();
    for (byte b : digest) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }

  // [BAD] CWE-327: AES in ECB mode + hardcoded key
  public byte[] encryptData(String plaintext) throws Exception {
    SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes(), "AES");
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, key);
    return cipher.doFinal(plaintext.getBytes());
  }

  // [BAD] CWE-330: java.util.Random for security tokens
  public String generateSessionToken() {
    Random random = new Random();
    return Long.toHexString(random.nextLong()) + Long.toHexString(random.nextLong());
  }

  // [BAD] CWE-918: Server-Side Request Forgery (SSRF)
  public String fetchUrl(String userSuppliedUrl) throws IOException {
    URL url = new URL(userSuppliedUrl);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    StringBuilder body = new StringBuilder();
    String line;
    while ((line = in.readLine()) != null) {
      body.append(line);
    }
    in.close();
    return body.toString();
  }

  // [BAD] CWE-601: Open redirect (returns target URL with no validation)
  public String resolveRedirectTarget(String userSuppliedNext) {
    return userSuppliedNext;
  }

  // [BAD] CWE-117 + CWE-532: Log injection + secret in logs
  public void logLogin(String username, String password) {
    log.info("User login attempt: user=" + username + " password=" + password);
  }

  // [BAD] CWE-295: TLS verification disabled (pattern shown in comment to avoid
  // accidentally registering an insecure TrustManager at class load).
  public void insecureHttpsCall() {
    // Real-world anti-pattern: install a TrustManager that accepts all certs
    // and a HostnameVerifier that returns true. Do NOT do this.
  }

  // [BAD] CWE-209: Information disclosure via stack trace
  public String handleError(Exception e) {
    StringBuilder out = new StringBuilder();
    out.append("Internal error: ").append(e.getMessage()).append("\n");
    for (StackTraceElement el : e.getStackTrace()) {
      out.append("  at ").append(el).append("\n");
    }
    return out.toString();
  }

  // [BAD] CWE-639: Insecure direct object reference (no authz check)
  public Order getOrder(String orderId) throws SQLException {
    Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    PreparedStatement ps = conn.prepareStatement("SELECT * FROM orders WHERE id = ?");
    ps.setString(1, orderId);
    ResultSet rs = ps.executeQuery();
    return rs.next() ? new Order(rs.getString("id")) : null;
  }

  // Minimal data classes
  static class User {
    String name;
    User(String n) { this.name = n; }
  }

  static class Order {
    String id;
    Order(String i) { this.id = i; }
  }

  // Smoke-test entry point so the class is runnable without a servlet container.
  public static void main(String[] args) throws Exception {
    VulnerableApp app = new VulnerableApp();
    System.out.println("MD5(hello)         = " + app.hashPassword("hello"));
    System.out.println("Session token      = " + app.generateSessionToken());
    System.out.println("Rendered profile   = " + app.renderProfile("<script>alert(1)</script>"));
    System.out.println("Redirect target    = " + app.resolveRedirectTarget("//evil.example.com"));
  }
}