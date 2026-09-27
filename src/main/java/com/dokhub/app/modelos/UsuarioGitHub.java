package com.dokhub.app.modelos;

public class UsuarioGitHub {

    // Las mismas columnas exactas de tu base de datos registro_dokploy.db
    private String username;
    private String bloqueado; // En SQLite lo guardas como texto 'true' / 'false'
    private String usado;
    private String fechaInicio;
    private String fechaFin;
    private double horasRealizadas;
    private String cumplioEsperadas;

    public UsuarioGitHub(String username, String bloqueado, String usado, String fechaInicio,
            String fechaFin, double horasRealizadas, String cumplioEsperadas) {
        this.username = username;
        this.bloqueado = bloqueado;
        this.usado = usado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.horasRealizadas = horasRealizadas;
        this.cumplioEsperadas = cumplioEsperadas;
    }

    // --- Getters (Para leer los datos) ---
    public String getUsername() {
        return username;
    }

    public String getBloqueado() {
        return bloqueado;
    }

    public String getUsado() {
        return usado;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public String getFechaFin() {
        return fechaFin;
    }

    public double getHorasRealizadas() {
        return horasRealizadas;
    }

    public String getCumplioEsperadas() {
        return cumplioEsperadas;
    }

    // --- Setters (Para modificar los datos en memoria antes de guardar) ---
    public void setBloqueado(String bloqueado) {
        this.bloqueado = bloqueado;
    }

    public void setUsado(String usado) {
        this.usado = usado;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaFin(String fechaFin) {
        this.fechaFin = fechaFin;
    }

    public void setHorasRealizadas(double horasRealizadas) {
        this.horasRealizadas = horasRealizadas;
    }

    public void setCumplioEsperadas(String cumplioEsperadas) {
        this.cumplioEsperadas = cumplioEsperadas;
    }
}
