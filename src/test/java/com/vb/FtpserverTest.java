package com.vb;


import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.UserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.PropertiesUserManager;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class FtpserverTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testFTPSConnection() throws Exception {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();
        factory.setImplicitSsl(false);
        factory.setPort(2221);
        SslConfigurationFactory sslFactory = new SslConfigurationFactory();
        sslFactory.setKeystoreFile(new File("src/test/resources/selfsigned.p12"));
        sslFactory.setKeystorePassword("abc@123");
        sslFactory.setKeystoreType("PKCS12");
        factory.setSslConfiguration(sslFactory.createSslConfiguration());
        serverFactory.addListener("default", factory.createListener());
        PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
        userManagerFactory.setFile(new File("src/test/resources/ftpusers.properties"));
        UserManager userManager = userManagerFactory.createUserManager();
        BaseUser user = new BaseUser();
        user.setName("foobar");
        user.setPassword("secret");
        user.setHomeDirectory(folder.newFolder("foobar").getAbsolutePath());
        userManager.save(user);
        serverFactory.setUserManager(userManager);
        FtpServer server = serverFactory.createServer();
        server.start();

        for (int i = 0; i < 100; i++) {
            System.out.println("Run #" + i);
            runClient();
        }
    }

    private void runClient() throws IOException {
        FTPSClient client = new FTPSClient("TLS", false);
        client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
        client.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
        client.setDefaultTimeout(5000);
        client.connect("localhost", 2221);
        if (client.login("foobar", "secret")) {
            client.enterLocalPassiveMode();
            client.execPBSZ(0);
            client.execPROT("P");
            client.changeWorkingDirectory("..");
            client.logout();
            client.disconnect();
        }
    }
}
