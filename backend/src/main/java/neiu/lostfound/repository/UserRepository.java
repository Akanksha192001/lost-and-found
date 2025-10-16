package neiu.lostfound.repository;

import neiu.lostfound.model.User;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);
  
  Optional<User> findById(Long id);
}
