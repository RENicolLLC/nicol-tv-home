# Nicol TV Home Preview

Nicol TV Home Preview is a remote-friendly home screen for Fire TV. It is currently installed as a separate preview beside the original Nicol TV application so new features can be tested without deleting the older version.

Nicol TV Home is open-source software released under the [MIT License](LICENSE).

## Current installed version

- Application name: **Nicol TV Preview**
- Package: `com.renicol.tv.preview`
- Version: **0.5.2-preview**
- Home on Fire target: **Nicol TV Preview**
- Replace Home: **On**
- Launch on Boot/Wake: **On**

## Install Nicol TV

Nicol TV is currently a preview for Fire TV devices that support Android APK installation. It does not replace Fire OS firmware. Some newer Amazon devices use Vega OS and do not support Android APK sideloading; this preview will not install on those devices.

### Without a computer: Fire TV Downloader

1. From the Fire TV home screen, choose **Find**, then **Search**.
2. Search for **Downloader** and install **Downloader by AFTVnews** from the Amazon Appstore.
3. If **Developer options** is hidden, open **Settings → My Fire TV → About**, highlight the name of the Fire TV device, and press **Select** seven times.
4. Open **Settings → My Fire TV → Developer options → Install unknown apps** and turn **Downloader** On. Older Fire OS versions may call this **Apps from Unknown Sources**.
5. Open Downloader and enter this official address:

   `https://github.com/RENicolLLC/nicol-tv-home/releases/latest/download/Nicol-TV-Home-v0.5.2-preview.apk`

6. When the download finishes, choose **Install**, then **Done** or **Open**.
7. Downloader may offer to delete the downloaded APK after installation. It is safe to delete that downloaded copy because the installed application remains on the Fire TV.
8. Follow **Make Nicol TV the Home screen** below if you want the remote's Home button to open Nicol TV.

Only install Nicol TV from this repository's official GitHub release. Leave Downloader's unknown-app permission Off when you are not installing or updating Nicol TV.

### With a computer: wireless ADB

The Fire TV and computer must be on the same local network.

1. On the Fire TV, reveal **Developer options** as described above.
2. Open **Settings → My Fire TV → Developer options** and turn **ADB debugging** On.
3. Find the Fire TV's address under **Settings → My Fire TV → About → Network**.
4. On the computer, download `Nicol-TV-Home-v0.5.2-preview.apk` from the latest GitHub release.
5. With Android Platform Tools installed, run:

   ```sh
   adb connect FIRE_TV_ADDRESS:5555
   adb install -r Nicol-TV-Home-v0.5.2-preview.apk
   ```

6. Accept the connection prompt on the television if Fire TV displays one.
7. When installation finishes, open **Nicol TV Preview** from the Fire TV applications screen.

Replace `FIRE_TV_ADDRESS` with the address displayed by the Fire TV, such as `192.168.1.50`.

### Make Nicol TV the Home screen

Nicol TV can be opened like a normal application without changing the Home button. Redirecting the Home button requires a compatible home-launcher helper such as **Home on Fire**.

In Home on Fire, confirm:

- Target application: **Nicol TV Preview** (`com.renicol.tv.preview`)
- Accessibility service: **On**
- Replace Home: **On**
- Launch on Boot/Wake: **On**

The availability and behavior of home-launcher helpers can vary by Fire OS version. Nicol TV remains separately installed and does not modify the device firmware.

### Update Nicol TV

Repeat either installation method with the newer APK. Android will update the existing installation while preserving its locally stored settings when the application signature and package name remain the same.

### Restore the standard Fire TV Home screen

Open Home on Fire and turn **Replace Home** Off, or change its target away from Nicol TV. You can then uninstall **Nicol TV Preview** from **Settings → Applications → Manage Installed Applications** if desired.

## Remote navigation

### Favorite apps

The first screen contains ten favorites in two rows of five.

