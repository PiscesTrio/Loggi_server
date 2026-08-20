package com.example.api.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.api.exception.BizException;
import com.example.api.model.dto.LoginDto;
import com.example.api.model.entity.Admin;
import com.example.api.repository.AdminRepository;
import com.example.api.service.EmailService;
import com.example.api.utils.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Passwords are hashed now, so these assertions replace the ones S00 wrote.
 *
 * <p>S00 deliberately pinned the plaintext behaviour — a derived query comparing the raw password,
 * and a {@code save} that stored whatever it was handed — and said in its own comment that the
 * BCrypt slice would turn them red. It did. Those two tests are gone with the behaviour they
 * described.
 *
 * <p>The encoder here is real, not a mock. Mocking it would assert that the service calls a method;
 * the question worth asking is whether what reaches the database can still be read back, and only a
 * real hash answers that.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock AdminRepository adminRepository;
    @Mock EmailService emailService;
    @Mock JwtTokenUtil jwtTokenUtil;

    private final PasswordEncoder encoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private AdminServiceImpl service() {
        AdminServiceImpl s = new AdminServiceImpl();
        ReflectionTestUtils.setField(s, "adminRepository", adminRepository);
        ReflectionTestUtils.setField(s, "emailService", emailService);
        ReflectionTestUtils.setField(s, "jwtTokenUtil", jwtTokenUtil);
        ReflectionTestUtils.setField(s, "passwordEncoder", encoder);
        return s;
    }

    @Test
    @DisplayName("save stores a hash, never the password it was given")
    void save_storesAHashNotThePlaintext() throws Exception {
        Admin admin = new Admin();
        admin.setEmail("admin@loggi.example");
        admin.setPassword("plaintext123");
        when(adminRepository.save(any(Admin.class))).thenAnswer(i -> i.getArgument(0));

        service().save(admin);

        assertThat(admin.getPassword()).isNotEqualTo("plaintext123");
        // DelegatingPasswordEncoder prefixes the algorithm; matches() uses it to route.
        assertThat(admin.getPassword()).startsWith("{bcrypt}");
        assertThat(encoder.matches("plaintext123", admin.getPassword())).isTrue();
        // createAt is no longer asserted here. The service used to set it by hand; JPA
        // auditing fills it now, and auditing needs a real persistence context — with a
        // mocked repository there is nothing to observe. AuditingIT checks it for real.
        verify(adminRepository).save(admin);
    }

    @Test
    @DisplayName("login looks the account up by e-mail alone and verifies the hash")
    void loginByPassword_looksUpByEmailThenVerifies() throws Exception {
        Admin stored = new Admin();
        stored.setEmail("admin@loggi.example");
        stored.setPassword(encoder.encode("correct-horse"));
        when(adminRepository.findAdminByEmail("admin@loggi.example")).thenReturn(stored);

        LoginDto dto = new LoginDto();
        dto.setEmail("admin@loggi.example");
        dto.setPassword("correct-horse");

        assertThat(service().loginByPassword(dto)).isSameAs(stored);
        // The e-mail is the only thing that may reach the query. A derived finder taking
        // the password would compare hashes for equality, which never matches: BCrypt
        // salts every call, so encode() of the same input differs every time.
        verify(adminRepository).findAdminByEmail("admin@loggi.example");
    }

    @Test
    @DisplayName("A wrong password is rejected even though the account exists")
    void loginByPassword_wrongPassword_throws() {
        Admin stored = new Admin();
        stored.setEmail("admin@loggi.example");
        stored.setPassword(encoder.encode("correct-horse"));
        when(adminRepository.findAdminByEmail("admin@loggi.example")).thenReturn(stored);

        LoginDto dto = new LoginDto();
        dto.setEmail("admin@loggi.example");
        dto.setPassword("wrong");

        assertThatThrownBy(() -> service().loginByPassword(dto))
                .isExactlyInstanceOf(Exception.class)
                .hasMessage("wrong email or password");
    }

    @Test
    @DisplayName("An unknown e-mail fails the same way a wrong password does")
    void loginByPassword_unknownEmail_throwsTheSameMessage() {
        when(adminRepository.findAdminByEmail(anyString())).thenReturn(null);

        LoginDto dto = new LoginDto();
        dto.setEmail("nobody@loggi.example");
        dto.setPassword("whatever");

        // Same message on purpose: a different one would tell an attacker which
        // e-mail addresses have accounts.
        assertThatThrownBy(() -> service().loginByPassword(dto))
                .isExactlyInstanceOf(Exception.class)
                .hasMessage("wrong email or password");
    }

    @Test
    @DisplayName("Two accounts with the same password get different hashes")
    void save_saltsEachHashIndependently() throws Exception {
        when(adminRepository.save(any(Admin.class))).thenAnswer(i -> i.getArgument(0));
        Admin a = new Admin();
        a.setEmail("a@loggi.example");
        a.setPassword("same-password");
        Admin b = new Admin();
        b.setEmail("b@loggi.example");
        b.setPassword("same-password");

        AdminServiceImpl s = service();
        s.save(a);
        s.save(b);

        // If this ever fails, the encoder has been swapped for an unsalted digest and
        // the hashes have become a lookup table.
        assertThat(a.getPassword()).isNotEqualTo(b.getPassword());
        assertThat(encoder.matches("same-password", a.getPassword())).isTrue();
        assertThat(encoder.matches("same-password", b.getPassword())).isTrue();
    }

    @Test
    @DisplayName("save rejects email length < 8 or password length < 5 (pins weak-validation bug)")
    void save_whenEmailUnder8OrPasswordUnder5_throws() {
        Admin admin = new Admin();
        admin.setEmail("a@b.com"); // length 7 < 8
        admin.setPassword("123456");
        // A BizException now, not a bare Exception: the caller can act on this one, and it
        // carries the code that says which failure it is.
        assertThatThrownBy(() -> service().save(admin))
                .isExactlyInstanceOf(BizException.class)
                .hasMessage("email or password too short");
        verifyNoInteractions(adminRepository); // not persisted when validation fails
    }
}
