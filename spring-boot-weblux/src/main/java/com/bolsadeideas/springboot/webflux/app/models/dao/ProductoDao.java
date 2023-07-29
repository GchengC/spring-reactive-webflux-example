package com.bolsadeideas.springboot.webflux.app.models.dao;

import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * @author GchengC.
 **/
public interface ProductoDao extends ReactiveMongoRepository<Producto, String> {


}
