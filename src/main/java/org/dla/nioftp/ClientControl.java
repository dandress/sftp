package org.dla.nioftp;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;
import org.dla.nioftp.client.Client;
import org.dla.nioftp.client.session.FileStatus;
import org.dla.nioftp.client.session.SessionStatus;
import org.dla.nioftp.client.session.ssh.packet.PacketException;
import org.dla.threads.ThreadSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to test from the command line.
 *
 * @author Dennis Andress
 */
public class ClientControl implements Observer {

	private static final Logger logger = LoggerFactory.getLogger(ClientControl.class);
	/**
	 * @param args the command line arguments
	 */
	private Client client;
	// directory holding the files to transfer
	private final String fileSourceDirectory = "";
	// A file to transfer
	private final String fileName = "";

	// Where the files will be sent
	private final String destinationAddress = "";
	private final int destinationPort = 22;
	private final String loginName = "";
	private final String loginPW = "";

	private boolean readyToTransfer = false;

	/**
	 *
	 * @param connectionAddress
	 * @param connectionPort
	 */
	public ClientControl() {
	}

	public void startSftpService() {
		try {
			client = new Client(this);
			client.init(destinationAddress, destinationPort, loginName, loginPW);

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, fileName);

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, "test1.rpm");

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, "test2.rpm");

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, "test3.rpm");

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, "test4.rpm");

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, "test5.rpm");

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, "test6.rpm");

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, "test7.rpm");

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, "test8.rpm");

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, "test9.rpm");

			while (readyToTransfer == false) {
				Thread.sleep(2000);
			}
			client.sendFile(fileSourceDirectory, "test10.rpm");

			while (readyToTransfer == false) {
				Thread.sleep(1000 * 60 * 4);
			}

		} catch (PacketException | ExecutionException | IOException | InterruptedException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	private void shutdown() {
		client.shutdown();
		ThreadSource.getInstance().interrupt();
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg instanceof SessionStatus) {
			final SessionStatus status = (SessionStatus) arg;
			logger.info("SessionStatus: {}  {}", status.getStatus());
			switch (status.getStatus()) {
				case SSH_KEX:
					break;
				case SSH_AUTH_SUCCESS:
					readyToTransfer = true;
					break;
				case SSH_AUTH_FAILED:
					break;
				case SSH_DISCONNECTED:
					logger.error("We are disconnected");
					shutdown();
					break;
				case SSH_USERAUTH_SVC_ACCEPT:
					break;
				default:
					throw new AssertionError();
			}
		} else if (arg instanceof FileStatus) {
			final FileStatus status = (FileStatus) arg;
			logger.info("FileStatus: {}   {}   {}", status.getStatus(), status.getFileName(), status.getErrorMsg());

			final String fileName = status.getFileName();
			final String errorMsg = status.getErrorMsg();

			switch (status.getStatus()) {
				case CHAN_OPENED:
					logger.info("Channel openned");
					break;
				case CHAN_CLOSED:
					logger.info("Channel closed");
					break;
				case CHAN_OPEN_FAILED:
					logger.warn("Channel open for file {} failed", fileName, errorMsg);
					break;
				case FILE_OPEN_FAILED:
					logger.warn("Transfer for file {} failed", fileName, errorMsg);
					break;
				case OPENED:
					logger.info("File {} openend on destination server", fileName);
					break;
				case SUCCESS:
					logger.info("Transfer of file {} successful", fileName);
					if (fileName.equals("test10.rpm"))
						shutdown();
					break;
				case DISCONNECTED:
					logger.error("We are disconnected {}  {}", fileName, errorMsg);
					shutdown();
					break;
				default:
					logger.error(status.getStatus().toString());
					throw new AssertionError();
			}
		} else
			logger.warn("Unknowen update");

	}

	public static void main(String[] args) {
		ClientControl runner = new ClientControl();
		runner.startSftpService();
	}
}
