export const ORGANIZATION_ROOT_ID = 'organization';

export const fullName = (node) => `${node?.name || ''} ${node?.surname || ''}`.trim();

export const initials = (node) =>
    `${(node?.name || '').charAt(0)}${(node?.surname || '').charAt(0)}`.toUpperCase() || '?';

const ROLE_PRIORITY = ['ROLE_ADMIN', 'ROLE_HR', 'ROLE_MANAGER', 'ROLE_EMPLOYEE'];

export const primaryRole = (node) =>
    ROLE_PRIORITY.find(role => node?.roles?.includes(role)) || node?.roles?.[0] || null;

export const flattenTree = (root) => {
    const people = [];
    const walk = (node, superior, depth) => {
        if (node.userId !== ORGANIZATION_ROOT_ID) {
            people.push({ node, superior, depth });
        }
        (node.subordinates || []).forEach(child =>
            walk(child, node.userId === ORGANIZATION_ROOT_ID ? null : node, depth + 1));
    };
    if (root) walk(root, null, 0);
    return people;
};

export const matchesQuery = (node, query) => {
    const needle = query.trim().toLocaleLowerCase();
    if (!needle || node.userId === ORGANIZATION_ROOT_ID) return false;
    return [fullName(node), node.email].some(value => (value || '').toLocaleLowerCase().includes(needle));
};

export const findMatches = (root, query) =>
    flattenTree(root).filter(({ node }) => matchesQuery(node, query)).map(({ node }) => node.userId);

export const ancestorsOf = (root, targetIds) => {
    const targets = new Set(targetIds);
    const result = new Set();
    const walk = (node, path) => {
        if (targets.has(node.userId)) path.forEach(id => result.add(id));
        (node.subordinates || []).forEach(child => walk(child, [...path, node.userId]));
    };
    if (root) walk(root, []);
    return result;
};

export const initiallyExpanded = (root, levels = 2) => {
    const expanded = new Set();
    const walk = (node, depth) => {
        if (depth >= levels || !node.subordinates?.length) return;
        expanded.add(node.userId);
        node.subordinates.forEach(child => walk(child, depth + 1));
    };
    if (root) walk(root, 0);
    return expanded;
};

const collectIds = (node, ids = new Set()) => {
    ids.add(node.userId);
    (node.subordinates || []).forEach(child => collectIds(child, ids));
    return ids;
};

export const buildOrganization = (trees, label) => {
    const valid = trees.filter(Boolean);
    const nestedIds = new Set();
    valid.forEach(tree => (tree.subordinates || []).forEach(child => collectIds(child, nestedIds)));

    const seen = new Set();
    const tops = valid.filter(tree => {
        if (nestedIds.has(tree.userId) || seen.has(tree.userId)) return false;
        seen.add(tree.userId);
        return true;
    }).sort((a, b) => fullName(a).localeCompare(fullName(b), 'pl'));

    return {
        userId: ORGANIZATION_ROOT_ID,
        name: label,
        surname: '',
        email: null,
        roles: [],
        active: true,
        directSubordinates: tops.length,
        totalSubordinates: tops.reduce((sum, tree) => sum + 1 + (tree.totalSubordinates || 0), 0),
        subordinates: tops,
    };
};
