package es.in2.wallet.application.port;

import reactor.core.publisher.Mono;

import java.util.Optional;

public interface BrokerService {

    Mono<Void> postEntity(String processId, String requestBody);

    Mono<Optional<String>> verifyIfWalletUserExistById(String processId, String userId);
    Mono<String> getCredentialsByUserId(String processId, String userId);
    Mono<String> getCredentialByAndUserId(String processId, String  userId, String credentialId);
    Mono<Void> deleteCredentialByIdAndUserId(String processId, String  userId, String credentialId);
    Mono<String> getCredentialByCredentialTypeAndUserId(String processId, String  userId, String credentialType);
    Mono<String> getTransactionThatIsLinkedToACredential(String processId, String credentialId);

    Mono<Void> updateEntity(String processId,  String entityId, String requestBody);
    Mono<Void> deleteTransactionByTransactionId(String processId, String transactionId);

}
