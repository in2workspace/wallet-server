package es.in2.wallet.infrastructure.ebsi.controller;

import es.in2.wallet.infrastructure.core.config.SwaggerConfig;
import es.in2.wallet.application.service.EbsiCredentialService;
import es.in2.wallet.domain.model.EbsiCredentialOfferContent;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static es.in2.wallet.domain.util.ApplicationUtils.getCleanBearerToken;


@Slf4j
@RestController
@RequestMapping("/api/v1/request-credential")
@RequiredArgsConstructor
public class CredentialIssuanceController {

    private final EbsiCredentialService ebsiCredentialIssuanceServiceFacade;

    /**
     * Processes a request for a verifiable credential when the credential offer is received via a redirect.
     * This endpoint is designed to handle the scenario where a user is redirected to this service with a credential
     * offer URI, as opposed to receiving the offer directly from scanning a QR code.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            tags = (SwaggerConfig.TAG_PUBLIC)
    )
    public Mono<Void> requestVerifiableCredential(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                                @RequestBody EbsiCredentialOfferContent ebsiCredentialOfferContent) {
        String processId = UUID.randomUUID().toString();
        MDC.put("processId", processId);
        return getCleanBearerToken(authorizationHeader)
                .flatMap(authorizationToken -> ebsiCredentialIssuanceServiceFacade.identifyAuthMethod(processId, authorizationToken, ebsiCredentialOfferContent.credentialOfferUri()));
    }

}
