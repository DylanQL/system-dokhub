package com.dokhub.app.integraciones;

import com.dokhub.app.utils.Utilidades;
import io.github.cdimascio.dotenv.Dotenv;

public class VpsConexion {

    // Cargamos las variables de entorno de tu archivo .env
    private static final Dotenv dotenv = Dotenv.load();
    private static final String VPS_IP = dotenv.get("VPS-IP-ADDRESS");
    private static final String VPS_USER = dotenv.get("VPS-USERNAME");

    private VpsConexion() {
        throw new IllegalStateException("Clase de integración");
    }

    // Equivale a preparar_vincular_vps()
    public static boolean prepararConexionSegura(String servidorCodespace) {
        System.out.println("\n🔑 [CONFIG] Configurando acceso seguro al VPS desde: " + servidorCodespace);

        if (VPS_IP == null || VPS_USER == null) {
            System.err.println("❌ Error: No se encontró VPS-IP-ADDRESS o VPS-USERNAME en el .env");
            return false;
        }

        // 1. Transferimos la llave
        String cmdCopiarLlave = "cat LLave.pem | gh codespace ssh -c \"" + servidorCodespace
                + "\" -- \"sudo tee \\$HOME/LLave.pem > /dev/null\"";
        Utilidades.ejecutarComandoLinux(cmdCopiarLlave);

        // 2. Ajustamos permisos y autorizamos conexión
        String cmdPermisos = "gh codespace ssh -c \"" + servidorCodespace
                + "\" -- \"sudo chown \\$(whoami) \\$HOME/LLave.pem && chmod 400 \\$HOME/LLave.pem && ssh -o StrictHostKeyChecking=accept-new -i \\$HOME/LLave.pem "
                + VPS_USER + "@" + VPS_IP + " exit\"";
        Utilidades.ejecutarComandoLinux(cmdPermisos);

        System.out.println("✅ Conexión Codespace -> VPS autorizada y lista.");
        return true;
    }

    // Equivale a crear_backup_dokploy()
    public static boolean crearBackup(String servidorOrigen) {
        System.out.println("\n📦 Iniciando creación de backup en: " + servidorOrigen + "...");

        // Obtenemos un timestamp limpio usando bash date
        String timestamp = Utilidades.ejecutarComandoLinux("date +'%Y-%m-%d_%H-%M-%S'");
        String rutaRemota = "/home/" + VPS_USER + "/backups_dokploy/" + timestamp;

        String scriptBackup = "gh codespace ssh -c \"" + servidorOrigen
                + "\" -- \"ssh -i /home/vscode/LLave.pem -o StrictHostKeyChecking=no " + VPS_USER + "@" + VPS_IP
                + " 'mkdir -p " + rutaRemota + "/dokploy " + rutaRemota
                + "/volumes' && sudo rsync -avz -e 'ssh -i /home/vscode/LLave.pem -o StrictHostKeyChecking=no' /etc/dokploy/ "
                + VPS_USER + "@" + VPS_IP + ":" + rutaRemota
                + "/dokploy/ && sudo rsync -avz -e 'ssh -i /home/vscode/LLave.pem -o StrictHostKeyChecking=no' /var/lib/docker/volumes/ "
                + VPS_USER + "@" + VPS_IP + ":" + rutaRemota + "/volumes/\"";

        String resultado = Utilidades.ejecutarComandoLinux(scriptBackup);
        if (resultado.contains("Error al ejecutar")) {
            System.err.println("❌ Error crítico durante la transferencia del backup.");
            return false;
        }

        System.out.println("✅ Backup generado y enviado al VPS exitosamente en: " + rutaRemota);
        return true;
    }
}
