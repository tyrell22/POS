package com.restaurant.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Restaurant POS System - Main Application
 * 
 * Ресторан POS Систем - Главна апликација
 */
@SpringBootApplication
public class RestaurantPosApplication {
    
    public static void main(String[] args) {
        System.out.println("🍽️  Starting Restaurant POS System...");
        System.out.println("Стартување на Ресторан POS Систем...");
        
        SpringApplication.run(RestaurantPosApplication.class, args);
        
        System.out.println("✅ Restaurant POS System started successfully!");
        System.out.println("✅ Ресторан POS Системот е успешно стартован!");
        System.out.println("📱 Frontend: http://localhost:3000");
        System.out.println("🔧 Backend API: http://localhost:8080/api");
        System.out.println("🗄️  H2 Database Console: http://localhost:8080/h2-console");
    }
}
