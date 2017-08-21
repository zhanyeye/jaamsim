/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.StringProviders;

import com.jaamsim.Samples.SampleProvider;

public class StringProvSample implements StringProvider {
	private final SampleProvider samp;

	public StringProvSample(SampleProvider s) {
		samp = s;
	}

	@Override
	public String getNextString(double simTime, String fmt, double siFactor) {
		return getNextString(simTime, fmt, siFactor, false);
	}

	@Override
	public String getNextString(double simTime, String fmt, double siFactor, boolean integerValue) {
		if (integerValue) {
			return String.format(fmt, (int)(samp.getNextSample(simTime)/siFactor));
		}
		else {
			return String.format(fmt, samp.getNextSample(simTime)/siFactor);
		}
	}

	@Override
	public String toString() {
		return samp.toString();
	}

	public SampleProvider getSampleProvider() {
		return samp;
	}

}
