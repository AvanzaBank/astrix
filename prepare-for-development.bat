@echo off
if [%1]==[] goto usage
call git checkout master
call mvn versions:set -DnewVersion=%1-SNAPSHOT -DgenerateBackupPoms=false
call git commit -am "Prepare for development of %1-SNAPSHOT" 
@echo.
@echo "Done: Current development version is %1-SNAPSHOT" 
goto :eof


:usage
@echo Usage: prepare-for-development.bat MAJOR.MINOR.PATCH
@echo.
@echo Example:
@echo ^> prepare-for-development.bat 1.0.1