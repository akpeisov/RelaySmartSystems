package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findById(String id) {
        return userRepository.getOne(id);
    }
    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public User findByToken(String token) {
        return userRepository.getOne(token);
    }
}
