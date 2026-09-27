package com.dokhub.app.integraciones;

import com.dokhub.app.utils.Utilidades;

public class GithubCli {

    // Extrae el ID largo original tomando el primer resultado (.[0]) del JSON[cite:
    // 2]
    public static String obtenerNombreCodespace() {
        String comando = "gh codespace list --json name --jq '.[0].name'";
        return Utilidades.ejecutarComandoLinux(comando);
    }

    // Extrae el usuario activo filtrando la respuesta de 'gh auth status'[cite: 2]
    public static String obtenerUsuarioActivo() {
        String comando = "gh auth status 2>&1 | grep -B 1 'Active account: true' | head -n 1 | sed -E 's/.*(as|account) ([^ ]+).*/\\2/'";
        return Utilidades.ejecutarComandoLinux(comando);
    }

    // Cambia la cuenta activa en GitHub CLI[cite: 2]
    public static boolean cambiarUsuarioActivo(String nuevoUsuario) {
        if (nuevoUsuario == null || nuevoUsuario.trim().isEmpty()) {
            System.out.println("❌ Error: Nombre de usuario vacío.");
            return false;
        }

        System.out.println("🔄 Solicitando cambio de identidad a: " + nuevoUsuario + "...");
        String comando = "gh auth switch -u \"" + nuevoUsuario + "\"";
        String resultado = Utilidades.ejecutarComandoLinux(comando);

        // Si el comando arrojó fallos, lo detectamos
        if (resultado.toLowerCase().contains("failed") || resultado.toLowerCase().contains("error")) {
            System.out.println("⚠️ Fallo en la matriz: No se pudo cambiar a '" + nuevoUsuario + "'.");
            return false;
        }

        System.out.println("✅ ¡Identidad cambiada! Ahora estás operando al mando de: " + nuevoUsuario);
        return true;
    }
}
