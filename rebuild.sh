#!/bin/sh

cd rips
wine "../tools/armips.exe" ../vwf.asm && cd dirty && wine "../../tools/psxbuild.exe" dimguil.cat dimguil
