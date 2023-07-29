package com.bolsadeideas.springboot.webflux.app.models;

/**
 * @author GchengC.
 **/
public class Categoria {

    private String id;
    private String nombre;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
