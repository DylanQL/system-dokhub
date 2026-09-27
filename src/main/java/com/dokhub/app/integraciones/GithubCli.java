package com.dokhub.app.integraciones;

import com.dokhub.app.utils.Utilidades;

public class GithubCli {

    private GithubCli() {
        throw new IllegalStateException("Clase de integración");
    }

    // Equivale a obtener_nombre_codespace()
    public static String obtenerNombreCodespace() {
        // Extrae el ID largo original tomando el primer resultado del JSON
        String comando = "gh codespace list --json name --jq '.[0].name'";
        return Utilidades.ejecutarComandoLinux(comando);
    }

    // Equivale a obtener_usuario_github_activo()
    public static String obtenerUsuarioActivo() {
        // Filtra el status para encontrar la cuenta marcada como "Active account: true"
        String comando = "gh auth status 2>&1 | grep -B 1 'Active account: true' | head -n 1 | sed -E 's/.*(as|account) ([^ ]+).*/\\2/'";
        return Utilidades.ejecutarComandoLinux(comando);
    }

    // Equivale a obtener_todos_los_usuarios_github()
    public static String obtenerTodosLosUsuarios() {
        // Atrapa tanto las conexiones exitosas como los tokens caídos/fallidos
        String comando = "gh auth status 2>&1 | grep -E -i '(Logged in to|Failed)' | sed -E 's/.*(as|account) ([^ ]+).*/\\2/' | sort -u | tr '\n' ' '";
        return Utilidades.ejecutarComandoLinux(comando);
    }

    // Equivale a cambiar_usuario_activo_github()
    public static boolean cambiarUsuarioActivo(String nuevoUsuario) {
        if (nuevoUsuario == null || nuevoUsuario.trim().isEmpty()) {
            System.err.println("❌ Error: No se proporcionó ningún nombre de usuario para el cambio.");
            return false;
        }

        System.out.println("🔄 Solicitando cambio de identidad a: " + nuevoUsuario + "...");

        // Ejecutamos el switch. Si el código interno devuelve error, lo capturamos
        String comando = "gh auth switch -u '" + nuevoUsuario + "'";
        String resultado = Utilidades.ejecutarComandoLinux(comando);

        if (resultado.contains("Error al ejecutar")) {
            System.err.println("⚠️ Fallo en la matriz: No se pudo cambiar a '" + nuevoUsuario + "'. ¿Token válido?");
            return false;
        }

        System.out.println("✅ ¡Identidad cambiada! Ahora estás operando al mando de: " + nuevoUsuario);
        return true;
    }
}
