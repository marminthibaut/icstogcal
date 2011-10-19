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

/* INSTRUCTION: This is a command line application. So please execute this template with the following arguments:

 arg[0] = username
 arg[1] = password
 */

/**
 * @author (Your Name Here)
 *
 */

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.*;

import java.io.FileInputStream;

/**
 * This is a test template
 */

public class Calendar {

	public static void main(String[] args) {
		try {
			FileInputStream file = new FileInputStream("cal.ics");
			CalendarBuilder builder = new CalendarBuilder();
			net.fortuna.ical4j.model.Calendar calendar = builder.build(file);
			ComponentList CL = calendar.getComponents();
			for (int i = 0; i < CL.size(); ++i) {
				Component c = (Component) CL.get(i);
				
				if(c.getName().equals("VEVENT")){
					System.out.println("Nouvel event : ");
					System.out.println("Name : "+c.getProperty("SUMMARY").getValue());
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e);
		}

	}
}
