package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
//    public Optional<User> findById(String id) {
//        return userRepository.findById(id);
//    }




    public User addUser(String username, String firstname, String lastname) {
        User user = new User();
        user.setUsername(username);
        user.setFirstName(firstname);
        user.setLastName(lastname);
        return userRepository.save(user);
    }
}
