export const STATUSES = ['office', 'home', 'leave', 'none'];

const STATUS_MAP = {
    OFFICE: 'office',
    HOME_OFFICE: 'home',
    LEAVE: 'leave',
    NOT_CHECKED_IN: 'none',
};

export const MAX_DAYS_FROM_TODAY = 365;

const pad = (n) => String(n).padStart(2, '0');

export const toIsoDate = (date) =>
    `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

export const todayIso = () => toIsoDate(new Date());

export const shiftIsoDate = (iso, days) => {
    const [y, m, d] = iso.split('-').map(Number);
    return toIsoDate(new Date(y, m - 1, d + days));
};

export const dateLimits = () => ({
    min: shiftIsoDate(todayIso(), -MAX_DAYS_FROM_TODAY),
    max: shiftIsoDate(todayIso(), MAX_DAYS_FROM_TODAY),
});

export const formatTime = (isoDateTime) => {
    if (!isoDateTime) return null;
    const match = /T(\d{2}):(\d{2})/.exec(isoDateTime);
    return match ? `${match[1]}:${match[2]}` : null;
};

export const initials = (name) =>
    name.split(' ').filter(Boolean).map(w => w[0]).slice(0, 2).join('');

export const percent = (value, total) => (total ? Math.round((value / total) * 100) : 0);

const normalizeMember = (member) => ({
    id: member.userId,
    name: member.fullName || '—',
    status: STATUS_MAP[member.status] || 'none',
    currentlyWorking: Boolean(member.currentlyWorking),
    startTime: formatTime(member.firstStartTime),
    leaveType: member.leaveType || null,
});

export const normalizeDashboard = (data) => {
    const members = Array.isArray(data?.members) ? data.members.map(normalizeMember) : [];
    const s = data?.summary;
    const counts = s
        ? { office: s.office ?? 0, home: s.homeOffice ?? 0, leave: s.onLeave ?? 0, none: s.notCheckedIn ?? 0 }
        : countByStatus(members);
    return {
        date: data?.date || null,
        total: s?.total ?? members.length,
        counts,
        members,
        workingNow: members.filter(m => m.currentlyWorking).length,
    };
};

export const countByStatus = (people) => {
    const counts = { office: 0, home: 0, leave: 0, none: 0 };
    people.forEach(person => { counts[person.status] += 1; });
    return counts;
};
