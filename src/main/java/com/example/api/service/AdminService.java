package com.example.api.service;

import com.example.api.model.dto.LoginDto;
import com.example.api.model.entity.Admin;

import java.util.List;

public interface AdminService {

    Admin save(Admin admin) throws Exception;

    Admin findById(String id);

    /**
     * Issues a one-time code to the address, if it belongs to an account.
     *
     * <p>Returns nothing and says nothing about whether the account exists. The
     * boolean it used to return was turned into a message by the controller, which
     * made a refusal by policy and a mail outage indistinguishable.
     */
    void sendEmail(String email);

    Admin loginByPassword(LoginDto dto) throws Exception;

    Admin loginByEmail(LoginDto dto) throws Exception;

    List<Admin> findAll();

    //generate token
    String createToken(Admin admin, long exp);

    void delete(String id);
}
