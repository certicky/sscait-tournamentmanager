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
* Install Java.
* Install MySQL database server and create a database called `sc`.
  * Create a user with read & write permissions to that database.
  * Import/run the `database.sql` file to create the required DB tables, such as `games`, `fos_user`, `achievements`, etc.

* Create a `Bots` folder somewhere on your Host machine.
* Get a few bots that you want to run and:
  * Edit the rows in `fos_user` DB table to correspond to those bots. Don't forget to set the correct `bot_race`, `bot_path` and `bot_type`.
    * `bot_race` supports values `Terran`, `Zerg`, `Protoss` and `Random`
    * `bot_type` supports values `EXE`, `AI_MODULE`, `JAVA_MIRROR` and `JAVA_JNI`
    * `bot_path` must be an absolute path to specific bot's binary within your `Bots` folder. **RULE:** Wach bot must have its own subfolder under `/Bots/` named by their `id` from `fos_user` table. For example, the bot with ID `3` can have the `bot_path` set to something like `/home/exampleUser/Bots/3/AI/StarterBot.exe` (notice that its subfolder is called `3` like their DB `id`).
  * Set up the subfolders of individual bots. Subfolder of each bot needs to:
    * Be named after its DB id.
    * Be placed directly inside the Bots folder.
    * Contain subfolder `AI` that contains its binary. Example: `/home/exampleUser/Bots/3/AI/StarterBot.exe`
    * Contain subfolders `read` and `write`. There are used if the bot needs to read and write some additional persistent files (e.g. when the bot learns something about specific opponents).

* Go to `host_folder_server` folder. This is the folder from which your server will run.
  * Make a copy of `server_settings.ini.template` file called `server_settings.ini` in the `host_folder_server` folder.
  * Update the settings in your newly created `server_settings.ini` file like this:
    * `ServerRequirementsDir` should be an absolute path to `host_folder_server/server_requirements` on your Host machine.
    * `BotsDir` is an absolute path to your Bots folder.
    * `ReplaysDir` is an absolute path to any folder on your Host machine, where you want the replays to be stored. Make sure the user running the server has write permissions there.
    * `LogFilePath` is an absolute path to a text file where the server stores some logs. Make sure the user running the server has write permissions there.
    * `Database*` are 5 variables holding the credentials of your MySQL DB and a DB user with access to the `sc` database.
    * `GmailFromEmail` and `GmailEmailPassword` are a credentials of a real Gmail user, which is used to send emails to bot authors (disabled by default) or to the admin (you).
    * `TM*` are 10+ variables that are propagated to Client machines and control various aspects of StarCraft games that run on the client, such as game speed, time limits and camera movement. Feel free to leave defaults there.

#### Running Host machine

* Go to `host_folder_server`.
* Run `java -jar sscai-server.jar server_settings.ini`
* Server's GUI window should appear and it should start waiting for client connections.

#### Client machine setup

* Make sure you have two Windows 7 machines prepared. SSCAIT uses two Windows 7 VMs running on VMWare Workstation, but you can use different virtualization software, or even physical machines.
* Make sure both Client machines have network access to the Host and to each other, so they can communicate with the Java server and play StarCraft via LAN.
* Mount the `host_folder_client` folder from the Host machine as a Windows network drive, so that its contents are accessible under path `Z:\Client\`. You might need to create some symbolic links on the Host machine to make this work. You should now have access to folders like `Z:\Client\additionalRequirements\` or `Z:\Client\chaoslauncher\`.
* Create a folder `C:\TM\` on your Client machines.
* Copy the contents of `client_copy_to_VM` to `C:\TM\`. You should now have a file `C:\TM\client_settings.ini.template` on your Client machines.
* Make a copy of `C:\TM\client_settings.ini.template` and save it as `C:\TM\client_settings.ini` on both machines. Edit the new settings file as follows:
  * `ServerAddress` should point to the IP of the Host machine, where the server is running (and a port on which it listens for connection - this is 4499 if you haven't changed it in server settings).
* Put an installation of StarCraft 1.16.1 into `C:\TM\Starcraft\`. There should now exist a file `C:\TM\Starcraft\StarCraft.exe` on your Client machines.
* Install Java on your Client machines.

#### Running a Client machine

* Go to `C:\TM\` and run `setupAndRun.bat` on both Client machines. This should get the Java Client app JAR and additional requirements from the Host machine and run the Java Client app.
  * The Java Client app should start up in a new commandline window, where you'll see debug messages. It will connect to the Host server.
  * Once both Clients are connected to the server, they will receive the bot files, the map, and the instruction to run the game.
  * The Java Clients will run the bot files they received from the server (or inject them into StarCraft process in case of DLL bots).
  * One of the Clients will create a game and the other will join.
  * The game will start.
  * Once the game ends, both Java Clients will report the game result to the server.
  * The clients will clean up the bot files after the game.
  * The server will save the game result in `games` DB table on the Host machine.
  * The server will get the new game from the schedule in `games` DB table and send new bot files & instructions to the Clients.
  * And the whole process repeats.

* (optional) You can use OBS (in `C:\TM\OBS\`) to stream what's happening on Client machines.
* (optional) You can use some of the AHK scripts (if you install AutoHotKey) on the Client machines to auto-close windows error messages, to control OBS, etc.
* (optional) You can use Chromium (`C:\TM\Chromium\run_chromium.bat`) to display the game schedule on the Client machine (makes sense when also streaming).

### Development

* You're mainly interested in the Java source files in folders: `server`, `client`, `objects`, `utility` (there are some additional dependencies in `lib`)
* **Host** app needs to be built as a **JAR** file `host_folder_server/sscai-server.jar` to be properly used. It needs to contain all the dependencies, so it's runnable as a standalone JAR from anywhere. You can prepare and test the runnable JAR using these commands:
  * Run to compile all the .java files: `javac -cp "lib/*" server/*.java client/*.java objects/*.java utility/*.java`
  * Run to create a runnable JAR file: `jar -cvfm host_folder_server/sscai-server.jar config/Manifest_Server.txt client/*.class objects/*.class utility/*.class server/*.class org/eclipse/jdt/internal/jarinjarloader/*.class ./mysql-connector-java.jar ./javax.mail.jar`
  * Run on the host to test: `java -jar host_folder_server/sscai-server.jar host_folder_server/server_settings.ini`
* **Client** app needs to be built as a **JAR** file `host_folder_client/sscai-client.jar` to be properly used. It needs to contain all the dependencies, so it's runnable as a standalone JAR from anywhere. You can prepare and test the runnable JAR using these commands:
  * Run to compile all the .java files: `javac -cp "lib/*" server/*.java client/*.java objects/*.java utility/*.java`
  * Run to create a runnable JAR file: `jar -cvfm host_folder_client/sscai-client.jar config/Manifest_Client.txt client/*.class objects/*.class utility/*.class server/*.class org/eclipse/jdt/internal/jarinjarloader/*.class ./mysql-connector-java.jar ./javax.mail.jar`
  * Run on the Windows machine to test: `java -jar sscai-client.jar client_settings.ini`

