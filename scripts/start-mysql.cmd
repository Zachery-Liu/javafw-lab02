@echo off
setlocal
for %%I in ("%~dp0..") do set "project=%%~fI"
set "config=%project%\.local\idea-native"
if not defined EMP_MYSQL_BIN set "EMP_MYSQL_BIN=D:\Program Files\MySQL\MySQL Server 8.0\bin"

if not exist "%config%\mysql.ini" (
    echo Missing local MySQL configuration: "%config%\mysql.ini"
    exit /b 1
)
if not exist "%EMP_MYSQL_BIN%\mysqld.exe" (
    echo Set EMP_MYSQL_BIN to the local MySQL 8 bin directory.
    exit /b 1
)

"%EMP_MYSQL_BIN%\mysql.exe" --defaults-file="%config%\mysql-client.ini" --execute="SELECT 1" >nul 2>&1
if not errorlevel 1 goto migrate

echo Starting the experiment MySQL 8 instance...
start "" /b "%EMP_MYSQL_BIN%\mysqld.exe" --defaults-file="%config%\mysql.ini" <nul >"%config%\mysql-start.log" 2>&1
for /l %%I in (1,1,60) do (
    "%EMP_MYSQL_BIN%\mysql.exe" --defaults-file="%config%\mysql-client.ini" --execute="SELECT 1" >nul 2>&1
    if not errorlevel 1 goto migrate
    ping.exe -n 2 127.0.0.1 >nul
)
echo MySQL did not become ready. See "%config%\mysql.log".
exit /b 1

:migrate
pushd "%project%"
"%EMP_MYSQL_BIN%\mysql.exe" --defaults-file="%config%\mysql-client.ini" --default-character-set=utf8mb4 --execute="source sql/init.sql" >nul
set "status=%errorlevel%"
popd
if not "%status%"=="0" exit /b %status%
echo Experiment MySQL 8 is ready on 127.0.0.1:52588.
