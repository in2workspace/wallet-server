package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.AppConfig;
import es.in2.wallet.domain.model.AuthorisationServerMetadata;
import es.in2.wallet.domain.model.CredentialIssuerMetadata;
import es.in2.wallet.domain.service.impl.AuthorisationServerMetadataServiceImpl;
import es.in2.wallet.domain.util.ApplicationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static es.in2.wallet.domain.util.ApplicationUtils.getRequest;
import static es.in2.wallet.domain.util.MessageUtils.CONTENT_TYPE;
import static es.in2.wallet.domain.util.MessageUtils.CONTENT_TYPE_APPLICATION_JSON;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorisationServerMetadataServiceImplTest {
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AppConfig appConfig;
    @InjectMocks
    private AuthorisationServerMetadataServiceImpl authorisationServerMetadataService;

    @Test
    void getAuthorizationServerMetadataFromCredentialIssuerMetadataWithTokenEndpointHardcodedTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().authorizationServer("example").build();
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            AuthorisationServerMetadata authorizationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("https://example.com/token").build();
            AuthorisationServerMetadata expectedAuthorizationServerMetadataWithTokenEndpointHardcodedTest = AuthorisationServerMetadata.builder().tokenEndpoint("https://example.com/example/token").build();


            when(appConfig.getAuthServerExternalUrl()).thenReturn("https://example.com");
            when(appConfig.getAuthServerTokenEndpoint()).thenReturn("https://example.com/example/token");
            when(getRequest("example/.well-known/openid-configuration", headers)).thenReturn(Mono.just("response"));
            when(objectMapper.readValue("response", AuthorisationServerMetadata.class)).thenReturn(authorizationServerMetadata);

            StepVerifier.create(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata))
                    .expectNext(expectedAuthorizationServerMetadataWithTokenEndpointHardcodedTest)
                    .verifyComplete();

        }
    }

    @Test
    void getAuthorizationServerMetadataFromCredentialIssuerMetadataWithoutTokenEndpointHardcodedTest() throws JsonProcessingException {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().authorizationServer("example").build();
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));
            AuthorisationServerMetadata authorizationServerMetadata = AuthorisationServerMetadata.builder().tokenEndpoint("https://ebsi.com/token").build();

            when(appConfig.getAuthServerExternalUrl()).thenReturn("https://example.com");
            when(getRequest("example/.well-known/openid-configuration", headers)).thenReturn(Mono.just("response"));
            when(objectMapper.readValue("response", AuthorisationServerMetadata.class)).thenReturn(authorizationServerMetadata);

            StepVerifier.create(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata))
                    .expectNext(authorizationServerMetadata)
                    .verifyComplete();

        }
    }

    @Test
    void getCredentialIssuerMetadataError() {
        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            String processId = "123";
            CredentialIssuerMetadata credentialIssuerMetadata = CredentialIssuerMetadata.builder().authorizationServer("example").build();
            List<Map.Entry<String, String>> headers = List.of(Map.entry(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON));

            when(getRequest("example/.well-known/openid-configuration", headers)).thenReturn(Mono.error(new RuntimeException()));

            StepVerifier.create(authorisationServerMetadataService.getAuthorizationServerMetadataFromCredentialIssuerMetadata(processId, credentialIssuerMetadata))
                    .expectError(RuntimeException.class)
                    .verify();
        }
    }

}

