@echo off
echo Iniciando Backend Java Spring Boot da Compra Coletiva...
cd /d "%~dp0\backend-spring"
call mvnw.cmd spring-boot:run
pause
