package org.himalay.ssh

import java.util.List

import org.codehaus.groovy.runtime.StringBufferWriter
import org.himalay.commandline.Arg
import org.himalay.commandline.RealMain

@RealMain
abstract class SshEnabledApp extends org.himalay.commandline.CLTBaseQuiet
{
 
	/**
	 * 
	 * @param sshHelper
	 * @param closure A closure that takes Expect as argument.
	 * @return If an exception happened then exception else null
	 */
	public def expect(SshHelper sshHelper, Closure closure)
	{
		return sshHelper.expectInAnShellChannel (closure);
	}
	
	/**
	 * 
	 * @param cmd
	 * @param sshHelper
	 * @return a map with two values: int exitCode and String retBuffer
	 */
	public def executeRemotely(SshHelper sshHelper , String cmd )
	{
		StringBuffer strBuff = new StringBuffer()
		StringBufferWriter sbw = new StringBufferWriter(strBuff)
		return executeRemotely(sshHelper,cmd,sbw)
	}

	
	/**
	 *
	 * @param cmd
	 * @param sshHelper
	 * @return a map with two values: int exitCode and String retBuffer
	 */
	public def executeRemotely(SshHelper sshHelper , String cmd, Writer writer )
	{
		int exitCode = sshHelper.executeInASession(cmd, writer);
		return [exitCode: exitCode, retBuffer: writer.toString()]
	}

	
	/**
	 * Creates SSH Helper Object
	 * @param userName
	 * @param server
	 * @param passwd
	 * @return
	 */
	public SshHelper createSshHelper(String userName, String server, String passwd, int port=22)
	{
		SshHelper sshHelper = new SshHelper(userName,server, passwd,port);
		return sshHelper
	}
	
	
	/**
	 * Creates SSH Helper Object
	 * @param userName
	 * @param server
	 * @param passwd
	 * @return
	 */
	public SshHelper createSshHelper(String userName, String server, int port=22)
	{
		SshHelper sshHelper = new SshHelper(userName,server, null,port);
		return sshHelper
	}
	
	/**
	 * Creates SSH Helper Object
	 * @param userName
	 * @param server
	 * @param passphrase
	 * @param idFile
	 * @return The SshHelepr Object
	 */
	public SshHelper createSshHelper(String userName, String server, String passphrase, File idFile, int port=22)
	{
		SshHelper sshHelper = new SshHelper(userName,server, passphrase, idFile.absolutePath, port);
		return sshHelper
	}

	/**
	 * Runs a command on a bunch of server.
	 * @param serverInfo A list of structures having following fields: serverName, port, userName, password, passphrase, idfile. passphrase is null then password based login is used as preferred. If passphrase is specied as 'agent' then a ssh agent (like pegeant, ssh-agent) is tried.
	 * @param cmd The command to execute
	 * @param threads Number of threads to be used
	 * @return A structure with serverInfo and a map with exitCode and return buffer ex
	 */
	public def runCommand(List serverInfo, String cmd, int threads) {
		def retData = []
		serverInfo.collate(threads).each{List smallList->
			List<Thread> threadList = smallList.collect{aServerInfo->
				def stats = [:]
				Thread aThread = Thread.start{
					stats.started = new Date()
					StringBuffer strBuff = new StringBuffer()
					stats.sbw = new StringBufferWriter(strBuff)
					SshHelper sshHelper = SshHelper.createSshHelper(aServerInfo)
					def retVal = executeRemotely(sshHelper,cmd, stats.sbw)
					synchronized (this) {
						retData << [serverInfo: aServerInfo, retVal: retVal]
					}
					stats.ended = new Date()
				}
				
				return [thread: aThread, stats : stats]
			}
			
			threadList.each{
				it.thread.join()
			}
		}
		
		return retData
	}

}