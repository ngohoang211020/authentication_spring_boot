package authentication.controller;

import authentication.entity.ERole;
import authentication.entity.Role;
import authentication.entity.User;
import authentication.request.LoginRequest;
import authentication.request.SignupRequest;
import authentication.response.JwtResponse;
import authentication.response.MessageResponse;
import authentication.security.jwt.JwtUtils;
import authentication.security.service.UserDetailsImpl;
import authentication.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // Xác thực từ username và password.
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            //Set chuỗi authentication đó cho UserPrincipal
            // Nếu không xảy ra exception tức là thông tin hợp lệ
            // Set thông tin authentication vào Security Context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // Trả về jwt cho người dùng.
            String jwt = jwtUtils.generateJwtToken(authentication);// Tạo ra jwt từ chuỗi authentication

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();//lay thong tin user
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(), userDetails.getName(),
                    userDetails.getPassword(), roles));
        } catch (AuthenticationException e) {
            return ResponseEntity.ok(new MessageResponse("Error: Authentication Fail", false));

        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        if (userService.existByUserName(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!", false));
        }


        // Create new user's account
        User user = new User(signUpRequest.getUsername(),signUpRequest.getName(),
                encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles.size() != 0) {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = userService.findByName(ERole.ROLE_ADMIN);
                        roles.add(adminRole);
                        break;
                    case "user":
                        Role userRole = userService.findByName(ERole.ROLE_USER);
                        roles.add(userRole);
                        break;
                }
            });
        } else {
            Role userRole = userService.findByName(ERole.ROLE_USER);
            roles.add(userRole);
        }

        user.setRoles(roles);
        userService.save(user);
        return ResponseEntity.ok(new MessageResponse("User registered successfully!", true));
    }
}
