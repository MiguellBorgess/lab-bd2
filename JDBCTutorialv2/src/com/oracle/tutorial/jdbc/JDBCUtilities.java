package com.oracle.tutorial.jdbc;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.BatchUpdateException;
import java.sql.SQLWarning;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class JDBCUtilities {

  public String dbms;
  public String jarFile;
  public String dbName;
  public String userName;
  public String password;
  public String urlString;
  
  private String driver;
  private String serverName;
  private int portNumber;
  private Properties prop;


  public JDBCUtilities(String propertiesFileName) throws IOException, InvalidPropertiesFormatException {
    super();
    this.setProperties(propertiesFileName);
  }

  public static void getWarningsFromResultSet(ResultSet rs) throws SQLException {
    JDBCUtilities.printWarnings(rs.getWarnings());
  }

  public static void getWarningsFromStatement(Statement stmt) throws SQLException {
    JDBCUtilities.printWarnings(stmt.getWarnings());
  }  

  public static void printWarnings(SQLWarning warning) {
    if (warning != null) {
      System.out.println("\n---Warning---\n");
      while (warning != null) {
        System.out.println("Message: " + warning.getMessage());
        System.out.println("SQLState: " + warning.getSQLState());
        System.out.print("Vendor error code: ");
        System.out.println(warning.getErrorCode());
        System.out.println("");
        warning = warning.getNextWarning();
      }
    }
  }
  
  public static boolean ignoreSQLException(String sqlState) {
    if (sqlState == null) {
      System.out.println("The SQL state is not defined!");
      return false;
    }
    // X0Y32: Jar file already exists in schema
    if (sqlState.equalsIgnoreCase("X0Y32")) return true;
    // 42Y55: Table already exists in schema
    if (sqlState.equalsIgnoreCase("42Y55")) return true;
    
    return false;
  }

  public static void printBatchUpdateException(BatchUpdateException b) {
    System.err.println("----BatchUpdateException----");
    System.err.println("SQLState:  " + b.getSQLState());
    System.err.println("Message:  " + b.getMessage());
    System.err.println("Vendor:  " + b.getErrorCode());
    System.err.print("Update counts:  ");
    int[] updateCounts = b.getUpdateCounts();
    for (int i = 0; i < updateCounts.length; i++) {
      System.err.print(updateCounts[i] + "   ");
    }
    System.err.println();
  }

  public static void printSQLException(SQLException ex) {
    for (Throwable e : ex) {
      if (e instanceof SQLException && !ignoreSQLException(((SQLException)e).getSQLState())) {
          e.printStackTrace(System.err);
          System.err.println("SQLState: " + ((SQLException)e).getSQLState());
          System.err.println("Error Code: " + ((SQLException)e).getErrorCode());
          System.err.println("Message: " + e.getMessage());
          Throwable t = ex.getCause();
          while (t != null) {
            System.out.println("Cause: " + t);
            t = t.getCause();
          }
      }
    }
  }

  private void setProperties(String fileName) throws IOException, InvalidPropertiesFormatException {
    this.prop = new Properties();
    try (var fis = new FileInputStream(fileName)) {
      prop.loadFromXML(fis);
    }

    this.dbms = this.prop.getProperty("dbms");
    this.jarFile = this.prop.getProperty("jar_file");
    this.driver = this.prop.getProperty("driver");
    this.dbName = this.prop.getProperty("database_name");
    this.userName = this.prop.getProperty("user_name");
    this.password = this.prop.getProperty("password");
    this.serverName = this.prop.getProperty("server_name");
    
    String portNumberString = this.prop.getProperty("port_number", "0");
    this.portNumber = Integer.parseInt(portNumberString.trim());
    
    System.out.println("Set the following properties:");
    System.out.println("dbms: " + dbms);
    System.out.println("driver: " + driver);
    System.out.println("dbName: " + dbName);
    System.out.println("userName: " + userName);
    System.out.println("serverName: " + serverName);
    System.out.println("portNumber: " + portNumber);
  }

  public Connection getConnection() throws SQLException {
    Properties connectionProps = new Properties();
    connectionProps.put("user", this.userName);
    connectionProps.put("password", this.password);

    this.urlString = switch (this.dbms) {
        case "mysql" -> "jdbc:" + this.dbms + "://" + this.serverName + ":" + this.portNumber + "/" + this.dbName;
        case "postgresql" -> "jdbc:" + this.dbms + "://" + this.serverName + ":" + this.portNumber + "/" + this.dbName;
        case "derby" -> "jdbc:" + this.dbms + ":" + this.dbName + ";create=true";
        case "sqlite" -> "jdbc:sqlite:" + this.dbName;
        default -> throw new SQLException("Database management system not supported: " + this.dbms);
    };

    Connection conn = DriverManager.getConnection(this.urlString, connectionProps);

    if ("mysql".equals(this.dbms) || "postgresql".equals(this.dbms)) {
        conn.setCatalog(this.dbName);
    }
   
    else if (this.dbms.equals("postgresql")) {
	String currentUrlString = "jdbc:" + this.dbms + "://" + this.serverName + ":" + this.portNumber + "/" + this.dbName;
	conn = DriverManager.getConnection(currentUrlString, connectionProps);
	this.urlString = currentUrlString + this.dbName;
	conn.setCatalog(this.dbName);
	}

   else if (this.dbms.equals("sqlite")) {
	String currentUrlString = "jdbc:" + this.dbms + ":" + this.dbName;
	conn = DriverManager.getConnection(currentUrlString, connectionProps);
	this.urlString = currentUrlString + this.dbName;
	conn.setCatalog(this.dbName);
	}

    System.out.println("Connected to database");
    return conn;
  }

  public Connection getConnection(String userName, String password) throws SQLException {
    this.userName = userName;
    this.password = password;
    return this.getConnection();
  }

  public static void closeConnection(Connection connArg) {
    System.out.println("Releasing all open resources ...");
    try {
      if (connArg != null) {
        connArg.close();
      }
    } catch (SQLException sqle) {
      printSQLException(sqle);
    }
  }
  
  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("Properties file not specified at command line");
      return;
    }

    try {
      var myJDBCUtilities = new JDBCUtilities(args[0]);
      System.out.println("Reading properties file " + args[0]);

      try (var myConnection = myJDBCUtilities.getConnection()) {
        System.out.println("Connection established successfully. It will be closed automatically.");
        // O código que usa a conexão viria aqui.
      }
      
    } catch (IOException e) {
        System.err.println("Problem reading properties file " + args[0]);
        e.printStackTrace();
    } catch (SQLException e) {
      JDBCUtilities.printSQLException(e);
    }
  }
}
