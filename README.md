# Crisis Cleanup

App is started from [Now in Android](https://github.com/android/nowinandroid).
- build-logic keeps nowinandroid/NIA naming from the original files.
- Normally demo builds are limited/free versions. In this case treat demo as non-production builds for local development using non-production services.
- Benchmark related files and settings were removed to simplify v1.

## E2e tests

### Run tests

1. Install maestro
    Recommended way: 
        - Install with `asdf install` or `rtx install`. This will install maestro with version defined in [`.tool-versions`](.tool-versions).
    Other: 
        - https://maestro.mobile.dev/getting-started/installing-maestro
2. Start android emulator
3. Make sure the required env vars defined in [`.envrc`](.envrc) are set in your environment.
4. Run tests
    ```sh
    maestro test ./.maestro/<test-dir>

    # Run all tests using test script (recommended)
    # Build app before to get app apk before running
    ./scripts/maestro.sh app/build/outputs/apk/demo/debug/app-demo-debug.apk local
    ```

### Start maestro studio

```sh
maestro studio
```

### View App's Hierarchy

```sh
maestro hierarchy
```
