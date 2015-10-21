call git checkout -b %1
call mvn versions:set -DnewVersion=%1 -DgenerateBackupPoms=false
call git commit -am "Release %1"
call git tag -a %1
call git checkout refs/tags/%1
call git branch -D %1

