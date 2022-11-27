Loop
{
	IfWinActive, Brood War
	{
		;MsgBox, BW exists
		;IfWinNotActive, Brood War 
		;{
		;WinActivate
		;}
		Sleep, 3000
		Send, .
		Send, {Space}
		;Sleep, 5000
	}
	Sleep, 1000
}
