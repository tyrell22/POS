# Restaurant POS Backend Startup Script
Write-Host "🔧 Starting Restaurant POS Backend..." -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green

# Check if Maven is installed
try {
    $mvnVersion = mvn --version
    Write-Host "✅ Maven found" -ForegroundColor Green
} catch {
    Write-Host "❌ Maven is not installed. Please install Maven first." -ForegroundColor Red
    Write-Host "   Download from: https://maven.apache.org/download.cgi" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Check Java version
try {
    $javaVersion = java -version 2>&1
    Write-Host "☕ Java version: $javaVersion" -ForegroundColor Blue
} catch {
    Write-Host "❌ Java is not installed. Please install Java 17+." -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Start the application
Write-Host "🚀 Starting Spring Boot application..." -ForegroundColor Yellow
mvn spring-boot:run
