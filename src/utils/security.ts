/**
 * Pure security calculations matching the Android Kotlin core engine.
 */

export function normalizeDomain(input: string): string {
  let cleaned = input.trim().toLowerCase();
  if (cleaned.startsWith('http://')) cleaned = cleaned.slice(7);
  if (cleaned.startsWith('https://')) cleaned = cleaned.slice(8);
  if (cleaned.startsWith('www.')) cleaned = cleaned.slice(4);
  const slashIdx = cleaned.indexOf('/');
  if (slashIdx !== -1) {
    cleaned = cleaned.substring(0, slashIdx);
  }
  const colonIdx = cleaned.indexOf(':');
  if (colonIdx !== -1) {
    cleaned = cleaned.substring(0, colonIdx);
  }
  return cleaned;
}

export function getCooldownDurationMs(failures: Int): number {
  if (failures < 3) return 0;
  if (failures < 5) return 30_000;         // 30 seconds
  if (failures < 7) return 300_000;        // 5 minutes
  if (failures < 10) return 1_800_000;     // 30 minutes
  return 86_400_000;                       // 24 hours
}

type Int = number;

export function evaluateLockout(failures: number, lastFailedTimestampMs: number, currentTimeMs: number): {
  isLockedOut: boolean;
  remainingSeconds: number;
} {
  if (failures < 3) {
    return { isLockedOut: false, remainingSeconds: 0 };
  }
  const cooldownMs = getCooldownDurationMs(failures);
  const elapsedMs = currentTimeMs - lastFailedTimestampMs;
  if (elapsedMs < cooldownMs) {
    const remainingMs = cooldownMs - elapsedMs;
    return {
      isLockedOut: true,
      remainingSeconds: Math.ceil(remainingMs / 1000),
    };
  }
  return { isLockedOut: false, remainingSeconds: 0 };
}

export function validateNextDnsProfile(hostname: string): {
  isValid: boolean;
  profileId: string | null;
  message: string;
} {
  const trimmed = hostname.trim().toLowerCase();
  if (!trimmed) {
    return { isValid: false, profileId: null, message: 'Hostname cannot be empty.' };
  }
  // Standard format: <profile-id>.dns.nextdns.io
  const match = trimmed.match(/^([a-z0-9]+)\.dns\.nextdns\.io$/);
  if (match) {
    return { isValid: true, profileId: match[1], message: `Valid NextDNS DoT profile: ${match[1]}` };
  }
  if (trimmed.includes('nextdns')) {
    return { isValid: true, profileId: null, message: 'Valid NextDNS domain.' };
  }
  return { isValid: false, profileId: null, message: 'Invalid format. Expected: <profile-id>.dns.nextdns.io' };
}