- **Left/Right:** Move across a row.
- **Down:** Move from the first row to the second row.
- **Down from the second row:** Continue to the next group of installed apps.
- **Up:** Return to the preceding row or group.
- **Select:** Open the highlighted app.

### Arrange the ten favorites

1. Highlight one of the ten favorite apps.
2. Hold **Select** until the tile becomes gold and the screen says **Arrange Mode**.
3. Use Left, Right, Up, and Down to move it.
4. Press **Select** once to save the order.

The order is stored locally and survives application and Fire TV restarts.

### Browse the other apps

Continue pressing Down beyond the favorite rows. Nicol TV shows the remaining launchable television apps in additional two-row groups. Press Up to return toward Favorites.

System applications can appear in the additional groups. A later Edit Apps screen will support hiding utilities and duplicate entries.

## Backgrounds

### Open the background picker

1. From Nicol TV Home, press the remote's **Menu** button once.
2. Highlight **Choose background**.
3. Press **Select**.
4. Move through the background grid with the directional ring.
5. Press **Select** to apply a background.
6. Press Back to cancel without changing it.

### Background controls

- **Choose background:** Opens the current 15-gradient picker.
- **Rotate now:** Immediately advances to the next available background.
- **Background rotation:** Enables or disables automatic changes.

If rotation is On, a manually selected background will eventually change. Turn rotation Off to keep one background fixed.

### Current and planned background libraries

Available now:

- 25 photographic scenery images from a global collection
- 15 generated gradients
- 3840×2160 PNG masters preserved in `scenery-masters-4k/`
- Optimized 1920×1080 WebP copies bundled in `assets/scenery/`
- Lightweight picker thumbnails bundled in `assets/scenery-thumbs/`

Planned:

- Imported images from a computer or phone
- Fire TV screenshots used as backgrounds
- Custom images created from written descriptions
- Crop, position, darken, preview, export, and delete controls

The included scenery collection was created specifically for Nicol TV, preserved as 4K masters, and optimized for responsive decoding on the Fire TV.

## Quick Settings

Press **Menu** once from Nicol TV Home. The current controls are:

1. Choose background
2. Rotate now
3. Background rotation
4. Weather
5. Clock
6. Date
7. Animations

Use Up and Down to move, Select to activate or toggle, and Menu or Back to close.

## Weather

The preview currently displays Phoenix weather using Open-Meteo. It shows current temperature, condition, feels-like temperature, and today's high and low. It retries transient failures three times, refreshes every 30 minutes, preserves the last successful forecast, and retries again automatically after an outage. Location search, hourly forecasts, multi-day forecasts, units, additional weather panels, and alerts are planned expansions.

## Home button and startup

Home on Fire redirects the Fire remote's Home button to Nicol TV Preview. The Preview is also configured to open when the Fire TV boots or wakes.

The original Nicol TV application remains installed. If Home on Fire is disabled or its target is changed back to `com.renicol.tv`, the older screen will return.

## Troubleshooting

### The old Nicol TV screen appears

Open Home on Fire and confirm:

- Target app: Nicol TV Preview (`com.renicol.tv.preview`)
- Accessibility service: On
- Replace Home: On
- Launch on boot: On

### A selected app starts slowly

Nicol TV sends the launch request immediately after Select is released. Streaming applications may still take time to initialize their own interface, network connection, profiles, advertising, or cached state. The Preview should use the application's official Leanback launch intent and reuse an existing task when possible.

### The selected background changes later

Turn **Background rotation** Off in Quick Settings.

### Choose Background launches an app instead

That button-release issue was corrected in version 0.4.3-preview. Confirm the installed version is current.

## Safety and privacy

- The Preview does not replace Fire OS firmware.
- The older Nicol TV app remains installed.
- Settings are stored locally.
- No analytics are included.
- Personal images will remain local unless the owner explicitly exports them.

## Build

The project builds with the Android SDK already installed on the development Mac:

```sh
./build.sh
```

The signed preview APK is produced at:

`build/apk/Nicol-TV-Home-v0.5.2-preview.apk`

