package com.bolsadeideas.springboot.webflux.app;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.services.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import java.util.Date;

@SpringBootApplication
@EnableEurekaClient
public class SpringBootWebfluxApirestApplication implements CommandLineRunner {
    @Autowired
    private ProductoService productoService;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApirestApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebfluxApirestApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        mongoTemplate.dropCollection("productos").subscribe();
        mongoTemplate.dropCollection("categorias").subscribe();

        Categoria electronico = new Categoria("Electronico");
        Categoria deporte = new Categoria("Deporte");
        Categoria computacion = new Categoria("Computacion");
        Categoria muebles = new Categoria("Muebles");

        Flux.just(electronico, deporte, computacion, muebles)
                .flatMap(productoService::saveCategoria)
                .doOnNext(c -> log.info("Categoria creada: " + c.getNombre()))
                .thenMany(Flux.just(new Producto("TV panasonic Pantalla LCD", 456.89, electronico),
                        new Producto("Sony Camara HD Digital", 177.89, electronico),
                        new Producto("Apple iPod", 46.89, electronico),
                        new Producto("Sony Notebook", 846.89, computacion),
                        new Producto("Hewlett Packard Multifuncional", 200.89, computacion),
                        new Producto("Bianchi Bicicleta", 70.89, deporte),
                        new Producto("HP Notebook Omen 17", 2500.89, computacion),
                        new Producto("Mica Comoda 5 cajones", 150.89, muebles),
                        new Producto("Tv Sony Bravia OLED 4K Ultra HD", 2255.89, electronico))
                                .flatMap(producto -> {
                                    producto.setCreateAt(new Date());
                                    return productoService.save(producto);
                                })
//                        .subscribe(producto -> log.info("Insert: " + producto.getId() + " " + producto.getNombre()));
                )
                .subscribe();
    }
}
