package com.bolsadeideas.springboot.webflux.app.controllers;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.services.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

/**
 * @author GchengC.
 **/
@SessionAttributes("producto")
@Controller
public class ProductoController {

    @Autowired
    private ProductoService service;

    @Value("${config.uploads.path}")
    private String path;

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

    @ModelAttribute("categorias")
    public Flux<Categoria> categorias() {
        return service.findAllCategoria();
    }


    @GetMapping(path = "/uploads/img/{nombreFoto:.+}")
    public Mono<ResponseEntity<Resource>> verFoto(@PathVariable String nombreFoto) throws MalformedURLException {
        Path ruta = Paths.get(this.path).resolve(nombreFoto).toAbsolutePath();

        Resource imagen = new UrlResource(ruta.toUri());

        return Mono.just(
                ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + imagen.getFilename() + "\"")
                        .body(imagen));
    }

    @GetMapping(path = "/ver/{id}")
    public Mono<String> ver(Model model, @PathVariable String id) {
        return service.findById(id)
                .doOnNext(p -> {
                    model.addAttribute("productos", p);
                    model.addAttribute("titulo", "Listado de Productos");
                })
                .switchIfEmpty(Mono.just(new Producto()))
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(p);
                }).then(Mono.just("ver"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=No+existe+el+producto"));

    }

    @GetMapping(path = {"/", "/listar"})
    public Mono<String> listar(Model model) {
        Flux<Producto> productoFlux = service.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return producto;
                });

        productoFlux.subscribe(producto -> log.info(producto.getNombre()));
        model.addAttribute("productos", productoFlux);
        model.addAttribute("titulo", "Listado de Productos");
        return Mono.just("listar");
    }

    @GetMapping(path = "/form")
    public Mono<String> crear(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("titulo", "Formulario de producto");
        model.addAttribute("boton", "Crear");
        return Mono.just("form");
    }

    @GetMapping(path = "/form/{id}")
    public Mono<String> editar(@PathVariable String id, Model model) {
        Mono<Producto> productoMono = service.findById(id)
                .doOnNext(p -> log.info("Producto: " + p.getNombre()))
                .defaultIfEmpty(new Producto());

        model.addAttribute("titulo", "Editar Producto")
                .addAttribute("producto", productoMono)
                .addAttribute("boton", "Editar");


        return Mono.just("/form");
    }

    @GetMapping(path = "/form-v2/{id}")
    public Mono<String> editarV2(@PathVariable String id, Model model) {
        return service.findById(id)
                .doOnNext(p -> {
                    log.info("Producto: " + p.getNombre());
                    model.addAttribute("titulo", "Editar Producto")
                            .addAttribute("producto", p)
                            .addAttribute("boton", "Editar");
                })
                .defaultIfEmpty(new Producto())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto"));
                    }
                    return Mono.just(p);
                })
                .then(Mono.just("form"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=No+existe+el+producto"));


    }

    @PostMapping(path = "/form")
    public Mono<String> guardar(@Valid Producto producto, BindingResult result,
                                Model model,
                                @RequestPart FilePart file,
                                SessionStatus status) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Errores en formulario de Producto")
                    .addAttribute("boton", "Guardar");
            return Mono.just("form");
        }
        status.setComplete();

        Mono<Categoria> categoriaMono = service.findCategoriaById(producto.getCategoria().getId());

        return categoriaMono
                .flatMap(categoria -> {
                    if (producto.getCreateAt() == null) {
                        producto.setCreateAt(new Date());
                    }

                    if (!file.filename().isEmpty()) {
                        producto.setFoto(UUID.randomUUID().toString() +
                                "-" +
                                file.filename()
                                        .replaceAll(" :", "")
                                        .replace("\\", ""));
                    }

                    producto.setCategoria(categoria);
                    return service.save(producto);
                })
                .doOnNext(p -> log.info("Categoria Asignada: " + p.getCategoria().getNombre() + "Id: " + p.getId()))
                .doOnNext(p -> log.info("Producto guardado: " + p.getNombre() + "Id: " + p.getId()))
                .flatMap(p -> {
                    if (!file.filename().isEmpty()) {
                        return file.transferTo(new File(this.path + p.getFoto()));
                    }
                    return Mono.empty();
                })
                .thenReturn("redirect:/listar?success=producto+guardado+con+exito");
//                .then(Mono.just("redirect:/listar"));
    }

    @GetMapping(path = "/eliminar/{id}")
    public Mono<String> eliminar(@PathVariable String id) {
        return service.findById(id)
                .defaultIfEmpty(new Producto())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("No existe el producto a eliminar!"));
                    }
                    return Mono.just(p);
                })
                .doOnNext(p -> log.info("Eliminando producto: " + p.getNombre()))
                .doOnNext(p -> log.info("Eliminando producto Id: " + p.getId()))
                .flatMap(service::delete)
                .then(Mono.just("redirect:/listar?success=producto+eliminado+con+existo"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=No+existe+el+producto+a+eliminar"));
    }

    @GetMapping(path = "/listar-datadriver")
    public String listarDataDriver(Model model) {
        Flux<Producto> productoFlux = service.findAllConNombreUpperCase()
                .delayElements(Duration.ofSeconds(1));

        productoFlux.subscribe(producto -> log.info(producto.getNombre()));
        model.addAttribute("productos",
                new ReactiveDataDriverContextVariable(productoFlux, 1));
        model.addAttribute("titulo", "Listado de Productos");
        return "listar";
    }

    @GetMapping(path = "/listar-full")
    public String listarFull(Model model) {
        Flux<Producto> productoFlux = service.findAllConNombreUpperCaseRepeat();

        model.addAttribute("productos", productoFlux);
        model.addAttribute("titulo", "Listado de Productos");
        return "listar";
    }

    @GetMapping(path = "/listar-chunked")
    public String listarChunked(Model model) {
        Flux<Producto> productoFlux = service.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return producto;
                }).repeat(5000);

        model.addAttribute("productos", productoFlux);
        model.addAttribute("titulo", "Listado de Productos");
        return "listar-chunked";
    }
}
