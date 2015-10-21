call git checkout master
call mvn versions:set -DnewVersion=%1 -DgenerateBackupPoms=false