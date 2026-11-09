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
      "SELECT s.SUP_NAME AS FORNECEDOR, c.COF_NAME AS NOME_CAFE, SUM(c.PRICE * i.QUAN) AS MONTANTE_POTENCIAL " +
      "FROM COFFEES c " +
      "JOIN COF_INVENTORY i ON c.COF_NAME = i.COF_NAME " +
      "JOIN SUPPLIERS s ON c.SUP_ID = s.SUP_ID " +
      "GROUP BY s.SUP_NAME, c.COF_NAME";


    try (PreparedStatement pstmt = con.prepareStatement(query)) {
            
      try (ResultSet rs = pstmt.executeQuery()) {
        boolean found = false;
        while (rs.next()) {
          found = true;
          String fornecedor = rs.getString("FORNECEDOR");
          String nomeCafe = rs.getString("NOME_CAFE");
          double montante = rs.getDouble("MONTANTE_POTENCIAL");
          System.out.println("Fornecedor: " + fornecedor + " | Café: " + nomeCafe + " | Montante: " + montante);
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
    if (args.length < 1) {
      System.err.println("Usage: java com.oracle.tutorial.jdbc.MyQueries <properties_file> <supplier_name>");
      return;
    }
    
    String propertiesFile = args[0];

    try {
      var myJDBCUtilities = new JDBCUtilities(propertiesFile);

      try (Connection myConnection = myJDBCUtilities.getConnection()) {
        MyQueries.getMyData(myConnection);
      }
      
    } catch (IOException e) {
        System.err.println("Problem reading properties file " + propertiesFile);
        e.printStackTrace();
    } catch (SQLException e) {
      JDBCUtilities.printSQLException(e);
    }
  }
}
