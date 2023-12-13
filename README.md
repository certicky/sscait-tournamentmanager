## SSCAIT Tournament Manager

### Disclaimer

This repository is originally based on several years old version of *StarCraftAITournamentManager* by Dave Churchill and Rick Kelly from https://github.com/davechurchill/StarcraftAITournamentManager. It still contains a considerable amount of its original code.

This particular implementation is currently (as of 2022) used to run Student StarCraft AI Tournament (SSCAIT): http://sscaitournament.com/

### Related repositories / addons
* Tournament Module DLL that's injected to StarCraft process and controls the camera, timeouts and results: https://github.com/certicky/sscait-tournamentmodule
* Web frontend for the tournament that displays the game schedule and bot scores and allows bot authors to upload their bots: https://github.com/certicky/sscait-web (live at https://sscaitournament.com)

### Prerequisites

* **One host machine**, where the server and the database are running. SSCAIT host runs on Ubuntu Linux server, but it should probably work almost anywhere.
* **Two client Windows 7 machines**. SSCAIT runs two Windows 7 virtual machines directly on the host. Each sould be able to:
  * Run Java client app.
  * Run StarCraft.
  * Connect to the server app running on the Host machine via local network or the internet.
  * Connect a folder from the host machine as a windows network drive.
  * Connect to the other Client machine, so they can play StarCraft LAN games.

### Installation

#### Host machine setup

