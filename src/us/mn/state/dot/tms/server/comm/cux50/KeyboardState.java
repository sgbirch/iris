/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2018  Minnesota Department of Transportation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package us.mn.state.dot.tms.server.comm.cux50;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import us.mn.state.dot.tms.Camera;
import us.mn.state.dot.tms.CameraHelper;
import us.mn.state.dot.tms.DeviceRequest;
import static us.mn.state.dot.tms.DeviceRequest.*;
import us.mn.state.dot.tms.VideoMonitor;
import us.mn.state.dot.tms.VideoMonitorHelper;
import us.mn.state.dot.tms.server.CameraImpl;
import us.mn.state.dot.tms.server.VideoMonitorImpl;

/**
 * Panasonic CU-x50 keyboard state.
 *
 * @author Douglas Lau
 */
public class KeyboardState {

	static private final Integer ZERO = new Integer(0);

	/** Packet start/end transmit */
	static private final byte STX = 0x02;
	static private final byte ETX = 0x03;

	/** Panasonic keyboard has char 0x80 mapped to "play" arrow */
	static private final char SEQ_PLAY = '\u0080';
	static private final char SEQ_PAUSE = '"';

	/** Keycodes for special functions */
	static private final byte KEY_MON = (byte) 'A';
	static private final byte KEY_CAM = (byte) 'B';
	static private final byte KEY_PRESET = (byte) 'D';
	static private final byte KEY_PREV = (byte) 'G';
	static private final byte KEY_NEXT = (byte) 'H';
	static private final byte KEY_SHIFT = (byte) 'M';
	static private final byte KEY_CLEAR = (byte) 'N';
	static private final byte KEY_WIPER = (byte) 'O';
	static private final byte KEY_PAUSE = (byte) 'X';
	static private final byte KEY_SEQ = (byte) 'Y';
	static private final byte KEY_MENU = (byte) 'h';
	static private final byte KEY_EXIT = (byte) 'm';
	static private final byte KEY_ENTER = (byte) 'n';
	static private final byte KEY_IRIS_CLOSE = (byte) 'p';
	static private final byte KEY_IRIS_OPEN = (byte) 'q';
	static private final byte KEY_FOCUS_NEAR = (byte) 'r';
	static private final byte KEY_FOCUS_FAR = (byte) 's';

	/** Joystick stop code */
	static private final int JOY_STOP = '@';

	/** Joystick left code */
	static private final int JOY_LEFT = 'a';

	/** Joystick left-up code */
	static private final int JOY_LEFT_UP = 'b';

	/** Joystick up code */
	static private final int JOY_UP = 'c';

	/** Joystick right-up code */
	static private final int JOY_RIGHT_UP = 'd';

	/** Joystick right code */
	static private final int JOY_RIGHT = 'e';

	/** Joystick right-down code */
	static private final int JOY_RIGHT_DOWN = 'f';

	/** Joystick down code */
	static private final int JOY_DOWN = 'g';

	/** Joystick left-down code */
	static private final int JOY_LEFT_DOWN = 'h';

	/** Dead zone for joystick slop */
	static private final int DEAD_ZONE = 0;

	/** Maximum pan/tilt value */
	static private final int MAX_PAN_TILT = 64;

	/** Map a pan/tilt value to [0, 1] range */
	static private float pt_range(int p) {
		return (p > DEAD_ZONE)
		     ? (p - DEAD_ZONE) / (float) (MAX_PAN_TILT - DEAD_ZONE)
		     : 0;
	}

	/** Parse pan value from joystick pkt */
	static private float parse_pan(byte[] rcv, int off) {
		int pt = rcv[off + 1];
		switch (pt) {
		case JOY_LEFT:
		case JOY_LEFT_UP:
		case JOY_LEFT_DOWN:
			int left = parse_hex2(rcv[off + 2], rcv[off + 3]);
			return -pt_range(left);
		case JOY_RIGHT:
		case JOY_RIGHT_UP:
		case JOY_RIGHT_DOWN:
			int right = parse_hex2(rcv[off + 2], rcv[off + 3]);
			return pt_range(right);
		default:
			return 0;
		}
	}

	/** Parse tilt value from joystick pkt */
	static private float parse_tilt(byte[] rcv, int off) {
		int pt = rcv[off + 1];
		switch (pt) {
		case JOY_DOWN:
		case JOY_LEFT_DOWN:
		case JOY_RIGHT_DOWN:
			int down = parse_hex2(rcv[off + 4], rcv[off + 5]);
			return -pt_range(down);
		case JOY_UP:
		case JOY_LEFT_UP:
		case JOY_RIGHT_UP:
			int up = parse_hex2(rcv[off + 4], rcv[off + 5]);
			return pt_range(up);
		default:
			return 0;
		}
	}

