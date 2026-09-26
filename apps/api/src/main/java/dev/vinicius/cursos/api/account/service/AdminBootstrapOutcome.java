package dev.vinicius.cursos.api.account.service;

import java.util.UUID;

/**
 * Resultado do bootstrap: conta criada agora ou administrador já existente (senha intocada).
 *
 * <p>Exemplo: {@code outcome.created() ? "created" : "already present"}.
 */
public record AdminBootstrapOutcome(UUID adminId, boolean created) {}
