/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2020  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.server.comm.streambed;

import us.mn.state.dot.tms.server.ControllerImpl;
import us.mn.state.dot.tms.server.FlowStreamImpl;
import us.mn.state.dot.tms.server.comm.BasePoller;
import us.mn.state.dot.tms.server.comm.FlowStreamPoller;
import us.mn.state.dot.tms.server.comm.Operation;
import static us.mn.state.dot.tms.server.comm.PriorityLevel.COMMAND;
import static us.mn.state.dot.tms.utils.URIUtil.TCP;

/**
 * StreambedPoller impelemnts the Streambed switching protocol.
 *
 * @author Douglas Lau
 */
public class StreambedPoller extends BasePoller implements FlowStreamPoller {

	/** Create a new Streambed poller */
	public StreambedPoller(String n) {
		super(n, TCP);
	}

	/** Start an operation */
	private void startOp(Operation op) {
		op.setPriority(COMMAND);
		addOp(op);
	}

	/** Send controller configuration.
	 * @param c Controller to configure. */
	@Override
	public void sendConfig(ControllerImpl c) {
		startOp(createConfigOp(c));
	}

	/** Create configuration operation */
	private Operation createConfigOp(ControllerImpl c) {
		OpStoreProps op = new OpStoreProps("config");
		op.addParam("flows", c.getMaxPin());
		return new Operation("flow.stream.op.config", c, op);
	}

	/** Send a flow stream setting.
	 * @param fs Flow stream to configure. */
	@Override
	public void sendFlow(FlowStreamImpl fs) {
		startOp(createFlowOp(fs));
	}

	/** Create flow operation */
	private Operation createFlowOp(FlowStreamImpl fs) {
		OpStoreProps op = new OpStoreProps("flow");
		op.addParam("number", fs.getPin() - 1);
		op.addParam("location", fs.getSourceUri());
		op.addParam("rtsp-transport", ""); // FIXME
		op.addParam("source-encoding", fs.getSourceEncoding());
		op.addParam("timeout", 5);
		op.addParam("latency", 0);
		op.addParam("overlay-text", fs.getOverlayText());
		op.addParam("address", fs.getSinkAddress());
		op.addParam("port", fs.getSinkPort());
		op.addParam("sink-encoding", fs.getSinkEncoding());
		return new Operation("flow.stream.op.update", fs, op);
	}
}
