package org.dla.nioftp.client.session.ssh.packet;

import java.util.HashMap;

/**
 *
 * @author Dennis Andress
 *
 *  Jan 24, 2013 9:38:57 AM
 */
public enum MessageType {

	SSH_UNKNOWN(0),
	SSH_MSG_DISCONNECT(1),
	SSH_MSG_IGNORE(2),
	SSH_MSG_UNIMPLEMENTED(3),
	SSH_MSG_DEBUG(4),
	SSH_MSG_SERVICE_REQUEST(5),
	SSH_MSG_SERVICE_ACCEPT(6),
	SSH_MSG_KEXINIT(20),
	SSH_MSG_NEWKEYS(21),
	SSH_MSG_KEXDH_INIT(30),
	SSH_MSG_KEXDH_REPLY(31),
	SSH_MSG_KEX_DH_GEX_GROUP(31),
	SSH_MSG_KEX_DH_GEX_INIT(32),
	SSH_MSG_KEX_DH_GEX_REPLY(33),
	SSH_MSG_KEX_DH_GEX_REQUEST(34),
	SSH_MSG_USERAUTH_REQUEST(50),
	SSH_MSG_USERAUTH_FAILURE(51),
	SSH_MSG_USERAUTH_SUCCESS(52),
	SSH_MSG_USERAUTH_BANNER(53),
	//	SSH_MSG_USERAUTH_INFO_REQUEST(60),
	SSH_MSG_USERAUTH_INFO_RESPONSE(61),
	SSH_MSG_USERAUTH_PASSWD_CHANGEREQ(60),
	SSH_MSG_USERAUTH_PK_OK(60),


	SSH_MSG_GLOBAL_REQUEST(80),
	SSH_MSG_REQUEST_SUCCESS(81),
	SSH_MSG_REQUEST_FAILURE(82),
	SSH_MSG_CHANNEL_OPEN(90),
	SSH_MSG_CHANNEL_OPEN_CONFIRMATION(91),
	SSH_MSG_CHANNEL_OPEN_FAILURE(92),
	SSH_MSG_CHANNEL_WINDOW_ADJUST(93),
	SSH_MSG_CHANNEL_DATA(94),
	SSH_MSG_CHANNEL_EXTENDED_DATA(95),
	SSH_MSG_CHANNEL_EOF(96),
	SSH_MSG_CHANNEL_CLOSE(97),
	SSH_MSG_CHANNEL_REQUEST(98),
	SSH_MSG_CHANNEL_SUCCESS(99),
	SSH_MSG_CHANNEL_FAILURE(100);
	private int code;

	private MessageType(final int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}

	public static MessageType fromInt(final int code) {
		switch (code) {
			case 1:
				return SSH_MSG_DISCONNECT;
			case 2:
				return SSH_MSG_IGNORE;
			case 3:
				return SSH_MSG_UNIMPLEMENTED;
			case 4:
				return SSH_MSG_DEBUG;
			case 5:
				return SSH_MSG_SERVICE_REQUEST;
			case 6:
				return SSH_MSG_SERVICE_ACCEPT;
			case 20:
				return SSH_MSG_KEXINIT;
			case 21:
				return SSH_MSG_NEWKEYS;
			case 30:
				return SSH_MSG_KEXDH_INIT;
			case 31:
				return SSH_MSG_KEXDH_REPLY; //  SSH_MSG_KEX_DH_GEX_GROUP
			case 32:
				return SSH_MSG_KEX_DH_GEX_INIT;
			case 33:
				return SSH_MSG_KEX_DH_GEX_REPLY;
			case 34:
				return SSH_MSG_KEX_DH_GEX_REQUEST;
			case 50:
				return SSH_MSG_USERAUTH_REQUEST;
			case 51:
				return SSH_MSG_USERAUTH_FAILURE;
			case 52:
				return SSH_MSG_USERAUTH_SUCCESS;
			case 53:
				return SSH_MSG_USERAUTH_BANNER;
			case 60:
				//	SSH_MSG_USERAUTH_INFO_REQUEST(60),
				//	SSH_MSG_USERAUTH_PK_OK(60),
				return SSH_MSG_USERAUTH_PASSWD_CHANGEREQ;
			case 61:
				return SSH_MSG_USERAUTH_INFO_RESPONSE;

			case 80:
				return SSH_MSG_GLOBAL_REQUEST;
			case 81:
				return SSH_MSG_REQUEST_SUCCESS;
			case 82:
				return SSH_MSG_REQUEST_FAILURE;
			case 90:
				return SSH_MSG_CHANNEL_OPEN;
			case 91:
				return SSH_MSG_CHANNEL_OPEN_CONFIRMATION;
			case 92:
				return SSH_MSG_CHANNEL_OPEN_FAILURE;
			case 93:
				return SSH_MSG_CHANNEL_WINDOW_ADJUST;
			case 94:
				return SSH_MSG_CHANNEL_DATA;
			case 95:
				return SSH_MSG_CHANNEL_EXTENDED_DATA;
			case 96:
				return SSH_MSG_CHANNEL_EOF;
			case 97:
				return SSH_MSG_CHANNEL_CLOSE;
			case 98:
				return SSH_MSG_CHANNEL_REQUEST;
			case 99:
				return SSH_MSG_CHANNEL_SUCCESS;
			case 100:
				return SSH_MSG_CHANNEL_FAILURE;
			default:
				return SSH_UNKNOWN;
		}
	}
}
