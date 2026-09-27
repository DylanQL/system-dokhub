package com.dokhub.app.servicios;

import com.dokhub.app.integraciones.DockerMotor;
import com.dokhub.app.utils.Utilidades;
import io.github.cdimascio.dotenv.Dotenv;

public class ServicioBackup {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String VPS_IP = dotenv.get("VPS-IP-ADDRESS");
    private static final String VPS_USER = dotenv.get("VPS-USERNAME");

    private ServicioBackup() {
        throw new IllegalStateException("Clase de servicio");
    }

    // Equivale a migrar_backup_dokploy()
    public static boolean migrarBackup(String servidorActual) {
        System.out.println("\n🚀 Iniciando restauración de datos en el nuevo servidor...");
        System.out.println("   🌐 Conectando al VPS para identificar el último backup...");

        Utilidades.ejecutarComandoLinux("chmod 400 LLave.pem");

        // 1. Identificar la última carpeta de backup en el VPS
        String cmdUltimoBackup = "ssh -i LLave.pem -o StrictHostKeyChecking=no " + VPS_USER + "@" + VPS_IP +
                " \"ls -1 /home/" + VPS_USER + "/backups_dokploy/ | grep '^202' | sort | tail -n 1\"";
        String ultimoBackup = Utilidades.ejecutarComandoLinux(cmdUltimoBackup);

        if (ultimoBackup == null || ultimoBackup.isEmpty() || ultimoBackup.contains("Error")) {
            System.err.println("❌ Error crítico: No se encontró ningún backup válido en el VPS.");
            return false;
        }

        System.out.println("   ⬇️ Transfiriendo backup: [" + ultimoBackup + "] hacia " + servidorActual + "...");

        // 2. Ejecutar la sincronización inversa (VPS -> Codespace)
        String scriptRestauracion = "gh codespace ssh -c \"" + servidorActual
                + "\" -- \"echo '   📂 Restaurando configuraciones de Dokploy...' && " +
                "sudo rsync -avz -e 'ssh -i /home/vscode/LLave.pem -o StrictHostKeyChecking=no' " + VPS_USER + "@"
                + VPS_IP + ":/home/" + VPS_USER + "/backups_dokploy/" + ultimoBackup + "/dokploy/ /etc/dokploy/ && " +
                "echo '   📂 Restaurando volúmenes de Docker...' && " +
                "sudo rsync -avz -e 'ssh -i /home/vscode/LLave.pem -o StrictHostKeyChecking=no' " + VPS_USER + "@"
                + VPS_IP + ":/home/" + VPS_USER + "/backups_dokploy/" + ultimoBackup
                + "/volumes/ /var/lib/docker/volumes/\"";

        Utilidades.ejecutarComandoLinux(scriptRestauracion);
        Utilidades.pausarTiempo(10, "s");

        // 3. Reiniciar y estabilizar
        DockerMotor.iniciarProcesos(servidorActual);
        sincronizarCredencialesBaseDatos(servidorActual);

        Utilidades.pausarTiempo(10, "s");
        System.out.println("   🔄 Realizando reinicio final para estabilizar servicios...");

        DockerMotor.detenerProcesos(servidorActual);
        DockerMotor.iniciarProcesos(servidorActual);
        DockerMotor.forzarDespliegue(servidorActual);

        System.out.println("🎉 ¡Migración completada y sistema reiniciado exitosamente!");
        return true;
    }

    private static void sincronizarCredencialesBaseDatos(String servidorActual) {
        System.out.println("   🔑 Sincronizando credenciales de la base de datos...");
        String scriptCredenciales = "gh codespace ssh -c \"" + servidorActual + "\" -- \"" +
                "CONTENEDOR_PG=\\$(docker ps -qf 'name=dokploy-postgres' | head -n 1) && " +
                "if [ -n \\\"\\$CONTENEDOR_PG\\\" ]; then " +
                "CLAVE_PG=\\$(docker exec \\\"\\$CONTENEDOR_PG\\\" cat /run/secrets/postgres_password) && " +
                "docker exec -e PGPASSWORD='x' \\\"\\$CONTENEDOR_PG\\\" psql -U dokploy -d dokploy -c \\\"ALTER USER dokploy WITH PASSWORD '\\$CLAVE_PG';\\\" >/dev/null 2>&1 && "
                +
                "echo '   ✅ Contraseña de base de datos sincronizada.'; fi\"";

        Utilidades.ejecutarComandoLinux(scriptCredenciales);
    }
}
