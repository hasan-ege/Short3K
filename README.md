Short3K
Short3K is a professional utility application designed for Android users of the Vita3K emulator. It streamlines the management of your PlayStation Vita library by automating the creation of .psvita shortcut files required for direct game launching.
🚀 Features
•
Automated Library Scanning: Instantly detects all games installed within the Vita3K directory structure.
•
Metadata Extraction: Automatically parses param.sfo files to retrieve game titles, versions, and unique Title IDs.
•
Resource Recognition: Identifies and links official game icons (icon0.png) and background assets (pic0.png) to their respective entries.
•
One-Tap Shortcut Generation: Generates .psvita files individually or in batches for compatibility with Android frontends and launchers.
•
Orphan File Detection: Identifies existing shortcuts that no longer have corresponding game data installed.
•
Multi-language Support: Localized for over 20 languages to ensure a global user experience.
📸 Configuration
Vita3K Directory Setup
To initialize the application, you must provide access to your Vita3K data directory. This enables the scanner to locate your installed applications and metadata.
Instruction: Select the root folder where your Vita3K ux0 data is stored.
Vita3K Folder Selection Example: Selecting the Vita3K system path in the configuration menu.
⚙️ Technical Workflow
1.
Directory Mapping: Define the Vita3K root path and the target output directory for shortcuts in the settings.
2.
Library Indexing: The app scans for valid Title IDs (e.g., PCSB00001) and cross-references them with system files.
3.
Shortcut Creation: Upon selection, the app writes the Title ID into a .psvita file. These files serve as intent triggers for the Vita3K emulator's launch system.
📥 Installation
1.
Download the latest APK from the Releases section.
2.
Enable "Install from Unknown Sources" on your Android device.
3.
Ensure the Vita3K emulator is installed and its data folders are accessible.
📜 License
This project is licensed under the MIT License - see the LICENSE file for details.
Disclaimer: Short3K is an independent utility and is not affiliated with the official Vita3K emulator project.
