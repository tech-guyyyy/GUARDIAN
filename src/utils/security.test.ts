import test from 'node:test';
import assert from 'node:assert/strict';
import {
  normalizeDomain,
  getCooldownDurationMs,
  evaluateLockout,
  validateNextDnsProfile
} from './security';

test('normalizeDomain properly strips protocols, www, ports, and subpaths', () => {
  assert.equal(normalizeDomain('https://www.example.com/login?param=1'), 'example.com');
  assert.equal(normalizeDomain('http://bad-site.org/videos/adult/'), 'bad-site.org');
  assert.equal(normalizeDomain('SUBDOMAIN.Domain.COM:8080/path'), 'subdomain.domain.com');
  assert.equal(normalizeDomain('  WWW.TEST.CO.UK  '), 'test.co.uk');
});

test('getCooldownDurationMs matches exponential lockout specification', () => {
  assert.equal(getCooldownDurationMs(0), 0);
  assert.equal(getCooldownDurationMs(2), 0);
  assert.equal(getCooldownDurationMs(3), 30_000);       // 30s
  assert.equal(getCooldownDurationMs(4), 30_000);       // 30s
  assert.equal(getCooldownDurationMs(5), 300_000);      // 5m
  assert.equal(getCooldownDurationMs(6), 300_000);      // 5m
  assert.equal(getCooldownDurationMs(7), 1_800_000);    // 30m
  assert.equal(getCooldownDurationMs(10), 86_400_000);  // 24h
});

test('evaluateLockout returns proper locked state and countdown', () => {
  const now = 1_000_000;
  // 2 failures: not locked
  const r1 = evaluateLockout(2, now - 1000, now);
  assert.equal(r1.isLockedOut, false);
  assert.equal(r1.remainingSeconds, 0);

  // 3 failures: 30s cooldown
  const r2 = evaluateLockout(3, now, now + 10_000); // 10s elapsed out of 30s
  assert.equal(r2.isLockedOut, true);
  assert.equal(r2.remainingSeconds, 20);

  // After 31s: expired
  const r3 = evaluateLockout(3, now, now + 31_000);
  assert.equal(r3.isLockedOut, false);
});

test('validateNextDnsProfile recognizes valid and invalid hostnames', () => {
  const valid = validateNextDnsProfile('a1b2c3.dns.nextdns.io');
  assert.equal(valid.isValid, true);
  assert.equal(valid.profileId, 'a1b2c3');

  const invalid = validateNextDnsProfile('google.com');
  assert.equal(invalid.isValid, false);
});
