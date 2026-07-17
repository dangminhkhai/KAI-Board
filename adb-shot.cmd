@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\adb-shot.ps1" %*
