#include "windows.h"
#include "tlhelp32.h"
#include "shlwapi.h" // Also requires shlwapi.lib.

#define PROCESS_NAME "Starcraft.exe"

char buf[MAX_PATH]={0};
char loaderDirectory[MAX_PATH]={0};
char gameDirectory[MAX_PATH]={0};

//I could just use PROCESS_ALL_ACCESS but it's always best to use the absolute bare minimum of priveleges, so that your code works in as
//many circumstances as possible.
#define CREATE_THREAD_ACCESS (PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION | PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ)
 
BOOL WriteProcessBYTES(HANDLE hProcess,LPVOID lpBaseAddress,LPCVOID lpBuffer,SIZE_T nSize);

BOOL LoadDll(char *procName);
BOOL InjectDLL(DWORD ProcessID, char *dllName);
unsigned long GetTargetProcessIdFromProcname(char *procName);

bool IsWindowsNT()
{
	// check current version of Windows
	DWORD version = GetVersion();
	// parse return
	DWORD majorVersion = (DWORD)(LOBYTE(LOWORD(version)));
	DWORD minorVersion = (DWORD)(HIBYTE(LOWORD(version)));
	return (version < 0x80000000);
}

int APIENTRY WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow){

	if(IsWindowsNT()){
		 LoadDll(PROCESS_NAME);
	}
	else{
		MessageBoxA(0, "Error: Your system does not support this method.", "Loader", 0);
	}
	 return 0;
}

BOOL LoadDll(char *procName){

	BOOL threadSuspended = false;
	HKEY hRegKey = 0;
	DWORD dwSize = 0;
	STARTUPINFO StartupInfo = {0};
	PROCESS_INFORMATION ProcessInformation = {0};
	WIN32_FIND_DATA FileInformation;
	HANDLE hStarcraftFile = 0;

	GetCurrentDirectory(sizeof(loaderDirectory), loaderDirectory);
	lstrcat(loaderDirectory, "\\*");

	GetStartupInfo(&StartupInfo);
	DWORD ProcID = GetTargetProcessIdFromProcname(procName);

	if(ProcID==0){
		lstrcpy(gameDirectory,"C:\\Program Files\\Starcraft\\Starcraft.exe");
		hStarcraftFile=CreateFile(gameDirectory,GENERIC_READ,(FILE_SHARE_DELETE|FILE_SHARE_READ|FILE_SHARE_WRITE),NULL,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,NULL);
		if(INVALID_HANDLE_VALUE!=hStarcraftFile){
			CloseHandle(hStarcraftFile);
		}
		else if(ERROR_SUCCESS==RegOpenKey(HKEY_LOCAL_MACHINE,"Software\\Blizzard Entertainment\\Starcraft",&hRegKey)){
			dwSize=sizeof(gameDirectory);
			if(ERROR_SUCCESS==RegQueryValueEx(hRegKey,"GamePath",NULL,NULL,(LPBYTE)gameDirectory,&dwSize)){
				dwSize=sizeof(gameDirectory);
			}
			else if(ERROR_SUCCESS==RegQueryValueEx(hRegKey,"Program",NULL,NULL,(LPBYTE)gameDirectory,&dwSize)){
				dwSize=sizeof(gameDirectory);
			}
			else{
				MessageBox(NULL,"Error: Starcraft installed, but not correctly.", "Loader", NULL);
				return false;
			}
			RegCloseKey(hRegKey);
		}
		else{
			MessageBox(NULL,"Error: No Starcraft installation found.", "Loader", NULL);
			return false;
		}

		lstrcat(gameDirectory, "\\..");
		SetCurrentDirectory(gameDirectory);
		gameDirectory[lstrlen(gameDirectory)-3] = 0;

		CreateProcess(gameDirectory,NULL,NULL,NULL,false,CREATE_SUSPENDED,NULL,NULL,&StartupInfo,&ProcessInformation);

		ProcID=ProcessInformation.dwProcessId;
		threadSuspended = true;
	}

	HANDLE hLoaderFile = FindFirstFile(loaderDirectory, &FileInformation);

	while(hLoaderFile != INVALID_HANDLE_VALUE){
		if(0==lstrcmp(&FileInformation.cFileName[lstrlen(FileInformation.cFileName)-4], ".dll")){

			lstrcpy(buf,loaderDirectory);
			buf[lstrlen(loaderDirectory)-1] = 0;
			lstrcat(buf,FileInformation.cFileName);

			if(!InjectDLL(ProcID, buf)){
				MessageBox(NULL, "Error: Unable to inject dll.", "Loader", NULL);
				return false;
			}
		}
		if(!FindNextFile(hLoaderFile, &FileInformation)){
			break;
		}
	}
	FindClose(loaderDirectory);

	if(threadSuspended){
		ResumeThread(ProcessInformation.hThread);
	}
	else{
		MessageBox(NULL, "Success!", "Loader", NULL);
	}
	return true;
}

