package com.dokhub.app.servicios;

import com.dokhub.app.integraciones.DockerMotor;
import com.dokhub.app.integraciones.GithubCli;
import com.dokhub.app.integraciones.VpsConexion;
import com.dokhub.app.repositorios.RegistroUsuariosDB;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ServicioRotacion {

    private static final String URL_DB = "jdbc:sqlite:registro_dokploy.db";
    private static final double LIMITE_HORAS = 28.0;

    private ServicioRotacion() {
        throw new IllegalStateException("Clase de servicio");
    }

    // Equivale a gestionar_caida_usuario_activo()
    public static void gestionarCaidaUsuarioActivo() {
        String usuarioActual = GithubCli.obtenerUsuarioActivo();
        String usuariosInvalidos = GithubCli.obtenerTodosLosUsuarios(); // En tu bash filtrabas los caídos aquí

        if (usuariosInvalidos.contains(usuarioActual)) {
            System.out.println("🚨 [ALERTA] El usuario activo (" + usuarioActual + ") ha perdido la conexión/token.");

            // 1. Marcar como caído en la DB
            RegistroUsuariosDB.actualizarEstadoBloqueo(usuarioActual, "true");
            marcarUsuarioComoUsado(usuarioActual, "false");

            // 2. Buscar nuevo candidato y cambiar
            String nuevoCandidato = buscarCandidatoLibre();
            GithubCli.cambiarUsuarioActivo(nuevoCandidato);
            registrarInicioUsuario(nuevoCandidato);

            // 3. Preparar nuevo entorno
            String nuevoServidor = GithubCli.obtenerNombreCodespace();
            VpsConexion.prepararConexionSegura(nuevoServidor);
            DockerMotor.detenerProcesos(nuevoServidor);
            // Aquí llamarías a ServicioBackup.migrarBackup() que armaremos luego
        } else {
            System.out.println("✅ El usuario activo (" + usuarioActual + ") se encuentra sano y operativo.");
        }
    }

    // Equivale a ejecutar_rotacion_por_tiempo()
    public static void ejecutarRotacionPorTiempo() {
        String usuarioActual = GithubCli.obtenerUsuarioActivo();
        System.out.println("⏱️  Verificando tiempo de vida del usuario: " + usuarioActual + "...");

        double horasTranscurridas = calcularHorasTranscurridas(usuarioActual);
        System.out.println("   ⏳ Horas consumidas: " + horasTranscurridas + " / " + LIMITE_HORAS);

        if (horasTranscurridas >= LIMITE_HORAS) {
            System.out.println("🔔 Tiempo límite alcanzado. Iniciando rotación programada...");

            String servidorActual = GithubCli.obtenerNombreCodespace();

            // 1. Backup del origen
            VpsConexion.prepararConexionSegura(servidorActual);
            VpsConexion.crearBackup(servidorActual);

            // 2. Actualizar DB
            marcarUsuarioComoUsado(usuarioActual, "true");

            // 3. Buscar reemplazo
            String nuevoCandidato = buscarCandidatoLibre();
            GithubCli.cambiarUsuarioActivo(nuevoCandidato);
            registrarInicioUsuario(nuevoCandidato);

            // 4. Salto al nuevo servidor
            String nuevoServidor = GithubCli.obtenerNombreCodespace();
            VpsConexion.prepararConexionSegura(nuevoServidor);
            // Aquí llamarías a ServicioBackup.migrarBackup()
        } else {
            System.out.println("✅ El usuario aún tiene tiempo disponible.");
        }
    }

    // --- Métodos auxiliares de Base de Datos para el servicio ---

    private static double calcularHorasTranscurridas(String username) {
        // Usa la misma lógica de julianday que tu script bash
        String sql = "SELECT IFNULL(ROUND((julianday(datetime('now', 'localtime')) - julianday(fecha_inicio)) * 24.0, 2), 0) AS horas FROM usuarios WHERE username = '"
                + username + "'";
        try (Connection conn = DriverManager.getConnection(URL_DB);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble("horas");
            }
        } catch (Exception e) {
            System.err.println("❌ Error calculando horas: " + e.getMessage());
        }
        return 0.0;
    }

    private static String buscarCandidatoLibre() {
        String sql = "SELECT username FROM usuarios WHERE usado='false' AND bloqueado='false' LIMIT 1";
        try (Connection conn = DriverManager.getConnection(URL_DB);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString("username");
            }
        } catch (Exception e) {
            System.err.println("❌ Error buscando candidato: " + e.getMessage());
        }
        throw new IllegalStateException("❌ FATAL: No quedan usuarios disponibles.");
    }

    private static void registrarInicioUsuario(String username) {
        String sql = "UPDATE usuarios SET fecha_inicio = datetime('now', 'localtime') WHERE username = '" + username
                + "'";
        ejecutarUpdateRapido(sql);
    }

    private static void marcarUsuarioComoUsado(String username, String cumplio) {
        String sql = "UPDATE usuarios SET usado = 'true', fecha_fin = datetime('now', 'localtime'), cumplio_esperadas = '"
                + cumplio + "' WHERE username = '" + username + "'";
        ejecutarUpdateRapido(sql);
    }

    private static void ejecutarUpdateRapido(String sql) {
        try (Connection conn = DriverManager.getConnection(URL_DB);
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.err.println("❌ Error actualizando BD: " + e.getMessage());
        }
    }
}
