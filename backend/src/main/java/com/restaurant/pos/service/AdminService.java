package com.restaurant.pos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    @Autowired
    private PrinterService printerService;

    public boolean validateAdminCode(String code) {
        return printerService.validateAdminCode(code);
    }
}