// ARC Day Planner — Google Apps Script backend
// Deploy as: Web app | Execute as: Me | Access: Anyone
// Paste the deployment URL into web/index.html as SHEET_URL

const SHEET_NAME = 'ARC_Data';
const DATA_CELL  = 'A1';

function getSheet() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  return ss.getSheetByName(SHEET_NAME) || ss.insertSheet(SHEET_NAME);
}

function doGet(e) {
  if (e && e.parameter && e.parameter.action === 'calendar') {
    return getCalendarEvents(e);
  }
  const raw = getSheet().getRange(DATA_CELL).getValue();
  const out = raw || '{"tasks":[],"vision":{},"research":[],"prefs":{},"archive":[]}';
  return ContentService.createTextOutput(out).setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  getSheet().getRange(DATA_CELL).setValue(e.postData.contents);
  return ContentService.createTextOutput('{"ok":true}').setMimeType(ContentService.MimeType.JSON);
}

function getCalendarEvents(e) {
  const email = (e.parameter.email || '').trim();
  const days  = Math.min(parseInt(e.parameter.days || '14'), 60);
  try {
    const cal = email
      ? CalendarApp.getCalendarById(email)
      : CalendarApp.getDefaultCalendar();
    if (!cal) {
      return json({ error: 'Calendar not found. Make sure the email is correct and the calendar is shared with you.' });
    }
    const now = new Date();
    const end = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);
    const events = cal.getEvents(now, end);
    const result = events.map(ev => ({
      id:          ev.getId(),
      title:       ev.getTitle(),
      description: ev.getDescription() || '',
      start:       ev.getStartTime().toISOString(),
      end:         ev.getEndTime().toISOString(),
      allDay:      ev.isAllDayEvent(),
      location:    ev.getLocation() || ''
    }));
    return json({ events: result });
  } catch (err) {
    return json({ error: err.toString() });
  }
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
