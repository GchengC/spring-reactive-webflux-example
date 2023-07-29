package com.bolsadeideas.springboot.webflux.app.models.dao;

import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

/**
 * @author GchengC.
 **/
public interface ProductoDao extends ReactiveMongoRepository<Producto, String> {

    Mono<Producto> findByNombre(String nombre);

    @Query("{ 'nombre': ?0 }")
    Mono<Producto> obtenerNombre(String nombre);

}
