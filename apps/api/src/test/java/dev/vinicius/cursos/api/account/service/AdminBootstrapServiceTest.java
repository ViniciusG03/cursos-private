package dev.vinicius.cursos.api.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.account.domain.UserAccount;
import dev.vinicius.cursos.api.account.domain.UserRole;
import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapServiceTest extends AccessIntegrationTest {

	private static final String BOOTSTRAP_PASSWORD = "senha inicial do ambiente";

	@Autowired
	private AdminBootstrapService bootstrapService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void noAccountExistsBeforeBootstrap() {
		assertThat(accountRepository.count()).isZero();
	}

	@Test
	void firstRunCreatesAdminWithHashedPassword() {
		AdminBootstrapOutcome outcome = bootstrapService.bootstrap(AccountEmail.parse(" Admin@X.com "),
				BOOTSTRAP_PASSWORD);

		UserAccount admin = accountRepository.findById(outcome.adminId()).orElseThrow();
		assertThat(outcome.created()).isTrue();
		assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
		assertThat(admin.getEmail().value()).isEqualTo("admin@x.com");
		assertThat(admin.getPasswordHash()).doesNotContain(BOOTSTRAP_PASSWORD);
		assertThat(passwordEncoder.matches(BOOTSTRAP_PASSWORD, admin.getPasswordHash())).isTrue();
	}

	@Test
	void repeatedRunIsNoOpAndDoesNotChangePassword() {
		bootstrapService.bootstrap(AccountEmail.parse("admin@x.com"), BOOTSTRAP_PASSWORD);
		String originalHash = accountRepository.findByEmail("admin@x.com").orElseThrow().getPasswordHash();

		AdminBootstrapOutcome secondRun = bootstrapService.bootstrap(AccountEmail.parse("admin@x.com"),
				"outra senha qualquer longa");

		assertThat(secondRun.created()).isFalse();
		assertThat(accountRepository.count()).isEqualTo(1);
		assertThat(accountRepository.findByEmail("admin@x.com").orElseThrow().getPasswordHash())
			.isEqualTo(originalHash);
	}

	@Test
	void differentEmailWhenAdminExistsIsConflict() {
		bootstrapService.bootstrap(AccountEmail.parse("admin@x.com"), BOOTSTRAP_PASSWORD);

		assertThatThrownBy(() -> bootstrapService.bootstrap(AccountEmail.parse("outro@x.com"), BOOTSTRAP_PASSWORD))
			.isInstanceOf(AdminBootstrapConflictException.class)
			.hasMessageContaining("outro@x.com")
			.hasMessageContaining("admin@x.com");
		assertThat(accountRepository.count()).isEqualTo(1);
	}

	@Test
	void memberEmailIsConflict() {
		createMember("ana@x.com");

		assertThatThrownBy(() -> bootstrapService.bootstrap(AccountEmail.parse("ana@x.com"), BOOTSTRAP_PASSWORD))
			.isInstanceOf(AdminBootstrapConflictException.class);
		assertThat(accountRepository.findByEmail("ana@x.com").orElseThrow().getRole()).isEqualTo(UserRole.MEMBER);
	}

	@Test
	void weakPasswordIsRejectedWithoutCreatingAccount() {
		assertThatThrownBy(() -> bootstrapService.bootstrap(AccountEmail.parse("admin@x.com"), "curta"))
			.isInstanceOf(PassphrasePolicyViolationException.class);
		assertThat(accountRepository.count()).isZero();
	}

	@Test
	void runnerFailsLoudlyWhenPasswordIsMissing() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(bootstrapService,
				new AdminBootstrapProperties(true, "admin@x.com", null));

		assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("LIBRARY_ADMIN_BOOTSTRAP_PASSWORD")
			.hasMessageContaining("password=missing");
		assertThat(accountRepository.count()).isZero();
	}

	@Test
	void runnerTreatsEmptyEnvironmentValuesAsMissing() {
		AdminBootstrapRunner runner = new AdminBootstrapRunner(bootstrapService,
				new AdminBootstrapProperties(true, "", BOOTSTRAP_PASSWORD));

		assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("email=missing")
			.hasMessageNotContaining(BOOTSTRAP_PASSWORD);
	}

	@Test
	void runnerCreatesAdminFromProperties() {
		new AdminBootstrapRunner(bootstrapService, new AdminBootstrapProperties(true, "admin@x.com", BOOTSTRAP_PASSWORD))
			.run(new DefaultApplicationArguments());

		assertThat(accountRepository.findAll()).extracting(UserAccount::getRole).isEqualTo(List.of(UserRole.ADMIN));
	}

	@Test
	void propertiesToStringOmitsPassword() {
		assertThat(new AdminBootstrapProperties(true, "admin@x.com", BOOTSTRAP_PASSWORD).toString())
			.doesNotContain(BOOTSTRAP_PASSWORD);
	}

}
