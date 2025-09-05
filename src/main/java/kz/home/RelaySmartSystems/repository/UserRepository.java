package kz.home.RelaySmartSystems.repository;

import kz.home.RelaySmartSystems.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

//@Repository
public interface UserRepository extends JpaRepository<User, String> {
    //User findByToken(String token);
//    User findByUsername(String username);
}
