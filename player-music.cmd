@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if "%~1"=="" goto help
if /i "%~1"=="install" goto install
if /i "%~1"=="login" goto login
if /i "%~1"=="pull" goto pull
if /i "%~1"=="push" goto push
if /i "%~1"=="build" goto build
if /i "%~1"=="status" goto status
if /i "%~1"=="apk" goto apk
goto help

:install
echo Installing Git and GitHub CLI with winget...
winget install --id Git.Git -e --source winget
winget install --id GitHub.cli -e --source winget
echo.
echo Close and reopen CMD, then run: player-music.cmd login
exit /b 0

:login
where git >nul 2>nul || (echo Git not found. Run: player-music.cmd install & exit /b 1)
where gh >nul 2>nul || (echo GitHub CLI not found. Run: player-music.cmd install & exit /b 1)
gh auth login --hostname github.com --git-protocol https --web
if errorlevel 1 exit /b 1
gh repo set-default rafijordan4/player-music
exit /b %errorlevel%

:pull
call :checktools || exit /b 1
git pull --rebase origin main
exit /b %errorlevel%

:push
call :checktools || exit /b 1
git add -A
git diff --cached --quiet
if errorlevel 1 (
  git commit -m "Update Rafi Music"
  if errorlevel 1 exit /b 1
) else (
  echo No local changes to commit.
)
git push origin main
if errorlevel 1 exit /b 1
echo.
echo Push complete. GitHub Actions should start automatically.
echo Check with: player-music.cmd status
exit /b 0

:build
call :checktools || exit /b 1
for /f %%i in ('gh run list --workflow build.yml --branch main --limit 1 --json databaseId --jq ".[0].databaseId"') do set "OLD_RUN=%%i"
echo Starting GitHub Actions build...
gh workflow run build.yml --ref main
if errorlevel 1 exit /b 1
set "RUN_ID="
set /a TRY=0

:waitrun
timeout /t 3 /nobreak >nul
for /f %%i in ('gh run list --workflow build.yml --branch main --limit 1 --json databaseId --jq ".[0].databaseId"') do set "NEW_RUN=%%i"
if defined NEW_RUN if not "%NEW_RUN%"=="%OLD_RUN%" set "RUN_ID=%NEW_RUN%"
if defined RUN_ID goto watchrun
set /a TRY+=1
if %TRY% GEQ 20 (
  echo Could not detect the new run. Check: gh run list
  exit /b 1
)
goto waitrun

:watchrun
echo Watching run %RUN_ID% ...
gh run watch %RUN_ID% --exit-status
if errorlevel 1 (
  echo Build failed. Inspect with: gh run view %RUN_ID% --log-failed
  exit /b 1
)
if not exist dist mkdir dist
gh run download %RUN_ID% --name RafiMusic-debug --dir dist
if errorlevel 1 exit /b 1
echo.
echo APK downloaded to: %CD%\dist
start "" "%CD%\dist"
exit /b 0

:status
call :checktools || exit /b 1
gh run list --limit 10
exit /b %errorlevel%

:apk
call :checktools || exit /b 1
for /f %%i in ('gh run list --workflow build.yml --branch main --status success --limit 1 --json databaseId --jq ".[0].databaseId"') do set "RUN_ID=%%i"
if not defined RUN_ID (
  echo No successful build found yet.
  exit /b 1
)
if not exist dist mkdir dist
gh run download %RUN_ID% --name RafiMusic-debug --dir dist
if errorlevel 1 exit /b 1
start "" "%CD%\dist"
exit /b 0

:checktools
where git >nul 2>nul || (echo Git is missing. Run: player-music.cmd install & exit /b 1)
where gh >nul 2>nul || (echo GitHub CLI is missing. Run: player-music.cmd install & exit /b 1)
gh auth status >nul 2>nul || (echo Login first: player-music.cmd login & exit /b 1)
exit /b 0

:help
echo Rafi Music - GitHub CMD helper
echo.
echo Commands:
echo   player-music.cmd install  Install Git and GitHub CLI
echo   player-music.cmd login    Sign in to GitHub
echo   player-music.cmd pull     Pull latest main branch
echo   player-music.cmd push     Commit and push changes (starts Actions)
echo   player-music.cmd build    Start Actions, watch build, download APK
echo   player-music.cmd status   Show recent Actions runs
echo   player-music.cmd apk      Download latest successful APK
exit /b 0
