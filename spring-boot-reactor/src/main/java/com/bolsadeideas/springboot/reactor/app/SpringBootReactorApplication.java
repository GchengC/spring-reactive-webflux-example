package com.bolsadeideas.springboot.reactor.app;

import models.Comentarios;
import models.Usuario;
import models.UsuarioComentarios;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootReactorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        this.ejemploIntervalInfinito();
    }

    public void ejemploContrapresion() {
        Flux.range(1, 10)
                .log()
                .limitRate(5)
                .subscribe();
    }

    public void ejemploContrapresionManual() {
        Flux.range(1, 10)
                .log()
                .subscribe(new Subscriber<Integer>() {
                    private Subscription s;
                    private Integer limite = 5;
                    private Integer consumido = 0;

                    @Override
                    public void onSubscribe(Subscription s) {
                        this.s = s;
                        s.request(limite);
                    }

                    @Override
                    public void onNext(Integer integer) {
                        log.info(integer.toString());
                        consumido++;
                        if (consumido.equals(limite)) {
                            consumido = 0;
                            s.request(limite);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void ejemploIntervalDesdeCreate() {
        Flux.create(emitter -> {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                private Integer contador = 0;

                @Override
                public void run() {
                    emitter.next(++contador);
                    if (contador == 10) {
                        timer.cancel();
                        emitter.complete();
                    } else if (contador == 5) {
                        timer.cancel();
                        emitter.error(new InterruptedException("Error, se ha detenido el flux en "
                                .concat(String.valueOf(contador))
                                .concat("!")));
                    }
                }
            }, 1000, 1000);
        })
//                .doOnNext(next -> log.info(next.toString()))
//                .doOnComplete(()-> log.info("Hemos terminado"))
                .subscribe(next -> log.info(next.toString()),
                        error -> log.error(error.getMessage()),
                        () -> log.info("Hemos terminado"));
    }

    public void ejemploIntervalInfinito() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        Flux.interval(Duration.ofSeconds(1))
                .doOnTerminate(latch::countDown)
                .flatMap(i -> {
                    if (i >= 5) {
                        return Flux.error(new InterruptedException("Solo hasta 5!"));
                    }
                    return Flux.just(i);
                })
                .map(i -> "Hola " + i)
                .retry(2)
                .subscribe(log::info, e -> log.error(e.getMessage()));

        latch.await();
    }

    public void ejemploDelayElements() throws InterruptedException {
        Flux<Integer> rango = Flux.range(1, 12)
                .delayElements(Duration.ofSeconds(1))
                .doOnNext(i -> log.info(i.toString()));

        rango.blockLast();

//        Thread.sleep(13000);
    }

    public void ejemploInterval() {
        Flux<Integer> rango = Flux.range(1, 12);
        Flux<Long> retraso = Flux.interval(Duration.ofSeconds(1));

        rango.zipWith(retraso, (ra, re) -> ra)
                .doOnNext(i -> log.info(i.toString()))
                .blockLast();
    }

    public void ejemploZipWithRangos() {
        Flux<Integer> rangos = Flux.range(0, 4);
        Flux.just(1, 2, 3, 4, 5)
                .map(i -> (i * 2))
                .zipWith(rangos,
                        (uno, dos) -> String.format("Primer Flux: %d, Segundo Flux: %d", uno, dos))
                .subscribe(log::info,
                        error -> log.error(error.getMessage()),
                        new Runnable() {
                            @Override
                            public void run() {
                                log.info("Finalizado");
                            }
                        });
    }

    public void ejemploUsuarioComentariosZipWithForma2() {
        Mono<Usuario> usuarioMono = Mono.fromCallable(this::crearUsuario);
        Mono<Comentarios> comentariosMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            return comentarios.addComentario("Hola Pepe, que tal!")
                    .addComentario("Mañana voy a la playa!")
                    .addComentario("Estoy tomando el curso de spring con reactor");

        });

        Mono<UsuarioComentarios> usuarioComentariosMono =
                usuarioMono.zipWith(comentariosMono)
                        .map(tuple -> {
                            Usuario u = tuple.getT1();
                            Comentarios c = tuple.getT2();
                            return new UsuarioComentarios(u, c);
                        });
        usuarioComentariosMono.subscribe(uc -> log.info(uc.toString()));
    }

    public void ejemploUsuarioComentariosZipWith() {
        Mono<Usuario> usuarioMono = Mono.fromCallable(this::crearUsuario);
        Mono<Comentarios> comentariosMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            return comentarios.addComentario("Hola Pepe, que tal!")
                    .addComentario("Mañana voy a la playa!")
                    .addComentario("Estoy tomando el curso de spring con reactor");

        });

        Mono<UsuarioComentarios> usuarioComentariosMono = usuarioMono.zipWith(comentariosMono, UsuarioComentarios::new);
        usuarioComentariosMono.subscribe(uc -> log.info(uc.toString()));
    }

    public void ejemploUsuarioComentariosFlatMap() {
        Mono<Usuario> usuarioMono = Mono.fromCallable(this::crearUsuario);
        Mono<Comentarios> comentariosMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            return comentarios.addComentario("Hola Pepe, que tal!")
                    .addComentario("Mañana voy a la playa!")
                    .addComentario("Estoy tomando el curso de spring con reactor");

        });

        usuarioMono.flatMap(u -> comentariosMono.map(c -> new UsuarioComentarios(u, c)))
                .subscribe(uc -> log.info(uc.toString()));
    }

    public void ejemploCollectList() throws Exception {
        List<Usuario> usuariosList = new ArrayList<>();
        usuariosList.add(new Usuario("Andres", "Guzman"));
        usuariosList.add(new Usuario("Pedro", "Fulano"));
        usuariosList.add(new Usuario("Maria", "Fulana"));
        usuariosList.add(new Usuario("Diego", "Sultano"));
        usuariosList.add(new Usuario("Juan", "Mengano"));
        usuariosList.add(new Usuario("Bruce", "Lee"));
        usuariosList.add(new Usuario("Bruce", "Willis"));

        Flux.fromIterable(usuariosList)
                .collectList()
                .subscribe(list -> list.forEach(item -> log.info(item.toString())));
    }

    public void ejemploToString() throws Exception {
        List<Usuario> usuariosList = new ArrayList<>();
        usuariosList.add(new Usuario("Andres", "Guzman"));
        usuariosList.add(new Usuario("Pedro", "Fulano"));
        usuariosList.add(new Usuario("Maria", "Fulana"));
        usuariosList.add(new Usuario("Diego", "Sultano"));
        usuariosList.add(new Usuario("Juan", "Mengano"));
        usuariosList.add(new Usuario("Bruce", "Lee"));
        usuariosList.add(new Usuario("Bruce", "Willis"));

        Flux.fromIterable(usuariosList)
                .map(usuario -> usuario.getNombre().toUpperCase().concat(" ").concat(usuario.getApellido().toUpperCase()))
                .flatMap(nombre -> {
                    if (nombre.contains("bruce".toUpperCase())) {
                        return Mono.just(nombre);
                    } else {
                        return Mono.empty();
                    }
                })
                .map(String::toLowerCase)
                .subscribe(log::info);
    }

    public void ejemploFlatMap() throws Exception {
        List<String> usuariosList = new ArrayList<>();
        usuariosList.add("Andres Guzman");
        usuariosList.add("Pedro Fulano");
        usuariosList.add("Maria Fulana");
        usuariosList.add("Diego Sultano");
        usuariosList.add("Juan Mengano");
        usuariosList.add("Bruce Lee");
        usuariosList.add("Bruce Willis");

        Flux.fromIterable(usuariosList)
                .map(nombre -> new Usuario(nombre.split(" ")[0].toUpperCase(),
                        nombre.split(" ")[1].toUpperCase()))
                .flatMap(usuario -> {
                    if (usuario.getNombre().equalsIgnoreCase("bruce")) {
                        return Mono.just(usuario);
                    } else {
                        return Mono.empty();
                    }
                })
                .map(usuario -> {
                    usuario.setNombre(usuario.getNombre().toLowerCase());
                    return usuario;
                })
                .subscribe(u -> log.info(u.toString()));
    }

    public void ejemploIterable() throws Exception {
        List<String> usuariosList = new ArrayList<>();
        usuariosList.add("Andres Guzman");
        usuariosList.add("Pedro Fulano");
        usuariosList.add("Maria Fulana");
        usuariosList.add("Diego Sultano");
        usuariosList.add("Juan Mengano");
        usuariosList.add("Bruce Lee");
        usuariosList.add("Bruce Willis");

        Flux<String> nombres = Flux.fromIterable(usuariosList); /*Flux.just("Andres Guzman", "Pedro Fulano", "Maria Fulana", "Diego Sultano", "Juan Mengano", "Bruce Lee", "Bruce Willis");*/

        Flux<Usuario> usuarios = nombres.map(nombre -> new Usuario(nombre.split(" ")[0].toUpperCase(), nombre.split(" ")[1].toUpperCase()))
                .filter(usuario -> usuario.getNombre().equalsIgnoreCase("bruce"))
                .doOnNext(usuario -> {
                    if (usuario == null) {
                        throw new RuntimeException("Nombres no puede ser vacíos");
                    }
                    System.out.println(usuario.getNombre().concat(" ").concat(usuario.getApellido()));
                })
                .map(usuario -> {
                    usuario.setNombre(usuario.getNombre().toLowerCase());
                    return usuario;
                });

        usuarios.subscribe(e -> log.info(e.toString()),
                error -> log.error(error.getMessage()),
                new Runnable() {
                    @Override
                    public void run() {
                        log.info("Ha finalizado la ejecución del observable con exito!");
                    }
                });
    }

    private Usuario crearUsuario() {
        return new Usuario("John", "Doe");
    }

}
