@REM Copyright (c) Microsoft. All rights reserved.
@REM Licensed under the MIT license. See LICENSE file in the project root for full license information.

ECHO We're working with "%ANDROID_DEVICE_NAME%"
@REM -- installing device and test apk--
ECHO installing apk on device
call adb -s %ANDROID_DEVICE_NAME% install -r -t "app\build\outputs\apk\debug\app-debug.apk" 
ECHO installing test apk on device
call adb -s %ANDROID_DEVICE_NAME% install -r -t "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"
@REM -- Starting Android Tests --
ECHO starting android tests
python runInstrumentationTestsLocal.py %ANDROID_DEVICE_NAME% %TEST_OPTION% %TEST_OPTION_VAL%