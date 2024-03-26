package es.in2.wallet.infrastructure.ebsi.controller;

import es.in2.wallet.infrastructure.core.config.SwaggerConfig;
import es.in2.wallet.infrastructure.ebsi.config.EbsiConfig;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1/ebsi-did")
@RequiredArgsConstructor
public class EbsiDidController {

    private final EbsiConfig ebsiConfig;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            tags = (SwaggerConfig.TAG_PUBLIC)
    )
    public Mono<String> getEbsiDid() {
        return ebsiConfig.getDid();
    }

}
