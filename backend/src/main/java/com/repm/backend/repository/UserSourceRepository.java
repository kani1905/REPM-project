package com.repm.backend.repository;

import com.repm.backend.entity.UserSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface UserSourceRepository extends JpaRepository<UserSource, Long> {
    
    List<UserSource> findByUserId(Long userId);

}
