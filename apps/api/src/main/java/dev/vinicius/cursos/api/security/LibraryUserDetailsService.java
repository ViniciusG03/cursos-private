package dev.vinicius.cursos.api.security;

import dev.vinicius.cursos.api.account.domain.AccountEmail;
import dev.vinicius.cursos.api.account.domain.InvalidAccountEmailException;
import dev.vinicius.cursos.api.account.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Busca de contas para o login do Spring Security. O e-mail é normalizado igual ao cadastro, e a
 * existência dele nunca aparece na resposta: o handler de falha devolve o mesmo erro genérico.
 * Sendo um bean, também impede o Spring Boot de gerar o usuário padrão com senha no log.
 */
@Service
class LibraryUserDetailsService implements UserDetailsService {

	private final UserAccountRepository accountRepository;

	LibraryUserDetailsService(UserAccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String rawEmail) {
		String normalizedEmail = normalizeOrReject(rawEmail);
		return accountRepository.findByEmail(normalizedEmail)
			.map(LibraryUserPrincipal::forAuthentication)
			.orElseThrow(() -> new UsernameNotFoundException("no account for the given email"));
	}

	private static String normalizeOrReject(String rawEmail) {
		try {
			return AccountEmail.parse(rawEmail).value();
		}
		catch (InvalidAccountEmailException malformedEmail) {
			throw new UsernameNotFoundException("no account for the given email");
		}
	}

}
