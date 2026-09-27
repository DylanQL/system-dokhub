package com.dokhub.app.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Utilidades {

    private Utilidades() {
        throw new IllegalStateException("Clase de utilería");
    }

    public static void pausarTiempo(int tiempo, String unidad) {
        // Segundos("s" ó "S") -- Minutos("m" ó "M") -- Horas("h" ó "H")

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
            // 1. Preparamos el comando para que se ejecute dentro del intérprete bash de tu
            // sistema
            ProcessBuilder constructor = new ProcessBuilder("bash", "-c", comando);

            // 2. Iniciamos el proceso (es como abrir una mini terminal invisible)
            Process proceso = constructor.start();

            // 3. Leemos lo que la terminal nos escupe (el resultado del "echo", por
            // ejemplo)
            BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            String linea;

            while ((linea = lector.readLine()) != null) {
                salida.append(linea).append("\n");
            }

            // 4. Esperamos a que el comando termine de ejecutarse completamente
            int codigoEstado = proceso.waitFor();

            // Si el código no es 0, significa que el comando de Linux falló
            if (codigoEstado != 0) {
                return "Error al ejecutar. Código de salida: " + codigoEstado;
            }

        } catch (IOException e) {
            return "Error de lectura/escritura: " + e.getMessage();
        } catch (InterruptedException e) {
            // Nuestra regla de oro: restablecer la interrupción
            Thread.currentThread().interrupt();
            return "El proceso en consola fue interrumpido.";
        }

        // Retornamos el texto limpio, quitando espacios o saltos de línea extra al
        // final
        return salida.toString().trim();
    }

}
