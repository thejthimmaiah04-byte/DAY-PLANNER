// ARC Day Planner — Google Apps Script backend
// Deploy as: Web app | Execute as: Me | Access: Anyone
//
// After pasting this version:
//   Deploy → Manage deployments → Edit → New version → Deploy
//
// Sheet layout:
//   ARC_Data        — raw JSON for app reads (A1=tasks, A2=vision, A3=research, A4=meta)
//   Tasks           — all tasks, human-readable table
//   Work            — Work category tasks
//   Personal        — Personal category tasks
//   2-Year Vision   — vision statement + milestones
//   Skill Building  — skill/learning tasks
//   Research: [name] — one sheet per research project (auto-created)

const DATA_SHEET = 'ARC_Data';
const ORANGE     = '#FF5500';
const DARK_HDR   = '#1A1A1A';
const WHITE      = '#FFFFFF';
const DONE_CLR   = '#888888';

/* ── Helpers ─────────────────────────────────────────────────────────────── */

function ss() { return SpreadsheetApp.getActiveSpreadsheet(); }

function getOrCreate(name) {
  return ss().getSheetByName(name) || ss().insertSheet(name);
}

function readCell(sheet, cell) {
  try { return JSON.parse(sheet.getRange(cell).getValue() || 'null') || null; } catch(e) { return null; }
}

function safeSheetName(name) {
  // Google Sheets sheet names: max 100 chars, no [ ] * ? : / \
  return String(name).replace(/[\[\]\*\?:\/\\]/g, '-').trim().substring(0, 100);
}

function hdr(range, labels, bg) {
  range.setValues([labels]);
  range.setBackground(bg || ORANGE);
  range.setFontColor(WHITE);
  range.setFontWeight('bold');
  range.setFontSize(10);
}

function clearAndFreeze(sheet, freezeRow) {
  sheet.clearContents();
  sheet.clearFormats();
  try { if (freezeRow) sheet.setFrozenRows(freezeRow); } catch(e) {}
}

/* ── API entry points ────────────────────────────────────────────────────── */

function doGet(e) {
  if (e && e.parameter && e.parameter.action === 'calendar') return getCalendarEvents(e);
  const s = getOrCreate(DATA_SHEET);
  return json({
    tasks:    readCell(s, 'A1') || [],
    vision:   readCell(s, 'A2') || {},
    research: readCell(s, 'A3') || [],
    meta:     readCell(s, 'A4') || {}
  });
}

function doPost(e) {
  const d = JSON.parse(e.postData.contents);
  const s = getOrCreate(DATA_SHEET);

  // ── Raw JSON for app reads (unchanged format) ─────────────────────────
  if (d.tasks    !== undefined) s.getRange('A1').setValue(JSON.stringify(d.tasks));
  if (d.vision   !== undefined) s.getRange('A2').setValue(JSON.stringify(d.vision));
  if (d.research !== undefined) s.getRange('A3').setValue(JSON.stringify(d.research));
  if (d.meta     !== undefined) s.getRange('A4').setValue(JSON.stringify(d.meta));

  // ── Human-readable sheets ─────────────────────────────────────────────
  if (d.tasks)    writeTaskSheets(d.tasks);
  if (d.vision)   writeVisionSheet(d.vision);
  if (d.research) writeResearchSheets(d.research);

  return json({ ok: true, syncedAt: new Date().toISOString() });
}

/* ── Task sheets ─────────────────────────────────────────────────────────── */

