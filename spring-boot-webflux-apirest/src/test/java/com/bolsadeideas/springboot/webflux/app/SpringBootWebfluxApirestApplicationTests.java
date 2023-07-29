package com.bolsadeideas.springboot.webflux.app;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.services.ProductoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

@AutoConfigureWebTestClient //Siempre acompaña a "SpringBootTest.WebEnvironment.MOCK"
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SpringBootWebfluxApirestApplicationTests {

    @Autowired
    private WebTestClient client;

    @Autowired
    private ProductoService productoService;

    @Value("${config.base.endpoint}")
    private String base;

    @Test
    void listarProductos() {
        client.get()
                .uri(this.base)
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Producto.class)
//                .hasSize(9)
                .consumeWith(response -> {
                    List<Producto> productos = response.getResponseBody();
                    productos.forEach(p -> System.out.println("p.getNombre() = " + p.getNombre()));

                    Assertions.assertTrue(productos.size() > 0);
                })
        ;

    }

    @Test
    void ver() {
        Producto productoMono = productoService.findByNombre("TV panasonic Pantalla LCD").block();

        client.get()
                .uri(this.base.concat("/{id}"), Collections.singletonMap("id", productoMono.getId()))
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
//                .expectBody()
//                .jsonPath("$.id").isNotEmpty()
//                .jsonPath("$.nombre").isEqualTo("TV panasonic Pantalla LCD")
                .expectBody(Producto.class)
                .consumeWith(response -> {
                    Producto producto = response.getResponseBody();
                    Assertions.assertNotNull(producto.getId());
                    Assertions.assertTrue(producto.getId().length() > 0);
                    Assertions.assertEquals("TV panasonic Pantalla LCD", producto.getNombre());
                })
        ;
    }

    @Test
    public void crearTest() {
        Categoria categoria = productoService.findCategoriaByNombre("Muebles").block();

        Producto producto = new Producto("Mesa Comedor", 100.00, categoria);
        client.post().uri(this.base)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .body(Mono.just(producto), Producto.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.producto.id").isNotEmpty()
//                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.producto.nombre").isEqualTo("Mesa Comedor")
//                .jsonPath("$.nombre").isEqualTo("Mesa Comedor")
                .jsonPath("$.producto.categoria.nombre").isEqualTo("Muebles")
//                .jsonPath("$.categoria.nombre").isEqualTo("Muebles")
        ;
    }

    @Test
    public void crear2Test() {
        Categoria categoria = productoService.findCategoriaByNombre("Muebles").block();

        Producto producto = new Producto("Mesa Comedor", 100.00, categoria);
        client.post().uri(this.base)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .body(Mono.just(producto), Producto.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
//                .expectBody(Producto.class)
                .expectBody(new ParameterizedTypeReference<LinkedHashMap<String, Object>>() {
                })
                .consumeWith(response -> {
//                    Producto p = response.getResponseBody();
                    Object o = response.getResponseBody().get("producto");
                    Producto p = new ObjectMapper().convertValue(o, Producto.class);

                    Assertions.assertNotNull(p.getId());
                    Assertions.assertTrue(p.getId().length() > 0);
                    Assertions.assertEquals(producto.getNombre(), p.getNombre());
                    Assertions.assertEquals(categoria.getNombre(), p.getCategoria().getNombre());
                })
        ;
    }

    @Test
    public void editarTest() {
        Producto producto = productoService.findByNombre("Sony Notebook").block();
        Categoria categoria = productoService.findCategoriaByNombre("Electronico").block();
        Producto productoEditado = new Producto("Asus Notebook", 700.00, categoria);

        client.put().uri(this.base.concat("/{id}"), Collections.singletonMap("id", producto.getId()))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .accept(MediaType.APPLICATION_PROBLEM_JSON)
                .body(Mono.just(productoEditado), Producto.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.nombre").isEqualTo("Asus Notebook")
                .jsonPath("$.categoria.nombre").isEqualTo("Electronico")
        ;

    }

    @Test
    public void eliminar() {
        Producto producto = productoService.findByNombre("Mica Comoda 5 cajones").block();

        client.delete()
                .uri(this.base.concat("/{id}"), Collections.singletonMap("id", producto.getId()))
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        client.get()
                .uri(this.base.concat("/{id}"), Collections.singletonMap("id", producto.getId()))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().isEmpty();

    }
}
