package com.example.api.task;

import com.example.api.model.entity.Admin;
import com.example.api.repository.AdminRepository;
import com.example.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//task executed on application startup
@Component
@Order(1)
public class ConsumeMqTask implements ApplicationRunner {
    @Autowired
    private AdminRepository adminRepository;

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumeMqTask.class);

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LOGGER.info("start to run ConsumeMqTask.");
// startup logic that bootstrapped the admin account has been removed
        LOGGER.info("end to run ConsumeMqTask.");
    }
}

