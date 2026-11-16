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
      "select nome_cliente from cliente c " +
      "where nome_cliente not in (select nome_cliente " +
                    "from emprestimo " +
                    "union " +
                    "select nome_cliente " +
                    "from deposito ) " + 
      "order by nome_cliente";


    try (PreparedStatement pstmt = con.prepareStatement(query)) {
            
      try (ResultSet rs = pstmt.executeQuery()) {
        boolean found = false;
        while (rs.next()) {
          found = true;
          String nome_cliente = rs.getString("nome_cliente");
          System.out.println( "Nome Cliente: " + nome_cliente);
        }
        if (!found) {
          System.out.println("     Nenhum cliente encontrado");
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
