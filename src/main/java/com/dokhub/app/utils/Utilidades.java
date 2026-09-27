package com.dokhub.app.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Utilidades {

    private Utilidades() {
        throw new IllegalStateException("Clase de utilería");
    }

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

    public static String ejecutarComandoLinux(String comando) {
        StringBuilder salida = new StringBuilder();

        try {
            ProcessBuilder constructor = new ProcessBuilder("bash", "-c", comando);
            // Fusionamos los canales para capturar errores como si fuera la terminal real
            constructor.redirectErrorStream(true);

            Process proceso = constructor.start();
            BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            String linea;

            while ((linea = lector.readLine()) != null) {
                salida.append(linea).append("\n");
            }

            proceso.waitFor();
        } catch (IOException e) {
            return "Error IO: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Proceso interrumpido";
        }

        return salida.toString().trim();
    }
}
