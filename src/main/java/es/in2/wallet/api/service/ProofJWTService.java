package es.in2.wallet.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;

public interface ProofJWTService {
    Mono<JsonNode> buildCredentialRequest(String nonce, String issuer, String did);
}
