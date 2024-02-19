package es.in2.wallet.api.ebsi.comformance.facade.impl;

import es.in2.wallet.api.ebsi.comformance.configuration.EbsiConfig;
import es.in2.wallet.api.ebsi.comformance.facade.EbsiCredentialServiceFacade;
import es.in2.wallet.api.ebsi.comformance.service.*;
import es.in2.wallet.api.model.AuthorisationServerMetadata;
import es.in2.wallet.api.model.CredentialIssuerMetadata;
import es.in2.wallet.api.model.CredentialOffer;
import es.in2.wallet.api.model.CredentialResponse;
import es.in2.wallet.api.service.*;
import es.in2.wallet.broker.service.BrokerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static es.in2.wallet.api.util.ApplicationUtils.extractResponseType;
import static es.in2.wallet.api.util.ApplicationUtils.getUserIdFromToken;

@Slf4j
@Service
@RequiredArgsConstructor
public class EbsiCredentialServiceFacadeImpl implements EbsiCredentialServiceFacade {

    private final CredentialOfferService credentialOfferService;
    private final EbsiConfig ebsiConfig;
    private final CredentialIssuerMetadataService credentialIssuerMetadataService;
    private final AuthorisationServerMetadataService authorisationServerMetadataService;
    private final CredentialEbsiService credentialEbsiService;
    private final UserDataService userDataService;
    private final BrokerService brokerService;
    private final PreAuthorizedService preAuthorizedService;
    private final IdTokenService idTokenService;
    private final VpTokenService vpTokenService;
    private final AuthorisationRequestService authorisationRequestService;
    private final AuthorisationResponseService authorisationResponseService;


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
        // get Credential Offer
        return ebsiConfig.getDid()
                .flatMap(did -> preAuthorizedService.getPreAuthorizedToken(processId,credentialOffer,authorisationServerMetadata,authorizationToken)
                        // get Credential
                        .flatMap(tokenResponse -> credentialEbsiService.getCredential(processId, did,tokenResponse, credentialIssuerMetadata,credentialOffer.credentials().get(0).format(),credentialOffer.credentials().get(0).types()))
                        // save Credential
                        .flatMap(credentialResponse -> processUserEntity(processId,authorizationToken,credentialResponse))
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
                .flatMap(did -> authorisationRequestService.getRequestWithOurGeneratedCodeVerifier(processId,credentialOffer,authorisationServerMetadata,credentialIssuerMetadata,did)
                        .flatMap(tuple -> extractResponseType(tuple.getT1())
                                .flatMap(responseType -> {
                                    if (responseType.equals("id_token")){
                                        return idTokenService.getIdTokenResponse(processId,did,authorisationServerMetadata,tuple.getT1());
                                    }
                                    else if (responseType.equals("vp_token")){
                                       return vpTokenService.getVpRequest(processId,authorizationToken,authorisationServerMetadata,tuple.getT1());
                                    }
                                    else {
                                        return Mono.error(new RuntimeException("Not known response_type."));
                                    }
                                })
                                .flatMap(params -> authorisationResponseService.sendTokenRequest(tuple.getT2(), did, authorisationServerMetadata,params))
                        )
                        // get Credential
                        .flatMap(tokenResponse -> credentialEbsiService.getCredential(processId, did, tokenResponse, credentialIssuerMetadata,credentialOffer.credentials().get(0).format(),credentialOffer.credentials().get(0).types()))
                )
                // save Credential
                .flatMap(credentialResponse -> processUserEntity(processId,authorizationToken,credentialResponse));
    }

    /**
     * Processes the user entity based on the credential response.
     * If the user entity exists, it is updated with the new credential.
     * If not, a new user entity is created and then updated with the credential.
     */
    private Mono<Void> processUserEntity(String processId, String authorizationToken, CredentialResponse credentialResponse) {
        return getUserIdFromToken(authorizationToken)
                .flatMap(userId -> brokerService.getEntityById(processId, userId)
                        .flatMap(optionalEntity -> optionalEntity
                                .map(entity -> updateEntity(processId, userId, credentialResponse, entity))
                                .orElseGet(() -> createAndUpdateUser(processId, userId, credentialResponse))
                        )
                );
    }

    /**
     * Updates the user entity with the DID information.
     * Following the update, a second operation is triggered to save the VC (Verifiable Credential) to the entity.
     * This process involves saving the VC, and updating the entity with the VC information.
     */
    private Mono<Void> updateEntity(String processId, String userId, CredentialResponse credentialResponse, String entity) {
        return userDataService.saveVC(entity, credentialResponse.credential())
                .flatMap(updatedEntity ->
                        brokerService.updateEntity(processId, userId, updatedEntity)
                );
    }

    /**
     * Handles the creation of a new user entity if it does not exist.
     * After creation, the entity is updated with the DID information.
     * This involves creating the user, posting the entity, saving the VC, and performing an update with the VC information.
     */
    private Mono<Void> createAndUpdateUser(String processId, String userId, CredentialResponse credentialResponse) {
        return userDataService.createUserEntity(userId)
                .flatMap(createdUserId -> brokerService.postEntity(processId, createdUserId))
                .then(brokerService.getEntityById(processId, userId))
                .flatMap(optionalEntity ->
                        optionalEntity.map(entity ->
                                        userDataService.saveVC(entity, credentialResponse.credential())
                                                .flatMap(updatedEntity -> brokerService.updateEntity(processId, userId, updatedEntity))
                                )
                                .orElseGet(() -> Mono.error(new RuntimeException("Entity not found after creation.")))
                );
    }

}