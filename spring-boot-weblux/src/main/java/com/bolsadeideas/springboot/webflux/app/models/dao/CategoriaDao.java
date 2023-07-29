package com.bolsadeideas.springboot.webflux.app.models.dao;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * @author GchengC.
 **/
public interface CategoriaDao extends ReactiveMongoRepository<Categoria, String> {
}
