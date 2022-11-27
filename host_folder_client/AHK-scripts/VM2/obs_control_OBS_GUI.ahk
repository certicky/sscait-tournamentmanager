Loop
{
	; NO UPDATES FOR OBS
	; Window with "Updates are available" message is identified as "ahk_class #32770"
	IfWinActive, ahk_class #32770
	{
		ControlClick, &No
		;MsgBox, Update window active 
	}

	; MINIMIZE OBS
	; Main OBS window has title starting with string "Profile" 
	IfWinActive, Profile
	{
		WinMinimize
	}
	Sleep, 5000
}
