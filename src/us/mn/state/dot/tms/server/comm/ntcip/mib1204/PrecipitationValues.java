/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2019  Minnesota Department of Transportation
 * Copyright (C) 2017  Iteris Inc.
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
package us.mn.state.dot.tms.server.comm.ntcip.mib1204;

import static us.mn.state.dot.tms.server.comm.ntcip.mib1204.MIB1204.*;
import us.mn.state.dot.tms.server.comm.snmp.ASN1Enum;
import us.mn.state.dot.tms.server.comm.snmp.ASN1Integer;

/**
 * Precipitation sensor sample values.
 *
 * @author Douglas Lau
 * @author Michael Darter
 */
public class PrecipitationValues {

	/** Convert humidity to an integer.
	 * @param rhu Relative humidity in percent. A value of 101 indicates an
	 *            error or missing value.
	 * @return Humidity as a percent or null if missing. */
	static private Integer convertHumidity(ASN1Integer rhu) {
		if (rhu != null) {
			int irhu = rhu.getInteger();
			if (irhu >= 0 && irhu <= 100)
				return new Integer(irhu);
		}
		return null;
	}

	/** Precipitation of 65535 indicates error or missing value */
	static private final int PRECIP_INVALID_MISSING = 65535;

	/** Convert precipitation rate to mm/hr.
	 * @param pr precipitation rate in 1/10s of gram per square meter per
	 *           second.
	 * @return Precipiration rate in mm/hr or null if missing */
	static private Integer convertPrecipRate(ASN1Integer pr) {
		if (pr != null) {
			// 1mm of water over 1 sqm is 1L which is 1Kg
			int tg = pr.getInteger();
			if (tg != PRECIP_INVALID_MISSING) {
				int mmhr = (int) Math.round((double) tg * 0.36);
				return new Integer(mmhr);
			}
		}
		return null;
	}

	/** Convert one hour precipitation amount.
	 * @param pr One hour precipitation in tenths of a mm.
	 * @return One hour precipitation in mm or null */
	static private Integer convertPrecip(ASN1Integer pr) {
		if (pr != null) {
			int pri = pr.getInteger();
			if (pri != PRECIP_INVALID_MISSING) {
				int cp = (int) Math.round((double) pri * 0.1);
				return new Integer(cp);
			}
		}
		return null;
	}

	/** Relative humidity */
	public final ASN1Integer relative_humidity = essRelativeHumidity
		.makeInt();

	/** Precipitation rate */
	public final ASN1Integer precip_rate = essPrecipRate.makeInt();

	/** One hour precipitation total */
	public final ASN1Integer precipitation_1_hour =
		essPrecipitationOneHour.makeInt();

	/** Three hour precipitation total */
	public final ASN1Integer precipitation_3_hours =
		essPrecipitationThreeHours.makeInt();

	/** Six hour precipitation total */
	public final ASN1Integer precipitation_6_hours =
		essPrecipitationSixHours.makeInt();

	/** Twelve hour precipitation total */
	public final ASN1Integer precipitation_12_hours =
		essPrecipitationTwelveHours.makeInt();

	/** Twenty-four hour precipitation total */
	public final ASN1Integer precipitation_24_hours =
		essPrecipitation24Hours.makeInt();

	/** Precipitation situation */
	public final ASN1Enum<EssPrecipSituation> precip_situation =
		new ASN1Enum<EssPrecipSituation>(EssPrecipSituation.class,
		essPrecipSituation.node);

	/** Get the relative humidity (%) */
	public Integer getRelativeHumidity() {
		return convertHumidity(relative_humidity);
	}

	/** Get the precipitation rate in mm/hr */
	public Integer getPrecipRate() {
		return convertPrecipRate(precip_rate);
	}

	/** Get the one hour precipitation in mm */
	public Integer getPrecip1Hour() {
		return convertPrecip(precipitation_1_hour);
	}

	/** Get the precipitation situation */
	public EssPrecipSituation getPrecipSituation() {
		return precip_situation.getEnum();
	}

	/** Get JSON representation */
	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append(Json.num("relative_humidity", getRelativeHumidity()));
		sb.append(Json.num("precipitation_rate", getPrecipRate()));
		sb.append(Json.num("precipitation_1_hour", getPrecip1Hour()));
		sb.append(Json.num("precipitation_3_hours",
			convertPrecip(precipitation_3_hours)));
		sb.append(Json.num("precipitation_6_hours",
			convertPrecip(precipitation_6_hours)));
		sb.append(Json.num("precipitation_12_hours",
			convertPrecip(precipitation_12_hours)));
		sb.append(Json.num("precipitation_24_hours",
			convertPrecip(precipitation_24_hours)));
		sb.append(Json.str("precip_situation", getPrecipSituation()));
		return sb.toString();
	}
}