	/** Parse zoom value from joystick pkt */
	static private float parse_zoom(byte[] rcv, int off) {
		switch (rcv[off + 6]) {
		case '0': return 0;
		case '1': return 0.25f;
		case '2': return 0.5f;
		case '3': return 0.75f;
		case '4': return 1.0f;
		case '5': return -0.25f;
		case '6': return -0.5f;
		case '7': return -0.75f;
		case '8': return -1.0f;
		default: return 0;
		}
	}

	/** Parse 2 hex digits */
	static private int parse_hex2(byte hi, byte lo) {
		int h0 = parse_hex(hi);
		int h1 = parse_hex(lo);
		if (h0 >= 0 && h1 >= 0)
			return h0 * 16 + h1;
		else
			return 0;
	}

	/** Parse one hex digit */
	static private int parse_hex(byte h) {
		if (h >= (byte) '0' && h <= (byte) '9')
			return h - (byte) '0';
		else if (h >= (byte) 'A' && h <= (byte) 'F')
			return 10 + (h - (byte) 'A');
		else if (h >= (byte) 'a' && h <= (byte) 'f')
			return 10 + (h - (byte) 'a');
		else
			return -1;
	}

	/** Heartbeat message */
	static private final byte[] HEARTBEAT =
		"@CU650".getBytes(StandardCharsets.US_ASCII);

	/** Compare two packets for equality */
	static private boolean pkt_equals(byte[] a, byte[] b, int off,
		int len)
	{
		if (a.length != len)
			return false;
		for (int i = 0; i < len; i++) {
			if (a[i] != b[off + i])
				return false;
		}
		return true;
	}

	/** Check if a packet is a heartbeat */
	static private boolean checkHeartbeat(byte[] rcv, int off, int len) {
		return pkt_equals(HEARTBEAT, rcv, off, len);
	}

	/** Get the buffer offset of a packet */
	static private int pkt_offset(byte[] rcv, int s) {
		for (int i = s; i < rcv.length; i++) {
			if (STX == rcv[i])
				return i + 1;
		}
		return -1;
	}

	/** Get the packet length */
	static private int pkt_length(byte[] rcv, int off) {
		for (int i = 0; off + i < rcv.length; i++) {
			if (ETX == rcv[off + i])
				return i;
		}
		return -1;
	}

	/** Format one LCD display message */
	static private String formatLCD(char ab, String line, String flicker) {
		return String.format("%c%-20.20s%-6.6s", ab, line, flicker);
	}

	/** Write buffer */
	private final ByteBuffer buf = ByteBuffer.allocate(128);

	/** Host string */
	private final String host;

	/** Selected monitor */
	private VideoMonitor monitor = null;

	/** Current keyboard entry data */
	private StringBuilder entry = new StringBuilder();

	/** Current shift state */
	private boolean shift = false;

	/** Create a new keyboard state */
	public KeyboardState(String h) {
		host = h;
	}

	/** Get the camera number */
	private Integer getCamNum() {
		CameraImpl c = getCamera();
		return (c != null) ? c.getCamNum() : null;
	}

	/** Get camera for selected video monitor */
	private CameraImpl getCamera() {
		VideoMonitor vm = monitor;
		if (vm != null) {
			Camera c = vm.getCamera();
			if (c instanceof CameraImpl)
				return (CameraImpl) c;
		}
		return null;
	}

