package com.thphatts.clinicportal;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration Test: Requires active DB connection (Supabase).
 * Run manually with environment variables set. Not part of CI unit test suite.
 */
@SpringBootTest
@Disabled("Integration test: requires real DB connection. Set SUPABASE_URL, SUPABASE_USER, SUPABASE_PASSWORD env vars.")
class ClinicPortalApplicationTests {

	@Test
	void contextLoads() {
	}

}
