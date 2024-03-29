package es.in2.wallet.application.service.impl;

import es.in2.wallet.domain.service.EbsiAuthorisationService;
import es.in2.wallet.domain.service.EbsiIdTokenService;
import es.in2.wallet.domain.service.EbsiVpTokenService;
import es.in2.wallet.application.service.EbsiCredentialService;
import es.in2.wallet.domain.model.*;
import es.in2.wallet.domain.service.*;
import es.in2.wallet.application.port.BrokerService;
import es.in2.wallet.infrastructure.ebsi.config.EbsiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static es.in2.wallet.domain.util.ApplicationUtils.extractResponseType;
import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class EbsiCredentialServiceImpl implements EbsiCredentialService {

    private final CredentialOfferService credentialOfferService;
    private final EbsiConfig ebsiConfig;
    private final CredentialIssuerMetadataService credentialIssuerMetadataService;
    private final AuthorisationServerMetadataService authorisationServerMetadataService;
    private final CredentialService credentialService;
    private final UserDataService userDataService;
    private final BrokerService brokerService;
    private final PreAuthorizedService preAuthorizedService;
    private final EbsiIdTokenService ebsiIdTokenService;
    private final EbsiVpTokenService ebsiVpTokenService;
    private final ProofJWTService proofJWTService;
    private final EbsiAuthorisationService ebsiAuthorisationService;
    private final SignerService signerService;


    /**
     * Identifies the authorization method based on the QR content and proceeds with the credential exchange flow.
     * This method orchestrates the flow to obtain credentials by first retrieving the credential offer,
     * then fetching the issuer and authorisation server metadata, and finally obtaining the credential
     * either through a pre-authorized code grant or an authorization code flow.
     */
    @Override
    public Mono<Void> identifyAuthMethod(String processId, String authorizationToken, String qrContent) {
        // get Credential Offer
        return credentialOfferService.getCredentialOfferFromCredentialOfferUri(processId, qrContent)
                //get Issuer Server Metadata
                .flatMap(credentialOffer -> credentialIssuerMetadataService.getCredentialIssuerMetadataFromCredentialOffer(processId, credentialOffer)
                        //get Authorisation Server Metadata
                        .flatMap(credentialIssuerMetadata -> authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId,credentialIssuerMetadata)
                                .flatMap(authorisationServerMetadata -> {
                                    if (credentialOffer.grant().preAuthorizedCodeGrant() != null){
                                        return getCredentialWithPreAuthorizedCodeEbsi(processId,authorizationToken,credentialOffer,authorisationServerMetadata,credentialIssuerMetadata);
                                    }
                                    else {
                                        return getCredentialWithAuthorizedCodeEbsi(processId,authorizationToken,credentialOffer,authorisationServerMetadata,credentialIssuerMetadata);
                                    }
                                })
                        )
                );

    }

    /**
     * Handles the credential acquisition flow using a pre-authorized code grant.
     * This method is chosen when the credential offer includes a pre-authorized code grant.
     */
    private Mono<Void> getCredentialWithPreAuthorizedCodeEbsi(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        return ebsiConfig.getDid()
                .flatMap(did -> preAuthorizedService.getPreAuthorizedToken(processId, credentialOffer, authorisationServerMetadata, authorizationToken)
                        .flatMap(tokenResponse -> getCredentialRecursive(
                                 tokenResponse, credentialOffer, credentialIssuerMetadata, did, tokenResponse.cNonce(), new ArrayList<>(), 0
                        ))
                        .flatMap(credentialResponses -> processUserEntity(processId, authorizationToken, credentialResponses))
                );
    }


    /**
     * Handles the credential acquisition flow using an authorization code grant.
     * This method is selected when the credential offer does not include a pre-authorized code grant,
     * requiring the user to go through an authorization code flow to obtain the credential.
     */
    private Mono<Void> getCredentialWithAuthorizedCodeEbsi(String processId, String authorizationToken, CredentialOffer credentialOffer, AuthorisationServerMetadata authorisationServerMetadata, CredentialIssuerMetadata credentialIssuerMetadata) {
        // get Credential Offer
        return  ebsiConfig.getDid()
                .flatMap(did -> ebsiAuthorisationService.getRequestWithOurGeneratedCodeVerifier(processId,credentialOffer,authorisationServerMetadata,credentialIssuerMetadata,did)
                        .flatMap(tuple -> extractResponseType(tuple.getT1())
                                .flatMap(responseType -> {
                                    if (responseType.equals("id_token")){
                                        return ebsiIdTokenService.getIdTokenResponse(processId,did,authorisationServerMetadata,tuple.getT1());
                                    }
                                    else if (responseType.equals("vp_token")){
                                       return ebsiVpTokenService.getVpRequest(processId,authorizationToken,authorisationServerMetadata,tuple.getT1());
                                    }
                                    else {
                                        return Mono.error(new RuntimeException("Not known response_type."));
                                    }
                                })
                                .flatMap(params -> ebsiAuthorisationService.sendTokenRequest(tuple.getT2(), did, authorisationServerMetadata,params))
                        )
                        // get Credentials
                        .flatMap(tokenResponse -> getCredentialRecursive(
                                 tokenResponse, credentialOffer, credentialIssuerMetadata, did, tokenResponse.cNonce(), new ArrayList<>(), 0
                        ))
                )
                // save Credential
                .flatMap(credentialResponse -> processUserEntity(processId,authorizationToken,credentialResponse));
    }

    /**
     * Processes the user entity based on the credential response.
     * If the user entity exists, it is updated with the new credential.
     * If not, a new user entity is created and then updated with the credential.
     */
    private Mono<Void> processUserEntity(String processId, String authorizationToken, List<CredentialResponse> credentials) {
        return getUserIdFromToken(authorizationToken)
                .flatMap(userId -> brokerService.getEntityById(processId, userId)
                        .flatMap(optionalEntity -> optionalEntity
                                .map(entity -> updateEntity(processId, userId, credentials, entity))
                                .orElseGet(() -> createAndUpdateUser(processId, userId, credentials))
                        )
                );
    }

    /**
     * Updates the user entity with the DID information.
     * Following the update, a second operation is triggered to save the VC (Verifiable Credential) to the entity.
     * This process involves saving the VC, and updating the entity with the VC information.
     */
    private Mono<Void> updateEntity(String processId, String userId, List<CredentialResponse> credentials, String entity) {
        return userDataService.saveVC(entity, credentials)
                .flatMap(updatedEntity ->
                        brokerService.updateEntity(processId, userId, updatedEntity)
                );
    }

    /**
     * Handles the creation of a new user entity if it does not exist.
     * After creation, the entity is updated with the DID information.
     * This involves creating the user, posting the entity, saving the VC, and performing an update with the VC information.
     */
    private Mono<Void> createAndUpdateUser(String processId, String userId, List<CredentialResponse> credentials) {
        return userDataService.createUserEntity(userId)
                .flatMap(createdUserId -> brokerService.postEntity(processId, createdUserId))
                .then(brokerService.getEntityById(processId, userId))
                .flatMap(optionalEntity ->
                        optionalEntity.map(entity ->
                                        userDataService.saveVC(entity, credentials)
                                                .flatMap(updatedEntity -> brokerService.updateEntity(processId, userId, updatedEntity))
                                )
                                .orElseGet(() -> Mono.error(new RuntimeException("Entity not found after creation.")))
                );
    }

    /**
     * Constructs a credential request using the nonce from the token response and the issuer's information.
     * The request is then signed using the generated DID and private key to ensure its authenticity.
     */
    private Mono<String> buildAndSignCredentialRequest(String nonce, String did, String issuer) {
        return proofJWTService.buildCredentialRequest(nonce, issuer,did)
                .flatMap(json -> signerService.buildJWTSFromJsonNode(json, did, "proof"));
    }

    private Mono<List<CredentialResponse>> getCredentialRecursive(TokenResponse tokenResponse, CredentialOffer credentialOffer, CredentialIssuerMetadata credentialIssuerMetadata, String did, String nonce, List<CredentialResponse> credentialResponses, int index) {
        if (index >= credentialOffer.credentials().size()) {
            return Mono.just(credentialResponses);
        }
        CredentialOffer.Credential credential = credentialOffer.credentials().get(index);
        return buildAndSignCredentialRequest(nonce, did, credentialIssuerMetadata.credentialIssuer())
                .flatMap(jwt -> credentialService.getCredential(jwt, tokenResponse, credentialIssuerMetadata, credential.format(), credential.types()))
                .flatMap(credentialResponse -> {
                    credentialResponses.add(credentialResponse);
                    String newNonce = credentialResponse.c_nonce() != null ? credentialResponse.c_nonce() : nonce;
                    return getCredentialRecursive(
                            tokenResponse, credentialOffer, credentialIssuerMetadata, did, newNonce, credentialResponses, index + 1
                    );
                });
    }
}
