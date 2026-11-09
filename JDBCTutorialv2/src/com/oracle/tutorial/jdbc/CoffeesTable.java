/*
 * Copyright (c) 1995, 2011, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.tutorial.jdbc;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CoffeesTable {

  private String dbName;
  private Connection con;
  private String dbms;


  public CoffeesTable(Connection connArg, String dbNameArg, String dbmsArg) {
    super();
    this.con = connArg;
    this.dbName = dbNameArg;
    this.dbms = dbmsArg;

  }

  public void createTable() throws SQLException {
    String createString = """
        CREATE TABLE COFFEES (
            COF_NAME varchar(32) NOT NULL,
            SUP_ID int NOT NULL,
            PRICE numeric(10,2) NOT NULL,
            SALES integer NOT NULL,
            TOTAL integer NOT NULL,
            PRIMARY KEY (COF_NAME),
            FOREIGN KEY (SUP_ID) REFERENCES SUPPLIERS (SUP_ID)
        )""";
    
    try (Statement stmt = con.createStatement()) {
      stmt.executeUpdate(createString);
    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
    }
  }

  public void populateTable() throws SQLException {
    String insertSql = "INSERT INTO COFFEES VALUES (?, ?, ?, ?, ?)";
    
    // Coffee data: name, supplier_id, price, sales, total
    Object[][] coffeeData = {
        {"Colombian", 101, 7.99, 0, 0},
        {"French_Roast", 49, 8.99, 0, 0},
        {"Espresso", 150, 9.99, 0, 0},
        {"Colombian_Decaf", 101, 8.99, 0, 0},
        {"French_Roast_Decaf", 49, 9.99, 0, 0}
    };
    
    try (PreparedStatement stmt = con.prepareStatement(insertSql)) {
      for (Object[] coffee : coffeeData) {
        stmt.setString(1, (String) coffee[0]);
        stmt.setInt(2, (Integer) coffee[1]);
        stmt.setDouble(3, (Double) coffee[2]);
        stmt.setInt(4, (Integer) coffee[3]);
        stmt.setInt(5, (Integer) coffee[4]);
        stmt.executeUpdate();
      }
    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
    }
  }


  public void updateCoffeeSales(HashMap<String, Integer> salesForWeek) throws SQLException {
    String updateSalesQuery = "UPDATE COFFEES SET SALES = ? WHERE COF_NAME = ?";
    String updateTotalQuery = "UPDATE COFFEES SET TOTAL = TOTAL + ? WHERE COF_NAME = ?";

    boolean originalAutoCommit = con.getAutoCommit();
    try {
      con.setAutoCommit(false);
      
      try (PreparedStatement updateSales = con.prepareStatement(updateSalesQuery);
           PreparedStatement updateTotal = con.prepareStatement(updateTotalQuery)) {

        for (Map.Entry<String, Integer> entry : salesForWeek.entrySet()) {
          updateSales.setInt(1, entry.getValue());
          updateSales.setString(2, entry.getKey());
          updateSales.executeUpdate();

          updateTotal.setInt(1, entry.getValue());
          updateTotal.setString(2, entry.getKey());
          updateTotal.executeUpdate();
        }
        con.commit();
      }
    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
      try {
        System.err.println("Transaction is being rolled back");
        con.rollback();
      } catch (SQLException rollbackException) {
        JDBCTutorialUtilities.printSQLException(rollbackException);
      }
      throw e;
    } finally {
      con.setAutoCommit(originalAutoCommit);
    }
  }

  public void modifyPrices(float percentage) throws SQLException {
    try (Statement stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
         ResultSet uprs = stmt.executeQuery("SELECT * FROM COFFEES")) {

      while (uprs.next()) {
        float currentPrice = uprs.getFloat("PRICE");
        uprs.updateFloat("PRICE", currentPrice * percentage);
        uprs.updateRow();
      }

    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
    }
  }


  public void modifyPricesByPercentage(String coffeeName, float priceModifier,
                                       float maximumPrice) throws SQLException {
    boolean originalAutoCommit = con.getAutoCommit();
    String query = "SELECT COF_NAME, PRICE FROM COFFEES WHERE COF_NAME = ?";

    try {
      con.setAutoCommit(false);
      var save1 = con.setSavepoint();
      
      try (var getPrice = con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
           var updatePrice = con.createStatement()) {
           
        getPrice.setString(1, coffeeName);
        
        try (var rs = getPrice.executeQuery()) {
          if (!rs.next()) {
            System.out.println("Could not find entry for coffee named " + coffeeName);
          } else {
            float oldPrice = rs.getFloat("PRICE");
            float newPrice = oldPrice + (oldPrice * priceModifier);
            System.out.printf("Old price of %s is %.2f%n", coffeeName, oldPrice);
            System.out.printf("New price of %s is %.2f%n", coffeeName, newPrice);
            System.out.println("Performing update...");
            
            updatePrice.executeUpdate("UPDATE COFFEES SET PRICE = " + newPrice +
                                    " WHERE COF_NAME = '" + coffeeName + "'");
            System.out.println("\nCOFFEES table after update:");
            CoffeesTable.viewTable(con);
            
            if (newPrice > maximumPrice) {
              System.out.printf("\nThe new price, %.2f, is greater than the maximum price, %.2f. Rolling back the transaction...%n",
                               newPrice, maximumPrice);
              con.rollback(save1);
              System.out.println("\nCOFFEES table after rollback:");
              CoffeesTable.viewTable(con);
            }
            con.commit();
          }
        }
      }
    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
      throw e;
    } finally {
      con.setAutoCommit(originalAutoCommit);
    }
  }


  public void insertRow(String coffeeName, int supplierID, float price,
                        int sales, int total) throws SQLException {
    try (var stmt = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
         var uprs = stmt.executeQuery("SELECT * FROM COFFEES")) {

      uprs.moveToInsertRow();
      uprs.updateString("COF_NAME", coffeeName);
      uprs.updateInt("SUP_ID", supplierID);
      uprs.updateFloat("PRICE", price);
      uprs.updateInt("SALES", sales);
      uprs.updateInt("TOTAL", total);
      uprs.insertRow();
      uprs.beforeFirst();

    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
    }
  }

  public void batchUpdate() throws SQLException {
    boolean originalAutoCommit = this.con.getAutoCommit();
    
    try {
      this.con.setAutoCommit(false);
      
      try (var stmt = this.con.createStatement()) {
        stmt.addBatch("INSERT INTO COFFEES VALUES('Amaretto', 49, 9.99, 0, 0)");
        stmt.addBatch("INSERT INTO COFFEES VALUES('Hazelnut', 49, 9.99, 0, 0)");
        stmt.addBatch("INSERT INTO COFFEES VALUES('Amaretto_decaf', 49, 10.99, 0, 0)");
        stmt.addBatch("INSERT INTO COFFEES VALUES('Hazelnut_decaf', 49, 10.99, 0, 0)");

        int[] updateCounts = stmt.executeBatch();
        this.con.commit();
      }
    } catch (BatchUpdateException b) {
      JDBCTutorialUtilities.printBatchUpdateException(b);
    } catch (SQLException ex) {
      JDBCTutorialUtilities.printSQLException(ex);
    } finally {
      this.con.setAutoCommit(originalAutoCommit);
    }
  }
  
  public static void viewTable(Connection con) throws SQLException {
    String query = "SELECT COF_NAME, SUP_ID, PRICE, SALES, TOTAL FROM COFFEES";
    
    try (Statement stmt = con.createStatement();
         ResultSet rs = stmt.executeQuery(query)) {

      while (rs.next()) {
        String coffeeName = rs.getString("COF_NAME");
        int supplierID = rs.getInt("SUP_ID");
        float price = rs.getFloat("PRICE");
        int sales = rs.getInt("SALES");
        int total = rs.getInt("TOTAL");
        System.out.printf("%s, %d, %.2f, %d, %d%n", 
                         coffeeName, supplierID, price, sales, total);
      }

    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
    }
  }

  public static void alternateViewTable(Connection con) throws SQLException {
    String query = "SELECT COF_NAME, SUP_ID, PRICE, SALES, TOTAL FROM COFFEES";
    
    try (var stmt = con.createStatement();
         var rs = stmt.executeQuery(query)) {
         
      while (rs.next()) {
        String coffeeName = rs.getString(1);
        int supplierID = rs.getInt(2);
        float price = rs.getFloat(3);
        int sales = rs.getInt(4);
        int total = rs.getInt(5);
        System.out.printf("%s, %d, %.2f, %d, %d%n", 
                         coffeeName, supplierID, price, sales, total);
      }
    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
    }
  }
  
  public Set<String> getKeys() throws SQLException {
    var keys = new HashSet<String>();
    String query = "SELECT COF_NAME FROM COFFEES";
    
    try (var stmt = con.createStatement();
         var rs = stmt.executeQuery(query)) {
         
      while (rs.next()) {
        keys.add(rs.getString(1));
      }
    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
    }
    return keys;
  }


  public void dropTable() throws SQLException {
    try (var stmt = con.createStatement()) {
      switch (this.dbms) {
        case "mysql" -> stmt.executeUpdate("DROP TABLE IF EXISTS COFFEES");
        case "derby" -> stmt.executeUpdate("DROP TABLE COFFEES");
        default -> throw new SQLException("Unsupported database: " + this.dbms);
      }
    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
    }
  }

  public static void main(String[] args) {
    if (args.length == 0 || args[0] == null) {
      System.err.println("Properties file not specified at command line");
      return;
    }

    JDBCTutorialUtilities myJDBCTutorialUtilities;
    try {
      myJDBCTutorialUtilities = new JDBCTutorialUtilities(args[0]);
    } catch (Exception e) {
      System.err.println("Problem reading properties file " + args[0]);
      e.printStackTrace();
      return;
    }

    try (Connection myConnection = myJDBCTutorialUtilities.getConnection()) {
      CoffeesTable myCoffeeTable = new CoffeesTable(
          myConnection, 
          myJDBCTutorialUtilities.getDbName(),
          myJDBCTutorialUtilities.getDbms());

      System.out.println("\nContents of COFFEES table:");
      CoffeesTable.viewTable(myConnection);

      System.out.println("\nRaising coffee prices by 25%");
      myCoffeeTable.modifyPrices(1.25f);

      System.out.println("\nInserting a new row:");
      myCoffeeTable.insertRow("Kona", 150, 10.99f, 0, 0);
      CoffeesTable.viewTable(myConnection);

      System.out.println("\nUpdating sales of coffee per week:");
      var salesCoffeeWeek = new HashMap<String, Integer>();
      salesCoffeeWeek.put("Colombian", 175);
      salesCoffeeWeek.put("French_Roast", 150);
      salesCoffeeWeek.put("Espresso", 60);
      salesCoffeeWeek.put("Colombian_Decaf", 155);
      salesCoffeeWeek.put("French_Roast_Decaf", 90);
      myCoffeeTable.updateCoffeeSales(salesCoffeeWeek);
      CoffeesTable.viewTable(myConnection);

      System.out.println("\nModifying prices by percentage");
      myCoffeeTable.modifyPricesByPercentage("Colombian", 0.10f, 9.00f);
      
      System.out.println("\nCOFFEES table after modifying prices by percentage:");
      myCoffeeTable.viewTable(myConnection);

      System.out.println("\nPerforming batch updates; adding new coffees");
      myCoffeeTable.batchUpdate();
      myCoffeeTable.viewTable(myConnection);

    } catch (SQLException e) {
      JDBCTutorialUtilities.printSQLException(e);
    }
  }
}
