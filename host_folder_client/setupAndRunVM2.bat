REM copy Z:\Client\chaoslauncher\* C:\TM\Starcraft /Y

"C:\Program Files\VMware\VMware Tools\VMWareResolutionSet.exe" 0 1 , 0 0 1280 720
copy Z:\Client\additionalRequirements\* C:\TM\Requirements /Y
C:
cd C:\TM\
java -jar Z:\Client\sscai-client.jar Z:\Client\client_settings.ini

pause
