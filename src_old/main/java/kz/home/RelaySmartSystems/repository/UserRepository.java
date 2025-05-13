package kz.home.RelaySmartSystems.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import kz.home.RelaySmartSystems.model.*;

//@Repository
public interface UserRepository extends JpaRepository<User, String> {
    //User findByToken(String token);
//    User findByUsername(String username);
}
