#!/bin/bash

function messageExit() {
    RED='\033[0;31m'
    NC='\033[0m'
    echo -e "\n${RED}$1${NC}\n"
    exit 1
}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Newline for easier reading
echo -e "\nBuilding app..."

# Script's dir must have a gradlew
GRADLEW=$DIR/gradlew
if [ ! -f "$GRADLEW" ]; then
    messageExit "Call this script from the project dir"
fi

# App dir must have a build file
APP_BUILD_FILE=$DIR/app/build.gradle.kts
if [[ ! -f "$APP_BUILD_FILE" ]]; then
  messageExit "App build file not found relative to the project dir"
fi

# Build file must not order signing (will be done later this script)
buildFileContents=$(<$APP_BUILD_FILE)
buildSigningLine=$(sed -n '/^[[:space:]]*signingConfig[[:space:]]*=[[:space:]]*signingConfigs\.getByName/p' "$APP_BUILD_FILE")
if [[ ! -z "${buildSigningLine}" ]]; then
    messageExit "Comment the signingConfig line in the app build file before running this script"
fi

# Signing properties must be set as envvars and keystore file must exist
if [[ -z "$CRISIS_CLEANUP_ANDROID_KEYSTORE_PW" ]]; then
  messageExit "Set CRISIS_CLEANUP_ANDROID_KEYSTORE_PW to continue building"
fi
if [[ -z "$CRISIS_CLEANUP_ANDROID_KEYSTORE_KEY_ALIAS" ]]; then
  messageExit "Set CRISIS_CLEANUP_ANDROID_KEYSTORE_KEY_ALIAS to continue building"
fi
if [[ -z "$CRISIS_CLEANUP_ANDROID_KEYSTORE_KEY_PW" ]]; then
  messageExit "Set CRISIS_CLEANUP_ANDROID_KEYSTORE_KEY_PW to continue building"
fi
if [[ -z "$CRISIS_CLEANUP_ANDROID_KEYSTORE_PATH" ]]; then
  messageExit "Set CRISIS_CLEANUP_ANDROID_KEYSTORE_PATH to continue building"
fi
if [[ ! -f "$CRISIS_CLEANUP_ANDROID_KEYSTORE_PATH" ]]; then
  messageExit "Keystore does not exist at specified path. Not attempting build."
fi

# Clean and build
$GRADLEW clean
$GRADLEW bundleAussieRelease

# Copy build (and related) to build dir
APP_OUT=$DIR/app/build/outputs
if [[ -z "$DIST_DIR" ]]; then
  DIST_DIR=$DIR/app/build
fi
DIST_AAB=$DIST_DIR/app-aussie-release.aab
MAPPING_FILE_NAME=release-aab-mapping.txt
cp $APP_OUT/bundle/aussieRelease/app-aussie-release.aab $DIST_AAB
cp $APP_OUT/mapping/aussieRelease/mapping.txt $DIST_DIR/$MAPPING_FILE_NAME

# Sign app bundle
jarsigner -verbose -storepass $CRISIS_CLEANUP_ANDROID_KEYSTORE_PW -keystore $CRISIS_CLEANUP_ANDROID_KEYSTORE_PATH $DIST_AAB $CRISIS_CLEANUP_ANDROID_KEYSTORE_KEY_ALIAS -keypass $CRISIS_CLEANUP_ANDROID_KEYSTORE_KEY_PW

# Most likely successful
if [[ -f "$DIST_AAB" && -f "$DIST_DIR/$MAPPING_FILE_NAME" ]]; then
  GREEN='\033[0;32m'
  NC='\033[0m'
  dirPathLength=${#DIR}
  bundleRelativePath=${DIST_AAB:dirPathLength}
  echo -e "\n${GREEN}Signed bundle at${NC} .$bundleRelativePath. Mapping $MAPPING_FILE_NAME is copied to same area.\n"
else
  messageExit "Something went wrong during build/signing"
fi