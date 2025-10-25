@echo off
mvn package
if %errorlevel% neq 0 exit /b %errorlevel%
java -jar mcp-server\target\mcp-server.jar
