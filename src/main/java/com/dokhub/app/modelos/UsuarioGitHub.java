package com.dokhub.app.modelos;

import java.time.LocalDateTime;

public class UsuarioGitHub {
    private String username;
    private boolean bloqueado;
    private boolean usado;
    private LocalDateTime fechaInicio;

    public UsuarioGitHub(String username, boolean bloqueado, boolean usado, LocalDateTime fechaInicio) {
        this.username = username;
        this.bloqueado = bloqueado;
        this.usado = usado;
        this.fechaInicio = fechaInicio;
    }

    public String getUsername() {
        return username;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }

    public boolean isUsado() {
        return usado;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }
}