* Clone this repo to preferred location on your Host machine.
* Install Java. On the host, it's not important whether Java is 64-bit or 32-bit (only the client machines need to use 32-bit Java).
* Install MySQL database server (note: versions since about version 8 aren't compatible with the driver, but 5.7 works) and create a database called `sc`.
  * Create a user with read & write permissions to that database.
  * Import/run the `database.sql` file to create the required DB tables, such as `games`, `fos_user`, `achievements`, etc.

* Create a `Bots` folder somewhere on your Host machine.
* Get a few bots that you want to run and:
  * Edit the rows in `fos_user` DB table to correspond to those bots. Don't forget to set the correct `bot_race`, `bot_path` and `bot_type`.
    * `bot_race` supports values `Terran`, `Zerg`, `Protoss` and `Random`
    * `bot_type` supports values `EXE`, `AI_MODULE`, `JAVA_MIRROR` and `JAVA_JNI`
    * `bot_path` must be an absolute path to specific bot's binary within your `Bots` folder.
      * **RULE:** Use the `/` character as the folder separator (not e.g. `\` or `\\`).
      * **RULE:** The filename only supports space characters for `AI_MODULE` bots. For `JAVA_MIRROR`/`JAVA_JNI`/`EXE` bots, the filename can not contain space character(s), so e.g. rename `FooBar Bot.exe` to `FooBar_Bot.exe` and update `bot_path` accordingly.
      * **RULE:** Each bot must have its own subfolder under `/Bots/` named by their `id` from `fos_user` table. For example, the bot with ID `3` can have the `bot_path` set to something like `/home/exampleUser/Bots/3/AI/StarterBot.exe` (notice that its subfolder is called `3` like their DB `id`).
  * Set up the subfolders of individual bots. Subfolder of each bot needs to:
    * Be named after its DB id.
    * Be placed directly inside the Bots folder.
    * Contain subfolder `AI` that contains its binary and correct version of BWAPI.dll. Example: `/home/exampleUser/Bots/3/AI/StarterBot.exe` and `/home/exampleUser/Bots/3/AI/BWAPI.dll`
    * Contain subfolders `read` and `write`. These are used if the bot needs to read and write some additional persistent files (e.g. when the bot learns something about specific opponents).
    * Some bots (e.g. Bereaver) might require some files to be placed into the `read` folder (`strategies.json` in Bereaver's case), as opposed to into the `AI` folder.

* Go to `host_folder_server` folder. This is the folder from which your server will run.
  * Make a copy of `server_settings.ini.template` file called `server_settings.ini` in the `host_folder_server` folder.
  * Update the settings in your newly created `server_settings.ini` file like this:
    * `ServerRequirementsDir` should be an absolute path to `host_folder_server/server_requirements` on your Host machine.
    * `BotsDir` is an absolute path to your Bots folder.
    * `ReplaysDir` is an absolute path to any folder on your Host machine, where you want the replays to be stored. Make sure that the folder exists and that the user running the server has write permissions there.
    * `LogFilePath` is an absolute path to a text file where the server stores some logs. Make sure the user running the server has write permissions there.
    * `Database*` are 5 variables holding the credentials of your MySQL DB and a DB user with access to the `sc` database.
    * `GmailFromEmail` and `GmailEmailPassword` are a credentials of a real Gmail user, which is used to send emails to bot authors (disabled by default) or to the admin (you).
    * `TM*` are 10+ variables that are propagated to Client machines and control various aspects of StarCraft games that run on the client, such as game speed, time limits, screen resolution and camera movement. Feel free to leave defaults there.

#### Running Host machine

* Go to `host_folder_server`.
* Run `java -jar sscai-server.jar server_settings.ini`
* Server's GUI window should appear and it should start waiting for client connections. The first time you run it, it might exit after it writes a log message saying that it needs to be rerun after it added some games to the games list, so if this happens, rerun it. Security software like Windows Security may also pop-up asking about blocking it, so unblock it if this happens.

#### Client machine setup

* Make sure you have two Windows 7 machines prepared. SSCAIT uses two Windows 7 VMs running on VMWare Workstation, but you can use different virtualization software, or even physical machines.
* Make sure both Client machines have network access to the Host and to each other, so they can communicate with the Java server and play StarCraft via LAN.
* Mount the `host_folder_client` folder from the Host machine as a Windows network drive, so that its contents are accessible under path `Z:\Client\`. You might need to create some symbolic links on the Host machine to make this work. You should now have access to folders like `Z:\Client\additionalRequirements\` and `Z:\Client\chaoslauncher\`.
* Create a folder `C:\TM\` on your Client machines.
* Copy the contents of `client_copy_to_VM` to `C:\TM\`. You should now have a file `C:\TM\client_settings.ini.template` on your Client machines.
* Make a copy of `C:\TM\client_settings.ini.template` and save it as `C:\TM\client_settings.ini` on both machines. Edit the new settings file as follows:
  * `ServerAddress` should point to the IP of the Host machine, where the server is running (and a port on which it listens for connection - this is 4499 if you haven't changed it in server settings).
* Install or put a copy of the StarCraft 1.16.1 program folder at `C:\TM\Starcraft\`. There should now exist a file `C:\TM\Starcraft\StarCraft.exe` on your Client machines.
* Install 32-bit Java on your Client machines. If multiple versions are installed, ensure that the "java" command uses a 32-bit version (not a 64-bit version).
* Install the following versions of Microsoft Visual C++ Redistributable. All need to be the 32-bit version (not 64-bit), although you can have the 64-bit version of each installed too without problems. At the time of writing, the latest supported downloads are available from Microsoft from https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist
* Install the 32-bit Microsoft Visual C++ Redistributable for Visual Studio 2015-2019+, e.g. 2015-2022.
* Install the 32-bit Microsoft Visual C++ Redistributable for Visual Studio 2008 (VC++ 9.0) SP1.
* Some security software like Windows Security may identify some BWAPI-related tools/bots as malware, so consider adding security software exclusions for drives/folders `C:\TM`, `Z:\`.
* If the machine is not a VMWare VM, create a dummy EXE file (and any necessary parent folders) at `C:\Program Files\VMWare\VMWare Tools\VMWareResolutionSet.exe`, e.g. by copying `C:\Windows\System32\rundll32.exe` (which simply exits).

#### Running a Client machine

* Run `setupAndRun.bat` on both Client machines. This should get the Java Client app JAR and additional requirements from the Host machine and run the Java Client app. A `java.net.UnknownHostException: sscaitournament.com` exception is shown for every message shown in the CMD window but you can ignore them.
  * The Java Client app should start up in a new commandline window, where you'll see debug messages. It will connect to the Host server.
  * Once both Clients are connected to the server, they will receive the bot files, the map, and the instruction to run the game.
  * The Java Clients will run the bot files they received from the server (or inject them into StarCraft process in case of DLL bots).
    * The first time each machine uses each individual version of BWAPI, a Chaoslauncher window appears because it needs to know which plugins and settings to use, so check the corresponding BWAPI injector plugin (Release mode if there is a choice), and check any other desired plugins (the RepFix & WMODE plugins are recommended), and in the Settings tab, check "Start Chaoslauncher minimized" and check "Minimize when running Starcraft" and check "Run Starcraft on Startup" and uncheck "Warn about missing adminprivilegues", then click the "Start" button.
  * One of the Clients will create a game and the other will join.
  * The game will start.
  * Once the game ends, both Java Clients will report the game result to the server.
  * The clients will clean up the bot files after the game.
  * The server will save the game result in `games` DB table on the Host machine.
  * The server will get the new game from the schedule in `games` DB table and send new bot files & instructions to the Clients.
  * And the whole process repeats.
* If desired, set up the machine so that it runs `setupAndRun.bat` when Windows starts.
  * **WARNING:** the machine should be restarted prior to ever rerunning the Java Client app, even if everything appeared to shut down and clean up gracefully, because it's possible that bot process(es) may still be running from previous games that could cause problems for new games. If this happens, a common problem is that one or both bots may not start: the game may appear to run smoothly but the bot(s) don't issue any commands during the game. When the Java Client app starts, although it does try to kill any StarCraft processes that are still running, it does not try to kill other bot process(es) that may be still running from previous games - especially Java bots and EXE bots, but any bot that runs separate process(es) than StarCraft.exe, e.g. CherryPiSSCAIT2017 which is a DLL bot (`AI_MODULE`) that also spawns an EXE process. Some DLL bots run a separate EXE to find good building placements to wall off their ramp/choke.

* (optional) You can use OBS (in `C:\TM\OBS\`) to stream what's happening on Client machines.
* (optional) You can use some of the AHK scripts (if you install AutoHotKey) on the Client machines to auto-close windows error messages, to control OBS, etc.
* (optional) You can use Chromium (`C:\TM\Chromium\run_chromium.bat`) to display the game schedule on the Client machine (makes sense when also streaming).

### Development

* At the time of writing, if you are developing for the current environment at sscaitournament.com, Java 8 can be used/targeted when building JARs (i.e. without needing to upgrade Java on those machines when deploying).
* You're mainly interested in the Java source files in folders: `server`, `client`, `objects`, `utility` (there are some additional dependencies in `lib`)
* **Host** app needs to be built as a **JAR** file `host_folder_server/sscai-server.jar` to be properly used. It needs to contain all the dependencies, so it's runnable as a standalone JAR from anywhere. You can prepare and test the runnable JAR using these commands:
  * Run to compile all the .java files: `javac -cp "lib/*" server/*.java client/*.java objects/*.java utility/*.java`
  * Run to create a runnable JAR file: `jar -cvfm host_folder_server/sscai-server.jar config/Manifest_Server.txt client/*.class objects/*.class utility/*.class server/*.class org/eclipse/jdt/internal/jarinjarloader/*.class ./mysql-connector-java.jar ./javax.mail.jar`
  * Run on the host to test: `java -jar host_folder_server/sscai-server.jar host_folder_server/server_settings.ini`
* **Client** app needs to be built as a **JAR** file `host_folder_client/sscai-client.jar` to be properly used. It needs to contain all the dependencies, so it's runnable as a standalone JAR from anywhere. You can prepare and test the runnable JAR using these commands:
  * Run to compile all the .java files: `javac -cp "lib/*" server/*.java client/*.java objects/*.java utility/*.java`
  * Run to create a runnable JAR file: `jar -cvfm host_folder_client/sscai-client.jar config/Manifest_Client.txt client/*.class objects/*.class utility/*.class server/*.class org/eclipse/jdt/internal/jarinjarloader/*.class ./mysql-connector-java.jar ./javax.mail.jar`
  * Run on the Windows machine to test: `java -jar sscai-client.jar client_settings.ini`

