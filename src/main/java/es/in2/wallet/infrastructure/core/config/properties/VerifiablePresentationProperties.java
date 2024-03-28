package es.in2.wallet.infrastructure.core.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Verifiable Presentation Properties
 *
 * @param expirationTime
 * @param expirationUnit
 */
@ConfigurationProperties(prefix = "verifiable-presentation")
public record VerifiablePresentationProperties(@NotNull Long expirationTime, @NotNull String expirationUnit) {

}
