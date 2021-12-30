#*******************************************************************************
# Copyright (c) 2009-2022 Jean-François Lamy
#
# Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
# License text at https://opensource.org/licenses/NPOSL-3.0
#*******************************************************************************
git pull

cd owlcms-heroku
git checkout develop
git pull
git commit -m "sync submodules [skip ci]" .
git push

cd ..\publicresults-heroku
git checkout develop
git pull
git commit -m "sync submodules [skip ci]" .
git push

cd ..
git commit -m "sync submodules [skip ci]" owlcms-heroku publicresults-heroku
git push
echo Done. synced develop submodules.


