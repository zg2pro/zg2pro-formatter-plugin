#! /bin/bash

if ! [ -x "$(command -v git)" ]; then
  echo 'Error: git is not installed in path.' >&2
  exit 1
fi

if ! [ -x "$(command -v mvn)" ]; then
  echo 'Error: maven is not installed in path.' >&2
  exit 1
fi

echo running validation and formatting
#If -v is specified twice, then also show the changes in the working tree that have not yet been staged
before=`git status -v -v`
echo $before

# pattern will always be found with mvn validate, it cant fail even if compilation fails,
# this helps waiting for mvn process to be done before going to git diffs
mvn clean validate | grep -e 'BUILD SUCCESS'


after=`git status -v -v`
echo $after

if [ "$before" != "$after" ]; then
	echo "\n"
	echo "COMMIT REJECTED Found unformatted code. Please check before commiting and try again"
	exit 1
fi
