@echo off
mvn package -pl mcp-client -am
if %errorlevel% neq 0 exit /b %errorlevel%
java -cp mcp-client\target\mcp-client.jar com.example.mcp.client.demo.ClientDemo
