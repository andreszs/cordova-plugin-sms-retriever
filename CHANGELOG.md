# Changelog

## Unreleased

## 4.0.5 - 2024-11-21

- Merged [#20](https://github.com/andreszs/cordova-plugin-sms-retriever/pull/20).

## 4.0.4 - 2024-10-31

- Merged [#19](https://github.com/andreszs/cordova-plugin-sms-retriever/pull/19).

## 4.0.3 - 2024-10-23

- Updated package.json as recommended by npm.

## 4.0.2 - 2024-10-23

- Merged #18.
- Removed the nbproject folder.

## 4.0.1 - 2023-07-20

- Restored support for SDK versions with minimum 21 and target 31.
- Tested on Android 5.1, 6.0, 13, and 14.

## 4.0.0 - 2023-07-19

- Added autofill support for [Android 14](https://developer.android.com/about/versions/14/behavior-changes-14#runtime-receivers-exported).
- Breaking change: set the minimum SDK to 24 and target SDK to 33.

## 3.0.0 - 2023-04-15

- Added `stopWatch()`.
- Added `getPhoneNumber()`.

## 2.0.1 - 2022-05-14

- Updated the README with important details about SMS generation.

## 2.0.0 - 2022-04-27

- Moved methods from the global `window` object to `cordova.plugins`.
- Improved method return values to make them easier to parse.
- Removed the requirement to include the `<#>` prefix in the SMS.
- Improved stability and error checking.
- Updated the demo app and made it available in Google Play.
