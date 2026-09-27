package com.dokhub.app.integraciones;

import com.dokhub.app.utils.Utilidades;

public class DockerMotor {

    public static void detenerProcesos(String servidor) {
        System.out.println("🛑 Iniciando apagado seguro en: " + servidor + "...");
        // Usamos sudo killall para detener el demonio de Docker y sus contenedores
        // hijos
        String comando = "gh codespace ssh -c \"" + servidor + "\" -- \"sudo killall dockerd\" >/dev/null 2>&1";
        Utilidades.ejecutarComandoLinux(comando);

        // Le damos un respiro de 2 segundos para que los procesos mueran por
        // completo[cite: 1]
        Utilidades.pausarTiempo(2, "s");
        System.out.println("✅ Todos los procesos de Dokploy han sido detenidos.");
    }

    public static void iniciarProcesos(String servidor) {
        System.out.println("⚡ Iniciando servicios de Dokploy en: " + servidor + "...");
        // Levantamos el motor de Docker en segundo plano[cite: 1]
        String comando = "gh codespace ssh -c \"" + servidor + "\" -- \"sudo dockerd >/dev/null 2>&1 &\"";
        Utilidades.ejecutarComandoLinux(comando);

        System.out.println("⏳ Esperando 15 segundos para la estabilización del sistema...");
        // Tiempo de gracia extendido para asegurar que el panel cargue bien[cite: 1]
        Utilidades.pausarTiempo(15, "s");
    }

    public static void limpiarServiciosResiduales(String servidor) {
        System.out.println("🧼 Limpiando servicios residuales de Docker Swarm...");
        // Protegemos los núcleos de Dokploy y borramos el resto[cite: 1]
        String comando = "gh codespace ssh -c \"" + servidor
                + "\" -- \"docker service ls --format '{{.Name}}' | grep -vE '^dokploy$|^dokploy-postgres$|^dokploy-redis$|^dokploy-traefik$' | xargs -r docker service rm\" >/dev/null 2>&1";
        Utilidades.ejecutarComandoLinux(comando);
        Utilidades.pausarTiempo(10, "s");
    }
}
