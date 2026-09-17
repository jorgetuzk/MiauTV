/**
 * Per-browser convenience: remembers the last folder the user created,
 * added to, or moved something into, so modals can offer a one-click
 * shortcut back to it. Intentionally local-only (localStorage) — this is
 * not meant to sync across devices.
 */
const STORAGE_KEY = 'miautv:last_used_folder';

export interface LastUsedFolder {
    id: number | null; // null means root
    name: string;
}

export function getLastUsedFolder(): LastUsedFolder | null {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) return null;
        const parsed = JSON.parse(raw);
        if (typeof parsed !== 'object' || parsed === null || typeof parsed.name !== 'string') return null;
        return { id: parsed.id ?? null, name: parsed.name };
    } catch {
        return null;
    }
}

export function setLastUsedFolder(folder: LastUsedFolder) {
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(folder));
    } catch {
        // Ignore storage errors (private browsing, quota, etc.) — this is a convenience only.
    }
}
