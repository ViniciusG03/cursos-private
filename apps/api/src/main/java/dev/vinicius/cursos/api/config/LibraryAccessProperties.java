package dev.vinicius.cursos.api.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do marco 2 por ambiente ({@code library.access.*}, alimentada por {@code LIBRARY_PUBLIC_BASE_URL},
 * {@code LIBRARY_MAIL_FROM}, {@code LIBRARY_ATTEMPT_*} e {@code LIBRARY_PASSWORD_RESET_*}). Não há default
 * para a base pública nem para o remetente: sem eles a aplicação não sobe, em vez de enviar links errados.
 * O formato da base pública é validado em {@link PublicOrigin}, que depende do perfil ativo.
 *
 * <p>Exemplo: {@code properties.passwordResetDispatch().maxAttempts()}.
 *
 * @param publicBaseUrl origem pública do frontend usada nos links de convite e recuperação
 * @param mailFrom remetente dos e-mails de acesso
 * @param attemptLimit limites de tentativas nos fluxos públicos
 * @param passwordResetDispatch processamento durável dos pedidos de recuperação
 */
@Validated
@ConfigurationProperties(prefix = "library.access")
public record LibraryAccessProperties(@NotNull URI publicBaseUrl, @NotBlank String mailFrom,
		@Valid @NotNull @DefaultValue AttemptLimit attemptLimit,
		@Valid @NotNull @DefaultValue PasswordResetDispatch passwordResetDispatch) {

	/**
	 * Janela fixa de contagem e máximos por origem (IP), por identidade (e-mail) vinda de uma mesma
	 * origem e por identidade somando todas as origens.
	 *
	 * <p>Exemplo: {@code attemptLimit.maxAttemptsPerClient()}.
	 */
	public record AttemptLimit(@NotNull @DefaultValue("15m") Duration window,
			@Positive @DefaultValue("30") int maxAttemptsPerClient,
			@Positive @DefaultValue("10") int maxAttemptsPerIdentity,
			@Positive @DefaultValue("100") int maxAttemptsPerIdentityAllClients,
			@NotNull @DefaultValue("10m") Duration cleanupInterval) {}

	/**
	 * Fila de pedidos de recuperação em {@code password_reset_requests}: frequência de varredura, tentativas
	 * de envio (espera dobra a cada falha, a partir de {@code retryBackoff}) e retenção dos concluídos.
	 *
	 * <p>Exemplo: {@code dispatch.retryBackoff().multipliedBy(2)}.
	 */
	public record PasswordResetDispatch(@NotNull @DefaultValue("5s") Duration pollInterval,
			@Positive @DefaultValue("5") int maxAttempts, @NotNull @DefaultValue("1m") Duration retryBackoff,
			@NotNull @DefaultValue("30d") Duration retention) {}

}
