import java.util.ArrayList;

/**
 * 
 * @author pringles
 * 
 */
public class Reminder {
	
	/**
	 * 
	 * @param rootFolder
	 * @param loggedUsername
	 */
	public Reminder() {

		// Empty
	}
	
	
	public static String BuildReminderEditorHTML (String loggedUsername, ArrayList<ReminderInfo> reminders) {
		
		
		return null;
	}
	
	/**
	 * 
	 * @return
	 */
	public static String BuildReminderHTML (String loggedUsername, ArrayList<ReminderInfo> reminders) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<center><h1>Reminders</h1>");	
		sb.append("<a href=\"reminder_editor.html\">Reminder Editor</a><br>");
		sb.append("<a href=\"main.html\">Main</a><br><br><br>");
		sb.append("Current logged Username: <b>" + loggedUsername + "</b><br><br>");
		sb.append("<table border=\"1\">");
		sb.append("<tr>");
		sb.append("<th>ID</th>");
		sb.append("<th>Reminder Title</th>");
		sb.append("<th>The date and time of creation</th>");
		sb.append("<th>The date and time of reminding</th>");
		sb.append("<th>Edit</th>");
		sb.append("<th>Delete</th>");
		sb.append("</tr>");
		
		for (ReminderInfo reminderInfo : reminders) {
			sb.append("<tr>");
			sb.append("<td>" + reminderInfo.getReminderId() + "</td>");
			sb.append("<td>" + reminderInfo.getReminderTitle() + "</td>");
			sb.append("<td>" + reminderInfo.getReminderCreationDate() + "</td>");
			sb.append("<td>" + reminderInfo.getReminderSendingDate() + "</td>");
			sb.append("<td><a href=\"/reminder_editor.html?id=" + reminderInfo.getReminderId() + "\">edit</a></td>");
			sb.append("<td><a href=\"/reminder_delete?id=" + reminderInfo.getReminderId() + "\">delete</a></td>");
			sb.append("</tr>");
		}
		sb.append("</table></center>");
		sb.append("</body>");
		sb.append("</html>");
		return sb.toString();
	}
}
