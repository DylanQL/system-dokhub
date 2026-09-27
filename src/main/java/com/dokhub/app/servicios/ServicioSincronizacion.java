package com.dokhub.app.servicios;

import com.dokhub.app.utils.Utilidades;
import java.util.Arrays;
import java.util.List;

public class ServicioSincronizacion {

    private ServicioSincronizacion() {
        throw new IllegalStateException("Clase de servicio");
    }

    // Equivale a obtener_puertos_docker_del_codespace()
    private static String obtenerPuertosDocker(String servidor) {
        String comando = "gh codespace ssh -c \"" + servidor
                + "\" -- \"docker ps --format '{{.Ports}}'\" 2>/dev/null | grep -oE '[0-9]+->' | cut -d'-' -f1 | sort -nu | tr '\n' ' '";
        return Utilidades.ejecutarComandoLinux(comando);
    }

    // Equivale a obtener_puertos_locales_vinculados_gh()
    private static String obtenerPuertosLocales() {
        String comando = "sudo lsof -i -P -n 2>/dev/null | grep LISTEN | grep gh | grep '*:' | awk '{print $9}' | cut -d':' -f2 | sort -nu | tr '\n' ' '";
        return Utilidades.ejecutarComandoLinux(comando);
    }

    // Equivale a sincronizar_puertos()
    public static void sincronizarPuertos(String servidor) {
        System.out.println("🔄 Iniciando sincronización inteligente de puertos...");

        String puertosDocker = obtenerPuertosDocker(servidor);
        String puertosLocales = obtenerPuertosLocales();

        System.out.println("📦 Puertos que Dokploy necesita: [" + puertosDocker + "]");
        System.out.println("💻 Puertos que Fedora ya tiene:  [" + puertosLocales + "]");

        List<String> listaDocker = Arrays.asList(puertosDocker.split("\\s+"));
        List<String> listaLocales = Arrays.asList(puertosLocales.split("\\s+"));

        StringBuilder puertosACerrar = new StringBuilder();
        StringBuilder puertosAAbrir = new StringBuilder();

        // 1. Identificar qué sobra (Están en local, pero ya no en Docker)
        for (String pLocal : listaLocales) {
            if (!pLocal.isEmpty() && !listaDocker.contains(pLocal)) {
                puertosACerrar.append(pLocal).append(" ");
            }
        }

        if (puertosACerrar.length() > 0) {
            System.out.println(
                    "🗑️ Se detectaron túneles obsoletos. Cerrando: [" + puertosACerrar.toString().trim() + "]");
            cerrarPuertos(puertosACerrar.toString().trim());
        }

        // 2. Identificar qué falta (Están en Docker, pero no en local)
        for (String pDocker : listaDocker) {
            if (!pDocker.isEmpty() && !listaLocales.contains(pDocker)) {
                puertosAAbrir.append(pDocker).append(" ");
            }
        }

        if (puertosAAbrir.length() > 0) {
            System.out
                    .println("✨ Se detectaron túneles faltantes. Abriendo: [" + puertosAAbrir.toString().trim() + "]");
            abrirPuertos(servidor, puertosAAbrir.toString().trim());
        } else {
            System.out.println("✅ Todo está perfectamente sincronizado. No se requieren nuevos túneles.");
        }
    }

    // Equivale a detener_puertos_locales_vinculados_gh()
    private static void cerrarPuertos(String puertos) {
        for (String puerto : puertos.split("\\s+")) {
            String comandoPid = "sudo lsof -t -i TCP:" + puerto + " -s TCP:LISTEN 2>/dev/null";
            String pid = Utilidades.ejecutarComandoLinux(comandoPid);

            if (!pid.isEmpty() && !pid.contains("Error")) {
                Utilidades.ejecutarComandoLinux("sudo kill -9 " + pid);
                System.out.println("   🔒 Túnel en el puerto " + puerto + " cerrado exitosamente.");
            }
        }
    }

    // Equivale a vincular_puertos_locales_gh()
    private static void abrirPuertos(String servidor, String puertos) {
        for (String puerto : puertos.split("\\s+")) {
            System.out.println("   🔗 Levantando túnel para el puerto " + puerto + "...");
            String comando = "sudo -E gh codespace ports forward \"" + puerto + ":" + puerto + "\" -c \"" + servidor
                    + "\" >/dev/null 2>&1 &";
            Utilidades.ejecutarComandoLinux(comando);
        }
        Utilidades.pausarTiempo(1, "s");
        System.out.println("✅ Nuevos túneles establecidos en segundo plano.");
    }
}
