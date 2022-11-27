#SingleInstance force
#Persistent

SetTimer, MaximizeSC, 3000 ; every 3 seconds

MaximizeSC:

    SetTitleMatchMode 2

    IfWinExist, Brood ; look for "Brood" string in the window title (it should be 'Brood War')
    {
           ;WinMaximize
           WinRestore
    }

return
