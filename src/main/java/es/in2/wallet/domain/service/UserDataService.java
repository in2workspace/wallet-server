package es.in2.wallet.domain.service;

import es.in2.wallet.domain.model.CredentialResponse;
import es.in2.wallet.domain.model.CredentialsBasicInfo;
import es.in2.wallet.domain.model.CredentialsBasicInfoWithExpirationDate;
import es.in2.wallet.domain.model.VCAttribute;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UserDataService {

    Mono<String> createUserEntity(String id);
    Mono<String> saveVC(String userEntity, List<CredentialResponse> credentials);
    Mono<List<CredentialsBasicInfoWithExpirationDate>> getUserVCsInJson(String userEntity);

    Mono<List<VCAttribute>> getVerifiableCredentialsByFormat(String userEntity, String format);

    Mono<String> getVerifiableCredentialByIdAndFormat(String userEntity, String id, String format);

    Mono<String> extractDidFromVerifiableCredential(String userEntity, String vcId);

    Mono<String> deleteVerifiableCredential(String userEntity, String vcId, String did);

    Mono<List<CredentialsBasicInfo>> getSelectableVCsByVcTypeList(List<String> vcTypeList, String userEntity);

    Mono<String> saveDid(String userEntity, String did, String didMethod);

    Mono<List<String>> getDidsByUserEntity(String userEntity);

    Mono<String> deleteSelectedDidFromUserEntity(String did, String userEntity);
}
