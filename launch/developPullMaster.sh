#!/bin/bash

#*******************************************************************************
# Copyright (c) 2009-2022 Jean-François Lamy
#
# Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
# License text at https://opensource.org/licenses/NPOSL-3.0
#*******************************************************************************

# merge the master branch back into develop
LOCAL=develop
REMOTE=master

cd publicresults-heroku
git checkout $LOCAL
git fetch
git merge origin/$REMOTE --no-ff -m "merge $REMOTE [skip ci]"
git push origin develop
cd ..
cd owlcms-heroku
git checkout $LOCAL
git fetch
git merge origin/$REMOTE --no-ff -m "merge $REMOTE [skip ci]"
git push origin develop
cd ..
git checkout $LOCAL
git fetch
git merge origin/$REMOTE --no-ff -m "merge $REMOTE [skip ci]"
git push origin develop
echo Done.  pulled $REMOTE into $LOCAL.
