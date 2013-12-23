import java.util.Date;

/**
 * Holds a single reminderInfo
 * @author pringles
 *
 */
public class ReminderInfo {
	
	public ReminderInfo() {
		//Empty
	}
	
	public void setReminderId(String reminderId) {
		this._reminderId = reminderId;
	}

	public void setReminderBody(String reminderBody) {
		this._reminderBody = reminderBody;
	}
	
	public void setReminderTitle(String reminderTitle) {
		this._reminderTitle = reminderTitle;
	}

	public void setReminderCreationDate(Date reminderCreationDate) {
		this._reminderCreationDate = reminderCreationDate;
	}

	public void setReminderSendingDate(Date reminderSendingDate) {
		this._reminderSendingDate = reminderSendingDate;
	}

	public String getReminderId() {
		return _reminderId;
	}

	public String getReminderTitle() {
		return _reminderTitle;
	}

	public Date getReminderCreationDate() {
		return _reminderCreationDate;
	}

	public Date getReminderSendingDate() {
		return _reminderSendingDate;
	}
	
	public String getReminderBody() {
		return _reminderBody;
	}

	private String _reminderId;
	private String _reminderTitle;
	private Date _reminderCreationDate;
	private Date _reminderSendingDate;
	private String _reminderBody;
	
	public ReminderInfo(String reminderId, String reminderName, String reminderTitle,
			Date reminderCreationDate, Date reminderSendingDate, String reminderBody) {
		_reminderId = reminderId;
		_reminderTitle = reminderTitle;
		_reminderCreationDate = reminderCreationDate;
		_reminderSendingDate = reminderSendingDate;
		_reminderBody = reminderBody;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append("ReminderId = " + _reminderId + "\r\n");
		sb.append("ReminderTitle = " + _reminderTitle + "\r\n");
		sb.append("ReminderCreationDate = " + _reminderCreationDate + "\r\n");
		sb.append("ReminderSendingDate = " + _reminderSendingDate + "\r\n");
		return sb.toString();
	}	
}