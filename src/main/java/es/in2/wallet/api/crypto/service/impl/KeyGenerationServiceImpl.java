package es.in2.wallet.api.crypto.service.impl;

import es.in2.wallet.api.crypto.service.KeyGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;

@Service
@Slf4j
public class KeyGenerationServiceImpl implements KeyGenerationService {
    public Mono<KeyPair> generateECKeyPair() {
        return Mono.fromCallable(() -> {
            try {
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
                ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
                keyPairGenerator.initialize(ecSpec, new SecureRandom());
                return keyPairGenerator.generateKeyPair();
            } catch (Exception e) {
                throw new RuntimeException("Error generating EC key pair", e);
            }
        });
    }
}
