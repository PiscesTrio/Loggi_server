package com.example.api.repository;

import com.example.api.model.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {

    // findAdminByEmailAndPassword is gone. A derived query cannot compare a password
    // once it is hashed: BCrypt salts every call, so two encodes of the same input
    // differ and an equality query never matches. The account is fetched by e-mail and
    // the hash is verified in the service.

    Admin findAdminByEmail(String email);

    boolean existsAdminByRoles(String roles);

}
