package com.oracle.tutorial.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MyQueries {
  
  Connection con;
  JDBCUtilities settings;  
  
  public MyQueries(Connection connArg, JDBCUtilities settingsArg) {
    this.con = connArg;
    this.settings = settingsArg;
  }

  public static void getMyData(Connection con) throws SQLException {
    String query =
      "SELECT c.COF_NAME FROM COFFEES c " +
      "JOIN SUPPLIERS s ON s.SUP_ID = c.SUP_ID " +
      "WHERE s.SUP_NAME LIKE ?";

    System.out.println("\nCoffees bought from " + supplierName + ": ");

    try (PreparedStatement pstmt = con.prepareStatement(query)) {
      
      pstmt.setString(1, supplierName);
      
      try (ResultSet rs = pstmt.executeQuery()) {
        boolean found = false;
        while (rs.next()) {
          found = true;
          String coffeeName = rs.getString("COF_NAME");
          System.out.println("     " + coffeeName);
        }
        if (!found) {
          System.out.println("     No coffees found for this supplier.");
        }
      }
    } catch (SQLException e) {
      JDBCUtilities.printSQLException(e);
    }
  }


  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: java com.oracle.tutorial.jdbc.MyQueries <properties_file> <supplier_name>");
      return;
    }
    
    String propertiesFile = args[0];
    String supplierName = args[1];

    try {
      var myJDBCUtilities = new JDBCUtilities(propertiesFile);

      try (Connection myConnection = myJDBCUtilities.getConnection()) {
        MyQueries.getMyData(supplierName,myConnection);
      }
      
    } catch (IOException e) {
        System.err.println("Problem reading properties file " + propertiesFile);
        e.printStackTrace();
    } catch (SQLException e) {
      JDBCUtilities.printSQLException(e);
    }
  }
}
