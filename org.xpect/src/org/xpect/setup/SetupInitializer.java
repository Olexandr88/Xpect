/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.xpect.setup;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.util.JavaReflectAccess;
import org.xpect.AbstractComponent;
import org.xpect.Assignment;
import org.xpect.BooleanLiteral;
import org.xpect.ClassLiteral;
import org.xpect.Component;
import org.xpect.IntLiteral;
import org.xpect.StringLiteral;
import org.xpect.Value;

import com.google.common.base.Joiner;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
@SuppressWarnings("restriction")
public class SetupInitializer<T> implements ISetupInitializer<T> {

	private final AbstractComponent rootInstance;

	public SetupInitializer(AbstractComponent rootInstance) {
		this.rootInstance = rootInstance;
	}

	protected Object create(BooleanLiteral val) {
		return val.isValue();
	}

	protected Object create(ClassLiteral val) {
		return new JavaReflectAccess().getRawType(val.getType());
	}

	protected Constructor<?> findConstructor(Class<?> clazz, Object[] params) {
		START: for (Constructor<?> c : clazz.getConstructors())
			if (c.getParameterTypes().length == params.length) {
				for (int i = 0; i < params.length; i++)
					if (!c.getParameterTypes()[i].isInstance(params[i]))
						continue START;
				return c;
			}
		throw new RuntimeException("Type " + clazz + " has no constructor suitable for params " + Joiner.on(", ").join(params));
	}

	protected Object create(Component val) {
		Class<?> type = new JavaReflectAccess().getRawType(val.getComponentClass());
		try {
			Object[] params = new Object[val.getParameters().size()];
			for (int i = 0; i < val.getParameters().size(); i++)
				params[i] = create(val.getParameters().get(i));
			Constructor<?> constructor = findConstructor(type, params);
			Object result = constructor.newInstance(params);
			initialize(result, val);
			return result;
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	protected Object create(IntLiteral val) {
		return val.getValue();
	}

	protected Object create(StringLiteral val) {
		return val.getValue();
	}

	protected Object create(Value val) {
		if (val instanceof Component)
			return create((Component) val);
		if (val instanceof BooleanLiteral)
			return create((BooleanLiteral) val);
		if (val instanceof StringLiteral)
			return create((StringLiteral) val);
		if (val instanceof IntLiteral)
			return create((IntLiteral) val);
		if (val instanceof ClassLiteral)
			return create((ClassLiteral) val);
		return null;
	}

	public AbstractComponent getRootInstance() {
		return rootInstance;
	}

	protected void initialize(Object obj, AbstractComponent init) {
		JavaReflectAccess jra = new JavaReflectAccess();
		for (Assignment a : init.getAssignments()) {
			JvmOperation target = a.getTarget();
			if (target != null) {
				Method m = jra.getMethod(target);
				Object object = create(a.getValue());
				try {
					m.invoke(obj, object);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public void initialize(T object) {
		if (rootInstance != null)
			initialize(object, rootInstance);
	}
}