function writeTaskSheets(tasks) {
  const COLS = ['Title', 'Priority', 'Due Date', 'Status', 'Time', 'Duration (min)', 'Repeat', 'Notes'];

  const groups = {
    'Tasks':         tasks,
    'Work':          tasks.filter(t => t.category === 'WORK'),
    'Personal':      tasks.filter(t => t.category === 'PERSONAL'),
    'Skill Building':tasks.filter(t => t.category === 'SKILLS' || t.category === 'SKILL'),
  };

  Object.keys(groups).forEach(name => {
    const list = groups[name];
    const sheet = getOrCreate(name);
    clearAndFreeze(sheet, 1);

    hdr(sheet.getRange(1, 1, 1, COLS.length), COLS);

    if (!list.length) {
      sheet.getRange(2, 1).setValue('No tasks in this category yet.');
      sheet.getRange(2, 1).setFontColor(DONE_CLR).setFontStyle('italic');
    } else {
      const pending = list.filter(t => !t.done);
      const done    = list.filter(t =>  t.done);
      const ordered = [...pending, ...done];

      const rows = ordered.map(t => [
        t.title    || '',
        t.priority || '',
        t.due      || '',
        t.done ? 'Done' : t.inProgress ? 'In Progress' : 'Pending',
        t.fixedTime || t.time || '',
        t.estimate  || t.duration || '',
        t.repeat    || '',
        t.notes     || ''
      ]);
      sheet.getRange(2, 1, rows.length, COLS.length).setValues(rows);

      // Grey out done rows
      ordered.forEach((t, i) => {
        if (t.done) {
          sheet.getRange(i + 2, 1, 1, COLS.length)
               .setFontColor(DONE_CLR)
               .setFontStyle('italic');
        }
      });

      // Priority colour in column B
      ordered.forEach((t, i) => {
        const cell = sheet.getRange(i + 2, 2);
        if      (t.priority === 'HIGH')   cell.setBackground('#FF5500').setFontColor(WHITE);
        else if (t.priority === 'MEDIUM') cell.setBackground('#F5A623').setFontColor(WHITE);
        else if (t.priority === 'LOW')    cell.setBackground('#444444').setFontColor(WHITE);
      });
    }

    try { sheet.autoResizeColumns(1, COLS.length); } catch(e) {}
  });
}

/* ── Vision sheet ────────────────────────────────────────────────────────── */

function writeVisionSheet(vision) {
  const sheet = getOrCreate('2-Year Vision');
  clearAndFreeze(sheet, 1);

  // Title banner
  const banner = sheet.getRange('A1:D1');
  banner.merge();
  banner.setValue('2-YEAR VISION');
  banner.setBackground(ORANGE).setFontColor(WHITE).setFontWeight('bold').setFontSize(13);

  sheet.getRange('A2').setValue('Vision Statement').setFontWeight('bold');
  sheet.getRange('B2:D2').merge().setValue(vision.statement || '—');

  sheet.getRange('A3').setValue('Target Date').setFontWeight('bold');
  sheet.getRange('B3').setValue(vision.target || '—');

  // Milestones header
  const milestones = Array.isArray(vision.milestones) ? vision.milestones : [];
  hdr(sheet.getRange('A5:E5'), ['Milestone', 'Target Month', 'Tasks Done', 'Progress %', 'Status'], DARK_HDR);

  if (milestones.length) {
    const rows = milestones.map(ms => {
      const msTasks   = Array.isArray(ms.tasks) ? ms.tasks : [];
      const total     = msTasks.length;
      const done      = msTasks.filter(t => t.done).length;
      const pct       = total ? Math.round(done / total * 100) : 0;
      const status    = pct === 100 ? '✓ Complete' : pct > 50 ? 'In Progress' : total ? 'Starting' : 'No tasks yet';
      return [ms.title || '', ms.target || '', `${done} / ${total}`, pct + '%', status];
    });
    sheet.getRange(6, 1, rows.length, 5).setValues(rows);

    // Colour status column
    milestones.forEach((ms, i) => {
      const msT  = Array.isArray(ms.tasks) ? ms.tasks : [];
      const pct  = msT.length ? Math.round(msT.filter(t=>t.done).length / msT.length * 100) : 0;
      const cell = sheet.getRange(i + 6, 5);
      if      (pct === 100) cell.setBackground('#1A7340').setFontColor(WHITE);
      else if (pct  >  50)  cell.setBackground('#F5A623').setFontColor(WHITE);
      else if (msT.length)  cell.setBackground('#C0392B').setFontColor(WHITE);
    });
  } else {
    sheet.getRange('A6').setValue('No milestones added yet.').setFontColor(DONE_CLR).setFontStyle('italic');
  }

  try { sheet.autoResizeColumns(1, 5); } catch(e) {}
}

/* ── Research sheets ─────────────────────────────────────────────────────── */

