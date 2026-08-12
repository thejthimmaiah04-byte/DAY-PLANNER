// ARC Day Planner — Google Apps Script backend
// Deploy as: Web app | Execute as: Me | Access: Anyone
//
// IMPORTANT: After pasting this new version, go to
//   Deploy → Manage deployments → edit → New version → Deploy
// Data is now split across cells A1–A4 to avoid the 50 KB cell limit.
// A1 = tasks, A2 = vision, A3 = research, A4 = meta (prefs + archive)

const SHEET_NAME = 'ARC_Data';

function getSheet() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  return ss.getSheetByName(SHEET_NAME) || ss.insertSheet(SHEET_NAME);
}

function readCell(sheet, cell) {
  try { return JSON.parse(sheet.getRange(cell).getValue() || 'null') || null; } catch { return null; }
}

function doGet(e) {
  if (e && e.parameter && e.parameter.action === 'calendar') return getCalendarEvents(e);
  const s = getSheet();
  return json({
    tasks:    readCell(s, 'A1') || [],
    vision:   readCell(s, 'A2') || {},
    research: readCell(s, 'A3') || [],
    meta:     readCell(s, 'A4') || {}
  });
}

function doPost(e) {
  const d = JSON.parse(e.postData.contents);
  const s = getSheet();
  if (d.tasks    !== undefined) s.getRange('A1').setValue(JSON.stringify(d.tasks));
  if (d.vision   !== undefined) s.getRange('A2').setValue(JSON.stringify(d.vision));
  if (d.research !== undefined) s.getRange('A3').setValue(JSON.stringify(d.research));
  if (d.meta     !== undefined) s.getRange('A4').setValue(JSON.stringify(d.meta));
  return json({ ok: true, syncedAt: new Date().toISOString() });
}

function getCalendarEvents(e) {
  const email = (e.parameter.email || '').trim();
  const days = Math.min(parseInt(e.parameter.days || '14'), 60);
  try {
    const cal = email ? CalendarApp.getCalendarById(email) : CalendarApp.getDefaultCalendar();
    if (!cal) return json({ error: 'Calendar not found. Make sure the email is correct.' });
    const now = new Date();
    const end = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);
    const events = cal.getEvents(now, end);
    const result = events.map(ev => ({
      id: ev.getId(), title: ev.getTitle(), description: ev.getDescription() || '',
      start: ev.getStartTime().toISOString(), end: ev.getEndTime().toISOString(),
      allDay: ev.isAllDayEvent(), location: ev.getLocation() || ''
    }));
    return json({ events: result });
  } catch (err) { return json({ error: err.toString() }); }
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
