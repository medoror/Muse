# Muse project commands

# Run all tests
test:
    ./gradlew :composeApp:testDebugUnitTest

# Run specific test class
test-class class:
    ./gradlew :composeApp:testDebugUnitTest --tests "*{{class}}*"

# Run tests and watch for changes
test-watch:
    ./gradlew :composeApp:testDebugUnitTest --continuous

# Clean and test
test-clean:
    ./gradlew clean :composeApp:testDebugUnitTest

# Build the app
build:
    ./gradlew build

# Clean build
clean:
    ./gradlew clean

# Run debug build
debug:
    ./gradlew assembleDebug

# Show test results summary
test-results:
    @echo "📊 Test Results:"
    @find composeApp/build/test-results -name "*.xml" -exec grep -H "tests=" {} \;