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
	"SELECT a.nome_agencia, COUNT(d.numero_deposito) AS quantidade, SUM(d.saldo_deposito) AS montante " +
	"FROM agencia a " +
	"JOIN deposito d ON a.nome_agencia = d.nome_agencia " +
	"GROUP BY a.nome_agencia";

    try (PreparedStatement pstmt = con.prepareStatement(query)) {
            
      try (ResultSet rs = pstmt.executeQuery()) {
        boolean found = false;
        while (rs.next()) {
          found = true;
          String nomeAgencia = rs.getString("nome_agencia");
          int quantidade = rs.getInt("quantidade");
          double montante = rs.getDouble("montante");
          System.out.println("Agência: " + nomeAgencia + " | Quantidade: " + quantidade + " | Montante: " + montante);
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
