Target app
`MAESTRO_APP_ID=com.crisiscleanup.demo maestro test auth-tests`

[Specify device](https://maestro.mobile.dev/advanced/specify-a-device)
`maestro --device emulator-5554 test auth-tests`

List devices

- Android `adb devices`
- iOS `xcrun simctl list devices booted`

Full test command
`MAESTRO_APP_ID=com.crisiscleanup.demo.debug maestro --device emulator-5554 test auth-tests`
`MAESTRO_APP_ID=com.crisiscleanup.dev maestro --device device-uuid-from-list test auth-tests`

[Flow file structure](https://maestro.mobile.dev/api-reference/configuration/flow-configuration)