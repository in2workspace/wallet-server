package es.in2.wallet.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.in2.wallet.application.port.AppConfig;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.model.VcSelectorResponse;
import es.in2.wallet.domain.service.SignerService;
import es.in2.wallet.domain.service.UserDataService;
import es.in2.wallet.domain.service.impl.PresentationServiceImpl;
import es.in2.wallet.domain.util.ApplicationUtils;
import es.in2.wallet.application.port.BrokerService;
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
import java.util.Optional;

import static es.in2.wallet.domain.util.ApplicationUtils.getUserIdFromToken;
import static es.in2.wallet.domain.util.MessageUtils.JWT_VC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresentationServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private UserDataService userDataService;

    @Mock
    private BrokerService brokerService;

    @Mock
    private SignerService signerService;

    @Mock
    private AppConfig appConfig;

    @InjectMocks
    private PresentationServiceImpl presentationService;

    @Test
    void createSignedVerifiablePresentation_UserExists_ReturnsSignedVP() throws JsonProcessingException {
        String processId = "processId";
        String authorizationToken = "authToken";
        String userId = "123";
        String nonce = "nonce";
        String audience = "audience";
        String entity = "entity";
        String signedVP = "signedVP";
        String vcJwt = "eyJraWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4jelEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2SyJ9.eyJzdWIiOiJkaWQ6a2V5OnpEbmFlZnk3amhwY0ZCanp0TXJFSktFVHdFU0NoUXd4cEpuVUpLb3ZzWUQ1ZkpabXAiLCJuYmYiOjE2OTgxMzQ4NTUsImlzcyI6ImRpZDprZXk6elEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsImV4cCI6MTcwMDcyNjg1NSwiaWF0IjoxNjk4MTM0ODU1LCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiTEVBUkNyZWRlbnRpYWwiXSwiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL2RvbWUtbWFya2V0cGxhY2UuZXUvLzIwMjIvY3JlZGVudGlhbHMvbGVhcmNyZWRlbnRpYWwvdjEiXSwiaWQiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkiLCJpc3N1ZXIiOnsiaWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4ifSwiaXNzdWFuY2VEYXRlIjoiMjAyMy0xMC0yNFQwODowNzozNVoiLCJpc3N1ZWQiOiIyMDIzLTEwLTI0VDA4OjA3OjM1WiIsInZhbGlkRnJvbSI6IjIwMjMtMTAtMjRUMDg6MDc6MzVaIiwiZXhwaXJhdGlvbkRhdGUiOiIyMDIzLTExLTIzVDA4OjA3OjM1WiIsImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImlkIjoiZGlkOmtleTp6RG5hZWZ5N2pocGNGQmp6dE1yRUpLRVR3RVNDaFF3eHBKblVKS292c1lENWZKWm1wIiwidGl0bGUiOiJNci4iLCJmaXJzdF9uYW1lIjoiSm9obiIsImxhc3RfbmFtZSI6IkRvZSIsImdlbmRlciI6Ik0iLCJwb3N0YWxfYWRkcmVzcyI6IiIsImVtYWlsIjoiam9obmRvZUBnb29kYWlyLmNvbSIsInRlbGVwaG9uZSI6IiIsImZheCI6IiIsIm1vYmlsZV9waG9uZSI6IiszNDc4NzQyNjYyMyIsImxlZ2FsUmVwcmVzZW50YXRpdmUiOnsiY24iOiI1NjU2NTY1NlYgSmVzdXMgUnVpeiIsInNlcmlhbE51bWJlciI6IjU2NTY1NjU2ViIsIm9yZ2FuaXphdGlvbklkZW50aWZpZXIiOiJWQVRFUy0xMjM0NTY3OCIsIm8iOiJHb29kQWlyIiwiYyI6IkVTIn0sInJvbGVzQW5kRHV0aWVzIjpbeyJ0eXBlIjoiTEVBUkNyZWRlbnRpYWwiLCJpZCI6Imh0dHBzOi8vZG9tZS1tYXJrZXRwbGFjZS5ldS8vbGVhci92MS82NDg0OTk0bjRyOWU5OTA0OTQifV0sImtleSI6InZhbHVlIn19LCJqdGkiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkifQ.2_YNY515CaohirD4AHDBMvzDagEn-p8uAsaiMT0H4ltK2uVfG8IWWqV_OOR6lFlXMzUhJd7nKsaWkhnAQY8kyA";
        String vpClaims = """
                {
                "@context":["https://www.w3.org/2018/credentials/v1"],
                "holder":"did:key:zDnaefy7jhpcFBjztMrEJKETwESChQwxpJnUJKovsYD5fJZmp",
                "id":"urn:uuid:00eaa273-f62f-40c8-945f-d4fc41414d07",
                "type":["VerifiablePresentation"],
                "verifiableCredential":["eyJraWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4jelEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsInR5cCI6IkpXVCIsImFsZyI6IkVTMjU2SyJ9.eyJzdWIiOiJkaWQ6a2V5OnpEbmFlZnk3amhwY0ZCanp0TXJFSktFVHdFU0NoUXd4cEpuVUpLb3ZzWUQ1ZkpabXAiLCJuYmYiOjE2OTgxMzQ4NTUsImlzcyI6ImRpZDprZXk6elEzc2h0Y0VRUDN5dXhia1oxU2pONTFUOGZRbVJ5WG5qclhtOEU4NFdMWEtEUWJSbiIsImV4cCI6MTcwMDcyNjg1NSwiaWF0IjoxNjk4MTM0ODU1LCJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiTEVBUkNyZWRlbnRpYWwiXSwiQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL2RvbWUtbWFya2V0cGxhY2UuZXUvLzIwMjIvY3JlZGVudGlhbHMvbGVhcmNyZWRlbnRpYWwvdjEiXSwiaWQiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkiLCJpc3N1ZXIiOnsiaWQiOiJkaWQ6a2V5OnpRM3NodGNFUVAzeXV4YmtaMVNqTjUxVDhmUW1SeVhuanJYbThFODRXTFhLRFFiUm4ifSwiaXNzdWFuY2VEYXRlIjoiMjAyMy0xMC0yNFQwODowNzozNVoiLCJpc3N1ZWQiOiIyMDIzLTEwLTI0VDA4OjA3OjM1WiIsInZhbGlkRnJvbSI6IjIwMjMtMTAtMjRUMDg6MDc6MzVaIiwiZXhwaXJhdGlvbkRhdGUiOiIyMDIzLTExLTIzVDA4OjA3OjM1WiIsImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImlkIjoiZGlkOmtleTp6RG5hZWZ5N2pocGNGQmp6dE1yRUpLRVR3RVNDaFF3eHBKblVKS292c1lENWZKWm1wIiwidGl0bGUiOiJNci4iLCJmaXJzdF9uYW1lIjoiSm9obiIsImxhc3RfbmFtZSI6IkRvZSIsImdlbmRlciI6Ik0iLCJwb3N0YWxfYWRkcmVzcyI6IiIsImVtYWlsIjoiam9obmRvZUBnb29kYWlyLmNvbSIsInRlbGVwaG9uZSI6IiIsImZheCI6IiIsIm1vYmlsZV9waG9uZSI6IiszNDc4NzQyNjYyMyIsImxlZ2FsUmVwcmVzZW50YXRpdmUiOnsiY24iOiI1NjU2NTY1NlYgSmVzdXMgUnVpeiIsInNlcmlhbE51bWJlciI6IjU2NTY1NjU2ViIsIm9yZ2FuaXphdGlvbklkZW50aWZpZXIiOiJWQVRFUy0xMjM0NTY3OCIsIm8iOiJHb29kQWlyIiwiYyI6IkVTIn0sInJvbGVzQW5kRHV0aWVzIjpbeyJ0eXBlIjoiTEVBUkNyZWRlbnRpYWwiLCJpZCI6Imh0dHBzOi8vZG9tZS1tYXJrZXRwbGFjZS5ldS8vbGVhci92MS82NDg0OTk0bjRyOWU5OTA0OTQifV0sImtleSI6InZhbHVlIn19LCJqdGkiOiJ1cm46dXVpZDo4NzAwYmVlNS00NjIxLTQ3MjAtOTRkZS1lODY2ZmI3MTk3ZTkifQ.2_YNY515CaohirD4AHDBMvzDagEn-p8uAsaiMT0H4ltK2uVfG8IWWqV_OOR6lFlXMzUhJd7nKsaWkhnAQY8kyA"]
                }""";

        CredentialsBasicInfo credentialsBasicInfo = CredentialsBasicInfo.builder().id("vc1").vcType(List.of("LEARCredential")).build();
        VcSelectorResponse vcSelectorResponse = VcSelectorResponse.builder().selectedVcList(List.of(credentialsBasicInfo)).build();

        try (MockedStatic<ApplicationUtils> ignored = Mockito.mockStatic(ApplicationUtils.class)) {
            when(getUserIdFromToken(authorizationToken)).thenReturn(Mono.just(userId));
            // Simulate the broker service returning an existing entity
            when(brokerService.getEntityById(processId, userId)).thenReturn(Mono.just(Optional.of(entity)));

            Long expirationTime = 10L;
            when(appConfig.getCredentialPresentationExpirationTime()).thenReturn(expirationTime);

            when(appConfig.getCredentialPresentationExpirationUnit()).thenReturn("minutes");

            // Simulate the user data service returning a list of verifiable credential JWTs
            when(userDataService.getVerifiableCredentialByIdAndFormat(entity, credentialsBasicInfo.id(), JWT_VC)).thenReturn(Mono.just(vcJwt));

            when(objectMapper.writeValueAsString(any())).thenReturn(vpClaims);
            when(objectMapper.readTree(anyString())).thenAnswer(invocation -> {
                String payload = invocation.getArgument(0, String.class);
                return new ObjectMapper().readTree(payload);
            });

            // Simulate the signer service signing the VP
            when(signerService.buildJWTSFromJsonNode(any(), anyString(), anyString())).thenReturn(Mono.just(signedVP));

            StepVerifier.create(presentationService.createSignedVerifiablePresentation(processId, authorizationToken, vcSelectorResponse, nonce, audience))
                    .expectNext(signedVP)
                    .verifyComplete();
        }
    }
}
