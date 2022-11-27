; Close unwanted windows whenever they appear:
#Persistent

SetTimer, CloseUnwanted, 1000

return



CloseUnwanted:

; "StarCraft stopped working" errors 

WinClose, StarCraft

; some Chaoslauncher errors (permissions?)

WinClose, Chaoslauncher for Starcraft:Broodwar
; Xsplit "Select Presentation" input box on start (sometimes appears)
IfWinActive,, Last time
{
    MouseClick, left, 240, 240
}
return
