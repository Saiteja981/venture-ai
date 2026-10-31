import { test, expect } from '@playwright/test';

const API_URL = 'http://localhost:8082/api';
const TEST_PASSWORD = 'Password123!';

test.describe('Venture AI Golden Path', () => {
    let adminEmail, startupEmail, investorEmail;

    // 1. Generate dynamic emails and register them BEFORE the tests run
    test.beforeAll(async ({ request }) => {
        const ts = Date.now();
        adminEmail = `admin_${ts}@test.com`;
        startupEmail = `startup_${ts}@test.com`;
        investorEmail = `investor_${ts}@test.com`;

        const reg = (email, role) =>
            request.post(`${API_URL}/auth/register`, {
                data: { name: `E2E ${role}`, email, password: TEST_PASSWORD, role },
            });

        const adminRes = await reg(adminEmail, 'ADMIN');
        const startupRes = await reg(startupEmail, 'STARTUP');
        const investorRes = await reg(investorEmail, 'INVESTOR');

        if (!adminRes.ok() || !startupRes.ok() || !investorRes.ok()) {
            throw new Error('Failed to create test users via API. Is the backend running on 8082?');
        }
    });

    test('Admin can login and view dashboard', async ({ page }) => {
        await page.goto('/login');
        await page.fill('input[name="email"]', adminEmail);
        await page.fill('input[name="password"]', TEST_PASSWORD);
        await page.click('button[type="submit"]');

        // Wait for navigation to dashboard
        await page.waitForURL('**/admin/dashboard', { timeout: 10000 });
        await expect(page.locator('h1')).toContainText('Admin Dashboard');
    });

    test('Startup can login and access AI Prediction', async ({ page }) => {
        await page.goto('/login');
        await page.fill('input[name="email"]', startupEmail);
        await page.fill('input[name="password"]', TEST_PASSWORD);
        await page.click('button[type="submit"]');

        await page.waitForURL('**/startup/dashboard', { timeout: 10000 });

        // Navigate to AI Potential page
        await page.click('text=AI Potential & Risk');
        await page.waitForURL('**/startup/ai-potential', { timeout: 10000 });
        await expect(page.locator('h1')).toContainText('AI Potential & Risk');
    });

    test('Investor can access Settings and Report Export', async ({ page }) => {
        await page.goto('/login');
        await page.fill('input[name="email"]', investorEmail);
        await page.fill('input[name="password"]', TEST_PASSWORD);
        await page.click('button[type="submit"]');

        await page.waitForURL('**/investor/dashboard', { timeout: 10000 });

        // Navigate to Settings
        await page.click('a[href="/investor/settings"]');
        await page.waitForURL('**/investor/settings', { timeout: 10000 });

        await expect(page.locator('text=Reports & Export')).toBeVisible();
    });
});