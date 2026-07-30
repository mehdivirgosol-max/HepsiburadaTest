@echo off
setlocal
chcp 65001 >nul

pushd "%~dp0"
if errorlevel 1 exit /b 4

set "EXIT_CODE=0"

if not defined HB_EMAIL (
  echo [HATA] HB_EMAIL ortam degiskeni tanimli degil.
  set "EXIT_CODE=2"
  goto finish
)

if not defined HB_PASSWORD (
  echo [HATA] HB_PASSWORD ortam degiskeni tanimli degil.
  set "EXIT_CODE=2"
  goto finish
)

where gauge >nul 2>nul
if errorlevel 1 (
  echo [HATA] Gauge PATH uzerinde bulunamadi.
  set "EXIT_CODE=3"
  goto finish
)

call mvn -q clean test-compile
if errorlevel 1 (
  set "EXIT_CODE=%errorlevel%"
  goto finish
)

gauge run specs\hepsiburada-shopping.spec
set "EXIT_CODE=%errorlevel%"

:finish
popd
exit /b %EXIT_CODE%
