package com.dokhub.app;

import com.dokhub.app.integraciones.GithubCli;
import com.dokhub.app.repositorios.RegistroUsuariosDB;
import com.dokhub.app.servicios.ServicioRotacion;
import com.dokhub.app.servicios.ServicioSincronizacion;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        System.out.println("🚀 INICIANDO GESTOR AUTOMÁTICO DE DOKPLOY (SISTEMA DOKHUB)...");

        // 1. Verificamos o creamos la tabla en SQLite
        RegistroUsuariosDB.inicializarBaseDeDatos();

        // 2. Pool de 2 hilos para ejecutar el radar y el cerebro en paralelo sin
        // bloquearse
        ScheduledExecutorService orquestador = Executors.newScheduledThreadPool(2);

        // Tarea 1: El Cerebro (Se ejecuta cada 60 segundos)
        Runnable tareaCerebro = () -> {
            try {
                System.out.println("\n-----------------------------------------------------");
                System.out.println("⏰ [Cerebro] Ejecutando chequeo del sistema...");
                System.out.println("-----------------------------------------------------");

                // Protocolo de emergencia: caída de token
                ServicioRotacion.gestionarCaidaUsuarioActivo();

                // Protocolo de rutina: límite de 27 horas
                ServicioRotacion.ejecutarRotacionPorTiempo();

            } catch (Exception e) {
                System.err.println("⚠️ Error en el ciclo del Cerebro: " + e.getMessage());
            }
        };

        // Tarea 2: El Guardián y Sincronizador de Puertos (Se ejecuta cada 15 segundos)
        Runnable tareaRadarPuertos = () -> {
            try {
                String servidorActual = GithubCli.obtenerNombreCodespace();

                if (servidorActual != null && !servidorActual.trim().isEmpty()) {
                    ServicioSincronizacion.sincronizarPuertos(servidorActual);
                } else {
                    System.out.println("⚠️ [Radar] No se detectó ningún Codespace activo para sincronizar puertos.");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error en el ciclo del Radar: " + e.getMessage());
            }
        };

        // Programación de los dos bucles continuos
        // scheduleAtFixedRate(tarea, retardoInicial, intervalo, unidadTiempo)
        orquestador.scheduleAtFixedRate(tareaCerebro, 0, 60, TimeUnit.SECONDS);
        orquestador.scheduleAtFixedRate(tareaRadarPuertos, 5, 15, TimeUnit.SECONDS);

        // 3. Shutdown Hook: Trampa de salida segura ante Ctrl+C o cierre abrupto
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 [Alerta] Deteniendo orquestador y cerrando hilos de fondo...");
            orquestador.shutdown();
            try {
                if (!orquestador.awaitTermination(3, TimeUnit.SECONDS)) {
                    orquestador.shutdownNow();
                }
            } catch (InterruptedException e) {
                orquestador.shutdownNow();
                Thread.currentThread().interrupt();
            }
            System.out.println("👋 ¡Hasta pronto! Entorno limpio.");
        }));
    }
}
