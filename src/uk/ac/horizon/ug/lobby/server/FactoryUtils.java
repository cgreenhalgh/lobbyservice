/**
 * Copyright 2010 The University of Nottingham
 * 
 * This file is part of lobbyservice.
 *
 *  lobbyservice is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  lobbyservice is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with lobbyservice.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package uk.ac.horizon.ug.lobby.server;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONString;
import org.json.JSONStringer;

import com.google.appengine.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.json.JSONWriter;

import uk.ac.horizon.ug.lobby.protocol.GameTimeOption;
import uk.ac.horizon.ug.lobby.protocol.GameTimeOptions;


/**
 * @author cmg
 *
 */
public class FactoryUtils {
	static Logger logger = Logger.getLogger(FactoryUtils.class.getName());
	enum Parts { 
		SECONDS(0,59,Calendar.SECOND), 
		MINUTES(0,59,Calendar.MINUTE), 
		HOURS(0,23,Calendar.HOUR_OF_DAY),
		DAY_OF_MONTH(1,31,Calendar.DAY_OF_MONTH), 
		MONTH(1,12,Calendar.MONTH),  
		DAY_OF_WEEK(1,7,Calendar.DAY_OF_WEEK), 
		YEAR(2010,2011,Calendar.YEAR); // cron range: 1970-2099
		private int min; private int max; private int calendarIx; 
		Parts(int min, int max, int calendarIx) { this.min = min; this.max = max; this.calendarIx = calendarIx; }
		public int min() { return min; }
		public int max() { return max; }
		public int calendarIx() { return calendarIx; }
		public int getValue(Calendar cal) {
			int val = cal.get(calendarIx);
			if (this!=YEAR)
				val = val-cal.getMinimum(calendarIx)+min;
			return val;
		}
		public void setValue(Calendar cal, int val) {
			if (this!=YEAR)
				val = val-min+cal.getMinimum(calendarIx);
			cal.set(calendarIx, val);
		}
	};
	enum Months { MONTH_ZERO, JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC };
	// fortunately Calendar also starts from SUNDAY
	enum DaysOfWeek { DAY_ZERO, SUN, MON, TUE, WED, THU, FRI, SAT };
	
