package com.avanza.astrix.test;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class TestApiResetRule implements TestRule {

	private final AstrixRule astrixRule;

	public TestApiResetRule(AstrixRule astrixRule) {
		this.astrixRule = astrixRule;
	}

	@Override
	public Statement apply(final Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				astrixRule.resetTestApis();
				base.evaluate();
			}
		};
	}
}

