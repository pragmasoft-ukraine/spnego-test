package ua.com.pragmasoft.spnego.test.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ua.com.pragmasoft.spnego.test.service.CredentialsService;

@RestController
public class CredentialsController {

    static final Logger logger = LoggerFactory.getLogger(CredentialsController.class);

    @Autowired
    CredentialsService credentialsService;

    @GetMapping("/credentials")
    public String credentials() {

        var currentDbUserName = credentialsService.currentDatabaseUser();

        return "Current DB user name: " + currentDbUserName;


    }
    
}