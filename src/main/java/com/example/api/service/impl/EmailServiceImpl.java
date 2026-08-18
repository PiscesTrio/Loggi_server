package com.example.api.service.impl;

import com.example.api.model.entity.Code;
import com.example.api.repository.CodeRepository;
import com.example.api.service.EmailService;
import com.example.api.utils.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    private CodeRepository codeRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public boolean sendVerificationCode(String email) throws MailException {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        //set the mail subject
        message.setSubject("验证码");
        String value = "123456";
        message.setText("你的验证码为:  " + value + "  十五分钟内有效");
        try {
//            mailSender.send(message);
        }catch (Exception e){
            log.error("Failed to send a verification code to {}", email, e);
            return false;
        }
        //persist the verification code
        //email is the primary key, so re-sending updates the existing row
        codeRepository.save(new Code(email, value));
        return true;
    }

    @Override
    public boolean checkVerificationCode(String email, String value) {
        Code code = codeRepository.findByEmailAndValue(email, value);
        if (code != null && code.getExp() > System.currentTimeMillis()) {
            //delete the verification code once the login succeeds
            codeRepository.delete(code);
            return true;
        }
        return false;
    }

}
