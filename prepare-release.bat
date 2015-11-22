@echo off
if [%1]==[] goto usage
call git checkout -b %1
call mvn versions:set -DnewVersion=%1 -DgenerateBackupPoms=false
call git commit -am "Release %1"
call git tag -a %1
call git checkout refs/tags/%1
call git branch -D %1
goto :eof


:usage
@echo Usage: prepare-release.bat MAJOR.MINOR.PATCH
@echo.
@echo Example:
@echo ^> prepare-release.bat 1.0.1