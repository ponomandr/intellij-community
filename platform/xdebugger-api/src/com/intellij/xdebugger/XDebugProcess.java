/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.xdebugger;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import com.intellij.xdebugger.frame.XValueMarkerProvider;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.ui.XDebugTabLayouter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkListener;

/**
 * Extends this class to provide debugging capabilities for custom language/framework.
 *
 * In order to start debugger by 'Debug' action for a specific run configuration implement {@link com.intellij.execution.runners.ProgramRunner}
 * and call {@link XDebuggerManager#startSession} from {@link com.intellij.execution.runners.ProgramRunner#execute} method
 *
 * Otherwise use method {@link XDebuggerManager#startSessionAndShowTab} to start new debugging session
 *
 * @author nik
 */
public abstract class XDebugProcess {
  private final XDebugSession mySession;
  private ProcessHandler myProcessHandler;

  /**
   * @param session pass <code>session</code> parameter of {@link XDebugProcessStarter#start} method to this constructor
   */
  protected XDebugProcess(@NotNull XDebugSession session) {
    mySession = session;
  }

  public final XDebugSession getSession() {
    return mySession;
  }

  /**
   * @return breakpoint handlers which will be used to set/clear breakpoints in the underlying debugging process
   */
  public XBreakpointHandler<?>[] getBreakpointHandlers() {
    return XBreakpointHandler.EMPTY_ARRAY;
  }

  /**
   * @return editor provider which will be used to produce editors for "Evaluate" and "Set Value" actions
   */
  @NotNull
  public abstract XDebuggerEditorsProvider getEditorsProvider();

  /**
   * Called when {@link XDebugSession} is initialized and breakpoints are registered in
   * {@link com.intellij.xdebugger.breakpoints.XBreakpointHandler}
   */
  public void sessionInitialized() {
  }

  /**
   * Interrupt debugging process and call {@link XDebugSession#positionReached}
   * when next line in current method/function is reached.
   * Do not call this method directly. Use {@link XDebugSession#pause()} instead
   */
  public void startPausing() {
  }

  /**
   * Resume execution and call {@link XDebugSession#positionReached}
   * when next line in current method/function is reached.
   * Do not call this method directly. Use {@link XDebugSession#stepOver} instead
   */
  public abstract void startStepOver();

  /**
   * Resume execution and call {@link XDebugSession#positionReached}
   * when next line is reached.
   * Do not call this method directly. Use {@link XDebugSession#stepInto} instead
   */
  public abstract void startStepInto();

  /**
   * Resume execution and call {@link XDebugSession#positionReached}
   * after returning from current method/function.
   * Do not call this method directly. Use {@link XDebugSession#stepOut} instead
   */
  public abstract void startStepOut();

  /**
   * Implement {@link com.intellij.xdebugger.stepping.XSmartStepIntoHandler} and return its instance from this method to enable Smart Step Into action
   * @return {@link com.intellij.xdebugger.stepping.XSmartStepIntoHandler} instance
   */
  @Nullable
  public XSmartStepIntoHandler<?> getSmartStepIntoHandler() {
    return null;
  }

  /**
   * Stop debugging and dispose resources.
   * Do not call this method directly. Use {@link XDebugSession#stop} instead
   */
  public abstract void stop();

  /**
   * Resume execution.
   * Do not call this method directly. Use {@link XDebugSession#resume} instead
   */
  public abstract void resume();

  /**
   * Resume execution and call {@link XDebugSession#positionReached(com.intellij.xdebugger.frame.XSuspendContext)}
   * when <code>position</code> is reached.
   * Do not call this method directly. Use {@link XDebugSession#runToPosition} instead
   * @param position position in source code
   */
  public abstract void runToPosition(@NotNull XSourcePosition position);

  /**
   * Check is it is possible to perform commands such as resume, step etc. And notify user if necessary
   * @return {@code true} if process can actually perform user requests at this moment
   */
  public boolean checkCanPerformCommands() {
    return true;
  }

  @Nullable
  protected ProcessHandler doGetProcessHandler() {
    return null;
  }

  @NotNull
  public final ProcessHandler getProcessHandler() {
    if (myProcessHandler == null) {
      myProcessHandler = doGetProcessHandler();
      if (myProcessHandler == null) {
        myProcessHandler = new DefaultDebugProcessHandler();
      }
    }
    return myProcessHandler;
  }

  @NotNull
  public ExecutionConsole createConsole() {
    return TextConsoleBuilderFactory.getInstance().createBuilder(getSession().getProject()).getConsole();
  }

  /**
   * Override this method to enable 'Mark Object' action
   * @return new instance of {@link XValueMarkerProvider}'s implementation or {@code null} if 'Mark Object' feature isn't supported
   */
  @Nullable
  public XValueMarkerProvider<?,?> createValueMarkerProvider() {
    return null;
  }

  /**
   * @deprecated override {@link #createTabLayouter()} and {@link com.intellij.xdebugger.ui.XDebugTabLayouter#registerAdditionalContent} instead
   */
  @Deprecated
  public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
  }

  /**
   * Override this method to provide additional actions in 'Debug' tool window
   */
  public void registerAdditionalActions(@NotNull DefaultActionGroup leftToolbar, @NotNull DefaultActionGroup topToolbar) {
  }

  /**
   * @return message to show in Variables View when debugger isn't paused
   */
  public String getCurrentStateMessage() {
    return mySession.isStopped() ? XDebuggerBundle.message("debugger.state.message.disconnected") : XDebuggerBundle.message("debugger.state.message.connected");
  }

  @Nullable
  public HyperlinkListener getCurrentStateHyperlinkListener() {
    return null;
  }

  /**
   * Override this method to customize content of tab in 'Debug' tool window
   */
  @NotNull
  public XDebugTabLayouter createTabLayouter() {
    return new XDebugTabLayouter() {
      @Override
      public void registerAdditionalContent(@NotNull RunnerLayoutUi ui) {
        XDebugProcess.this.registerAdditionalContent(ui);
      }
    };
  }

  /**
   * Add or not SortValuesAction (alphabetically sort)
   * @todo this action should be moved to "Variables" as gear action
   */
  public boolean isValuesCustomSorted() {
    return false;
  }

}
