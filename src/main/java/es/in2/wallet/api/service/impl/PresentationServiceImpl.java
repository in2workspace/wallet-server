package es.in2.wallet.api.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import es.in2.wallet.api.model.VcSelectorResponse;
import es.in2.wallet.api.model.VerifiablePresentation;
import es.in2.wallet.api.service.PresentationService;
import es.in2.wallet.api.service.SignerService;
import es.in2.wallet.api.service.UserDataService;
import es.in2.wallet.broker.service.BrokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static es.in2.wallet.api.util.ApplicationUtils.getUserIdFromToken;
import static es.in2.wallet.api.util.MessageUtils.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresentationServiceImpl implements PresentationService {
    private final ObjectMapper objectMapper;
    private final UserDataService userDataService;
    private final BrokerService brokerService;
    private final SignerService signerService;

    /**
     * Creates and signs a Verifiable Presentation (VP) using the selected Verifiable Credentials (VCs).
     * This method retrieves the subject DID from the first VC, constructs an unsigned VP, and signs it.
     *
     * @param authorizationToken   The authorization token to identify the user.
     * @param vcSelectorResponse   The response containing the selected VCs for the VP.
     * @param nonce                A unique nonce for the VP.
     * @param audience             The intended audience of the VP.
     */
    @Override
    public Mono<String> createSignedVerifiablePresentation(String processId, String authorizationToken, VcSelectorResponse vcSelectorResponse,String nonce, String audience) {
        // Get the subject DID from the first credential in the list
        return  getUserIdFromToken(authorizationToken)
                .flatMap(userId -> brokerService.getEntityById(processId,userId))
                .flatMap(optionalEntity -> optionalEntity
                        .map(entity -> getVerifiableCredentials(entity,vcSelectorResponse))
                        .orElseGet(() -> Mono.error(new RuntimeException("Failed to retrieve entity."))
                )
                .flatMap(verifiableCredentialsList -> getSubjectDidFromTheFirstVcOfTheList(verifiableCredentialsList)
                        .flatMap(did ->
                                // Create the unsigned verifiable presentation
                                createUnsignedPresentation(verifiableCredentialsList, did,nonce,audience)
                                        .flatMap(document -> signerService.buildJWTSFromJsonNode(document,did,"vp"))
                        )
                )
                        // Log success
                        .doOnSuccess(verifiablePresentation -> log.info("ProcessID: {} - Verifiable Presentation created successfully: {}", processId, verifiablePresentation))
                    // Handle errors
                    .onErrorResume(e -> {
                        log.error("Error in creating Verifiable Presentation: ", e);
                        return Mono.error(e);
                    })
                );
    }

    /**
     * Retrieves a list of Verifiable Credential JWTs based on the VCs selected in the VcSelectorResponse.
     *
     * @param entity               The entity ID associated with the VCs.
     * @param vcSelectorResponse   The VcSelectorResponse containing the IDs of the selected VCs.
     */
    private Mono<List<String>> getVerifiableCredentials(String entity, VcSelectorResponse vcSelectorResponse) {
        return Flux.fromIterable(vcSelectorResponse.selectedVcList())
                .flatMap(verifiableCredential -> userDataService.getVerifiableCredentialByIdAndFormat(entity,verifiableCredential.id(),VC_JWT))
                .collectList();
    }

    /**
     * Extracts the subject DID from the first Verifiable Credential in the list.
     *
     * @param verifiableCredentialsList The list of VC JWTs.
     */
    private Mono<String> getSubjectDidFromTheFirstVcOfTheList(List<String> verifiableCredentialsList) {
        return Mono.fromCallable(() -> {
            // Check if the list is not empty
            try {
                if (!verifiableCredentialsList.isEmpty()) {
                    // Get the first verifiable credential's JWT and parse it
                    String verifiableCredential = verifiableCredentialsList.get(0);
                    SignedJWT parsedVerifiableCredential = SignedJWT.parse(verifiableCredential);
                    // Extract the subject DID from the JWT claims
                    return (String) parsedVerifiableCredential.getJWTClaimsSet().getClaim("sub");
                } else {
                    // Throw an exception if the credential list is empty
                    throw new NoSuchElementException("Verifiable credentials list is empty");
                }
            } catch (Exception e) {
                throw new IllegalStateException("Error obtaining the subject DID from the verifiable credential" + e);
            }
        });
    }

    /**
     * Creates an unsigned Verifiable Presentation containing the selected VCs.
     *
     * @param vcs       The list of VC JWTs to include in the VP.
     * @param holderDid The DID of the holder of the VPs.
     * @param nonce     A unique nonce for the VP.
     * @param audience  The intended audience of the VP.
     */
    private Mono<JsonNode> createUnsignedPresentation(
            List<String> vcs,
            String holderDid,
            String nonce,
            String audience) {
        return Mono.fromCallable(() -> {
            String id = "urn:uuid:" + UUID.randomUUID();

            VerifiablePresentation vpBuilder = VerifiablePresentation
                    .builder()
                    .id(id)
                    .holder(holderDid)
                    .context(List.of(JSONLD_CONTEXT_W3C_2018_CREDENTIALS_V1))
                    .type(List.of(VERIFIABLE_PRESENTATION))
                    .verifiableCredential(vcs)
                    .build();

            Instant issueTime = Instant.now();
            Instant expirationTime = issueTime.plus(10, ChronoUnit.DAYS);
            Map<String, Object> vpParsed = JWTClaimsSet.parse(objectMapper.writeValueAsString(vpBuilder)).getClaims();
            JWTClaimsSet payload = new JWTClaimsSet.Builder()
                    .issuer(holderDid)
                    .subject(holderDid)
                    .audience(audience)
                    .notBeforeTime(java.util.Date.from(issueTime))
                    .expirationTime(java.util.Date.from(expirationTime))
                    .issueTime(java.util.Date.from(issueTime))
                    .jwtID(id)
                    .claim("vp", vpParsed)
                    .claim("nonce", nonce)
                    .build();
            log.debug(payload.toString());
            return objectMapper.readTree(payload.toString());
        });
    }

}
