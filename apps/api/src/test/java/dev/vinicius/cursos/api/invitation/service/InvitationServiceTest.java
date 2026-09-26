package dev.vinicius.cursos.api.invitation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vinicius.cursos.api.account.domain.InvalidAccountEmailException;
import dev.vinicius.cursos.api.invitation.domain.InvitationDeliveryStatus;
import dev.vinicius.cursos.api.onetimetoken.InvalidOneTimeTokenException;
import dev.vinicius.cursos.api.onetimetoken.OneTimeTokenDigest;
import dev.vinicius.cursos.api.support.AccessIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class InvitationServiceTest extends AccessIntegrationTest {

	@Autowired
	private InvitationService invitationService;

	@Autowired
	private InvitationAcceptance invitationAcceptance;

	private UUID adminId;

	@BeforeEach
	void createInvitingAdmin() {
		adminId = createAdmin().getId();
	}

	@Test
	void sendsInvitationToNormalizedEmailAndStoresOnlyTokenHash() {
		InvitationView view = invitationService.issue(" Ana@X.com ", adminId);

		String rawToken = mailbox.lastTokenSentTo("ana@x.com");
		assertThat(view.email()).isEqualTo("ana@x.com");
		assertThat(view.deliveryStatus()).isEqualTo(InvitationDeliveryStatus.SENT);
		assertThat(view.expiresAt()).isEqualTo(clock.instant().plusSeconds(72 * 3600));
		assertThat(readTokenHashes()).containsExactly(OneTimeTokenDigest.sha256Hex(rawToken));
		assertThat(countRowsContaining(rawToken)).isZero();
	}

	@Test
	void doesNotCreateMemberBeforeAcceptance() {
		invitationService.issue("ana@x.com", adminId);

		assertThat(accountRepository.existsByEmail("ana@x.com")).isFalse();
	}

	@Test
	void resendRevokesPreviousTokenAndIssuesNewOne() {
		invitationService.issue("ana@x.com", adminId);
		String firstToken = mailbox.lastTokenSentTo("ana@x.com");

		invitationService.issue("ana@x.com", adminId);
		String secondToken = mailbox.lastTokenSentTo("ana@x.com");

		assertThat(secondToken).isNotEqualTo(firstToken);
		assertThat(invitationService.listOpen()).hasSize(1);
		assertThatThrownBy(() -> invitationAcceptance.accept(firstToken, MEMBER_PASSWORD))
			.isInstanceOf(InvalidOneTimeTokenException.class);
		assertThat(invitationAcceptance.accept(secondToken, MEMBER_PASSWORD)).isNotNull();
	}

	@Test
	void invitingActiveAccountIsConflict() {
		createMember("ana@x.com");

		assertThatThrownBy(() -> invitationService.issue("ANA@x.com", adminId))
			.isInstanceOf(InvitationConflictException.class)
			.hasMessageContaining("ana@x.com");
		assertThat(mailbox.deliveredMails()).isEmpty();
	}

	@Test
	void invitingAdminEmailIsConflict() {
		assertThatThrownBy(() -> invitationService.issue(ADMIN_EMAIL, adminId))
			.isInstanceOf(InvitationConflictException.class);
	}

	@Test
	void rejectsMalformedEmail() {
		assertThatThrownBy(() -> invitationService.issue("ana", adminId)).isInstanceOf(InvalidAccountEmailException.class);
	}

	@Test
	void smtpFailureIsRecordedAsFailedAndVisibleToAdmin() {
		mailbox.simulateOutage(true);

		InvitationView view = invitationService.issue("ana@x.com", adminId);

		assertThat(view.deliveryStatus()).isEqualTo(InvitationDeliveryStatus.FAILED);
		assertThat(view.deliveryAttemptedAt()).isEqualTo(clock.instant());
		assertThat(view.deliveredAt()).isNull();
		assertThat(invitationService.listOpen()).extracting(InvitationView::deliveryStatus)
			.containsExactly(InvitationDeliveryStatus.FAILED);
	}

	@Test
	void resendAfterFailureDeliversNewToken() {
		mailbox.simulateOutage(true);
		invitationService.issue("ana@x.com", adminId);
		mailbox.simulateOutage(false);

		InvitationView resent = invitationService.issue("ana@x.com", adminId);

		assertThat(resent.deliveryStatus()).isEqualTo(InvitationDeliveryStatus.SENT);
		assertThat(invitationAcceptance.accept(mailbox.lastTokenSentTo("ana@x.com"), MEMBER_PASSWORD)).isNotNull();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM invitations WHERE revoked_at IS NOT NULL",
				Integer.class))
			.isEqualTo(1);
	}

	@Test
	void neverLogsTheRawToken(CapturedOutput output) {
		mailbox.simulateOutage(true);
		invitationService.issue("falha@x.com", adminId);
		mailbox.simulateOutage(false);
		invitationService.issue("ana@x.com", adminId);

		assertThat(output.getAll()).contains("mail delivery failed")
			.doesNotContain(mailbox.lastTokenSentTo("ana@x.com"));
	}

	private List<String> readTokenHashes() {
		return jdbcTemplate.queryForList("SELECT token_hash FROM invitations", String.class);
	}

	private int countRowsContaining(String rawToken) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM invitations i WHERE i::text LIKE ?", Integer.class,
				"%" + rawToken + "%");
	}

}
