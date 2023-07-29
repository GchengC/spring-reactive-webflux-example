package models;

import java.util.ArrayList;
import java.util.List;

/**
 * @author GchengC.
 **/
public class Comentarios {

    private List<String> comentarios;

    public Comentarios() {
        this.comentarios = new ArrayList<>();
    }

    public Comentarios addComentario(String comentario) {
        this.comentarios.add(comentario);
        return this;
    }

    @Override
    public String toString() {
        return "comentarios=" + comentarios;
    }
}
