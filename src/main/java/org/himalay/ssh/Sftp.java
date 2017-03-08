package org.himalay.ssh;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import com.jcraft.jsch.IdentityRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.ConnectorFactory;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.IdentityInfo;
import org.apache.commons.vfs2.provider.sftp.IdentityRepositoryFactory;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.slf4j.LoggerFactory;


public class Sftp {
	static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Sftp.class);
	static Pattern ABSFILENAME = Pattern.compile("[a-zA-Z][:](.)*");

	String host;
	String userName;
	String idFile;
	int    port = 22;
	public boolean twoway = false;
	public String baseFileLocation =".";
	boolean userDirIsRoot = true;
	File knownHostsFile;
	public Sftp(String host, String userName, String idFile) {
		super();
		this.host = host;
		this.userName = userName;
		this.idFile = idFile;
		

	}
	public Exception transferFile(String localFilePath, String rFilePath, String action , String pass) throws FileSystemException{
		if (localFilePath.startsWith("/") || ABSFILENAME.matcher(localFilePath).matches())
		{
			return transferFile(localFilePath, rFilePath, action , pass, true);
		}else
			return transferFile(localFilePath, rFilePath, action , pass, false);
	}
	public Exception transferFile(String localFilePath, String rFilePath, String action , String pass, boolean absLocalPath ) throws FileSystemException{
		Exception retVal = null;
        String passphrase = pass;
        // Set a tmporary file as known hosts file
        try {
			knownHostsFile = File.createTempFile("KnownHosts", ".tmp");
			knownHostsFile.deleteOnExit();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


        StandardFileSystemManager sysManager = new StandardFileSystemManager();

        try {
            sysManager.init();
            LOGGER.info("local: "+ localFilePath+ ", remote: "+rFilePath);
            if (absLocalPath == false)
            {
            	localFilePath = new File(this.baseFileLocation).getAbsolutePath() +"/"+ localFilePath;
            }
            FileObject localFile = sysManager.resolveFile( localFilePath);
            String connStr =             		createConnectionString(
    				host, 
    				this.userName, 
    				"", 
    				this.idFile, 
    				passphrase, 
    				rFilePath,
    				port);
            FileObject remoteFile = sysManager.resolveFile(connStr,	createDefaultOptions(this.idFile, passphrase)  	);
            if (rFilePath.endsWith(".sh")){
            	remoteFile.setExecutable(true, true);
            }
            //Selectors.SELECT_FILES --> A FileSelector that selects only the base file/folder.
            if ( action.equals("download"))
            {
            	localFile.copyFrom(remoteFile, Selectors.SELECT_FILES);
            }else if ( action.equals("attributes"))
            {
            	long fileSize     = remoteFile.getContent().getSize();
            	long modifiedTime = remoteFile.getContent().getLastModifiedTime();
            	localFile.createFile();
            	OutputStream os = localFile.getContent().getOutputStream();
            	os.write(("modifiedTime:"+modifiedTime+"\n").getBytes());
            	os.write(("fileSize:"+fileSize+"\n").getBytes());
            	os.close();
            	localFile.getContent().setLastModifiedTime(modifiedTime);
            }else if ( action.equals("upload") && twoway == true)
            {
            	remoteFile.copyFrom(localFile, Selectors.SELECT_FILES);
            }
            if (rFilePath.endsWith(".sh")){
            	remoteFile.setExecutable(true, true);
            }

        } catch (Exception e) {
        	e.printStackTrace();
            LOGGER.info("Downloading file failed: " + e.toString());
            retVal = e;
        }finally{
            sysManager.close();
        }
        
        knownHostsFile.delete();
        return retVal;

	}

	/**
	 * 
	 * @param localFilePath
	 * @param rFilePath
	 * @param action
	 * @param host
	 * @param user
	 * @param idfile
	 * @param pass
	 * @return
	 */
//    public static boolean transferFile(String localFilePath, String rFilePath, String action , String host, String user, String idfile, String pass, int port){
//
//
//        String keyPath = idfile;
//        String passphrase = pass;
//
//
//        StandardFileSystemManager sysManager = new StandardFileSystemManager();
//
//        try {
//            sysManager.init();
//            System.out.LOGGER.info("local: "+ localFilePath+ ", remote: "+rFilePath);
//            FileObject localFile = sysManager.resolveFile(localFilePath);
//
//            FileObject remoteFile = sysManager.resolveFile(
//            		createConnectionString(
//            				host, 
//            				user, 
//            				"", 
//            				keyPath, 
//            				passphrase, 
//            				rFilePath,
//            				port), 
//            		createDefaultOptions(keyPath, passphrase)
//            	);
//            if (rFilePath.endsWith(".sh")){
//            	remoteFile.setExecutable(true, true);
//            }
//            //Selectors.SELECT_FILES --> A FileSelector that selects only the base file/folder.
//            if ( action.equals("download"))
//            {
//            	localFile.copyFrom(remoteFile, Selectors.SELECT_FILES);
//            }else if ( action.equals("upload"))
//            {
//            	remoteFile.copyFrom(localFile, Selectors.SELECT_FILES);
//            }
//            if (rFilePath.endsWith(".sh")){
//            	remoteFile.setExecutable(true, true);
//            }
//
//        } catch (Exception e) {
//            System.out.LOGGER.info("Downloading file failed: " + e.toString());
//            return false;
//        }finally{
//            sysManager.close();
//        }
//        return true;
//    }


    private static String createConnectionString(String hostName, String username, String password, String keyPath, String passphrase, String remoteFilePath, int port) {
    	
        if (keyPath != null) {
            return "sftp://" + username + "@" + hostName + ":"+ port+"/" + remoteFilePath;
        } else {
            return "sftp://" + username + ":" + password + "@" + hostName + ":"+ port+ "/" + remoteFilePath;
        }
    }



    private FileSystemOptions createDefaultOptions(final String keyPath, final String pass) throws Exception{

        //create options for sftp
        FileSystemOptions options = new FileSystemOptions();
        // Strict Host checking
        SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(options, "no");
        SftpFileSystemConfigBuilder.getInstance().setKnownHosts(options, knownHostsFile);
        //set root directory to user home
        SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(options, userDirIsRoot);
        //timeout
        SftpFileSystemConfigBuilder.getInstance().setTimeout(options, 10000);
        if (pass != null)
        {
            SftpFileSystemConfigBuilder.getInstance().setUserInfo(options, new SftpPasswordUserInfo(pass));
            SftpFileSystemConfigBuilder.getInstance().setPreferredAuthentications(options, "password");
        } else if (keyPath != null) {
            SftpFileSystemConfigBuilder.getInstance().setUserInfo(options, new SftpPassphraseUserInfo(pass));
            IdentityInfo  ii= new IdentityInfo(new File(keyPath));
            //ssh key
            SftpFileSystemConfigBuilder.getInstance().setIdentityInfo(options, ii);
        }else{
            SftpFileSystemConfigBuilder.getInstance().setUserInfo(options, new SftpPassphraseUserInfo(pass));
            org.apache.commons.vfs2.provider.sftp.IdentityRepositoryFactory iirf = new IdentityRepositoryFactory() {
				
				@Override
				public IdentityRepository create(JSch arg0) {
					ConnectorFactory cf = ConnectorFactory.getDefault();
					IdentityRepository irepo = null;
					try {
						irepo = new RemoteIdentityRepository(cf.createConnector());
					} catch (AgentProxyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return irepo;
				}
			};
            SftpFileSystemConfigBuilder.getInstance().setIdentityRepositoryFactory(options, iirf);
     	
        }

        return options;
    }



    public static class SftpPassphraseUserInfo implements UserInfo {

        private String passphrase = null;

        public SftpPassphraseUserInfo(final String pp) {
            passphrase = pp;
        }

        public String getPassphrase() {
            return passphrase;
        }

        public String getPassword() {
            return null;
        }

        public boolean promptPassphrase(String arg0) {
            return true;
        }

        public boolean promptPassword(String arg0) {
            return false;
        }

        public void showMessage(String message) {

        }

        public boolean promptYesNo(String str) {
            return true;
        }

    }

    public static class SftpPasswordUserInfo implements UserInfo {

        private String pass = null;

        public SftpPasswordUserInfo(final String pp) {
        	pass = pp;
        }

        public String getPassphrase() {
        	return null;
        }

        public String getPassword() {
            
            return pass;
        }

        public boolean promptPassphrase(String arg0) {
            return false;
        }

        public boolean promptPassword(String arg0) {
            return true;
        }

        public void showMessage(String message) {

        }

        public boolean promptYesNo(String str) {
            return true;
        }

    }

}