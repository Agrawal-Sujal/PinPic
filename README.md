# PinPic

PinPic helps you stop asking “where was this taken?” by pinning every photo you take to an interactive map. Open the map to explore your memories geographically — tap any dot to view the photo you took there.

A satisfying, simple way to turn your camera roll into a map of moments.

## Features

- Tap to shoot: take photos quickly from inside the app and have them automatically pinned to your exact location.
- Map view: see all your pinned photos as dots; tap any dot to preview the photo and its time/location.
- Built-in chat: send pinned photos to friends directly inside PinPic without switching apps.
- Persistent pins: photos and their locations are saved so you can revisit memories later.
- Great for travel, hikes, local discovery, or documenting everyday life.

## Why PinPic?

Photos are better with context. PinPic gives you a fast, visual way to explore where you’ve been — not just what you saw. Whether you’re retracing a trip, sharing a moment with friends, or organizing your camera roll by place, PinPic makes location-first photo browsing delightful.

## Getting started

These are general setup steps — check the repo for platform-specific folders (e.g. `ios`, `android`, `src`, `app`) for exact instructions.

1. Clone the repo

   git clone https://github.com/Agrawal-Sujal/PinPic.git

2. Install dependencies

   - If this is a React Native / Expo project: run `yarn` or `npm install` in the project root.
   - If it’s a native iOS/Android project use the platform tooling described in the corresponding folder.

3. Run the app

   - For React Native / Expo: `yarn ios` / `yarn android` or `expo start` then open on a simulator or device.
   - For native projects: open the workspace in Xcode or Android Studio and run on a simulator or device.

4. Grant permissions when prompted: Camera, Location, and Photo Library (read/write) for full functionality.

## Usage

- Launch the app and tap to take a photo. PinPic will request permission and then capture the photo and current location.
- Open the map tab to see pins. Tap a pin to open the photo preview with its timestamp and location info.
- Use the chat feature to share a pinned photo with a friend right from the app.

## Privacy & Data

PinPic stores photos and location pins locally by default so you keep full control of your data. If the app syncs or shares photos via a backend (e.g., for messaging), check the repo and settings for storage and sharing configuration. Always request and explain location access to users following platform privacy guidelines.

## Contributing

Contributions are welcome! Good first steps:

- Open an issue to discuss larger changes or feature ideas.
- Fork the repo, create a feature branch, and submit a pull request with a clear description of your changes.
- Keep changes small and focused; include tests where appropriate.

## Troubleshooting

- If photos aren’t pinned: make sure Location Services are enabled and the app has permission to access location while in use.
- If camera doesn’t work: ensure Camera permission is granted and your device/simulator supports a camera.

## Licence

This project is open source — add a LICENSE file (for example, MIT) to clarify terms for contributions and reuse.

## Contact

If you have questions or want to collaborate, open an issue or reach out through the repo owner’s GitHub profile: https://github.com/Agrawal-Sujal
