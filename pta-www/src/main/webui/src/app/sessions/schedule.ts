import { minutesOf } from './calendar';
import { Session } from './session.model';

/** Hours shown on the time axis: [startHour, endHour). */
export interface Axis {
  startHour: number;
  endHour: number;
}

/** Usual opening hours of the agency; extended when sessions start earlier or end later. */
export const DEFAULT_AXIS: Axis = { startHour: 7, endHour: 21 };

/** A session placed on a day column, in minutes from the start of the axis. */
export interface Placed {
  session: Session;
  /** Minutes from the axis start to the session start. */
  top: number;
  /** Visual height in minutes: the duration, at least the minimum readable height. */
  height: number;
  /** Side-by-side position among overlapping sessions, from 0. */
  lane: number;
  /** Number of side-by-side lanes in the group of overlapping sessions. */
  lanes: number;
}

/** The axis covering the default hours and every session. */
export function axisFor(sessions: Session[], base: Axis = DEFAULT_AXIS): Axis {
  let { startHour, endHour } = base;
  for (const s of sessions) {
    startHour = Math.min(startHour, Math.floor(minutesOf(s.start) / 60));
    endHour = Math.max(endHour, Math.ceil(minutesOf(s.stop) / 60));
  }
  return { startHour, endHour: Math.min(24, Math.max(endHour, startHour + 1)) };
}

/** Hours labelled on the axis. */
export function hoursOf(axis: Axis): number[] {
  return Array.from({ length: axis.endHour - axis.startHour }, (_, i) => axis.startHour + i);
}

/**
 * Places the sessions of one day on the axis. Sessions which overlap visually share the column side by side
 * (each in its lane); a short session still gets `minHeight` minutes of room so its text stays readable,
 * and that room counts when deciding what overlaps.
 */
export function layoutDay(sessions: Session[], axis: Axis, minHeight: number): Placed[] {
  const origin = axis.startHour * 60;
  const items = sessions
    .map(session => {
      const start = minutesOf(session.start) - origin;
      const duration = Math.max(0, minutesOf(session.stop) - minutesOf(session.start));
      return { session, top: start, height: Math.max(duration, minHeight) };
    })
    .sort((a, b) => a.top - b.top || b.height - a.height);

  const placed: Placed[] = [];
  let group: { item: (typeof items)[number]; lane: number }[] = [];
  let laneEnds: number[] = [];
  let groupEnd = -Infinity;

  const closeGroup = () => {
    for (const { item, lane } of group) placed.push({ ...item, lane, lanes: laneEnds.length });
    group = [];
    laneEnds = [];
  };

  for (const item of items) {
    if (item.top >= groupEnd) {
      closeGroup();
    }
    let lane = laneEnds.findIndex(end => end <= item.top);
    if (lane === -1) {
      lane = laneEnds.length;
      laneEnds.push(0);
    }
    laneEnds[lane] = item.top + item.height;
    group.push({ item, lane });
    groupEnd = Math.max(groupEnd, item.top + item.height);
  }
  closeGroup();
  return placed;
}

/** A session is over once it has started: it can no longer be booked or cancelled. */
export function hasStarted(session: Session, today: string, nowMinutes: number): boolean {
  return session.day < today || (session.day === today && minutesOf(session.start) <= nowMinutes);
}
