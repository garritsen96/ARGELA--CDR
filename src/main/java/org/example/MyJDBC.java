package org.example;

import java.sql.*;

public class MyJDBC {
    public static void main(String[] args) {


        try {
            Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3307/Cdr",
                    "demo",
                    "demo" // Parola
            );
            Statement statement = connection.createStatement();
            ResultSet resultSet=statement.executeQuery("SELECT * FROM Cdr");

            while (resultSet.next()) {
                String arayanNumara = resultSet.getString("arayanNumara");
                String arananNumara = resultSet.getString("arananNumara");
                int aramaSuresi = resultSet.getInt("aramaSuresi");
                String aramaZamani = resultSet.getString("aramaZamani");

                System.out.println("Arayan Numara: " + arayanNumara);
                System.out.println("Aranan Numara: " + arananNumara);
                System.out.println("Arama Suresi: " + aramaSuresi);
                System.out.println("Arama Zamani: " + aramaZamani);
                System.out.println("-----------------------------");
            }
        }catch (SQLException e){
            e.printStackTrace();
        }

    }
}
