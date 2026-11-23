import { addDays, addMonths, dayHeading, daysBetween, isIsoDate, longDay, minutesOf, monthLabel, monthWeeks, nowMinutes, periodLabel, shortDay, startOfMonth, startOfWeek, today, weekDays } from './calendar';

describe('calendar', () => {
  it('formats today as a local ISO date, whatever the time of day', () => {
    expect(today(new Date(2026, 10, 22, 23, 59))).toBe('2026-11-22');
    expect(today(new Date(2026, 0, 1, 0, 0))).toBe('2026-01-01');
  });

  it('adds days across month and year boundaries', () => {
    expect(addDays('2026-11-30', 1)).toBe('2026-12-01');
    expect(addDays('2026-12-29', 7)).toBe('2027-01-05');
    expect(addDays('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('counts the days between two dates, across a daylight saving change', () => {
    expect(daysBetween('2026-10-24', '2026-10-26')).toBe(2);
    expect(daysBetween('2026-11-23', '2026-11-22')).toBe(-1);
  });

  it('accepts only valid ISO dates', () => {
    expect(isIsoDate('2026-11-23')).toBe(true);
    expect(isIsoDate('23.11.2026')).toBe(false);
    expect(isIsoDate('2026-13-45')).toBe(false);
    expect(isIsoDate('2026-02-30')).toBe(false);
    expect(isIsoDate(undefined)).toBe(false);
  });

  it('names today and tomorrow, and the weekday afterwards', () => {
    expect(dayHeading('2026-11-22', '2026-11-22', 'en-GB')).toEqual({ relative: 'Today', date: '22 November' });
    expect(dayHeading('2026-11-23', '2026-11-22', 'en-GB')).toEqual({ relative: 'Tomorrow', date: '23 November' });
    expect(dayHeading('2026-11-26', '2026-11-22', 'en-GB')).toEqual({ relative: 'Thursday', date: '26 November' });
  });

  it('labels a period compactly', () => {
    // spacing around the dash depends on the ICU data of the runtime
    expect(periodLabel('2026-11-23', '2026-11-29', 'en-GB')).toMatch(/^23\s?–\s?29 November 2026$/);
    expect(periodLabel('2026-11-30', '2026-12-06', 'en-GB')).toMatch(/^30 November\s?–\s?6 December 2026$/);
  });
});

describe('weeks', () => {
  it('run from Monday to Sunday', () => {
    // 26 November 2026 is a Thursday
    expect(startOfWeek('2026-11-26')).toBe('2026-11-23');
    expect(startOfWeek('2026-11-23')).toBe('2026-11-23');
    expect(startOfWeek('2026-11-29')).toBe('2026-11-23');
    expect(weekDays('2026-12-31')).toEqual(['2026-12-28', '2026-12-29', '2026-12-30', '2026-12-31', '2027-01-01', '2027-01-02', '2027-01-03']);
  });

  it('read times as minutes of the day', () => {
    expect(minutesOf('18:30:00')).toBe(1110);
    expect(minutesOf('07:05')).toBe(425);
    expect(nowMinutes(new Date(2026, 10, 26, 9, 41))).toBe(581);
  });

  it('name days briefly or fully', () => {
    expect(shortDay('2026-11-26', 'en-GB')).toEqual({ weekday: 'Thu', day: '26' });
    expect(longDay('2026-11-26', 'en-GB')).toBe('Thursday 26 November');
  });
});

describe('months', () => {
  it('start on the first and move by whole months, clamping the day', () => {
    expect(startOfMonth('2026-11-26')).toBe('2026-11-01');
    expect(addMonths('2026-11-26', 1)).toBe('2026-12-26');
    expect(addMonths('2026-12-15', 1)).toBe('2027-01-15');
    expect(addMonths('2027-01-31', 1)).toBe('2027-02-28');
    expect(addMonths('2026-03-31', -1)).toBe('2026-02-28');
  });

  it('are shown as whole weeks from Monday to Sunday', () => {
    // November 2026 starts on a Sunday and ends on a Monday: 6 rows
    const november = monthWeeks('2026-11-18');
    expect(november.length).toBe(6);
    expect(november[0][0]).toBe('2026-10-26');
    expect(november[0][6]).toBe('2026-11-01');
    expect(november[5][0]).toBe('2026-11-30');
    expect(november[5][6]).toBe('2026-12-06');
    // February 2027 starts on a Monday and ends on a Sunday: exactly 4 rows
    const february = monthWeeks('2027-02-10');
    expect(february.length).toBe(4);
    expect(february[0][0]).toBe('2027-02-01');
    expect(february[3][6]).toBe('2027-02-28');
  });

  it('are named with their year', () => {
    expect(monthLabel('2026-11-26', 'en-GB')).toBe('November 2026');
  });
});
