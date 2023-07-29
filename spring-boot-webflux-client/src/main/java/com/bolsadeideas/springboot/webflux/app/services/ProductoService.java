package com.bolsadeideas.springboot.webflux.app.services;

import com.bolsadeideas.springboot.webflux.app.models.Producto;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author GchengC.
 **/
public interface ProductoService {

    Flux<Producto> findAll();

    Mono<Producto> findById(String id);

    Mono<Producto> save(Producto producto);

    Mono<Producto> update(Producto producto, String id);

    Mono<Void> delete(String id);

    Mono<Producto> upload(FilePart file, String id);
}
