package com.bolsadeideas.springboot.webflux.app.controllers;

import com.bolsadeideas.springboot.webflux.app.models.dao.ProductoDao;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author GchengC.
 **/
@RestController
@RequestMapping(path = "/api/productos")
public class ProductoRestController {

    @Autowired
    private ProductoDao dao;

    private static final Logger log = LoggerFactory.getLogger(ProductoRestController.class);

    @GetMapping()
    public Flux<Producto> index() {
        Flux<Producto> productoFlux = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return producto;
                }).doOnNext(producto -> log.info(producto.getNombre()));


        return productoFlux;
    }

    @GetMapping(path = "/{id}")
    public Mono<Producto> show(@PathVariable String id) {
//        Mono<Producto> productoMono = dao.findById(id);
        Mono<Producto> productoMono = dao.findAll()
                .filter(producto -> producto.getId().equals(id))
                .next()
                .doOnNext(producto -> log.info(producto.getNombre()));

        return productoMono;
    }

}
