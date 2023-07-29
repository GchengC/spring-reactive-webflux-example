package com.bolsadeideas.springboot.webflux.app.models.dao;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

/**
 * @author GchengC.
 **/
public interface CategoriaDao extends ReactiveMongoRepository<Categoria, String> {

    Mono<Categoria> findByNombre(String nombre);
}
