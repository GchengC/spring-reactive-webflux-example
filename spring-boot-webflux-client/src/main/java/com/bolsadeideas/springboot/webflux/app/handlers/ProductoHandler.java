package com.bolsadeideas.springboot.webflux.app.handlers;

import com.bolsadeideas.springboot.webflux.app.models.Producto;
import com.bolsadeideas.springboot.webflux.app.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author GchengC.
 **/
@Component
public class ProductoHandler {

    @Autowired
    private ProductoService productoService;

    @Value("${config.base.endpoint}")
    private String base;


    public Mono<ServerResponse> listar(ServerRequest serverRequest) {
        return ServerResponse.ok()
                .contentType(APPLICATION_JSON)
                .body(productoService.findAll(), Producto.class);
    }

    public Mono<ServerResponse> ver(ServerRequest serverRequest) {
        return this.errorHandler(productoService.findById(serverRequest.pathVariable("id"))
                .flatMap(p -> ServerResponse.ok().contentType(APPLICATION_JSON).bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build())
        );
    }

    public Mono<ServerResponse> crear(ServerRequest serverRequest) {
        Mono<Producto> productoMono = serverRequest.bodyToMono(Producto.class);

        return productoMono.flatMap(p -> {
                    if (p.getCreateAt() == null) {
                        p.setCreateAt(new Date());
                    }
                    return productoService.save(p);
                })
                .flatMap(p -> ServerResponse.created(URI.create(this.base.concat(p.getId())))
                        .contentType(APPLICATION_JSON)
                        .bodyValue(p))
                .onErrorResume(error -> {
                    WebClientResponseException errorResponse = (WebClientResponseException) error;
                    if (errorResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        return ServerResponse.badRequest()
                                .contentType(APPLICATION_JSON)
                                .bodyValue(errorResponse.getResponseBodyAsString());
                    }

                    return Mono.error(errorResponse);
                });
    }

    public Mono<ServerResponse> editar(ServerRequest serverRequest) {
        Mono<Producto> productoMono = serverRequest.bodyToMono(Producto.class);
        String id = serverRequest.pathVariable("id");

        return this.errorHandler(productoMono
                .flatMap(p -> productoService.update(p, id))
                .flatMap(p -> ServerResponse.created(URI.create(this.base.concat(p.getId())))
                        .contentType(APPLICATION_JSON)
                        .bodyValue(p))
                .onErrorResume(error -> {
                    WebClientResponseException errorResponse = (WebClientResponseException) error;
                    if (errorResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return ServerResponse.notFound().build();
                    }
                    return Mono.error(errorResponse);
                })
        );
    }

    public Mono<ServerResponse> eliminar(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");

        return this.errorHandler(productoService.delete(id).then(ServerResponse.noContent().build()));
    }

    public Mono<ServerResponse> upload(ServerRequest serverRequest) {
        String id = serverRequest.pathVariable("id");
        return this.errorHandler(serverRequest.multipartData()
                .map(multipart -> multipart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(filePart -> productoService.upload(filePart, id))
                .flatMap(p -> ServerResponse.created(URI.create(this.base.concat(id)))
                        .contentType(APPLICATION_JSON)
                        .bodyValue(p))
        );
    }

    private Mono<ServerResponse> errorHandler(Mono<ServerResponse> response) {
        return response.onErrorResume(error -> {
            WebClientResponseException errorResponse = (WebClientResponseException) error;
            if (errorResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
                Map<String, Object> body = new HashMap<>();
                body.put("error", "No existe el producto: ".concat(errorResponse.getMessage()));
                body.put("timestamp", new Date());
                body.put("status", errorResponse.getStatusCode().value());
                return ServerResponse.status(HttpStatus.NOT_FOUND)
                        .bodyValue(body);
            }
            return Mono.error(errorResponse);
        });
    }

}
