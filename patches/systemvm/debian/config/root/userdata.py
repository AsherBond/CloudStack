#!/usr/bin/python

import sys
import base64
import string 
import os
import tempfile
from subprocess import call

def vm_data(args):

    router_ip = args.pop('routerIP')
    vm_ip = args.pop('vmIP')

    for pair in args:
        pairList = pair.split(',')
        vmDataFolder = pairList[0]
        vmDataFile = pairList[1]
        vmDataValue = args[pair]
        cmd = ["/bin/bash", "/root/userdata.sh", "-v", vm_ip, "-F", vmDataFolder, "-f", vmDataFile]
        
        fd = None
        tmp_path = None
        if (vmDataValue != "none"):
            try:
                fd,tmp_path = tempfile.mkstemp()
                tmpfile = open(tmp_path, 'w')

                if (vmDataFolder == "userdata"):
                    vmDataValue = base64.urlsafe_b64decode(vmDataValue)
                    
                tmpfile.write(vmDataValue)
                tmpfile.close()
                cmd.append("-d")
                cmd.append(tmp_path)
            except:
		if (fd !=None):
                	os.close(fd)
                	os.remove(tmp_path)
                return ''

        try:
            call(cmd)
            txt = 'success'
        except:
            txt = ''

        if (fd != None):
            os.close(fd)
            os.remove(tmp_path)

    return txt

def parseFileData(fileName):
    args = {} 
    fd = open(fileName)

    line = fd.readline()
    while (line != ""):
        key=string.strip(line[:], '\n')
        if (key == ""):
            break
	  
        line=fd.readline()
        val=string.strip(line[:], '\n')
        args[key]=val
        line=fd.readline()
    return args

if __name__ == "__main__":
	vm_data(parseFileData("/tmp/" + sys.argv[1]))

