############################################
#          Muzix Python server             #
#                                          #
#          This Script created for         #
#           Multi Client using Thread      #
#          This is echo server             #
#          created 2019.05.23              #
#           Version 0.03                   #
#           created by KKH                 #
############################################


import os
from socket import *
import _thread as thread
from sheetmake import audio_to_midi_melodia
from generate import generate
from modify import generateModifyChord

def handler(clientsock,addr):
    #This Handler do some tasks
    sendcnt =0
    a =0
    filename=""
    key_sig = ""
    val = ""
    send_val = ""
    tempo = 0
    url = "http://54.180.95.158:8080/server/filestorage"
    userDir = "/opt/tomcat/server/webapps/ROOT/server/filestorage"
    _type=""
    while True :
        recv_val = clientsock.recv(BUFSIZE)
        recv_val = os.fsdecode(recv_val)
        recv_val = recv_val.split('\n')
        print(recv_val)
        if(recv_val[0] == 'type'):
            val = recv_val[1]
            _type=val
            send_val = os.fsencode('getType\n')
            clientsock.sendall(send_val)
            print(send_val)
            
        if(_type=='create'):
            if(recv_val[0] == 'member_id'):
                val = recv_val[1]
                userDir = userDir+'/'+val
                url = url +'/'+val
                if not os.path.isdir(userDir): #This make user's own directory
                    os.mkdir(userDir)
                    #ex ) /opt/tomcat/server/webapps/ROOT/server/filestorage/ardente6320/
                send_val = os.fsencode('getMemberId\n')
                clientsock.sendall(send_val)
                print(send_val)
            
            elif(recv_val[0]=='key_sig'):
                val = recv_val[1]
                key_sig = val
                send_val=os.fsencode('getKeySig\n')
                clientsock.sendall(send_val)
                print(send_val)
            
            elif(recv_val[0]=='tempo'):
                val = recv_val[1]
                tempo = int(val)
                send_val=os.fsencode('getTempo\n')
                clientsock.sendall(send_val)
                print(send_val)
            
            elif(recv_val[0]=='filename'):
                val = recv_val[1]
                filename=val
                wavfile = filename + '.wav'
                send_val=os.fsencode('getFilename\n')
                clientsock.sendall(send_val)

                infile = userDir+'/'+filename+'.wav'
                outfile = userDir+'/'+filename+'.mid'

                audio_to_midi_melodia(infile, outfile, tempo, smooth = 0.25, minduration=0.1, savejams=False)
                generate(userDir,filename,key_sig) #path and name, key
                send_val=os.fsencode('generated')
                clientsock.sendall(send_val)
                print(send_val)
                clientsock.close()
                return

        elif(_type=="modify"):
            
            if(recv_val[0] == 'member_id'):
                val = recv_val[1]
                userDir = userDir+'/'+val
                writer= val
                url = url +'/'+val
                if not os.path.isdir(userDir): #This make user's own directory
                    os.mkdir(userDir)
                    #ex ) /opt/tomcat/server/webapps/ROOT/server/filestorage/ardente6320/
                send_val = os.fsencode('getMemberId\n')
                clientsock.sendall(send_val)
                print(send_val)

            elif (recv_val[0] == 'generate'):    
                generateModifyChord("melody",writer) #path and name, key
                send_val=os.fsencode('generated')
                clientsock.sendall(send_val)
                print(send_val)
                clientsock.close()
                return

if __name__=='__main__':
    HOST = '0.0.0.0'
    PORT = 8011
    BUFSIZE = 1024
    ADDR = (HOST,PORT)
    serversock = socket(AF_INET,SOCK_STREAM)
    serversock.bind(ADDR)
    serversock.listen(0)

    while True:
        try:
            print ("waiting for connection")
            clientsock, addr = serversock.accept()
            print ("... connected from : ", addr)
            thread.start_new_thread(handler,(clientsock,addr))
        except KeyboardInterrupt:
            print('server now closed..\n')
            serversock.close()
            print('successfully closed!!! \n')
