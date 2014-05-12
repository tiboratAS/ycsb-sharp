#!/usr/bin/env python
import paramiko
import threading
import os
import time
import sys
import getopt

# username on multi client
username='ec2-user'

# dir that summary will be put
dirname=os.getcwd()+'/'+time.strftime("%Y%m%d%H%M%S")

# user need to edit this cmd, which is used to run on a single client 
cmd = '/home/ec2-user/YCSB/bin/ycsb'
option = ' run hbase -zkserver 54.86.134.220'
phase = ' -phase -p columnfamily=family -threads 5 -s -P'
workload = ' /home/ec2-user/YCSB/workloads/workloada'

# private key file
privatekey = '/home/ec2-user/bigdata.pem'

#port=22
def ssh(hostname, threadID, count):
	paramiko.util.log_to_file('paramiko.log')
        k = paramiko.RSAKey.from_private_key_file(privatekey)
        c = paramiko.SSHClient()
        c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        print "connecting"
	c.connect(hostname = hostname,username=username, pkey=k)
        print "connected"
	#channel = c.get_transport().open_session()
	#s.load_system_host_keys()
	
	localcmd = 'cat '+workload
	stdin,stdout,stderr = c.exec_command(localcmd)
	channel = stdout.channel
	while not channel.exit_status_ready():
		print 'waiting for parsing workload...'
		time.sleep(3)

	filecontent = stdout.readlines();
	summaryfile = ''
	for line in filecontent:
		if line.startswith('exportfile='):
			summaryfile = line.split('=')[1]

	localcmd = cmd + option + phase + workload
	#localcmd = cmd + option + ' -clients ' + str(count) + phase
	print localcmd
	stdin,stdout,stderr = c.exec_command(localcmd)
	channel = stdout.channel
	while not channel.exit_status_ready():
		print "waiting for command complete..."
		time.sleep(3)

	print "Thread: ", threadID, " done!"
	filename = "hbase_summary_file_client_"+str(threadID)
	filename = dirname+'/'+filename
	print 'summary data written into:', filename
	
	logfile = open(filename,"w");
	if summaryfile == '':
		logfile.writelines(stdout.readlines())
		logfile.flush()
	else:
		localcmd = 'cat '+os.getenv("HOME")+'/'+summaryfile
		print localcmd
		stdin,stdout,stderr = c.exec_command(localcmd)
        	channel = stdout.channel
        	while not channel.exit_status_ready():
                	time.sleep(3)
		
		logfile.writelines(stdout.readlines())
                logfile.flush()

	logfile.close()
	c.close()

if __name__=='__main__':
	print "start..."
	if not os.path.exists(dirname):
		print 'create new dir...'
		os.makedirs(dirname)
	
	threads = []
	conf = 'multiclient.conf'

	opts, args = getopt.getopt(sys.argv[1:], "c:", ["config"])
	for opt, arg in opts:
        	print opt, '+', arg
		if opt in ("-c", "--config"):
			conf = arg  	

	fp = open(conf,"r")
	i = 0
	count = 0
	client_list = fp.readlines()
	for hostname in client_list:
		count = count + 1

	for hostname in client_list:
		a = threading.Thread(target=ssh, args=(hostname,i,count))
		a.start()
		threads.append(a)
		i = i+1
