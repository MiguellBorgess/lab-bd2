package com.oracle.tutorial.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MyQueries2 {
  
  Connection con;
  JDBCUtilities settings;  
  
  public MyQueries2(Connection connArg, JDBCUtilities settingsArg) {
    this.con = connArg;
    this.settings = settingsArg;
  }

  public static void getMyData(Connection con) throws SQLException {
    String query = 
      "SELECT cl.nome_cliente " +
      "FROM cliente cl " +
      "EXCEPT " +
      "SELECT cl.nome_cliente " +
      "FROM cliente cl " +
      "JOIN emprestimo e ON e.nome_cliente = cl.nome_cliente " +
      "JOIN deposito d ON d.nome_cliente = cl.nome_cliente";


    try (PreparedStatement pstmt = con.prepareStatement(query)) {
            
      try (ResultSet rs = pstmt.executeQuery()) {
        boolean found = false;
        while (rs.next()) {
          found = true;
          String nomeCliente = rs.getString("nome_cliente");
          System.out.println("Cliente: " + nomeCliente);
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
      System.err.println("Usage: java com.oracle.tutorial.jdbc.MyQueries2 <properties_file> <supplier_name>");
      return;
    }
    
    String propertiesFile = args[0];

    try {
      var myJDBCUtilities = new JDBCUtilities(propertiesFile);

      try (Connection myConnection = myJDBCUtilities.getConnection()) {
        MyQueries2.getMyData(myConnection);
      }
      
    } catch (IOException e) {
        System.err.println("Problem reading properties file " + propertiesFile);
        e.printStackTrace();
    } catch (SQLException e) {
      JDBCUtilities.printSQLException(e);
    }
  }
}
