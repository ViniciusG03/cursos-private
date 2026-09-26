package dev.vinicius.cursos.api.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/** Restrições de V4–V8 verificadas diretamente no PostgreSQL, sem passar pelas regras da aplicação. */
class AccessSchemaTest extends AccessIntegrationTest {

	private static final Timestamp NOW = Timestamp.from(Instant.parse("2026-01-05T12:00:00Z"));

	private static final Timestamp LATER = Timestamp.from(Instant.parse("2026-01-08T12:00:00Z"));

	private static final String HASH_A = "a".repeat(64);

	private static final String HASH_B = "b".repeat(64);

	@Test
	void flywayAppliesAccessMigrationsAfterCatalog() {
		List<String> appliedVersions = jdbcTemplate.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);

		assertThat(appliedVersions).containsSubsequence("1", "2", "3", "4", "5", "6", "7", "8");
	}

	@Test
	void credentialVersionStartsAtZeroByDefault() {
		UUID userId = insertUser("ana@x.com", "MEMBER");

		assertThat(jdbcTemplate.queryForObject("SELECT credential_version FROM users WHERE id = ?", Long.class, userId))
			.isZero();
	}

	@Test
	void rejectsDuplicateEmail() {
		insertUser("ana@x.com", "MEMBER");

		assertThatThrownBy(() -> insertUser("ana@x.com", "MEMBER")).isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uq_users_email");
	}

	@Test
	void rejectsUnknownRole() {
		assertThatThrownBy(() -> insertUser("ana@x.com", "OWNER")).isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_users_role");
	}

	@Test
	void allowsOnlyOneAdmin() {
		insertUser("admin@x.com", "ADMIN");

		assertThatThrownBy(() -> insertUser("other-admin@x.com", "ADMIN"))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uq_users_single_admin");
	}

	@Test
	void invitationRequiresExistingCreator() {
		assertThatThrownBy(() -> insertInvitation("ana@x.com", HASH_A, UUID.randomUUID()))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("fk_invitations_created_by");
	}

	@Test
	void allowsOnlyOneOpenInvitationPerEmail() {
		UUID adminId = insertUser("admin@x.com", "ADMIN");
		insertInvitation("ana@x.com", HASH_A, adminId);

		assertThatThrownBy(() -> insertInvitation("ana@x.com", HASH_B, adminId))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uq_invitations_open_email");
	}

	@Test
	void revokedInvitationFreesTheEmailForANewOne() {
		UUID adminId = insertUser("admin@x.com", "ADMIN");
		UUID revokedId = insertInvitation("ana@x.com", HASH_A, adminId);
		jdbcTemplate.update("UPDATE invitations SET revoked_at = ? WHERE id = ?", LATER, revokedId);

		assertThat(insertInvitation("ana@x.com", HASH_B, adminId)).isNotNull();
	}

	@Test
	void rejectsTokenHashThatIsNotSha256Hex() {
		UUID adminId = insertUser("admin@x.com", "ADMIN");

		assertThatThrownBy(() -> insertInvitation("ana@x.com", "raw-token-not-a-hash", adminId))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_invitations_token_hash_sha256_hex");
	}

	@Test
	void rejectsInvitationBothConsumedAndRevoked() {
		UUID invitationId = insertInvitation("ana@x.com", HASH_A, insertUser("admin@x.com", "ADMIN"));

		assertThatThrownBy(() -> jdbcTemplate.update(
				"UPDATE invitations SET consumed_at = ?, revoked_at = ? WHERE id = ?", LATER, LATER, invitationId))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_invitations_consumed_or_revoked");
	}

	@Test
	void resetTokenRequiresExistingUser() {
		assertThatThrownBy(() -> insertResetToken(UUID.randomUUID(), HASH_A))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("fk_password_reset_tokens_user");
	}

	@Test
	void allowsOnlyOneOpenResetTokenPerUser() {
		UUID userId = insertUser("ana@x.com", "MEMBER");
		insertResetToken(userId, HASH_A);

		assertThatThrownBy(() -> insertResetToken(userId, HASH_B)).isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("uq_password_reset_tokens_open_user");
	}

	@Test
	void resetRequestStatusMustMatchCompletion() {
		assertThatThrownBy(() -> insertResetRequest("SENT", null)).isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_password_reset_requests_completed_matches_status");
	}

	@Test
	void rejectsUnknownResetRequestStatus() {
		assertThatThrownBy(() -> insertResetRequest("LOST", LATER)).isInstanceOf(DataIntegrityViolationException.class)
			.hasMessageContaining("ck_password_reset_requests_status");
	}

	private void insertResetRequest(String status, Timestamp completedAt) {
		jdbcTemplate.update("""
				INSERT INTO password_reset_requests (email, requested_at, status, next_attempt_at, completed_at)
				VALUES ('ana@x.com', ?, ?, ?, ?)""", NOW, status, NOW, completedAt);
	}

	private UUID insertUser(String email, String role) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO users (email, password_hash, role, created_at, password_changed_at)
				VALUES (?, '{noop}not-used', ?, ?, ?) RETURNING id""", UUID.class, email, role, NOW, NOW);
	}

	private UUID insertInvitation(String email, String tokenHash, UUID createdByUserId) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO invitations (email, token_hash, created_at, expires_at, created_by_user_id, delivery_status)
				VALUES (?, ?, ?, ?, ?, 'PENDING') RETURNING id""", UUID.class, email, tokenHash, NOW, LATER,
				createdByUserId);
	}

	private UUID insertResetToken(UUID userId, String tokenHash) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO password_reset_tokens (user_id, token_hash, created_at, expires_at)
				VALUES (?, ?, ?, ?) RETURNING id""", UUID.class, userId, tokenHash, NOW, LATER);
	}

}
