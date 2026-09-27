package com.dokhub.app.servicios;

import com.dokhub.app.integraciones.GithubCli;
import com.dokhub.app.repositorios.RegistroUsuariosDB;
import com.dokhub.app.utils.Utilidades;
import com.dokhub.app.integraciones.VpsConexion;

public class ServicioRotacion {

    // Límite extraído de tu script original
    private static final double LIMITE_HORAS = 27.0;

    public static void gestionarCaidaUsuarioActivo() {
        String usuarioActual = GithubCli.obtenerUsuarioActivo();

        // Obtenemos los usuarios con token caído
        String comandoCaidos = "gh auth status 2>&1 | grep 'X Failed' | sed -E 's/.*(as|account) ([^ ]+).*/\\2/'";
        String usuariosInvalidos = Utilidades.ejecutarComandoLinux(comandoCaidos);

        if (usuariosInvalidos.contains(usuarioActual)) {
            System.out.println("🚨 [ALERTA] El usuario activo (" + usuarioActual + ") ha perdido la conexión/token.");

            // Actualizamos la DB marcándolo como caído
            Utilidades.ejecutarComandoLinux(
                    "sqlite3 registro_dokploy.db \"UPDATE usuarios SET usado = 'true', fecha_fin = datetime('now', 'localtime'), cumplio_esperadas = 'false' WHERE username = '"
                            + usuarioActual + "';\"");
            Utilidades.ejecutarComandoLinux(
                    "sqlite3 registro_dokploy.db \"UPDATE usuarios SET horas_realizadas = ROUND((julianday(fecha_fin) - julianday(fecha_inicio)) * 24.0, 2) WHERE username = '"
                            + usuarioActual + "' AND fecha_inicio IS NOT NULL;\"");

            ejecutarSaltoDeServidor(usuarioActual);
        } else {
            System.out.println("✅ El usuario activo (" + usuarioActual + ") se encuentra sano y operativo.");
        }
    }

    public static void ejecutarRotacionPorTiempo() {
        String usuarioActual = GithubCli.obtenerUsuarioActivo();
        System.out.println("⏱️  Verificando tiempo de vida del usuario: " + usuarioActual + "...");

        // Extraemos las horas calculadas por SQLite
        String queryHoras = "sqlite3 registro_dokploy.db \"SELECT IFNULL(ROUND((julianday(datetime('now', 'localtime')) - julianday(fecha_inicio)) * 24.0, 2), 0) FROM usuarios WHERE username = '"
                + usuarioActual + "';\"";
        String resultadoHoras = Utilidades.ejecutarComandoLinux(queryHoras);

        try {
            double horasTranscurridas = Double.parseDouble(resultadoHoras.trim());
            System.out.println("   ⏳ Horas consumidas: " + horasTranscurridas + " / " + LIMITE_HORAS);

            if (horasTranscurridas >= LIMITE_HORAS) {
                System.out.println("🔔 Tiempo límite alcanzado. Iniciando rotación programada...");

                // Actualizamos la DB marcándolo como exitoso
                Utilidades.ejecutarComandoLinux(
                        "sqlite3 registro_dokploy.db \"UPDATE usuarios SET usado = 'true', fecha_fin = datetime('now', 'localtime'), cumplio_esperadas = 'true' WHERE username = '"
                                + usuarioActual + "';\"");
                Utilidades.ejecutarComandoLinux(
                        "sqlite3 registro_dokploy.db \"UPDATE usuarios SET horas_realizadas = ROUND((julianday(fecha_fin) - julianday(fecha_inicio)) * 24.0, 2) WHERE username = '"
                                + usuarioActual + "';\"");

                ejecutarSaltoDeServidor(usuarioActual);
            } else {
                System.out.println("✅ El usuario aún tiene tiempo disponible.");
            }
        } catch (NumberFormatException e) {
            System.out.println("⚠️ No se pudo calcular el tiempo para " + usuarioActual);
        }
    }

    private static void ejecutarSaltoDeServidor(String usuarioViejo) {
        String servidorActual = GithubCli.obtenerNombreCodespace();

        // 1. Preparamos y respaldamos el servidor viejo
        VpsConexion.prepararVincularVps(servidorActual);
        ServicioBackup.crearBackupDokploy(servidorActual);

        // 2. Buscamos el reemplazo
        String queryCandidato = "sqlite3 registro_dokploy.db \"SELECT username FROM usuarios WHERE usado='false' AND bloqueado='false' LIMIT 1;\"";
        String nuevoCandidato = Utilidades.ejecutarComandoLinux(queryCandidato);

        if (nuevoCandidato == null || nuevoCandidato.trim().isEmpty()) {
            System.out.println("🔄 Todos los usuarios han sido usados. Reiniciando ciclo...");
            RegistroUsuariosDB.liberarUsuariosUsados();
            nuevoCandidato = Utilidades.ejecutarComandoLinux(queryCandidato);

            if (nuevoCandidato == null || nuevoCandidato.trim().isEmpty()) {
                System.err.println("❌ FATAL: No quedan usuarios disponibles en absoluto.");
                System.exit(1);
            }
        }

        // 3. Hacemos el cambio en GitHub y en la DB
        GithubCli.cambiarUsuarioActivo(nuevoCandidato);
        Utilidades.ejecutarComandoLinux(
                "sqlite3 registro_dokploy.db \"UPDATE usuarios SET fecha_inicio = datetime('now', 'localtime') WHERE username = '"
                        + nuevoCandidato + "';\"");

        // 4. Preparamos el nuevo servidor destino
        String nuevoServidor = GithubCli.obtenerNombreCodespace();
        VpsConexion.prepararVincularVps(nuevoServidor);
        ServicioBackup.prepararSistemaDokploy(nuevoServidor);
        ServicioBackup.migrarBackupDokploy(nuevoServidor);
    }
}
