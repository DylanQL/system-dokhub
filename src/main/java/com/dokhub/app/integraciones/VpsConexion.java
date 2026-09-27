package com.dokhub.app.integraciones;

import com.dokhub.app.utils.Utilidades;
import io.github.cdimascio.dotenv.Dotenv;

public class VpsConexion {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String VPS_IP = dotenv.get("VPS-IP-ADDRESS");
    private static final String VPS_USER = dotenv.get("VPS-USERNAME");

    public static void prepararVincularVps(String servidor) {
        System.out.println("🔑 [CONFIG] Configurando acceso seguro al VPS desde el Codespace " + servidor + "...");

        if (VPS_IP == null || VPS_USER == null) {
            throw new IllegalStateException(
                    "❌ Error: No se pudo obtener la IP o el Usuario del VPS desde el archivo .env");
        }

        System.out.println("   📦 Transfiriendo llave privada (LLave.pem)...");
        String inyectarLlave = "cat LLave.pem | gh codespace ssh -c \"" + servidor
                + "\" -- \"sudo tee \\$HOME/LLave.pem > /dev/null && sudo chown \\$(whoami) \\$HOME/LLave.pem && chmod 400 \\$HOME/LLave.pem\"";
        Utilidades.ejecutarComandoLinux(inyectarLlave);

        System.out.println("   🔐 Ajustando permisos y pre-aprobando identidad del VPS...");
        String aceptarHost = "gh codespace ssh -c \"" + servidor
                + "\" -- \"ssh -o StrictHostKeyChecking=accept-new -i \\$HOME/LLave.pem " + VPS_USER + "@" + VPS_IP
                + " exit\" >/dev/null 2>&1";
        Utilidades.ejecutarComandoLinux(aceptarHost);

        System.out.println("✅ Conexión Codespace -> VPS autorizada y lista.");
    }

    // Métodos para que el ServicioBackup pueda saber a dónde enviar los archivos
    public static String getVpsIp() {
        return VPS_IP;
    }

    public static String getVpsUser() {
        return VPS_USER;
    }
}
