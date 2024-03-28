package es.in2.wallet.application.port;

import java.util.List;

public interface AppConfig {

    List<String> getWalletDrivingUrls();

    String getAuthServerInternalUrl();

    String getAuthServerExternalUrl();

    String getAuthServerTokenEndpoint();

    String getIdentityProviderUrl();

    String getIdentityProviderUsername();

    String getIdentityProviderPassword();

    String getIdentityProviderClientId();

    String getIdentityProviderClientSecret();

    Long getCredentialPresentationExpirationTime();

    String getCredentialPresentationExpirationUnit();

}
