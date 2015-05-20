
echo "git checkout -b %1"
echo "mvn versions:set -DnewVersion=%1 -DgenerateBackupPoms=false"
echo "git commit -am "Release %1"
echo "git tag -a %1"

echo "git push --tags origin heads/%1"

