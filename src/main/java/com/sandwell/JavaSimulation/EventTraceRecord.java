/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.sandwell.JavaSimulation;

import java.util.ArrayList;

import com.jaamsim.events.ProcessTarget;
import com.sandwell.JavaSimulation.EventManager.Event;

class EventTraceRecord extends ArrayList<String> {
	private String eventManagerName;
	private long internalTime;
	private String targetName;
	private int traceLevel;

	public EventTraceRecord() {
		traceLevel = 0;
	}

	void parse() {
		String[] temp;

		// The first line of the trace is always <eventManagerName>\t<InternalSimulationTime>
		temp = this.get(0).split("\t");
		eventManagerName = temp[0];
		internalTime = Long.parseLong(temp[1]);

		// Try to parse a target entity and method form the second line
		temp = this.get(1).split("\t");

		// A regular event wakeup, parse target/method
		if (temp[0].endsWith("Event")) {
			targetName = temp[3];
			return;
		}

		// We need to look one line further for the Wait line that accompanies a
		// conditonal wait wakeup
		if (temp[0].endsWith("Event-WaitUntilEnded")) {
			temp = this.get(2).split("\t");
			targetName = temp[3];
			return;
		}

		if (temp[0].endsWith("StartProcess")) {
			targetName = temp[1];
			return;
		}

		if (temp[0].endsWith("SchedProcess")) {
			targetName = temp[3];
			return;
		}
	}

	private void append(String record) {
		StringBuilder rec = new StringBuilder();

		for (int i = 0; i < traceLevel; i++) {
			rec.append("  ");
		}
		rec.append(record);
		this.add(rec.toString());
	}

	private void addHeader(String name, long internalTime) {
		// Don't write anything if not at level 0
		if (traceLevel != 0)
			return;

		StringBuilder header = new StringBuilder(name).append("\t").append(internalTime);
		this.add(header.toString());
		traceLevel++;
	}

	private void finish() {
		if(traceLevel != 1)
			return;

		this.add("");
		this.parse();
		EventTracer.processTraceData(this);
		this.clear();
		traceLevel--;
	}


	synchronized void clearTrace() {
		traceLevel = 0;
		clear();
	}

	private static final String[] eventStates = {"Wait", "Event", "Int", "Kill"};
	synchronized void formatEventTrace(String name, Event evt, int reason) {
		this.addHeader(name, evt.schedTick);
		if (reason == 0)
			traceLevel--;

		this.append(String.format("%s\t%d\t%d\t%s", eventStates[reason],
		            evt.schedTick, evt.priority, evt.getDesc()));

		if (reason == 1 || reason == 2)
			traceLevel++;

		this.finish();
	}

	synchronized void formatWaitUntilTrace(String name, long currentTime, int reason) {
		this.addHeader(name, currentTime);
		if (reason == 0) {
			traceLevel--;
			this.append("WaitUntil");
		} else {
			this.append("Event-WaitUntilEnded");
			traceLevel++;
		}
		this.finish();
	}

	synchronized void formatBegin(String name, long currentTime, ProcessTarget t) {
		this.addHeader(name, currentTime);
		this.append(String.format("StartProcess\t%s", t.getDescription()));
		traceLevel++;
		this.finish();
	}

	synchronized void formatExit(String name, long currentTime) {
		this.addHeader(name, currentTime);
		traceLevel--;
		this.append("Exit");
		this.finish();
	}

	synchronized void formatSchedProcessTrace(String name, long currentTime, Event evt) {
		this.addHeader(name, currentTime);
		this.append(String.format("SchedProcess\t%d\t%d\t%s",
		            evt.schedTick, evt.priority, evt.getDesc()));
		this.finish();
	}

	boolean isDefaultEventManager() {
		return eventManagerName.equals("DefaultEventManager");
	}

	long getInternalTime() {
		return internalTime;
	}

	/**
	 * Does a superficial comparison of two records, check number of entries,
	 * time/target/method and finally the basic contents of the record.
	 */
	boolean basicCompare(EventTraceRecord record) {
		if (record.size() != this.size())
			return false;

		if (record.internalTime != this.internalTime)
			return false;

		if (!record.targetName.equals(this.targetName))
			return false;

		return true;
	}
}
