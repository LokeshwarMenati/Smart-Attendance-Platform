@echo off
echo ========================================================
echo Starting Smart Attendance Platform - Backend (Port 8080)
echo Profile: local (zero-config, auto-seeded test accounts)
echo ========================================================
call .\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
pause

