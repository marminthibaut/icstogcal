/**
 * This file is part of icstogcal.
 *
 * icstogcal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * icstogcal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with icstogcal.  If not, see <http://www.gnu.org/licenses/>.
 */
package cal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gdata.client.batch.BatchInterruptedException;
import com.google.gdata.client.calendar.CalendarService;
import com.google.gdata.data.DateTime;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.calendar.CalendarEventEntry;
import com.google.gdata.data.calendar.CalendarEventFeed;
import com.google.gdata.data.extensions.When;
import com.google.gdata.data.extensions.Where;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import com.google.gdata.data.batch.*;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;

/**
 * Main class of the program
 * 
 * @author Thibaut Marmin
 * 
 */
public class googleCalendar extends Calendar {

	private static final long serialVersionUID = 175979019207340368L;
	String gcalID;
	String user;
	String password;

	CalendarService service;

	/**
	 * Constructor of a googleCalendar
	 * 
	 * @param c
	 *            a calendar object from ical4j library
	 * @param gcalID
	 *            ID of the google calendar
	 * @param user
	 *            Google user
	 * @param password
	 *            Google password
	 * @param attempts
	 *            Number of attempts when connection error (TODO)
	 * @throws ParseException
	 *             when a bad ics file is given
	 * @throws IOException
	 *             when the file is not accessible
	 * @throws URISyntaxException
	 * @throws AuthenticationException
	 *             when google API not responding or when user and/or password
	 *             is wrong
	 */

	public googleCalendar(Calendar c, String gcalID, String user,
			String password, int attempts) throws ParseException, IOException,
			URISyntaxException, AuthenticationException {
		super(c);
		this.gcalID = gcalID;
		this.user = user;
		this.password = password;
		this.googleConnect();
	}

	/**
	 * Main method that run the process.
	 */

	public void proceed() {
		System.out.println("Remove all old events (purge calendar)...");
		try {
			this.purge();
		} catch (Exception e) {
			System.err.println("Error while removing old events.\nEXCEPTION : "
					+ e.getMessage());
		}
		System.out.println("End of purge.\nBegin add processing...");

		ArrayList<Component> events = this.getEvents();
		CalendarEventFeed batchRequest = new CalendarEventFeed();

		for (Iterator<Component> i = events.iterator(); i.hasNext();) {
			Component event = i.next();
			try {
				CalendarEventEntry entry = this.convertEvent(event);

				BatchUtils.setBatchId(entry, entry.getId());
				BatchUtils.setBatchOperationType(entry,
						BatchOperationType.INSERT);
				batchRequest.getEntries().add(entry);

				// this.service.insert(this.getPostUrl(), entry);
				System.out.println("Added new event in queue for insert ("
						+ entry.getTitle().getPlainText() + ")");

			} catch (Exception e) {
				System.err.println("Error while converting the event "
						+ event.getProperty("UID").getValue()
						+ "\n EXCEPTION : " + e.getMessage());
			}

		}

		System.out.println("Sending batch of " + batchRequest.getEntries().size()
				+ " instructions to google API...");
		this.send(batchRequest);

	}

	private void purge() throws MalformedURLException, IOException,
			ServiceException {
		CalendarEventFeed feed = this.service.getFeed(this.getFeedUrl(),
				CalendarEventFeed.class);
		List<CalendarEventEntry> events = feed.getEntries();

		System.out.println(events.size());

		for (int i = 0; i < events.size(); ++i) {
			CalendarEventEntry event = events.get(i);
			System.out.println("Added new event in queue for delete ("
					+ event.getTitle().getPlainText() + ")");

			BatchUtils.setBatchId(event, event.getId());
			BatchUtils.setBatchOperationType(event, BatchOperationType.DELETE);
		}

		System.out.println("Sending batch of " + feed.getEntries().size()
				+ " instructions to google API...");
		this.send(feed);
	}

	/**
	 * Send the batch (list of operations) in one request.
	 * 
	 * @param batchRequest
	 */

