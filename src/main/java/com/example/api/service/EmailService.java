package com.example.api.service;

import org.springframework.mail.MailException;

public interface EmailService {

    //Send a verification code
    boolean sendVerificationCode(String email) throws MailException;

    //Validate a verification code
    boolean checkVerificationCode(String email, String code);

}
