package com.dokhub.app.integraciones;

import com.dokhub.app.utils.Utilidades;
import io.github.cdimascio.dotenv.Dotenv;

public class DockerMotor {

    private static final Dotenv dotenv = Dotenv.load();

    private DockerMotor() {
        throw new IllegalStateException("Clase de integración");
    }

    // Equivale a detener_procesos_dokploy()
    public static void detenerProcesos(String servidorActual) {
        System.out.println("\n🛑 Iniciando apagado seguro en: " + servidorActual + "...");
        String comando = "gh codespace ssh -c \"" + servidorActual + "\" -- \"sudo killall dockerd\"";

        String resultado = Utilidades.ejecutarComandoLinux(comando);
        if (!resultado.contains("Error al ejecutar")) {
            Utilidades.pausarTiempo(2, "s"); // Pausa de 2 segundos para estabilizar
            System.out.println("✅ Todos los procesos de Dokploy han sido detenidos correctamente.");
        } else {
            System.out.println("⚠️ Hubo un problema al intentar detener Docker, o ya estaba apagado.");
        }
    }

    // Equivale a iniciar_procesos_dokploy()
    public static void iniciarProcesos(String servidorDestino) {
        System.out.println("\n⚡ Iniciando servicios de Dokploy en: " + servidorDestino + "...");
        String comando = "gh codespace ssh -c \"" + servidorDestino + "\" -- \"sudo dockerd >/dev/null 2>&1 &\"";
        Utilidades.ejecutarComandoLinux(comando);

        System.out.println("   ⏳ Esperando 15 segundos para la estabilización del sistema...");
        Utilidades.pausarTiempo(15, "s");
        System.out.println("✅ ¡Sistema Dokploy totalmente operativo y en línea!");
    }

    // Equivale a forzar_despliegue_dokploy()
    public static boolean forzarDespliegue(String servidorActual) {
        System.out.println("\n🚀 Iniciando despliegue de aplicaciones en Dokploy...");
        String apiKey = dotenv.get("API-KEY-DOKPLOY");

        if (apiKey == null) {
            System.err.println("❌ Error: No se encontró API-KEY-DOKPLOY en el archivo .env.");
            return false;
        }

        // Como el script de despliegue es un bloque multilinea complejo en bash con jq,
        // lo más limpio es ejecutarlo pasándole la API KEY como argumento
        String scriptDespliegue = "cat <<'EOF' | gh codespace ssh -c \"" + servidorActual + "\" -- bash -s \"" + apiKey
                + "\"\n" +
                "DOKPLOY_TOKEN=\"$1\"\n" +
                "DOKPLOY_URL=\"http://localhost:3000/api\"\n" +
                "if ! command -v jq &> /dev/null; then sudo apt-get update >/dev/null && sudo apt-get install -y jq >/dev/null; fi\n"
                +
                "for i in {1..10}; do if curl -s -o /dev/null -f \"http://localhost:3000\"; then break; fi; sleep 3; done\n"
                +
                "PROJECTS_JSON=$(curl -s -X 'GET' \"$DOKPLOY_URL/project.all\" -H 'accept: application/json' -H \"x-api-key: $DOKPLOY_TOKEN\")\n"
                +
                "deploy() {\n" +
                "  local tipo=$1\n" +
                "  local id_key=$2\n" +
                "  mapfile -t ids < <(echo \"$PROJECTS_JSON\" | jq -r \".. | .${id_key}? | select(. != null)\" | sort -u)\n"
                +
                "  if [[ ${#ids[@]} -eq 0 ]]; then return 0; fi\n" +
                "  for id in \"${ids[@]}\"; do\n" +
                "      curl -s -o /dev/null -X POST \"$DOKPLOY_URL/${tipo}.deploy\" -H \"accept: application/json\" -H \"Content-Type: application/json\" -H \"x-api-key: $DOKPLOY_TOKEN\" -d \"{\\\"${id_key}\\\":\\\"${id}\\\"}\"\n"
                +
                "      sleep 1\n" +
                "  done\n" +
                "}\n" +
                "deploy \"postgres\" \"postgresId\"\n" +
                "deploy \"redis\" \"redisId\"\n" +
                "deploy \"application\" \"applicationId\"\n" +
                "EOF";

        Utilidades.ejecutarComandoLinux(scriptDespliegue);
        System.out.println("🎉 Misión cumplida: Sistema Dokploy 100% sincronizado y aplicaciones en línea.");
        return true;
    }
}
