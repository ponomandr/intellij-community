/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.ui.popup;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.ui.ActiveComponent;
import com.intellij.util.BooleanFunction;
import com.intellij.util.Processor;
import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * User: anna
 * Date: 15-Mar-2006
 */
public class ComponentPopupBuilderImpl implements ComponentPopupBuilder {
  private String myTitle = "";
  private boolean myResizable;
  private boolean myMovable;
  private final JComponent myComponent;
  private final JComponent myPreferredFocusedComponent;
  private boolean myRequestFocus;
  private boolean myForceHeavyweight;
  private String myDimensionServiceKey = null;
  private Computable<Boolean> myCallback = null;
  private Project myProject;
  private boolean myCancelOnClickOutside = true;
  private boolean myCancelOnWindowDeactivation = true;
  private final Set<JBPopupListener> myListeners = new LinkedHashSet<JBPopupListener>();
  private boolean myUseDimServiceForXYLocation;

  private IconButton myCancelButton;
  private MouseChecker myCancelOnMouseOutCallback;
  private boolean myCancelOnWindow;
  private ActiveIcon myTitleIcon = new ActiveIcon(EmptyIcon.ICON_0);
  private boolean myCancelKeyEnabled = true;
  private boolean myLocateByContent = false;
  private boolean myPlacewithinScreen = true;
  private Processor<JBPopup> myPinCallback = null;
  private Dimension myMinSize;
  private MaskProvider myMaskProvider;
  private float myAlpha;
  private ArrayList<Object> myUserData;

  private boolean myInStack = true;
  private boolean myModalContext = true;
  private Component[] myFocusOwners = new Component[0];

  private String myAd;
  private boolean myShowShadow = true;
  private boolean myFocusable = true;
  private boolean myHeaderAlwaysFocusable;
  private ActiveComponent myCommandButton;
  private List<Pair<ActionListener, KeyStroke>> myKeyboardActions = Collections.emptyList();
  private Component mySettingsButtons;
  private boolean myMayBeParent;
  private int myAdAlignment = SwingConstants.LEFT;
  private BooleanFunction<KeyEvent> myKeyEventHandler;

  public ComponentPopupBuilderImpl(@NotNull JComponent component,
                                   final JComponent preferredFocusedComponent) {
    myComponent = component;
    myPreferredFocusedComponent = preferredFocusedComponent;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setMayBeParent(boolean mayBeParent) {
    myMayBeParent = mayBeParent;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setTitle(String title) {
    myTitle = title;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setResizable(final boolean resizable) {
    myResizable = resizable;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setMovable(final boolean movable) {
    myMovable = movable;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setCancelOnClickOutside(final boolean cancel) {
    myCancelOnClickOutside = cancel;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setCancelOnMouseOutCallback(final MouseChecker shouldCancel) {
    myCancelOnMouseOutCallback = shouldCancel;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder addListener(final JBPopupListener listener) {
    myListeners.add(listener);
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setRequestFocus(final boolean requestFocus) {
    myRequestFocus = requestFocus;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setFocusable(final boolean focusable) {
    myFocusable = focusable;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setForceHeavyweight(final boolean forceHeavyweight) {
    myForceHeavyweight = forceHeavyweight;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setDimensionServiceKey(final Project project, final String dimensionServiceKey, final boolean useForXYLocation) {
    myDimensionServiceKey = dimensionServiceKey;
    myUseDimServiceForXYLocation = useForXYLocation;
    myProject = project;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setCancelCallback(final Computable<Boolean> shouldProceed) {
    myCallback = shouldProceed;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setCancelButton(@NotNull final IconButton cancelButton) {
    myCancelButton = cancelButton;
    return this;
  }
  @Override
  @NotNull
  public ComponentPopupBuilder setCommandButton(@NotNull ActiveComponent button) {
    myCommandButton = button;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setCouldPin(@Nullable final Processor<JBPopup> callback) {
    myPinCallback = callback;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setKeyboardActions(@NotNull List<Pair<ActionListener, KeyStroke>> keyboardActions) {
    myKeyboardActions = keyboardActions;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setSettingButtons(@NotNull Component button) {
    mySettingsButtons = button;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setCancelOnOtherWindowOpen(final boolean cancelOnWindow) {
    myCancelOnWindow = cancelOnWindow;
    return this;
  }

  @Override
  public ComponentPopupBuilder setCancelOnWindowDeactivation(boolean cancelOnWindowDeactivation) {
    myCancelOnWindowDeactivation = cancelOnWindowDeactivation;
    return this;
  }

  @NotNull
  @Override
  public ComponentPopupBuilder setKeyEventHandler(@NotNull BooleanFunction<KeyEvent> handler) {
    myKeyEventHandler = handler; 
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setProject(Project project) {
    myProject = project;
    return this;
  }

  @Override
  @NotNull
  public JBPopup createPopup() {
    final AbstractPopup popup = new AbstractPopup().init(myProject, myComponent, myPreferredFocusedComponent, myRequestFocus, myFocusable, myForceHeavyweight,
                                              myMovable, myDimensionServiceKey, myResizable, myTitle,
                                              myCallback, myCancelOnClickOutside, myListeners, myUseDimServiceForXYLocation, myCommandButton,
                                              myCancelButton,
                                              myCancelOnMouseOutCallback, myCancelOnWindow, myTitleIcon, myCancelKeyEnabled, myLocateByContent,
                                              myPlacewithinScreen, myMinSize, myAlpha, myMaskProvider, myInStack, myModalContext, myFocusOwners, myAd, myAdAlignment,
                                              myHeaderAlwaysFocusable, myKeyboardActions, mySettingsButtons, myPinCallback, myMayBeParent,
                                              myShowShadow, myCancelOnWindowDeactivation, myKeyEventHandler);
    if (myUserData != null) {
      popup.setUserData(myUserData);
    }
    //default disposable parent
    Disposer.register(ApplicationManager.getApplication(), popup);
    return popup;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setRequestFocusCondition(Project project, Condition<Project> condition) {
    myRequestFocus = condition.value(project);
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setTitleIcon(@NotNull final ActiveIcon icon) {
    myTitleIcon = icon;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setCancelKeyEnabled(final boolean enabled) {
    myCancelKeyEnabled = enabled;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setLocateByContent(final boolean byContent) {
    myLocateByContent = byContent;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setLocateWithinScreenBounds(final boolean within) {
    myPlacewithinScreen = within;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setMinSize(final Dimension minSize) {
    myMinSize = minSize;
    return this;
  }

  @NotNull
  public ComponentPopupBuilder setMaskProvider(MaskProvider maskProvider) {
    myMaskProvider = maskProvider;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setAlpha(final float alpha) {
    myAlpha = alpha;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setBelongsToGlobalPopupStack(final boolean isInStack) {
    myInStack = isInStack;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder addUserData(final Object object) {
    if (myUserData == null) {
      myUserData = new ArrayList<Object>();
    }
    myUserData.add(object);
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setModalContext(final boolean modal) {
    myModalContext = modal;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setFocusOwners(@NotNull final Component[] focusOwners) {
    myFocusOwners = focusOwners;
    return this;
  }

  @Override
  @NotNull
  public ComponentPopupBuilder setAdText(@Nullable final String text) {
    return setAdText(text, SwingConstants.LEFT);
  }

  @NotNull
  @Override
  public ComponentPopupBuilder setAdText(@Nullable String text, int textAlignment) {
    myAd = text;
    myAdAlignment = textAlignment;
    return this;
  }

  @NotNull
  @Override
  public ComponentPopupBuilder setShowShadow(boolean show) {
    myShowShadow = show;
    return this;
  }
}
