/**
 * Calendar helpers working on ISO local dates (`YYYY-MM-DD`).
 * Dates are handled in the participant's local time zone: never through `toISOString()`, which shifts to UTC.
 */

export const WEEK_LENGTH = 7;

const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

export function isIsoDate(value: string | null | undefined): value is string {
  // Round-trip check: Date silently rolls invalid dates over (2026-13-45 becomes 2027-02-14).
  return !!value && ISO_DATE.test(value) && format(parse(value)) === value;
}

export function today(now: Date = new Date()): string {
  return format(now);
}

export function addDays(isoDate: string, days: number): string {
  const date = parse(isoDate);
  date.setDate(date.getDate() + days);
  return format(date);
}

/** Days between two ISO dates (`to - from`). */
export function daysBetween(from: string, to: string): number {
  const ms = parse(to).getTime() - parse(from).getTime();
  return Math.round(ms / 86_400_000);
}

/** `Today`, `Tomorrow`, or the weekday, followed by the day and month in the participant's locale. */
export function dayHeading(isoDate: string, reference: string = today(), locale?: string): { relative: string; date: string } {
  const date = parse(isoDate);
  const offset = daysBetween(reference, isoDate);
  const weekday = new Intl.DateTimeFormat(locale, { weekday: 'long' }).format(date);
  return {
    relative: offset === 0 ? 'Today' : offset === 1 ? 'Tomorrow' : weekday,
    date: new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'long' }).format(date),
  };
}

/** Monday of the week the date belongs to (weeks run Monday to Sunday). */
export function startOfWeek(isoDate: string): string {
  const mondayBased = (parse(isoDate).getDay() + 6) % 7;
  return addDays(isoDate, -mondayBased);
}

/** The 7 days, Monday to Sunday, of the week the date belongs to. */
export function weekDays(isoDate: string): string[] {
  const monday = startOfWeek(isoDate);
  return Array.from({ length: WEEK_LENGTH }, (_, i) => addDays(monday, i));
}

/** First day of the month the date belongs to. */
export function startOfMonth(isoDate: string): string {
  return `${isoDate.slice(0, 7)}-01`;
}

/** The same day of month `months` later (or earlier), clamped to the length of the target month. */
export function addMonths(isoDate: string, months: number): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  const target = new Date(year, month - 1 + months, 1);
  const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate();
  return format(new Date(target.getFullYear(), target.getMonth(), Math.min(day, lastDay)));
}

/**
 * The weeks (Monday to Sunday) covering the month the date belongs to: 4 to 6 rows of 7 days,
 * starting with the Monday on or before the 1st and ending with the Sunday on or after the last day.
 */
export function monthWeeks(isoDate: string): string[][] {
  const first = startOfMonth(isoDate);
  const last = addDays(addMonths(first, 1), -1);
  const weeks: string[][] = [];
  for (let monday = startOfWeek(first); monday <= last; monday = addDays(monday, 7)) {
    weeks.push(weekDays(monday));
  }
  return weeks;
}

/** e.g. `November 2026` */
export function monthLabel(isoDate: string, locale?: string): string {
  return new Intl.DateTimeFormat(locale, { month: 'long', year: 'numeric' }).format(parse(isoDate));
}

/** `18:30:00` or `18:30` → minutes since midnight */
export function minutesOf(isoTime: string): number {
  const [hours, minutes] = isoTime.split(':').map(Number);
  return hours * 60 + minutes;
}

/** Minutes since midnight of the given moment, in local time. */
export function nowMinutes(now: Date = new Date()): number {
  return now.getHours() * 60 + now.getMinutes();
}

/** e.g. `Mon` and `24` */
export function shortDay(isoDate: string, locale?: string): { weekday: string; day: string } {
  const date = parse(isoDate);
  return {
    weekday: new Intl.DateTimeFormat(locale, { weekday: 'short' }).format(date),
    day: String(date.getDate()),
  };
}

/** e.g. `Thursday 26 November` */
export function longDay(isoDate: string, locale?: string): string {
  return new Intl.DateTimeFormat(locale, { weekday: 'long', day: 'numeric', month: 'long' }).format(parse(isoDate));
}

/** e.g. `23 – 29 November 2026`, or `30 November – 6 December 2026` */
export function periodLabel(from: string, to: string, locale?: string): string {
  return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'long', year: 'numeric' })
    .formatRange(parse(from), parse(to));
}

function parse(isoDate: string): Date {
  const [year, month, day] = isoDate.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function format(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}
