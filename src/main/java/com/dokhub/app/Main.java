package com.dokhub.app;

import com.dokhub.app.integraciones.GithubCli;
import com.dokhub.app.repositorios.RegistroUsuariosDB;
import com.dokhub.app.servicios.ServicioRotacion;
import com.dokhub.app.servicios.ServicioSincronizacion;
import com.dokhub.app.utils.Utilidades;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        System.out.println("🚀 INICIANDO GESTOR AUTOMÁTICO DE DOKPLOY (VERSIÓN JAVA)...");

        // 1. Configuramos la base de datos antes de arrancar cualquier proceso
        RegistroUsuariosDB.inicializarBaseDeDatos();

        // 2. Buscamos el servidor actual para pasarlo al guardián
        System.out.println("⏳ Buscando tu servidor Codespace...");
        String servidorActual = GithubCli.obtenerNombreCodespace();

        if (servidorActual == null || servidorActual.isEmpty() || servidorActual.contains("Error")) {
            System.err.println("❌ Error: No se encontró ningún Codespace activo.");
            System.exit(1);
        }
        System.out.println("✅ Conectado a mi servidor: " + servidorActual);

        // 3. Creamos el orquestador de tareas concurrentes
        ScheduledExecutorService orquestador = Executors.newScheduledThreadPool(2);

        // 4. Hilo 1: El Guardián y Radar de Puertos (Cada 15 segundos)
        Runnable radarPuertos = () -> {
            try {
                // Mantiene los túneles sanos tal como tu script "Guardián"
                ServicioSincronizacion.sincronizarPuertos(servidorActual);
            } catch (Exception e) {
                System.err.println("⚠️ Error no fatal en el Guardián: " + e.getMessage());
            }
        };

        // 5. Hilo 2: El Cerebro de Rotación (Cada 60 segundos)
        Runnable cerebroRotacion = () -> {
            try {
                System.out.println("\n-----------------------------------------------------");
                System.out.println("⏰ Ejecutando chequeo del sistema de usuarios...");
                System.out.println("-----------------------------------------------------");

                // Mismas validaciones que `iniciar_cerebro()`
                ServicioRotacion.gestionarCaidaUsuarioActivo();
                ServicioRotacion.ejecutarRotacionPorTiempo();

            } catch (Exception e) {
                System.err.println("⚠️ Error no fatal en el Cerebro: " + e.getMessage());
            }
        };

        // 6. Programamos las ejecuciones en paralelo
        orquestador.scheduleWithFixedDelay(radarPuertos, 0, 15, TimeUnit.SECONDS);
        orquestador.scheduleWithFixedDelay(cerebroRotacion, 5, 60, TimeUnit.SECONDS);

        // 7. Seguridad Anti-Zombies: Se activa si presionas Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 [Alerta] Apagando el sistema y limpiando memoria...");
            orquestador.shutdownNow();

            // Cierra los túneles activos de Fedora
            String puertosLocales = Utilidades.ejecutarComandoLinux(
                    "sudo lsof -i -P -n 2>/dev/null | grep LISTEN | grep gh | grep '*:' | awk '{print $9}' | cut -d':' -f2 | sort -nu | tr '\n' ' '");
            if (!puertosLocales.isEmpty()) {
                for (String puerto : puertosLocales.split("\\s+")) {
                    String pid = Utilidades
                            .ejecutarComandoLinux("sudo lsof -t -i TCP:" + puerto + " -s TCP:LISTEN 2>/dev/null");
                    if (!pid.isEmpty() && !pid.contains("Error")) {
                        Utilidades.ejecutarComandoLinux("sudo kill -9 " + pid);
                    }
                }
            }
            System.out.println("👋 ¡Hasta pronto! Entorno limpio.");
        }));
    }
}
