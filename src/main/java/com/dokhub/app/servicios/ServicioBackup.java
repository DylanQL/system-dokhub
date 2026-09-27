package com.dokhub.app.servicios;

import com.dokhub.app.integraciones.DockerMotor;
import com.dokhub.app.integraciones.VpsConexion;
import com.dokhub.app.utils.Utilidades;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServicioBackup {

    public static void crearBackupDokploy(String servidor) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String vpsUser = VpsConexion.getVpsUser();
        String vpsIp = VpsConexion.getVpsIp();
        String rutaRemota = "/home/" + vpsUser + "/backups_dokploy/" + timestamp;

        DockerMotor.detenerProcesos(servidor);
        System.out.println("📦 Iniciando creación de backup. Destino en VPS: " + timestamp);

        String comandoBackup = "gh codespace ssh -c \"" + servidor + "\" -- \"" +
                "ssh -i /home/vscode/LLave.pem -o StrictHostKeyChecking=no " + vpsUser + "@" + vpsIp + " 'mkdir -p "
                + rutaRemota + "/dokploy " + rutaRemota + "/volumes' && " +
                "sudo rsync -avz -e 'ssh -i /home/vscode/LLave.pem -o StrictHostKeyChecking=no' /etc/dokploy/ "
                + vpsUser + "@" + vpsIp + ":" + rutaRemota + "/dokploy/ && " +
                "sudo rsync -avz -e 'ssh -i /home/vscode/LLave.pem -o StrictHostKeyChecking=no' /var/lib/docker/volumes/ "
                + vpsUser + "@" + vpsIp + ":" + rutaRemota + "/volumes/\"";

        Utilidades.ejecutarComandoLinux(comandoBackup);
        System.out.println("✅ Backup generado y enviado al VPS exitosamente.");
    }

    public static void prepararSistemaDokploy(String servidor) {
        System.out.println("🧹 Purgando base de datos y configuraciones antiguas en " + servidor + "...");
        DockerMotor.detenerProcesos(servidor);

        String purgarDirs = "gh codespace ssh -c \"" + servidor
                + "\" -- \"sudo rm -rf /etc/dokploy /var/lib/docker/volumes && sudo mkdir -p /etc/dokploy /var/lib/docker/volumes\" >/dev/null 2>&1";
        Utilidades.ejecutarComandoLinux(purgarDirs);

        DockerMotor.iniciarProcesos(servidor);
        DockerMotor.limpiarServiciosResiduales(servidor);
        DockerMotor.detenerProcesos(servidor);
    }

    public static void migrarBackupDokploy(String servidor) {
        System.out.println("🚀 Iniciando restauración de datos en el nuevo servidor...");
        String vpsUser = VpsConexion.getVpsUser();
        String vpsIp = VpsConexion.getVpsIp();

        String extraerUltimo = "ssh -i LLave.pem -o StrictHostKeyChecking=no " + vpsUser + "@" + vpsIp
                + " \"ls -1 /home/" + vpsUser + "/backups_dokploy/ | grep '^202' | sort | tail -n 1\"";
        String ultimoBackup = Utilidades.ejecutarComandoLinux(extraerUltimo);

        String comandoRestaurar = "gh codespace ssh -c \"" + servidor + "\" -- \"" +
                "sudo rsync -avz -e 'ssh -i /home/vscode/LLave.pem -o StrictHostKeyChecking=no' " + vpsUser + "@"
                + vpsIp + ":/home/" + vpsUser + "/backups_dokploy/" + ultimoBackup + "/dokploy/ /etc/dokploy/ && " +
                "sudo rsync -avz -e 'ssh -i /home/vscode/LLave.pem -o StrictHostKeyChecking=no' " + vpsUser + "@"
                + vpsIp + ":/home/" + vpsUser + "/backups_dokploy/" + ultimoBackup
                + "/volumes/ /var/lib/docker/volumes/\" >/dev/null 2>&1";
        Utilidades.ejecutarComandoLinux(comandoRestaurar);

        DockerMotor.iniciarProcesos(servidor);
        System.out.println("🎉 ¡Migración completada exitosamente!");
    }
}
