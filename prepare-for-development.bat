call git checkout master
call mvn versions:set -DnewVersion=%1-SNAPSHOT -DgenerateBackupPoms=false