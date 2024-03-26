package es.in2.wallet.api.ebsi.comformance.controller;

import es.in2.wallet.infrastructure.ebsi.config.EbsiConfig;
import es.in2.wallet.infrastructure.ebsi.controller.EbsiDidController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EbsiDidControllerTest {
    @Mock
    private EbsiConfig ebsiConfig;
    @InjectMocks
    private EbsiDidController ebsiDidController;
    
    @Test
    void testGetEbsiDid() {
        when(ebsiConfig.getDid()).thenReturn(Mono.just("did:key:1234"));
        // Act & Assert
        WebTestClient
                .bindToController(ebsiDidController)
                .build()
                .get()
                .uri("/api/v1/ebsi-did")
                .exchange()
                .expectStatus().isOk();
    }

}
