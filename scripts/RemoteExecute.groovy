@Grapes(
    [
		@GrabResolver(name='jitpack', root='https://jitpack.io'),
        @Grab(group='com.github.krishnact', module='sshEnabledApp', version='0.0.1-SNAPSHOT')
    ]
)
package org.himalay.ssh;

import org.himalay.commandline.Arg;
import org.himalay.commandline.CLTBase;
import org.himalay.ssh.SshEnabledApp;
import org.himalay.ssh.SshHelper;

/**
 * This example will connect to specified user@server and execute specified command. It will use pageant to perform authentication. 
 * @author krishna
 *
 */
public class RemoteExecute extends SshEnabledApp{
	/**
	 * The user in user@server format. Multiple users can be specified by using --user option multiple time.
	 */
	@Arg(required=true, description="User names in user@server format")
	List<String> users;

	/**
	 * The command to execute. By default, ifconfig will be executed
	 */
	@Arg(required=false)
	String command = "/sbin/ifconfig";
	
	@Arg
	int port = 22

	@Arg 
	String password

	@Override
	protected void realMain(OptionAccessor arg0) {

		def serverInfo = users.collect {
			return [ username:it , port: port, password: password]
		}
		info (serverInfo)
		println runCommand(serverInfo,command, 1)
	}
	public static void main(String [] args) {
		CLTBase._main(new RemoteExecute(), args);
	}
	
}
