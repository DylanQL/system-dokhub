package com.dokhub.app.utils;

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

}