	private void send(CalendarEventFeed batchRequest) {
		try {
			// Get the URL to make batch requests to
			CalendarEventFeed feed = null;

			feed = this.service.getFeed(this.getFeedUrl(),
					CalendarEventFeed.class);

			Link batchLink = feed.getLink(Link.Rel.FEED_BATCH, Link.Type.ATOM);
			URL batchUrl;
			batchUrl = new URL(batchLink.getHref());

			// Submit the batch request
			CalendarEventFeed batchResponse = null;
			batchResponse = service.batch(batchUrl, batchRequest);

			// Ensure that all the operations were successful.
			boolean isSuccess = true;
			for (CalendarEventEntry entry : batchResponse.getEntries()) {
				String batchId = BatchUtils.getBatchId(entry);
				if (!BatchUtils.isSuccess(entry)) {
					isSuccess = false;
					BatchStatus status = BatchUtils.getBatchStatus(entry);
					System.out.println("\n" + batchId + " failed ("
							+ status.getReason() + ") " + status.getContent());
				}
			}
			if (isSuccess) {
				System.out.println("Successfully add all events.");
			}

		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BatchInterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Convert a Component to a CalendarEventEntry to Google API.
	 * 
	 * @param event
	 *            from ical4j API
	 * @return CalendarEventEntry to Google API
	 * @throws Exception
	 */

	private CalendarEventEntry convertEvent(Component event) throws Exception {
		PlainTextConstruct title = new PlainTextConstruct(event.getProperty(
				"SUMMARY").getValue());
		PlainTextConstruct description = new PlainTextConstruct(event
				.getProperty("DESCRIPTION").getValue());
		Where location = new Where("", "", event.getProperty("LOCATION")
				.getValue());
		DateTime startTime = this.convertTime(event.getProperty("DTSTART")
				.getValue());
		DateTime endTime = this.convertTime(event.getProperty("DTEND")
				.getValue());
		PlainTextConstruct uid = new PlainTextConstruct(event
				.getProperty("UID").getValue());

		CalendarEventEntry entry = new CalendarEventEntry();
		entry.setTitle(title);
		entry.setContent(description);
		entry.addLocation(location);
		When eventTimes = new When();
		eventTimes.setStartTime(startTime);
		eventTimes.setEndTime(endTime);
		entry.addTime(eventTimes);

		return entry;
	}

	/**
	 * Convert a String time formatted AAAAMMDDTHHMMSS to AAAA-MM-DDTHH:MM:SS
	 * and return a DateTime Object.
	 * 
	 * @param time
	 * @return
	 */
	private DateTime convertTime(String time) {
		// En entrée une string du type "20110912T080000"
		// A convertir en "2011-09-12T08:00:00"
		int i = 0;
		String t = "";
		while (i < 15) {
			t += time.toCharArray()[i];
			switch (i) {
			case 3:
			case 5:
				t += '-';
				break;
			case 10:
			case 12:
				t += ':';
			default:
				break;
			}
			++i;
		}
		return DateTime.parseDateTime(t);
	}

	/**
	 * Return true if the googleCalendar Object is initialized.
	 * 
	 * @return
	 */

	private boolean initialized() {
		return this.gcalID != null && this.user != null
				&& this.password != null && this.service != null;
	}

	/**
	 * Connection to the google account.
	 * 
	 * @throws AuthenticationException
	 *             if network not responding, or if authentication failed.
	 */
	private void googleConnect() throws AuthenticationException {
		this.service = new CalendarService("icstogcal");
		this.service.setUserCredentials(this.user, this.password);
	}

	/**
	 * Generate the feedUrl from gcalID
	 * 
	 * @return an URL object
	 * @throws MalformedURLException
	 */

	private URL getFeedUrl() throws MalformedURLException {
		return new URL("https://www.google.com/calendar/feeds/" + this.gcalID
				+ "/private/full?max-results=999999999");
	}

	/**
	 * Generate the postUrl from de gcalID
	 * 
	 * @return an URL object
	 * @throws MalformedURLException
	 */

	private URL getPostUrl() throws MalformedURLException {
		return new URL("https://www.google.com/calendar/feeds/" + this.gcalID
				+ "/private/full");
	}

	/**
	 * @return a list of events (filter)
	 */
	private ArrayList<Component> getEvents() {
		ArrayList<Component> events = new ArrayList<Component>();
		for (Iterator i = this.getComponents().iterator(); i.hasNext();) {
			Component component = (Component) i.next();
			if (component.getName().equals("VEVENT"))
				events.add(component);
		}
		return events;
	}
}
