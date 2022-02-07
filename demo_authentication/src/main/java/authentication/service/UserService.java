package authentication.service;

import authentication.entity.User;
import authentication.repository.RoleRepository;
import authentication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void encodePassword(User user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
    }

    public void save(User user) {
        userRepo.save(user);
    }

    public User findByName(String name) {
        return userRepo.findByUsername(name).get();
    }

    public void update(User user, Integer id) {
        User existedUser = userRepo.findById(id).get();
        if (existedUser != null) {
            if (user.getPassword().isEmpty()) {
                user.setPassword(existedUser.getPassword());
            } else {
                encodePassword(user);
            }
            user.setId(id);
        }
        save(user);
    }

    public void deleteById(Integer id) {
        userRepo.deleteById(id);
    }
}
