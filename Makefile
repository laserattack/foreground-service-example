BUILD_DIR = app/build
APK_PATH = $(BUILD_DIR)/outputs/apk/debug/app-debug.apk
AVD_NAME = Pixel_API_35

.PHONY: build install emulator clean prepare

prepare:
	chmod +x ./gradlew

build: prepare
	./gradlew clean assembleDebug --no-daemon

install: build
	adb install -r $(APK_PATH)

emulator:
	emulator -avd $(AVD_NAME) -no-snapshot -wipe-data &

clean:
	./gradlew clean --no-daemon
	rm -rf $(BUILD_DIR)
