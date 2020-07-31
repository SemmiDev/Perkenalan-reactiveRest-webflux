package com.sammidev.demo;

import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.reactivestreams.Publisher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;


@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}
//
//	@Bean
//	RouterFunction<ServerResponse> route(ReserveationRepository rr) {
//		return RouterFunctions
//				.route()
//				.GET("/reservations", serverRequest -> ok().body(rr.findAll(), Reservation.class))
//				.build();
//	}
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest {

	private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse {

	private String greeting;
}

@Component
class IntervalMessageProducer {

	Flux<GreetingResponse> produceGreeting(GreetingRequest name) {
		return Flux.fromStream(Stream.generate(() -> "hello " + name.getName() + " -> " + Instant.now()))
				.map(GreetingResponse::new)
				.delayElements(Duration.ofSeconds(1));
	}
}

@RestController
@RequiredArgsConstructor
class ReservationController{

	private final ReserveationRepository reserveationRepository;
	private final IntervalMessageProducer imp;

	@GetMapping("/reservations")
	Publisher<Reservation> reservationPublisher() {
		return this.reserveationRepository.findAll();
	}

	@GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "sse/{n}")
	Publisher<GreetingResponse> StringPublisher (@PathVariable String n) {
		return this.imp.produceGreeting(new GreetingRequest(n));
	}
}

@Component
@RequiredArgsConstructor
@Log4j2
class SampleDataInitializer {

	private final ReserveationRepository reserveationRepository;

	@EventListener(ApplicationReadyEvent.class)
	public void initialize() {

		var saved = Flux
				.just("Sammidev", "Ayatullah", "Adis", "Dandi", "Adit", "Rauf", "Gusnur")
				.map(name -> new Reservation(null, name))
				.flatMap(this.reserveationRepository::save);

		reserveationRepository
				.deleteAll()
				.thenMany(saved)
				.thenMany(this.reserveationRepository.findAll())
				.subscribe(log::info);
	}
}
interface ReserveationRepository extends ReactiveCrudRepository<Reservation, String> {}

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {

	@Id
	private String id;
	private String name;
}