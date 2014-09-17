#!/bin/bash
# deploy mmsapp

tmpDir=/tmp/mmsappZip

mmsappDeployDir=$1
mmsappDir=$2
backupDir=$3
mmsappZip=$4

if [ ! -d backupDir ]; then
  backupDir=${mmsappDeployDir}/..
fi

# unzip zip file if provided
if [ -f "$mmsappZip" ) ]; then
  mkdir $tmpDir
  pushd $tmpDir
  unzip $mmsappZip
  unzippedDir=`/bin/ls -1`
  #unzippedDir=`readlink -e $unzippedDir`
  mmsappDir=`readlink -e $unzippedDir`
  popd
fi

# backup existing mmsapp dir
if [ -e "$mmsappDeployDir" ]; then
  echo "/bin/mv $mmsappDeployDir ${backupDir}/mmsapp.`date '+%Y%m%d-%H%M%S'`"
  /bin/mv $mmsappDeployDir ${backupDir}/mmsapp.`date '+%Y%m%d-%H%M%S'`
  #/bin/rm -rf $mmsappDeployDir
fi

# copy the mmsapp directory to the deployed location
if [ -d $mmsappDir ]; then
  echo cp -pRf $mmsappDir $mmsappDeployDir
  cp -pRf $mmsappDir $mmsappDeployDir
fi

# delete the temporary unzip directory if it exists
if [ -e "$tmpDir" ]; then
  echo /bin/rm -rf $tmpDir
  /bin/rm -rf $tmpDir
fi

# change permissions for the deployed mmsapp
if [ -d "$mmsappDeployDir" ]; then
  echo chown -Rh ${owner}:${owner} $mmsappDeployDir
  chown -Rh ${owner}:${owner} $mmsappDeployDir
fi
