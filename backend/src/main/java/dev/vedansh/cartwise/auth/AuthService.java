package dev.vedansh.cartwise.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.vedansh.cartwise.auth.AuthDtos.AuthResponse;
import dev.vedansh.cartwise.auth.AuthDtos.LoginRequest;
import dev.vedansh.cartwise.auth.AuthDtos.RegisterRequest;
import dev.vedansh.cartwise.auth.AuthDtos.UserView;
import dev.vedansh.cartwise.common.ApiException;
import dev.vedansh.cartwise.config.JwtProperties;
import dev.vedansh.cartwise.user.Role;
import dev.vedansh.cartwise.user.User;
import dev.vedansh.cartwise.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties props;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtEncoder jwtEncoder, JwtProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
        this.props = props;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = users.save(new User(req.name().trim(), email, encoder.encode(req.password()), Role.CUSTOMER));
        return issue(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        return issue(user);
    }

    @Transactional(readOnly = true)
    public UserView me(Long userId) {
        return view(users.findById(userId).orElseThrow(() -> ApiException.notFound("User")));
    }

    private AuthResponse issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("cartwise")
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(props.ttlMinutes(), ChronoUnit.MINUTES))
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(token, view(user));
    }

    static UserView view(User u) {
        return new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }
}
