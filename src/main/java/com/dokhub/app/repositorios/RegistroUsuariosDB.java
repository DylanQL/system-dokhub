package com.dokhub.app.repositorios;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class RegistroUsuariosDB {

    // Apunta al archivo exacto de tu base de datos en la raíz del proyecto
    private static final String URL_DB = "jdbc:sqlite:registro_dokploy.db";

    public static void inicializarBaseDeDatos() {
        String sql = """
                    CREATE TABLE IF NOT EXISTS usuarios (
                        username TEXT PRIMARY KEY,
                        bloqueado TEXT,
                        usado TEXT,
                        fecha_inicio TEXT,
                        fecha_fin TEXT,
                        horas_realizadas REAL,
                        cumplio_esperadas TEXT
                    );
                """;

        try (Connection conn = DriverManager.getConnection(URL_DB);
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("🗄️ Base de datos SQLite sincronizada y lista.");
        } catch (SQLException e) {
            System.err.println("❌ Error inicializando DB: " + e.getMessage());
        }
    }

    public static void liberarUsuariosUsados() {
        String sql = "UPDATE usuarios SET usado = 'false' WHERE bloqueado = 'false'";

        try (Connection conn = DriverManager.getConnection(URL_DB);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int actualizados = pstmt.executeUpdate();
            System.out.println("🔄 Ciclo reiniciado. Usuarios liberados: " + actualizados);
        } catch (SQLException e) {
            System.err.println("❌ Error al reiniciar ciclo de usuarios: " + e.getMessage());
        }
    }
}
