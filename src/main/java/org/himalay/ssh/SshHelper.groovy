package org.himalay.ssh;

import java.io.File
import java.nio.channels.AlreadyConnectedException

import org.codehaus.groovy.runtime.StringBufferWriter
import org.slf4j.LoggerFactory

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.IdentityRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Logger
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import com.jcraft.jsch.agentproxy.AgentProxy
import com.jcraft.jsch.agentproxy.Connector
import com.jcraft.jsch.agentproxy.ConnectorFactory
import com.jcraft.jsch.agentproxy.Identity
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository
import com.jcraft.jsch.agentproxy.connector.PageantConnector

/**
 * A class to quickly use ssh functionality 
 * @author krishna
 *
 */
public class SshHelper implements UserInfo, Logger{
	static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SshHelper.class);
	
	public String userName ;
	public String host;
	public String password
	public String passphrase
	public String idFile
	public int    port = 22;
	Session sess;
	public String getPassphrase() {
		return ph;
	}

	
	public SshHelper() {
	}
	
	/**
	 * 
	 * @param userName
	 * @param host
	 * @param passphrase
	 * @param idFile
	 */
	public SshHelper(String userName, String host, String passphrase, String idFile, int port = 22) {
		super();
		this.userName = userName.split(/@/)[0];
		if ( host == null){
			this.host = userName.split(/@/)[1]
		}else{
			this.host = host;
		}
		this.passphrase = passphrase;
		this.idFile     = idFile;
		this.port = port
		init()
	}

	/**
	 * 
	 * @param userName
	 * @param host
	 * @param password
	 */
	public SshHelper(String userName, String host, String password, int port = 22) {
		super();
		this.userName = userName.split(/@/)[0];
		if ( host == null){
			this.host = userName.split(/@/)[1]
		}else{
			this.host = host;
		}
		this.password = password;
		this.port = port
		init()
	}
	
	private void init()
	{
		
	}

	public String getPh()
	{
		log(1,"Passphrase asked.")
		String pp = this.passphrase
		return pp
	}
	public String getPassword() {
		log(1,"Password asked.")
		return this.password
	}

	public boolean promptPassphrase(String arg0) {
		
		return false
	}

	public boolean promptPassword(String arg0) {
		return this.password != null;
	}

	public boolean promptYesNo(String arg0) {
		log ">>>${arg0}, Yes/no?"
		
		return true;
	}

	public void showMessage(String arg0) {
		LOGGER.info( arg0);
	}

	public JSch getJsh()
	{
		JSch jsch=new JSch();
		jsch.setLogger(this)
		jsch.setConfig("StrictHostKeyChecking", "no");
		
		if ( this.password == null)
		{
			jsch.setConfig("PreferredAuthentications", "publickey");
			if ( this.idFile == null)
			{
				//String comment =this.idFile.substring("pageant/".length()) 

				ConnectorFactory cf = ConnectorFactory.getDefault();
				IdentityRepository irepo = new RemoteIdentityRepository(cf.createConnector());
				jsch.setIdentityRepository(irepo);

			}else{
				jsch.addIdentity(this.idFile, getPassphrase())
			}
		}else{
			jsch.setConfig("PreferredAuthentications", "password");
		}
		return jsch;
	}
	
	public Session getASession(int port = 22)
	{
		Session ret = getJsh().getSession(this.userName, this.host, port);
		ret.setUserInfo(this);
		
		return ret
	}
	
	
	public def inAnExecChannel(Closure clsureWithChannel)
	{
		def retVal = null
		this.sess=getASession();
		this.sess.connect();
		Channel channel = this.sess.openChannel("exec");
		try{
			retVal= clsureWithChannel(channel,this.sess);
		}catch(Exception ex)
		{
			retVal= ex
			LOGGER.warn("Exception while in exec channel", ex)
			//ex.printStackTrace()
		}finally{
			if (!channel.isClosed())channel.close();
			if (this.sess.isConnected())this.sess.disconnect();
		}
		
		return retVal
	}
	
	/**
	 * Perform expect operations 
	 * @param clsureWithExpect A closure that takes Expect as argument
	 * @return
	 */
	public  def expectInAnShellChannel(Closure clsureWithExpect)
	{
		def retVal = null
		Session session=getASession(this.port);
		this.sess = session
		session.connect();
		Channel channel = session.openChannel("shell");
		try{
			InputStream is = channel.getInputStream();
			OutputStream os = channel.getOutputStream();
			Expect expect = new Expect(is,os);
			channel.connect();
			retVal = clsureWithExpect( expect);
			//expect.close();
		}catch(Exception ex)
		{
			try{
			if (!channel.isClosed())channel.close();
			if (session.isConnected())session.disconnect();
			}catch(Exception ex2)
			{
				getLOGGER().warn("Exception while performing excpet ", ex2);
			}
			return ex
		}finally{
			if (!channel.isClosed())channel.close();
			if (this.sess.isConnected())this.sess.disconnect();
		}
		
		return retVal
	}
	
	public def executeInASession(String command, File outFile)
	{
		FileWriter fw = new FileWriter(outFile)

		def retVal = executeInASession(command,fw)
		fw.closeQuietly()
		return retVal
	}

	public def executeInASession(String command, PrintStream ps)
	{
		PrintWriter pw = new PrintWriter(ps)
		return executeInASession(command, pw)
	}
	public def executeInASession(String command, Writer fw)
	{
		return inAnExecChannel{ChannelExec channel, Session session->
			((ChannelExec)channel).setCommand(command);
			((ChannelExec)channel).setErrStream(System.err);
			
			InputStream inStr=channel.getInputStream();
			channel.connect();
			byte[] tmp=new byte[1024];
			while(true){
			  while(inStr.available()>0){
				int i=inStr.read(tmp, 0, 1024);
				if(i<0)break;
				fw.print(new String(tmp, 0, i));
			  }
			  if(channel.isClosed()){
				if(inStr.available()>0) continue;
				//fw.println("exit-status: "+channel.getExitStatus());
				break;
			  }
			  try{Thread.sleep(1000);}catch(Exception ee){}
			}
			//fw.closeQuietly()
			return channel.exitStatus
		}
	}

	public boolean isEnabled(int paramInt) {
		// TODO Auto-generated method stub
		return true;
	}

	public void log(int paramInt, String paramString) {
		LOGGER.debug (paramString)
		
	}
	
	/**
	 * Sets a forwarded port
	 * @param localport
	 * @param remoteHost
	 * @param remotePort
	 * @return
	 */
	public int setPortForwarding_L(int localport, String remoteHost, int remotePort)
	{
		return this.sess.setPortForwardingL("127.0.0.1",localport, remoteHost, remotePort)
	}

	/**
	 * removes previously forwarded port
	 */
	public void delPortForwarding_L(int localport)
	{
		if ( this.sess.isConnected){
			this.sess.delPortForwardingL("127.0.0.1",localport)
		}
	}

	public static SshHelper createSshHelper(def serverInfo)
	{
		if ( serverInfo.password == null)
		{
			return new SshHelper (
				serverInfo.username, 
				serverInfo.serverName,
				serverInfo.passphrase,
				serverInfo.idFile,
				serverInfo.port);		
		}else{
			return new SshHelper (
				serverInfo.username,
				serverInfo.serverName,
				serverInfo.password,
				serverInfo.port);
		}
	}	
	
	public def copyFrom(String remoteFile, String localFile)
	{
		Sftp sftp = new Sftp(this.userName, this.password, this.idFile);
		sftp.transferFile(localFile, remoteFile, "download", this.passphrase);
	}
	
	public def copyTo(String localFile,String remoteFile)
	{
		Sftp sftp = new Sftp(this.userName, this.password, this.idFile);
		sftp.twoway = true;
		sftp.transferFile(localFile, remoteFile, "upload", this.passphrase);
	}

}