BOOL InjectDLL(DWORD ProcessID, char *dllName)
{
	HANDLE hProcess;
	HANDLE hProcResticted;
	HANDLE hToken;
	TOKEN_PRIVILEGES tp;
	TOKEN_PRIVILEGES tpPrev;
	DWORD tpLength;

	LPVOID RemoteString, LoadLibAddy;
	hProcResticted=OpenProcess(PROCESS_QUERY_INFORMATION,FALSE,GetCurrentProcessId());
	if(!hProcResticted){
		return false;
	}
	if(!OpenProcessToken(hProcResticted,TOKEN_ALL_ACCESS,&hToken)){
		return false;
	}
	CloseHandle(hProcResticted);
	LookupPrivilegeValue(NULL,"SeDebugPrivilege",&tp.Privileges[0].Luid);
	tp.PrivilegeCount = 1;
	tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
	
	if(!AdjustTokenPrivileges(hToken,false,&tp,sizeof(TOKEN_PRIVILEGES),&tpPrev,&tpLength)){
		CloseHandle(hToken);
		return false;
	}
	hProcess=OpenProcess(CREATE_THREAD_ACCESS,FALSE,ProcessID);
	if(!hProcess){
		CloseHandle(hToken);
		return false;
	}

	LoadLibAddy = (LPVOID)GetProcAddress(GetModuleHandleA("kernel32.dll"), "LoadLibraryA");
	if(!LoadLibAddy){
		CloseHandle(hToken);
		CloseHandle(hProcess);
		return false;
	}

	RemoteString = (LPVOID)VirtualAllocEx(hProcess, NULL, strlen(buf), MEM_RESERVE|MEM_COMMIT, PAGE_READWRITE);
	if(!RemoteString){
		CloseHandle(hToken);
		CloseHandle(hProcess);
		return false;
	}
		if(!WriteProcessMemory(hProcess, (LPVOID)RemoteString, dllName, strlen(dllName), NULL)){
		CloseHandle(hToken);
		CloseHandle(hProcess);
		return false;
	}
	
	if(!CreateRemoteThread(hProcess, NULL, NULL, (LPTHREAD_START_ROUTINE)LoadLibAddy, (LPVOID)RemoteString, NULL, NULL)){
		CloseHandle(hToken);
		CloseHandle(hProcess);
		return false;
	}
	if(!AdjustTokenPrivileges(hToken,FALSE,&tpPrev,NULL,NULL,NULL)){
		CloseHandle(hToken);
		CloseHandle(hProcess);
		return false;
	}
	CloseHandle(hToken);
	CloseHandle(hProcess);

	return true;
}

unsigned long GetTargetProcessIdFromProcname(char *procName)
{
	PROCESSENTRY32 pe;
	HANDLE thSnapshot;
	BOOL retval, ProcFound = false;

	thSnapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);

	if(thSnapshot == INVALID_HANDLE_VALUE){
		MessageBoxA(NULL, "Error: unable to create toolhelp snapshot", "Loader", NULL);
		return false;
	}

	pe.dwSize = sizeof(PROCESSENTRY32);

	retval = Process32First(thSnapshot, &pe);

	while(retval!=0)
	{
		if(StrStrI(pe.szExeFile, procName))
		{
			ProcFound = true;
			break;
		}
		retval	 = Process32Next(thSnapshot,&pe);
		pe.dwSize = sizeof(PROCESSENTRY32);
	}
	if(retval==0){
		return 0;
	}
	return pe.th32ProcessID;
}

// Credits go to..
//		Darawk for the module finding code and dll loading code.
//		Perma for accessing privilages code.
//		Rest was made by hellinsect.