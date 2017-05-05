@Grapes(
    [
		@GrabResolver(name='jitpack', root='https://jitpack.io'),
        @Grab(group='com.github.krishnact', module='sshEnabledApp', version='0.0.1-SNAPSHOT')
    ]
)
package org.himalay.ssh;

import org.apache.ivy.osgi.updatesite.xml.FeatureParser.DescriptionHandler
import org.himalay.commandline.Arg;
import org.himalay.commandline.CLTBase;
import org.himalay.ssh.SshEnabledApp;
import org.himalay.ssh.SshHelper;
import org.himalay.ssh.Expect;

/**
 * This example will connect to specified user@server and ping the hosts from /etc/hosts file. It will use pageant to perform authentication. 
 * @author krishna
 *
 */
public class RemotePing extends SshEnabledApp{
	/**
	 * The user in user@server format. Multiple users can be specified by using --user option multiple time.
	 */
	@Arg(required=true, description="User names in user@server format")
	List<String> users;

	@Arg
	int port = 22

	def progress = [:].withDefault {0}
	
	@Override
	protected void realMain(OptionAccessor arg0) {
		String prompt = '~# '
		def serverInfo = users.collect {
			return [ username:it , port: port]
		}
		def retData = []; 
		def threads = serverInfo.collect{si->
			Thread th = Thread.start{
				Thread.currentThread().setName(si.username)
				info("Processing ${si.username}")
				
				this.manageProgress('scheduledTasks',1)
				String ret = expect(createSshHelper(si.username, null)){Expect expect->
					StringBuffer retBuffer = new StringBuffer()
					expect.detectPrompt()
					// fire will send the commnd and wait for prompt
					//expect.fire("set")
					//retBuffer.append (expect.freshData)
					expect.send("cat /etc/hosts\n")
					expect.expectPrompt()
					String all = expect.freshData
					// We are not intersted in this data so we will not append it to our buffer
					// retBuffer.append (expect.freshData)
					def ips = all.split("\n").collect{it.trim()}
					ips = ips.findAll{
						def retVal = it ==~/[\s]*[1-9]+(.*)/
						return retVal
					};
					ips = ips.collect{  it.find(/[0-9\.]+/)  };
					this.manageProgress('scheduledTasks',ips.size())
					ips.each{String ip->
						expect.send ("ping -c 4 ${ip}\n")
						expect.expectPrompt()
						String strIn = expect.freshData
						retBuffer.append (strIn)
						//System.err.println strIn
						Thread.sleep(1000)
						this.manageProgress('completedTasks',1)
					}
					return retBuffer.toString()
				}
				
				synchronized (retData) {
					retData << [server: si, result: ret]
					// We can also write this data to a file/database if we want
				} 
				this.manageProgress('completedTasks',1)
			}
			return th;
		}
		
		
		// Wait for all thread to finish their job
		threads.each{
			it.join()
		}
		
		
		info("Collected data from all servers")
		println retData
	}
	public static void main(String [] args) {
		CLTBase._main(new RemotePing(), args);
	}
	
	synchronized private manageProgress(String progressType, int count)
	{
		this.progress[progressType] += count
		info("Progress = " + this.progress) 
	} 
}
