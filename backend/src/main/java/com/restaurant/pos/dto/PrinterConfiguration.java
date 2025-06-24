package com.restaurant.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class PrinterConfiguration {

    // Fiscal printer configuration
    private boolean fiscalEnabled;

    @NotBlank(message = "Портата за фискален принтер е задолжителна")
    private String fiscalPort;

    @NotBlank(message = "Типот на фискален принтер е задолжителен")
    private String fiscalType;

    // Thermal printer configuration
    private boolean thermalEnabled;

    @NotBlank(message = "Името на термален принтер е задолжително")
    private String thermalName;

    @NotBlank(message = "Типот на врска е задолжителен")
    private String thermalConnection; // USB, NETWORK, BLUETOOTH

    private String thermalIP;

    @Min(value = 1024, message = "Портата мора да биде најмалку 1024")
    @Max(value = 65535, message = "Портата мора да биде најмногу 65535")
    private int thermalPort;

    // Constructors
    public PrinterConfiguration() {
        // Default values
        this.fiscalEnabled = true;
        this.fiscalPort = "COM1";
        this.fiscalType = "SYNERGY";
        this.thermalEnabled = true;
        this.thermalName = "Epson TM-T20II";
        this.thermalConnection = "USB";
        this.thermalIP = "192.168.1.100";
        this.thermalPort = 9100;
    }

    // Getters and Setters
    public boolean isFiscalEnabled() {
        return fiscalEnabled;
    }

    public void setFiscalEnabled(boolean fiscalEnabled) {
        this.fiscalEnabled = fiscalEnabled;
    }

    public String getFiscalPort() {
        return fiscalPort;
    }

    public void setFiscalPort(String fiscalPort) {
        this.fiscalPort = fiscalPort;
    }

    public String getFiscalType() {
        return fiscalType;
    }

    public void setFiscalType(String fiscalType) {
        this.fiscalType = fiscalType;
    }

    public boolean isThermalEnabled() {
        return thermalEnabled;
    }

    public void setThermalEnabled(boolean thermalEnabled) {
        this.thermalEnabled = thermalEnabled;
    }

    public String getThermalName() {
        return thermalName;
    }

    public void setThermalName(String thermalName) {
        this.thermalName = thermalName;
    }

    public String getThermalConnection() {
        return thermalConnection;
    }

    public void setThermalConnection(String thermalConnection) {
        this.thermalConnection = thermalConnection;
    }

    public String getThermalIP() {
        return thermalIP;
    }

    public void setThermalIP(String thermalIP) {
        this.thermalIP = thermalIP;
    }

    public int getThermalPort() {
        return thermalPort;
    }

    public void setThermalPort(int thermalPort) {
        this.thermalPort = thermalPort;
    }
}