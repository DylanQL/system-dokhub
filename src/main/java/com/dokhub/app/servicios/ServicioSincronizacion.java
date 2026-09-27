package com.dokhub.app.servicios;

import com.dokhub.app.utils.Utilidades;
import java.util.Arrays;
import java.util.List;

public class ServicioSincronizacion {

    public static void sincronizarPuertos(String servidor) {
        System.out.println("🔄 Iniciando sincronización inteligente de puertos...");

        // 1. Extraemos los puertos de Docker (Lo que Dokploy necesita)
        // Usamos la misma lógica de grep y cut que tenías en Bash
        String comandoDocker = "gh codespace ssh -c \"" + servidor
                + "\" -- \"docker ps --format '{{.Ports}}'\" 2>/dev/null | grep -oE '[0-9]+->' | cut -d'-' -f1 | sort -nu";
        String resultadoDocker = Utilidades.ejecutarComandoLinux(comandoDocker);
        List<String> puertosDocker = Arrays.asList(resultadoDocker.split("\\s+"));

        // 2. Extraemos los puertos locales (Lo que Fedora ya tiene)
        String comandoLocal = "sudo lsof -i -P -n 2>/dev/null | grep LISTEN | grep gh | grep '\\*:' | awk '{print $9}' | cut -d':' -f2 | sort -nu";
        String resultadoLocal = Utilidades.ejecutarComandoLinux(comandoLocal);
        List<String> puertosLocales = Arrays.asList(resultadoLocal.split("\\s+"));

        System.out.println("📦 Puertos que Dokploy necesita: " + String.join(" ", puertosDocker));
        System.out.println("💻 Puertos que Fedora ya tiene:  " + String.join(" ", puertosLocales));

        // 3. IDENTIFICAR QUÉ SOBRA (Están en local, pero ya no en Docker)
        for (String pLocal : puertosLocales) {
            if (!pLocal.trim().isEmpty() && !puertosDocker.contains(pLocal)) {
                System.out.println("🗑️ Se detectó túnel obsoleto. Cerrando puerto: " + pLocal);
                String cerrarTubo = "sudo kill -9 $(sudo lsof -t -i TCP:" + pLocal
                        + " -s TCP:LISTEN 2>/dev/null) 2>/dev/null";
                Utilidades.ejecutarComandoLinux(cerrarTubo);
            }
        }

        // 4. IDENTIFICAR QUÉ FALTA (Están en Docker, pero no en local)
        for (String pDocker : puertosDocker) {
            if (!pDocker.trim().isEmpty() && !puertosLocales.contains(pDocker)) {
                System.out.println("✨ Se detectó túnel faltante. Abriendo puerto: " + pDocker);
                String abrirTubo = "sudo -E gh codespace ports forward \"" + pDocker + ":" + pDocker + "\" -c \""
                        + servidor + "\" >/dev/null 2>&1 &";
                Utilidades.ejecutarComandoLinux(abrirTubo);
            }
        }

        System.out.println("✅ Sincronización de túneles finalizada.");
    }
}
