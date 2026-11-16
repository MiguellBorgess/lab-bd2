package com.oracle.tutorial.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MyQueries3 {
  
  Connection con;
  JDBCUtilities settings;  
  
  public MyQueries3(Connection connArg, JDBCUtilities settingsArg) {
    this.con = connArg;
    this.settings = settingsArg;
  }

  public static void getMyData(Connection con) throws SQLException {
    String query = 
      "select nome_agencia, count(saldo_deposito) as quantidade_deposito, sum(saldo_deposito) as soma_deposito " +
      "from deposito " +
      "group by nome_agencia " +
      "order by nome_agencia ";



    try (PreparedStatement pstmt = con.prepareStatement(query)) {
            
      try (ResultSet rs = pstmt.executeQuery()) {
        boolean found = false;
        while (rs.next()) {
          found = true;
          String nome_agencia = rs.getString("nome_agencia");
          long count_deposito = rs.getLong("quantidade_deposito");
          double sum_deposito = rs.getDouble("soma_deposito");
          System.out.println("Agencia: " + nome_agencia + " | Quantidade Depositos: " + count_deposito + " | Soma Depositos: " + String.format("%.2f", sum_deposito));
        }
        if (!found) {
          System.out.println("    Nenhuma agencia encontrada");
        }
      }
    } catch (SQLException e) {
      JDBCUtilities.printSQLException(e);
    }
  }


  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: java com.oracle.tutorial.jdbc.MyQueries3 <properties_file> <supplier_name>");
      return;
    }
    
    String propertiesFile = args[0];

    try {
      var myJDBCUtilities = new JDBCUtilities(propertiesFile);

      try (Connection myConnection = myJDBCUtilities.getConnection()) {
        MyQueries3.getMyData(myConnection);
      }
      
    } catch (IOException e) {
        System.err.println("Problem reading properties file " + propertiesFile);
        e.printStackTrace();
    } catch (SQLException e) {
      JDBCUtilities.printSQLException(e);
    }
  }
}
