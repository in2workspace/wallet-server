package es.in2.wallet.infrastructure.broker.service;

import reactor.core.publisher.Mono;

import java.util.Optional;


public interface GenericBrokerService {

    Mono<Void> postEntity(String processId, String requestBody);

    Mono<Optional<String>>  getEntityById(String processId, String  userId);

    Mono<Void> updateEntity(String processId,  String  userId, String requestBody);

}