function writeResearchSheets(research) {
  if (!Array.isArray(research)) return;

  research.forEach(proj => {
    const sheetName = safeSheetName('Research: ' + (proj.title || 'Untitled'));
    const sheet     = getOrCreate(sheetName);
    clearAndFreeze(sheet, 0);

    let row = 1;

    // Project title banner
    const titleCell = sheet.getRange(row, 1, 1, 3);
    titleCell.merge();
    titleCell.setValue(proj.title || 'Untitled Project');
    titleCell.setBackground(ORANGE).setFontColor(WHITE).setFontWeight('bold').setFontSize(13);
    row += 2;

    // ── Tasks ──────────────────────────────────────────────────────────
    hdr(sheet.getRange(row, 1, 1, 2), ['TASKS', 'STATUS'], DARK_HDR);
    row++;

    const tasks = Array.isArray(proj.tasks) ? proj.tasks : [];
    if (tasks.length) {
      const tRows = tasks.map(t => [t.title || '', t.done ? '✓ Done' : '○ Pending']);
      sheet.getRange(row, 1, tRows.length, 2).setValues(tRows);
      tasks.forEach((t, i) => {
        const r = sheet.getRange(row + i, 1, 1, 2);
        if (t.done) { r.setFontColor(DONE_CLR).setFontStyle('italic'); }
        sheet.getRange(row + i, 2).setHorizontalAlignment('center');
      });
      row += tRows.length;
    } else {
      sheet.getRange(row, 1).setValue('No tasks yet').setFontColor(DONE_CLR).setFontStyle('italic');
      row++;
    }

    row++; // blank spacer

    // ── Targets ────────────────────────────────────────────────────────
    hdr(sheet.getRange(row, 1, 1, 3), ['TARGETS', 'STATUS', 'TARGET DATE'], DARK_HDR);
    row++;

    const targets = Array.isArray(proj.targets) ? proj.targets : [];
    if (targets.length) {
      const tgRows = targets.map(t => [t.title || '', t.done ? '✓ Done' : '○ Pending', t.targetDate || '']);
      sheet.getRange(row, 1, tgRows.length, 3).setValues(tgRows);
      targets.forEach((t, i) => {
        if (t.done) sheet.getRange(row + i, 1, 1, 3).setFontColor(DONE_CLR).setFontStyle('italic');
        sheet.getRange(row + i, 2).setHorizontalAlignment('center');
        if (t.targetDate) sheet.getRange(row + i, 3).setFontColor(ORANGE).setFontWeight('bold');
      });
      row += tgRows.length;
    } else {
      sheet.getRange(row, 1).setValue('No targets yet').setFontColor(DONE_CLR).setFontStyle('italic');
      row++;
    }

    row++;

    // ── Notes ──────────────────────────────────────────────────────────
    hdr(sheet.getRange(row, 1, 1, 3), ['NOTES', '', ''], DARK_HDR);
    row++;
    const notesCell = sheet.getRange(row, 1, 1, 3);
    notesCell.merge();
    notesCell.setValue(proj.notes || '');
    notesCell.setWrap(true);
    row += 3;

    // Progress summary in top-right
    const total = tasks.length, done = tasks.filter(t=>t.done).length;
    const pct   = total ? Math.round(done / total * 100) : 0;
    const summaryCell = sheet.getRange(1, 5, 1, 2);
    summaryCell.merge();
    summaryCell.setValue(`Tasks: ${done}/${total}  (${pct}%)`);
    summaryCell.setBackground(pct === 100 ? '#1A7340' : pct > 0 ? '#F5A623' : DARK_HDR);
    summaryCell.setFontColor(WHITE).setFontWeight('bold').setHorizontalAlignment('center');

    try { sheet.setColumnWidth(1, 260); sheet.setColumnWidth(2, 110); sheet.setColumnWidth(3, 110); } catch(e) {}
  });
}

/* ── Google Calendar ─────────────────────────────────────────────────────── */

function getCalendarEvents(e) {
  const email = (e.parameter.email || '').trim();
  const days  = Math.min(parseInt(e.parameter.days || '14'), 60);
  try {
    const cal = email ? CalendarApp.getCalendarById(email) : CalendarApp.getDefaultCalendar();
    if (!cal) return json({ error: 'Calendar not found. Check that the email is correct and the calendar is shared.' });
    const now = new Date();
    const end = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);
    const result = cal.getEvents(now, end).map(ev => ({
      id: ev.getId(), title: ev.getTitle(), description: ev.getDescription() || '',
      start: ev.getStartTime().toISOString(), end: ev.getEndTime().toISOString(),
      allDay: ev.isAllDayEvent(), location: ev.getLocation() || ''
    }));
    return json({ events: result });
  } catch(err) { return json({ error: err.toString() }); }
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