	/** Get the current keyboard entry */
	private Integer getEntry() {
		String ent = entry.toString();
		entry.setLength(0);
		try {
			return Integer.parseInt(ent);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	/** Get the camera sequence number */
	private String getSeqNum() {
		VideoMonitor vm = monitor;
		if (vm instanceof VideoMonitorImpl) {
			VideoMonitorImpl vmi = (VideoMonitorImpl) vm;
			Integer n = vmi.getSeqNum();
			if (n != null)
				return n.toString();
		}
		return null;
	}

	/** Is the sequence running */
	private boolean isSeqenceRunning() {
		VideoMonitor vm = monitor;
		if (vm instanceof VideoMonitorImpl) {
			VideoMonitorImpl vmi = (VideoMonitorImpl) vm;
			return vmi.isSequenceRunning();
		}
		return false;
	}

	/** Parse one packet */
	private void parsePkt(byte[] rcv, int off, int len) {
		if (checkHeartbeat(rcv, off, len))
			return;
        	else if (3 == len && rcv[off] == (byte) 'A') {
			if (rcv[off + 2] == (byte) '+')
				handleKeyDown(rcv[off + 1]);
			else if (rcv[off + 2] == (byte) '-')
				handleKeyUp(rcv[off + 1]);
		} else if (7 == len && rcv[off] == (byte) 'B')
			handleJoystick(rcv, off);
		else
			beepInvalid();
	}

	/** Write a packet */
	private void writePkt(byte[] pkt) {
		buf.put(STX);
		buf.put(pkt);
		buf.put(ETX);
	}

	/** Write a packet */
	private void writePkt(String pkt) {
		// Must use ISO_8859_1 for 8-bit characters (SEQ_PLAY)
		writePkt(pkt.getBytes(StandardCharsets.ISO_8859_1));
	}

	/** Format LCD for line A */
	private String formatLineA() {
		VideoMonitor vm = monitor;
		String line = (vm != null)
		    ? String.format("Mon %-4s%12s", vm.getMonNum(), formatSeq())
		    : "  Select monitor #";
		return formatLCD('a', line, "000000");
	}

	/** Format sequence */
	private String formatSeq() {
		String sn = getSeqNum();
		char sr = isSeqenceRunning() ? SEQ_PLAY : SEQ_PAUSE;
		return (sn != null)
		    ? String.format("%c Seq %-4s", sr, sn)
		    : "";
	}

	/** Format LCD for line B */
	private String formatLineB() {
		String line = String.format("%-12s  ~ %-4.4s", formatCam(),
			entry + "_");
		return formatLCD('b', line, "30000f");
	}

	/** Format camera */
	private String formatCam() {
		Integer cam = getCamNum();
		return (cam != null)
		    ? String.format("Cam %d", cam)
		    : "";
	}

	/** Update the LCD display */
	private void updateLCD() {
		writePkt(formatLineA());
		writePkt(formatLineB());
	}

	/** Handle key down message */
	private void handleKeyDown(byte k) {
		if (k >= (byte) '0' && k <= (byte) '9')
			addToEntry(k);
		else if (KEY_MON == k)
			selectMon();
		else if (KEY_CAM == k)
			selectCam();
		else if (KEY_PREV == k)
			selectPrevCam();
		else if (KEY_NEXT == k)
			selectNextCam();
		else if (KEY_SHIFT == k)
			shift = true;
		else if (KEY_IRIS_CLOSE == k)
			deviceReq(CAMERA_IRIS_CLOSE);
		else if (KEY_IRIS_OPEN == k)
			deviceReq(CAMERA_IRIS_OPEN);
		else if (KEY_FOCUS_NEAR == k)
			deviceReq(CAMERA_FOCUS_NEAR);
		else if (KEY_FOCUS_FAR == k)
			deviceReq(CAMERA_FOCUS_FAR);
		else if (KEY_WIPER == k)
			deviceReq(CAMERA_WIPER_ONESHOT);
		else if (KEY_PRESET == k)
			handlePreset();
		else if (KEY_MENU == k)
			deviceReq(CAMERA_MENU_OPEN);
		else if (KEY_EXIT == k)
			deviceReq(CAMERA_MENU_CANCEL);
		else if (KEY_ENTER == k)
			deviceReq(CAMERA_MENU_ENTER);
		else if (KEY_SEQ == k)
			selectSeq();
		else if (KEY_PAUSE == k)
			pausePlay();
		else if (KEY_CLEAR == k)
			entry.setLength(0);
		else {
			entry.setLength(0);
			beepInvalid();
		}
	}

	/** Handle key up message */
	private void handleKeyUp(byte k) {
		if (KEY_SHIFT == k)
			shift = false;
	}

	/** Add a character to entry */
	private void addToEntry(byte k) {
		if (entry.length() < 4) {
			entry.append((char) k);
			if (entry.length() > 1 && entry.charAt(0) =='0')
				entry.deleteCharAt(0);
		} else
			beepInvalid();
	}

	/** Select a monitor */
	private void selectMon() {
		Integer n = getEntry();
		VideoMonitor vm = findMonitor(n);
		if (vm != null || ZERO.equals(n))
			monitor = vm;
		else
			beepInvalid();
	}

	/** Find a video monitor */
	private VideoMonitor findMonitor(Integer n) {
		return (n != null && n != 0)
		      ? VideoMonitorHelper.findUID(n)
		      : null;
	}

	/** Select a camera */
	private void selectCam() {
		Integer n = getEntry();
		CameraImpl c = findCamera(n);
		if (c != null || ZERO.equals(n))
			selectCamera(c, "SEL " + host);
		else
			beepInvalid();
	}

	/** Find a camera by number */
	private CameraImpl findCamera(Integer n) {
		if (n != null) {
			Camera c = CameraHelper.findNum(n);
			if (c instanceof CameraImpl)
				return (CameraImpl) c;
		}
		return null;
	}

	/** Select a camera on the selected video monitor */
	private void selectCamera(CameraImpl c, String src) {
		VideoMonitor vm = monitor;
		if (vm != null) {
			int mn = vm.getMonNum();
			stopCamControl();
			VideoMonitorImpl.setCamMirrored(mn, c, src);
		} else
			beepInvalid();
	}

	/** Stop camera control on selected camera */
	private void stopCamControl() {
		CameraImpl c = getCamera();
		if (c != null)
			c.sendPTZ(0, 0, 0);
	}

	/** Select previous camera on a video monitor */
	private void selectPrevCam() {
		VideoMonitor vm = monitor;
		if (vm instanceof VideoMonitorImpl) {
			VideoMonitorImpl vmi = (VideoMonitorImpl) vm;
			stopCamControl();
			vmi.selectPrevCam(host);
		} else
			beepInvalid();
	}

	/** Select next camera on a video monitor */
	private void selectNextCam() {
		VideoMonitor vm = monitor;
		if (vm instanceof VideoMonitorImpl) {
			VideoMonitorImpl vmi = (VideoMonitorImpl) vm;
			stopCamControl();
			vmi.selectNextCam(host);
		} else
			beepInvalid();
	}

	/** Handle a preset key */
	private void handlePreset() {
		Integer n = getEntry();
		CameraImpl c = getCamera();
		if (n != null && c != null) {
			if (shift)
				c.setStorePreset(n);
			else
				c.setRecallPreset(n);
		} else
			beepInvalid();
	}

	/** Send a device request to a camera */
	private void deviceReq(DeviceRequest dr) {
		CameraImpl c = getCamera();
		if (c != null)
			c.setDeviceRequest(dr.ordinal());
		else
			beepInvalid();
	}

	/** Select a camera sequence */
	private void selectSeq() {
		Integer n = getEntry();
		VideoMonitor vm = monitor;
		if (n != null && vm instanceof VideoMonitorImpl) {
			VideoMonitorImpl vmi = (VideoMonitorImpl) vm;
			vmi.setSeqNum(n);
		} else
			beepInvalid();
	}

	/** Toggle sequence pause/play */
	private void pausePlay() {
		VideoMonitor vm = monitor;
		if (vm instanceof VideoMonitorImpl) {
			VideoMonitorImpl vmi = (VideoMonitorImpl) vm;
			if (vmi.isSequenceRunning())
				vmi.pauseSequence();
			else
				vmi.unpauseSequence();
		} else
			beepInvalid();
	}

	/** Handle a joystick message */
	private void handleJoystick(byte[] rcv, int off) {
		CameraImpl c = getCamera();
		if (c != null)
			handleJoystick(c, rcv, off);
		else
			beepInvalid();
	}

	/** Handle a joystick message */
	private void handleJoystick(CameraImpl c, byte[] rcv, int off) {
		float pan = parse_pan(rcv, off);
		float tilt = parse_tilt(rcv, off);
		float zoom = parse_zoom(rcv, off);
		c.sendPTZ(pan, tilt, zoom);
	}

	/** Beep for invalid input */
	private void beepInvalid() {
		writePkt("d8");
	}

	/** Get data to send */
	private byte[] getSend() {
		buf.flip();
		byte[] snd = new byte[buf.remaining()];
		buf.get(snd);
		return snd;
	}

	/** Handle receive for a keyboard state */
	public synchronized byte[] handleReceive(byte[] rcv) {
		buf.clear();
		int s = 0;
		while (true) {
			int off = pkt_offset(rcv, s);
			if (off < 0)
				break;
			int len = pkt_length(rcv, off);
			if (len > 0)
				parsePkt(rcv, off, len);
			else
				break;
			s = off + len;
		}
		updateLCD();
		return getSend();
	}
}
