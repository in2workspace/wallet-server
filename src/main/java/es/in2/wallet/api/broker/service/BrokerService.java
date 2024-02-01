package es.in2.wallet.api.broker.service;

import reactor.core.publisher.Mono;

public interface BrokerService {

    Mono<Boolean> checkIfEntityAlreadyExist(String processId, String userId);
    Mono<Void> postEntity(String processId, String userId, String requestBody);

    Mono<String> getEntityById(String processId,  String userId);

    Mono<Void> updateEntity(String processId,  String userId, String requestBody);

}