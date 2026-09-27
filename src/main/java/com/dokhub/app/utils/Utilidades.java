package com.dokhub.app.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Utilidades {

    // Bloqueamos la creación de objetos, es solo una clase de herramientas
    private Utilidades() {
        throw new IllegalStateException("Clase de utilería");
    }

    // Tu método mejorado para manejar pausas exactas
    public static void pausarTiempo(int tiempo, String unidad) {
        long valorConversion = switch (unidad.toLowerCase()) {
            case "s" -> 1000L;
            case "m" -> 60000L;
            case "h" -> 3600000L;
            default -> throw new IllegalArgumentException("Unidad de tiempo no válida: " + unidad);
        };

        try {
            Thread.sleep(tiempo * valorConversion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // El motor para ejecutar bash (y capturar los errores correctamente)
    public static String ejecutarComandoLinux(String comando) {
        StringBuilder salida = new StringBuilder();

        try {
            ProcessBuilder constructor = new ProcessBuilder("bash", "-c", comando);
            // Redirigimos el canal de errores estándar para leerlo como en la terminal
            constructor.redirectErrorStream(true);

            Process proceso = constructor.start();
            BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            String linea;

            while ((linea = lector.readLine()) != null) {
                salida.append(linea).append("\n");
            }

            int codigoEstado = proceso.waitFor();
            if (codigoEstado != 0) {
                return "Error al ejecutar (Código " + codigoEstado + "):\n" + salida.toString().trim();
            }

        } catch (IOException e) {
            return "Error de lectura/escritura: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "El proceso en consola fue interrumpido.";
        }

        return salida.toString().trim();
    }
}
