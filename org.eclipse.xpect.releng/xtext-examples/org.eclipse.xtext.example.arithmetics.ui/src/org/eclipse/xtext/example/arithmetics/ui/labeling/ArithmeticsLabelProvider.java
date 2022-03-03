/**
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.xtext.example.arithmetics.ui.labeling;

import com.google.inject.Inject;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.xtext.example.arithmetics.arithmetics.DeclaredParameter;
import org.eclipse.xtext.example.arithmetics.arithmetics.Definition;
import org.eclipse.xtext.ui.label.DefaultEObjectLabelProvider;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

/**
 * Provides labels for a EObjects.
 * 
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#label-provider
 */
@SuppressWarnings("all")
public class ArithmeticsLabelProvider extends DefaultEObjectLabelProvider {
  @Inject
  public ArithmeticsLabelProvider(final AdapterFactoryLabelProvider delegate) {
    super(delegate);
  }
  
  public String text(final org.eclipse.xtext.example.arithmetics.arithmetics.Module ele) {
    return ele.getName();
  }
  
  public String text(final Definition ele) {
    String _name = ele.getName();
    final Function1<DeclaredParameter, CharSequence> _function = (DeclaredParameter it) -> {
      return it.getName();
    };
    String _join = IterableExtensions.<DeclaredParameter>join(ele.getArgs(), "(", ",", ")", _function);
    return (_name + _join);
  }
  
  public String image(final org.eclipse.xtext.example.arithmetics.arithmetics.Module ele) {
    return "home_nav.gif";
  }
}
