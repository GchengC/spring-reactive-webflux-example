package com.bolsadeideas.springboot.webflux.app.services;

import com.bolsadeideas.springboot.webflux.app.models.Producto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

/**
 * @author GchengC.
 **/
@Service
public class ProductoServiceImpl implements ProductoService {

    @Autowired
    private WebClient.Builder client;

    @Override
    public Flux<Producto> findAll() {
        return client.build().get().accept(APPLICATION_JSON)
                .exchangeToFlux(clientResponse -> clientResponse.bodyToFlux(Producto.class));
    }

    @Override
    public Mono<Producto> findById(String id) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", id);
        return client.build().get()
//                .uri("/{id}", Collections.singletonMap("id", id))
                .uri("/{id}", params)
                .accept(APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Producto.class);
//                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(Producto.class));
    }

    @Override
    public Mono<Producto> save(Producto producto) {
        return client.build().post()
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
//                .body(fromValue(producto))
                .bodyValue(producto)
                .retrieve()
                .bodyToMono(Producto.class)
                ;
    }

    @Override
    public Mono<Producto> update(Producto producto, String id) {
        return client.build().put()
                .uri("/{id}", Collections.singletonMap("id", id))
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
//                .body(fromValue(producto))
                .bodyValue(producto)
                .retrieve()
                .bodyToMono(Producto.class)
                ;
    }

    @Override
    public Mono<Void> delete(String id) {
        return client.build().delete()
                .uri("/{id}", Collections.singletonMap("id", id))
                .retrieve()
                .bodyToMono(Void.class)
                ;
    }

    @Override
    public Mono<Producto> upload(FilePart file, String id) {
        MultipartBodyBuilder parts = new MultipartBodyBuilder();
        parts.asyncPart("file", file.content(), DataBuffer.class)
                .headers(h -> {
                    h.setContentDispositionFormData("file", file.filename());
                });
        return client.build().post()
                .uri("/upload/{id}", Collections.singletonMap("id", id))
                .contentType(MULTIPART_FORM_DATA)
                .bodyValue(parts.build())
                .retrieve()
                .bodyToMono(Producto.class);
    }
}
