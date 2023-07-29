package com.bolsadeideas.springboot.webflux.app.handlers;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;

/**
 * @author GchengC.
 **/
@Component
public class ProductoHandler {

    @Autowired
    private ProductoService service;

    @Value("${config.uploads.path}")
    private String path;

    @Autowired
    private Validator validator;

    public Mono<ServerResponse> crearConFoto(ServerRequest request) {
        Mono<Producto> productoMono = request.multipartData()
                .map(multiPart -> {
                    FormFieldPart nombre = (FormFieldPart) multiPart.toSingleValueMap().get("nombre");
                    FormFieldPart precio = (FormFieldPart) multiPart.toSingleValueMap().get("precio");
                    FormFieldPart categoriaId = (FormFieldPart) multiPart.toSingleValueMap().get("categoria.id");
                    FormFieldPart categoriaNombre = (FormFieldPart) multiPart.toSingleValueMap().get("categoria.nombre");

                    Categoria categoria = new Categoria(categoriaNombre.value());
                    categoria.setId(categoriaId.value());

                    return new Producto(nombre.value(), Double.parseDouble(precio.value()), categoria);
                });

        return request.multipartData()
                .map(multiPart -> multiPart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(filePart -> productoMono
                        .flatMap(p -> {
                            if (p.getCreateAt() == null) {
                                p.setCreateAt(new Date());
                            }
                            p.setFoto(UUID.randomUUID().toString()
                                    .concat("-")
                                    .concat(filePart.filename()
                                            .replaceAll("\s:", "")
                                            .replace("\\", "")));
                            return filePart.transferTo(new File(path + p.getFoto())).then(service.save(p));
                        }))
                .flatMap(p -> ServerResponse.created(
                        URI.create(request.path()
                                .substring(0, request.path().indexOf("crear"))
                                .concat(p.getId())))
                        .body(fromValue(p))
                );
    }

    public Mono<ServerResponse> upload(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.multipartData()
                .map(multiPart -> multiPart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(filePart -> service.findById(id)
                        .flatMap(p -> {
                            p.setFoto(UUID.randomUUID().toString()
                                    .concat("-")
                                    .concat(filePart.filename()
                                            .replaceAll("\s:", "")
                                            .replace("\\", "")));
                            return filePart.transferTo(new File(path + p.getFoto())).then(service.save(p));
                        }))
                .flatMap(p -> ServerResponse.created(
                        URI.create(request.path()
                                .substring(0, request.path().indexOf("upload"))
                                .concat(p.getId())))
                        .body(fromValue(p))
                )
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> listar(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findAll(), Producto.class);
    }

    public Mono<ServerResponse> ver(ServerRequest request) {
        String id = request.pathVariable("id");
        return service.findById(id).flatMap(p -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(fromValue(p)))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> crear(ServerRequest request) {
        Mono<Producto> productoMono = request.bodyToMono(Producto.class);

        return productoMono.flatMap(p -> {
            Errors errors = new BeanPropertyBindingResult(p, Producto.class.getName());
            validator.validate(p, errors);
            if (errors.hasErrors()) {
                return Flux.fromIterable(errors.getFieldErrors())
                        .map(fieldError -> "El campo "
                                .concat(fieldError.getField())
                                .concat(" ")
                                .concat(fieldError.getDefaultMessage()))
                        .collectList()
                        .flatMap(list -> ServerResponse.badRequest().body(fromValue(list)));
            }
            if (p.getCreateAt() == null) {
                p.setCreateAt(new Date());
            }

            return service.save(p).flatMap(pdb ->
                    ServerResponse.created(URI.create(request.path().concat("/").concat(pdb.getId())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(fromValue(pdb)));
        }).switchIfEmpty(ServerResponse.badRequest().build());
    }

    public Mono<ServerResponse> editar(ServerRequest request) {
        Mono<Producto> productoRequest = request.bodyToMono(Producto.class);
        String id = request.pathVariable("id");
        Mono<Producto> productoDb = service.findById(id);

        return productoDb.zipWith(productoRequest,
                (db, req) -> {
                    db.setNombre(req.getNombre());
                    db.setPrecio(req.getPrecio());
                    db.setCategoria(req.getCategoria());
                    return db;
                })
                .flatMap(service::save)
                .flatMap(p -> ServerResponse.created(URI.create(request.path()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromValue(p)))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> eliminar(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<Producto> productoDb = service.findById(id);

        return productoDb.flatMap(p -> service.delete(p).then(ServerResponse.noContent().build()))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}
