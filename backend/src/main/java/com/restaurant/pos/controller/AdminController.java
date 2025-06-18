package com.restaurant.pos.controller;

import com.restaurant.pos.dto.AdminLoginRequest;
import com.restaurant.pos.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private AdminService adminService;
    
    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginRequest request) {
        // Add debugging logs
        logger.info("Received admin login request");
        logger.info("Request object: {}", request);
        logger.info("Admin code from request: '{}'", request != null ? request.getAdminCode() : "null");
        
        // Manual validation to provide better error messages
        if (request == null) {
            logger.warn("Request object is null");
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Неважечки податоци - празен објект"
            ));
        }
        
        if (request.getAdminCode() == null) {
            logger.warn("Admin code is null");
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Админ кодот е задолжителен"
            ));
        }
        
        if (request.getAdminCode().trim().isEmpty()) {
            logger.warn("Admin code is empty or whitespace");
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Админ кодот не може да биде празен"
            ));
        }
        
        try {
            if (adminService.validateAdminCode(request.getAdminCode())) {
                logger.info("Admin login successful");
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Успешна најава"
                ));
            } else {
                logger.warn("Admin login failed - invalid code");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Неточен админ код"
                ));
            }
        } catch (Exception e) {
            logger.error("Error during admin login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Грешка на серверот"
            ));
        }
    }
}