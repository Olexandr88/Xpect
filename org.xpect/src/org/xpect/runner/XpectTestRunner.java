/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.xpect.runner;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

import junit.framework.AssertionFailedError;

import org.eclipse.emf.common.util.EList;
import org.junit.runner.Description;
import org.xpect.XjmXpectMethod;
import org.xpect.XpectArgument;
import org.xpect.XpectInvocation;
import org.xpect.setup.ThisArgumentType;
import org.xpect.setup.ThisTestObject;
import org.xpect.state.Configuration;
import org.xpect.state.Creates;
import org.xpect.state.Managed;
import org.xpect.state.ResolvedConfiguration;
import org.xpect.state.StateContainer;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Primitives;

/**
 * Runs a single test by retrieving arguments from parsers and state container. In order to call a test method, the arguments are to be computed. This is done via
 * {@link StateContainer#tryGet(Class, Object...)}.
 *
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class XpectTestRunner extends AbstractTestRunner {
	private final XpectInvocation invocation;
	private final StateContainer state;

	public XpectTestRunner(StateContainer state, XpectFileRunner uriRunner, XpectInvocation invocation) {
		super(uriRunner);
		Preconditions.checkNotNull(invocation);
		this.invocation = invocation;
		this.state = state;
	}

	protected Object cast(Managed<?> value, XpectArgument expectedType) {
		if (value == null)
			throw new RuntimeException("Could not create value for " + expectedType.toString(true, true));
		Class<?> exactType = expectedType.getJavaType();
		Class<?> javaType = exactType.isPrimitive() ? Primitives.wrap(exactType) : exactType;
		if (javaType.isInstance(javaType))
			return javaType;
		Object object = value.get();
		if (object != null && !javaType.isInstance(object))
			throw new RuntimeException("Object of type " + object.getClass().getName() + " is not assignable to argument " + expectedType.toString(true, true));
		return object;
	}

	@Creates
	public XpectTestRunner create() {
		return this;
	}

	protected Configuration[] createArgumentConfigurations(XpectInvocation statement) {
		EList<XpectArgument> arguments = statement.getArguments();
		Configuration[] result = new Configuration[arguments.size()];
		for (int i = 0; i < arguments.size(); i++) {
			XpectArgument argument = arguments.get(i);
			Configuration configuration = new Configuration(argument.toString(false, true));
			configuration.addDefaultValue(XpectArgument.class, argument);
			configuration.addValue(ThisArgumentType.class, argument.getJavaType());
			result[i] = configuration;
		}
		return result;
	}

	protected StateContainer[] createArgumentStateContainers(ResolvedConfiguration[] configurations) {
		StateContainer[] result = new StateContainer[configurations.length];
		for (int i = 0; i < configurations.length; i++)
			result[i] = new StateContainer(state, configurations[i]);
		return result;
	}

	protected Object[] createArgumentValues(StateContainer[] stateContainers) {
		EList<XpectArgument> arguments = invocation.getArguments();
		Object[] result = new Object[stateContainers.length];
		for (int i = 0; i < stateContainers.length; i++) {
			StateContainer state = stateContainers[i];
			XpectArgument argument = arguments.get(i);
			try {
				Class<?> keyType = argument.getJavaType();
				Annotation keyAnnotation = argument.getStateAnnotation();
				Managed<?> managed = keyAnnotation != null ? state.get(keyType, keyAnnotation) : state.get(keyType);
				result[i] = cast(managed, argument);
			} catch (Throwable t) {
				throw new RuntimeException("Error creating value for argument " + argument.toString(true, true), t);
			}
		}
		return result;
	}

	public Description createDescription() {
		XpectRunner runner = getFileRunner().getRunner();
		Class<?> javaClass = runner.getTestClass().getJavaClass();
		Description description = DescriptionFactory.createTestDescription(javaClass, runner.getUriProvider(), invocation);
		return description;
	}

	public XpectInvocation getInvocation() {
		return invocation;
	}

	public XjmXpectMethod getMethod() {
		return invocation.getMethod();
	}

	@Override
	public StateContainer getState() {
		return state;
	}

	@Override
	protected boolean isIgnore() {
		return invocation.getFile().isIgnore() || invocation.isIgnore() || super.isIgnore();
	}

	protected ResolvedConfiguration[] resolveArgumentConfiguration(Configuration[] configurations) {
		ResolvedConfiguration[] result = new ResolvedConfiguration[configurations.length];
		for (int i = 0; i < configurations.length; i++)
			result[i] = new ResolvedConfiguration(state.getConfiguration(), configurations[i]);
		return result;
	}

	@Override
	protected void runInternal() throws Throwable {
		Object test = state.get(Object.class, ThisTestObject.class).get();
		boolean fixmeMessage = false;
		try {
			ArgumentContributor contributor = state.get(ArgumentContributor.class).get();
			Configuration[] configurations = createArgumentConfigurations(invocation);
			contributor.contributeArguments(configurations);
			ResolvedConfiguration[] resolved = resolveArgumentConfiguration(configurations);
			StateContainer[] states = createArgumentStateContainers(resolved);
			try {
				Object[] args = createArgumentValues(states);

				getMethod().getJavaMethod().invoke(test, args);
				// reaching this point implies that no exception was thrown, hence the test passes.
				if (invocation.isFixme()) {
					fixmeMessage = true;
					throw new InvocationTargetException(new AssertionFailedError("Congrats, this FIXME test is suddenly fixed!"));
				}
			} finally {
				for (StateContainer state : states) {
					state.invalidate();
				}
			}
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (invocation.isFixme() && !fixmeMessage) {
				// FIXME-tests pass when they throw an exception
			} else {
				throw cause;
			}
		}
	}

}
