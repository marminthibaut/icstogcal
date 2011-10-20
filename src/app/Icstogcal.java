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
package app;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import com.google.gdata.util.ServiceException;

import net.fortuna.ical4j.data.CalendarBuilder;

import cal.*;

/**
 * @author Thibaut Marmin
 * 
 *         TODO Replace message method (at the end of this class) to a list of
 *         message variable.
 */
public class Icstogcal {

	/**
	 * Main method
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) {
		String user = null;
		String password = null;
		String gcal = null;
		String filepath = null;
		try {
			try {
				user = args[0];
				password = args[1];
				gcal = args[2];
				filepath = args[3];
			} catch (Exception e) {
				System.err.println(argsError());
				exitProgram(e);
			}

			try {
				System.out.println(welcomText(user, gcal, filepath));
				FileInputStream input = new FileInputStream(filepath);
				System.out.println(filepath + " loaded.");
				CalendarBuilder builder = new CalendarBuilder();
				googleCalendar calendar = new googleCalendar(
						builder.build(input), gcal, user, password, 5);
				System.out
						.println("File parsed and program connected to the google API.");
				calendar.proceed();
			} catch (FileNotFoundException e) {
				System.out.println(fileNoFoundError(filepath));
				throw (e);
			} catch (IOException e) {
				System.out.println(ioError(filepath));
				throw (e);
			} catch (ParseException e) {
				System.out.println(parseError(filepath));
				throw (e);
			} catch (URISyntaxException e) {
				System.out.println(uriSyntaxError(filepath));
				throw (e);
			} catch (Exception e) {
				System.out.println(e);
				throw (e);
			}

		} catch (Exception e) {
			exitProgram(e);
		}

		exitProgram(null);
	}

	/**
	 * Exit method that prompt a message.
	 * 
	 * @param e
	 */

	private static void exitProgram(Exception e) {
		if (null == e) {
			System.out.println("End of execution with no error.");
		} else {
			System.out.println("End of execution WITH ERROR : "
					+ e.getMessage());
		}
		System.exit(0);
	}

	/**
	 * @return
	 */
	private static String AuthenticationError() {
		return "Authentication error. Verify your google username and password.";
	}

	/**
	 * @param filepath
	 * @return
	 */
	private static String uriSyntaxError(String filepath) {
		return "URI syntax not correct.";
	}

	/**
	 * @param filepath
	 * @return
	 */
	private static String ioError(String filepath) {
		return "Read error on file " + filepath + ".";
	}

	/**
	 * @param filepath
	 * @return
	 */
	private static String parseError(String filepath) {
		return "An error has occured while parsing " + filepath
				+ ". It is not correctly written.";
	}

	public static String argsError() {
		String err = "Argument error. The application take four parameters in this order :\n";
		err += "[google_user] [google_password] [google_calendar_id] [path_to_ics]\n";
		err += "Example : toto mypwd idididididid@group.calendar.google.com /home/toto/myics.ics\n";

		return err;
	}

	public static String serviceError(ServiceException e) {
		return "Google service error : " + e.getMessage();
	}

	public static String fileNoFoundError(String filepath) {
		String err = "The file located at " + filepath
				+ " have not been founded.";

		return err;
	}

	public static String welcomText(String user, String agenda, String filepath) {
		String s = "- Lauching icstogcal whith variables :\n";
		s += "  > user    : " + user + "\n";
		s += "  > gcalID  : " + agenda + "\n";
		s += "  > ics file: " + filepath;

		return s;
	}
}
