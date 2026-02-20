/**
 * Mock Data Index
 * Exports all mock data and utilities for AURA platform testing
 *
 * BYPASS LOGIN CREDENTIALS:
 * Email: test@aura.dev
 * Password: aura123
 */

import MockProvider from './MockProvider';
import { BYPASS_CREDENTIALS as MOCK_BYPASS_CREDENTIALS } from './mockUsers';

export * from './mockUsers';
export * from './mockAura';
export * from './MockProvider';

// Re-export for convenience
export { MockProvider };

// Quick reference for test credentials
export const TEST_CREDENTIALS = {
  email: MOCK_BYPASS_CREDENTIALS.email,
  password: MOCK_BYPASS_CREDENTIALS.password,
  hint: 'Use these credentials to bypass auth and test AURA features',
};

export default {
  ...MockProvider,
  TEST_CREDENTIALS,
};
