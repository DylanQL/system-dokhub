package com.dokhub.app.repositorios;

import com.dokhub.app.modelos.UsuarioGitHub;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class RegistroUsuariosDB {

    // Apunta al mismo archivo que ya usas en tu proyecto
    private static final String URL_DB = "jdbc:sqlite:registro_dokploy.db";

    private RegistroUsuariosDB() {
        throw new IllegalStateException("Clase repositorio");
    }

    // Inicializa la tabla de usuarios si no existe
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

    // Inserta usuarios nuevos ignorando duplicados
    public static void registrarUsuarioNuevo(String username) {
        String sql = "INSERT OR IGNORE INTO usuarios (username, bloqueado, usado) VALUES (?, 'false', 'false')";

        try (Connection conn = DriverManager.getConnection(URL_DB);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error registrando usuario: " + e.getMessage());
        }
    }

    // Actualiza el estado de bloqueo de un usuario
    public static void actualizarEstadoBloqueo(String username, String estadoBloqueado) {
        String sql = "UPDATE usuarios SET bloqueado = ? WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(URL_DB);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, estadoBloqueado);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error actualizando bloqueo para " + username + ": " + e.getMessage());
        }
    }

    // Método que utiliza la clase UsuarioGitHub para empaquetar los datos de SQLite
    public static UsuarioGitHub obtenerUsuario(String usernameABuscar) {
        String sql = "SELECT * FROM usuarios WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(URL_DB);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usernameABuscar);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new UsuarioGitHub(
                        rs.getString("username"),
                        rs.getString("bloqueado"),
                        rs.getString("usado"),
                        rs.getString("fecha_inicio"),
                        rs.getString("fecha_fin"),
                        rs.getDouble("horas_realizadas"),
                        rs.getString("cumplio_esperadas"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error buscando al usuario: " + e.getMessage());
        }
        return null;
    }
}