	/** parse and return basic value set for part of cron expression */
	private static TreeSet<Integer> parseValues(String text, Parts part) throws CronExpressionException {
		TreeSet<Integer> values = new TreeSet<Integer>();
		String atCommas[] = text.split("[,]");
		for (int i=0; i<atCommas.length; i++)
		{
			String t2 = atCommas[i];
			int slashIx = t2.lastIndexOf("/");
			if (slashIx>=0) {
				int interval = 0;
				try {
					interval = Integer.parseInt(t2.substring(slashIx+1));
				}
				catch (NumberFormatException nfe) {
					throw new CronExpressionException("'/n' clause misformed in "+part+" of "+text);
				}
				if (interval<=0)
					throw new CronExpressionException("'/n' clause with n <= 0 in "+part+" of "+text);
				Set<Integer> baseValues = parseValues(text.substring(0, slashIx), part);
				for (int offset=0; offset<=part.max(); offset+=interval) {
					for (Integer baseValue : baseValues) {
						if (baseValue + offset <= part.max())
							values.add(baseValue+offset);
					}
				}
			}
			else {
				int hyphIx = t2.indexOf("-");
				if (hyphIx>=0) {
					int from = parseSimpleValue(t2.substring(0, hyphIx), part);
					int to = parseSimpleValue(t2.substring(0, hyphIx), part);
					if (to<from) 
						throw new CronExpressionException("'n-m' clause with n>m in "+part+" of "+text);
					for (int val = from; val<=to; val++) {
						values.add(val);
					}
				}
				else {
					if (t2.equals("*") || t2.equals("?")) {
						// all values
						for (int val=part.min(); val<=part.max(); val++)
							values.add(val);
					}
					else {
						// simple value
						values.add(parseSimpleValue(t2, part));
					}
				}
			}
		}
		return values;
	}
	/** 
	 * @param substring
	 * @param part
	 * @return
	 * @throws CronExpressionException 
	 */
	private static int parseSimpleValue(String substring, Parts part) throws CronExpressionException {
		if (part==Parts.DAY_OF_WEEK) {
			for (int i=0; i<DaysOfWeek.values().length; i++) 
				if (substring.equals(DaysOfWeek.values()[i]))
					return DaysOfWeek.values()[i].ordinal();			
		}
		if (part==Parts.MONTH) {
			for (int i=0; i<Months.values().length; i++) 
				if (substring.equals(Months.values()[i]))
					return Months.values()[i].ordinal();						
		}
		try {
			int value = Integer.parseInt(substring);
			if (value<part.min() || value>part.max())
				throw new CronExpressionException(part+" value out of range: "+value+" ("+part.min()+"-"+part.max()+")");
			return value;
		}
		
		catch (NumberFormatException nfe) {
			throw new CronExpressionException(part+" value invalid: "+substring+" ("+part.min()+"-"+part.max()+")");
		}
	}
	private static String [] getParts(String cronExpression) throws CronExpressionException {
		String parts[] = cronExpression.split("[ \t]");
		if (parts.length<Parts.values().length-1 || parts.length>Parts.values().length)
			throw new CronExpressionException("Expression has "+parts.length+" parts; should have 6 or 7: "+cronExpression);
		if (parts.length<Parts.values().length) {
			// add year wildcard
			String newparts [] = new String[parts.length+1];
			newparts[parts.length] = "*";
			for (int i=0; i<parts.length; i++)
				newparts[i] = parts[i];
			parts = newparts;
		}
		return parts;
	}
	/**
	 * @param parts
	 * @return
	 * @throws CronExpressionException 
	 */
	private static TreeSet[] getValues(String[] parts) throws CronExpressionException {
		TreeSet values[] = new TreeSet[parts.length];
		for (int pi=0; pi<parts.length; pi++) {
			values[pi] = parseValues(parts[pi], Parts.values()[pi]);
			if (values[pi].size()==0) 
				throw new CronExpressionException("Part "+Parts.values()[pi]+" has no valid value(s): "+parts[pi]);
		}
		return values;
	}
	/** return first time matching cron expression no earlier than minTime.
	 * Supports number, day name, month name, range (-), repeat every (/).
	 * Does not support 'W', 'L', '#', 'C'.
	 * 
	 * return 0 if no match.
	 */
	public static long getNextCronTime(String cronExpression, TreeSet values[], long minTime, long maxTime) throws CronExpressionException {
		// iteratively increase minTime until all constraints are satisified or maxTime reached
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Z"));
		// next second?!
		cal.setTimeInMillis(minTime);
		if (cal.get(Calendar.MILLISECOND)!=0) {
			cal.set(Calendar.MILLISECOND, 0);
			cal.add(Calendar.SECOND, 1);
			minTime = cal.getTimeInMillis();
		}
		long lastMinTime = 0;
		recheck:
		while(minTime<=maxTime) {
			// almost a second
			if (minTime<=lastMinTime+900)
				throw new CronExpressionException("Evaluating "+cronExpression+" stuck at "+minTime+"/"+lastMinTime);
			lastMinTime = minTime;
			//logger.info("Check time "+minTime+": "+cal.get(Calendar.SECOND)+" "+cal.get(Calendar.MINUTE)+" "+cal.get(Calendar.HOUR_OF_DAY)+" "+cal.get(Calendar.DAY_OF_MONTH)+" "+cal.get(Calendar.MONTH)+" "+cal.get(Calendar.DAY_OF_WEEK)+" "+cal.get(Calendar.YEAR));
			//cal.setTimeInMillis(minTime);
			nextpart:
			for (int pi=values.length-1; pi>=0; pi--) {
				Parts part = Parts.values()[pi];
				int val = part.getValue(cal);
				if (!values[pi].contains(val))
				{
					//logger.info(part+" "+val+" not in "+values[pi]+"...");
					// need to reset to 0 all smaller components of the time...
					// then advance to the next value of the unit in question...
					// If that will wrap the next bigger unit we also reset this unit
					// then go back around the loop
					switch (part) {
					case YEAR:
						cal.set(Calendar.MONTH, cal.getMinimum(Calendar.MONTH));
						// drop through
					case MONTH:
						cal.set(Calendar.DAY_OF_MONTH, cal.getMinimum(Calendar.DAY_OF_MONTH));
						// drop through
					case DAY_OF_MONTH:
					case DAY_OF_WEEK:
						cal.set(Calendar.HOUR_OF_DAY, cal.getMinimum(Calendar.HOUR_OF_DAY));
						// drop through
					case HOURS:
						cal.set(Calendar.MINUTE, cal.getMinimum(Calendar.MINUTE));
						// drop through
					case MINUTES:
						cal.set(Calendar.SECOND, cal.getMinimum(Calendar.SECOND));
						// drop through
					case SECONDS:
						cal.set(Calendar.MILLISECOND, cal.getMinimum(Calendar.MILLISECOND));
						break;
					}	
					// advance
					SortedSet<Integer> tailSet = ((TreeSet<Integer>)values[pi]).tailSet(val+1);
					if (tailSet.size()>0) {
						cal.add(part.calendarIx(), tailSet.first()-val);
						long newMinTime = cal.getTimeInMillis();
						if (newMinTime<=minTime)
							throw new CronExpressionException("Evaluating "+cronExpression+" stuck at "+minTime+"/"+newMinTime+", "+part+"="+val);
						minTime = newMinTime;
						//logger.info("Fail on "+part+": "+val+" vs "+values[pi]+"; advance to "+minTime);
						continue recheck;
					}
					// wrap around
					// set this part to min.
					cal.set(part.calendarIx(), cal.getMinimum(part.calendarIx()));
					// advance the next part
					switch (part) {
					case YEAR:
						// years cannot wrap around
						return 0;
					case MONTH:
						cal.add(Calendar.YEAR, 1);
						break;
					case DAY_OF_MONTH:
						cal.add(Calendar.MONTH, 1);
						break;
					case DAY_OF_WEEK:
						cal.add(Calendar.WEEK_OF_YEAR, 1);
						break;
					case HOURS:
						cal.add(Calendar.DAY_OF_YEAR, 1);
						break;
					case MINUTES:
						cal.add(Calendar.HOUR_OF_DAY, 1);
						break;
					case SECONDS:
						cal.add(Calendar.MINUTE, 1);
						break;
					}
					// try again...
					long newMinTime = cal.getTimeInMillis();
					if (newMinTime<=minTime)
						throw new CronExpressionException("Evaluating "+cronExpression+" stuck at "+minTime+"/"+newMinTime+", "+part+"="+val+" (wrap-around)");
					minTime = newMinTime;
					//logger.info("Fail on "+part+": "+val+" vs "+values[pi]+"; advance/wrap to "+minTime);
					continue recheck;
				}
				else {
					//logger.info("ok: "+part+" "+val+" in "+values[pi]);
				}
			}
			//logger.info("Match "+minTime);
			// got one!
			return minTime;
		}
		// no deal
		return 0;
	}
	/** get game time options for cron / time 
	 * @throws CronExpressionException */
	public static GameTimeOptions getGameTimeOptions(String cronExpression, long minTime) throws CronExpressionException {
		GameTimeOptions gto = new GameTimeOptions();
		// parse expression
		String parts[] = getParts(cronExpression);
		TreeSet values[] = getValues(parts);
		// iteratively increase minTime until all constraints are satisified or maxTime reached
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Z"));
		// next second?!
		cal.setTimeInMillis(minTime);
		gto.setDayOfMonth(getGameTimeOption(cal, values, Parts.DAY_OF_MONTH));
		gto.setDayOfWeek(getGameTimeOption(cal, values, Parts.DAY_OF_WEEK));
		gto.setHour(getGameTimeOption(cal, values, Parts.HOURS));
		gto.setMinute(getGameTimeOption(cal, values, Parts.MINUTES));
		gto.setMonth(getGameTimeOption(cal, values, Parts.MONTH));
		gto.setSecond(getGameTimeOption(cal, values, Parts.SECONDS));
		gto.setYear(getGameTimeOption(cal, values, Parts.YEAR));
		return gto;
	}
	/**
	 * @param cal
	 * @param values
	 * @param dayOfMonth
	 * @return
	 */
	private static GameTimeOption getGameTimeOption(Calendar cal, TreeSet values[], Parts part) {
		GameTimeOption gto = new GameTimeOption();
		gto.setInitialValue(part.getValue(cal));
		int vs[] = new int[values[part.ordinal()].size()];
		Iterator<Integer> vi = values[part.ordinal()].iterator();
		for (int i=0; i<vs.length && vi.hasNext(); i++)
			vs[i] = vi.next();
		gto.setOptions(vs);
		return gto;
	}
	@SuppressWarnings("unchecked")
	public static String getTimeOptionsJson(String cronExpression) throws CronExpressionException {
		// parse expression
		String parts[] = getParts(cronExpression);
		TreeSet values[] = getValues(parts);
		StringWriter sw = new StringWriter();
		JSONWriter jw = new JSONWriter(sw);
	
		try {
			jw.array();
			for (int i=0; i<values.length; i++) {
				jw.array();
				for (Object o : values[i])
					jw.value(o);
				jw.endArray();
			}
			jw.endArray();
		} catch (JSONException e) {
			logger.warning("getTimeOptionsJson("+cronExpression+"): "+e);				
			throw new CronExpressionException("getTimeOptionsJson("+cronExpression+"): "+e);
		}
		return sw.toString();
	}
	/** get TreeSet[] of values for use with getNextCronTime 
	 * @throws org.json.JSONException */
	public static TreeSet[] parseTimeOptionsJson(String timeOptionsJson) throws org.json.JSONException {
		JSONArray array = new JSONArray(timeOptionsJson);
		TreeSet values[] = new TreeSet[array.length()];
		for (int i=0; i<array.length(); i++) {
			TreeSet<Integer> ts = new TreeSet<Integer>();
			values[i] = ts;
			JSONArray va = array.getJSONArray(i);
			for (int j=0; j<va.length(); j++)
				ts.add(va.getInt(j));
		}
		return values;
	}
}
