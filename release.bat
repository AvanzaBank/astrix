
call git checkout -b %1
call versions:set -DnewVersion=%1 -DgenerateBackupPoms=false
call git commit -am "Release %1"
call git tag -a %1
call git checkout master

rem echo "git push --tags origin heads/%1"

