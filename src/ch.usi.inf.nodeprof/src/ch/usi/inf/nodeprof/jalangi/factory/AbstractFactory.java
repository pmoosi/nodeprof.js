/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2022, Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * *****************************************************************************/
package ch.usi.inf.nodeprof.jalangi.factory;

import ch.usi.inf.nodeprof.analysis.AnalysisFactory;
import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.utils.GlobalObjectCache;
import ch.usi.inf.nodeprof.utils.Logger;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.nodes.control.YieldException;
import com.oracle.truffle.js.runtime.GraalJSException;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSInterruptedExecutionException;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSAbstractArray;
import com.oracle.truffle.js.runtime.builtins.JSArray;
import com.oracle.truffle.js.runtime.builtins.JSFunction;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.Null;
import com.oracle.truffle.js.runtime.objects.Undefined;

// a factory corresponds to a callback defined in Jalangi-like analysis
public abstract class AbstractFactory implements
        AnalysisFactory<BaseEventHandlerNode> {
    // the jalangi analysis object
    protected final Object jalangiAnalysis;

    protected final JSDynamicObject pre;
    protected final JSDynamicObject post;
    protected final JSDynamicObject enter;
    protected final JSDynamicObject onInput;
    protected final JSDynamicObject onException;


    protected final String jalangiCallback;

    private static final TruffleString YIELD_STR = Strings.constant("yield");
    private static final TruffleString EXCEPTION_STR = Strings.constant("exception");
    private static final TruffleString UNKNOWN_EXCEPTION_STR = Strings.constant("Unknown Exception");

    protected static boolean readBoolean(DynamicObject cb, String name) {
        Object ret = readCBProperty(cb, name);
        if (ret == null) {
            return false;
        } else {
            return ret instanceof Boolean && (Boolean) ret; // unchecked
        }
    }

    @TruffleBoundary
    protected static String readString(DynamicObject cb, String name) {
        Object ret = readCBProperty(cb, name);
        if (ret == null) {
            return null;
        } else {
            return ret.toString();
        }
    }

    @TruffleBoundary
    protected static Object[] readArray(DynamicObject cb, String name) {
        Object ret = readCBProperty(cb, name);
        if (JSArray.isJSArray(ret)) {
            return JSAbstractArray.toArray((JSDynamicObject) ret);
        }
        return null;
    }

    @TruffleBoundary
    protected static boolean isPropertyUndefined(DynamicObject cb, String name) {
        Object ret = readCBProperty(cb, name);
        if (ret == null) {
            return true;
        } else {
            return ret == Undefined.instance;
        }
    }

    protected static Object readCBProperty(DynamicObject cb, String name) {
        if (cb == null) {
            return null;
        }
        try {
            Object val = InteropLibrary.getFactory().getUncached(cb).readMember(cb, name);
            return val == null ? null : val;
        } catch (UnknownIdentifierException | UnsupportedMessageException e) {
            return null;
        }
    }

    public AbstractFactory(String jalangiCallback, Object jalangiAnalysis, JSDynamicObject pre,
                           JSDynamicObject post, JSDynamicObject onInput, JSDynamicObject onException, JSDynamicObject enter) {
        this.jalangiCallback = jalangiCallback;
        this.jalangiAnalysis = jalangiAnalysis;
        this.pre = pre;
        this.post = post;
        this.onInput = onInput;
        this.onException = onException;
        this.enter = enter;
    }

    public AbstractFactory(String jalangiCallback, Object jalangiAnalysis, JSDynamicObject pre,
                           JSDynamicObject post) {
        this(jalangiCallback, jalangiAnalysis, pre, post, null, null, null);
    }

    /**
     * Only interop type can be passed to JS
     *
     * @param result Object to be converted
     * @return the converted Object
     */
    public Object convertResult(Object result) {
        if (result == null) {
            return Null.instance;
        }
        return result;
    }

    @TruffleBoundary
    private static Object parseErrorObject(Throwable exception) {
        return exception instanceof GraalJSException ? ((GraalJSException) exception).getErrorObject() : Strings.fromJavaString(exception.getMessage());
    }

    @TruffleBoundary
    protected static Object createWrappedException(Throwable exception) {
        if (exception == null) {
            return GlobalObjectCache.getInstance().getEmptyWrappedException();
        } else {
            JSContext ctx = GlobalObjectCache.getInstance().getJSContext();
            JSRealm realm = JSRealm.get(null);
            JSDynamicObject wrapped = JSOrdinary.create(ctx, realm);
            if (exception instanceof YieldException) {
                JSObject.set(wrapped, YIELD_STR, true);
            } else {
                Object errObj = parseErrorObject(exception);
                JSObject.set(wrapped, EXCEPTION_STR, errObj == null ? UNKNOWN_EXCEPTION_STR : errObj);
            }
            return wrapped;
        }
    }

    /**
     * nestedControl is a tag to avoid instrumentation of the Jalangi analysis being called
     * recursively
     */
    private static boolean nestedControl = false;

    /**
     * @return true to proceed with the call or false to skip the call
     */
    private static boolean beforeCall() {
        if (nestedControl) {
            return false;
        }
        nestedControl = true;
        return true;
    }

    /**
     * afterCall to reset nestedControl
     */
    private static void afterCall() {
        nestedControl = false;
        return;
    }

    public class CallbackNode extends Node {
        @Node.Child
        DirectCallNode preCall = pre != null ? Truffle.getRuntime().createDirectCallNode(JSFunction.getCallTarget(pre)) : null;
        @Node.Child
        DirectCallNode postCall = post != null ? Truffle.getRuntime().createDirectCallNode(JSFunction.getCallTarget(post)) : null;

        @Node.Child
        DirectCallNode onInputCall = onInput != null ? Truffle.getRuntime().createDirectCallNode(JSFunction.getCallTarget(onInput)) : null;

        @Node.Child
        DirectCallNode onExceptionCall = onException != null ? Truffle.getRuntime().createDirectCallNode(JSFunction.getCallTarget(onException)) : null;

        @Node.Child
        DirectCallNode enterCall = enter != null ? Truffle.getRuntime().createDirectCallNode(JSFunction.getCallTarget(enter)) : null;

        @Child
        private InteropLibrary interopLibrary = InteropLibrary.getFactory().createDispatched(3);

        private void checkDeactivate(Object ret, BaseEventHandlerNode handler) {
            Object deactivate = readReturnMember(ret, "deactivate");
            if (deactivate == null) return;

            try {
                if (interopLibrary.asBoolean(deactivate)) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    handler.deactivate();
                }
            } catch (UnsupportedMessageException e) {
            }
        }

        private Object readReturnMember(Object obj, String prop) {
            if (obj == null || obj == Undefined.instance || !JSObject.isJSObject(obj)) return null;

            try {
                return interopLibrary.readMember(obj, prop);
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                return null;
            }
        }

        private Object readResult(Object ret) {
            // only read return __result prop if ret is no analysis proxy (else it might get injected)
            Object returnMember = this.readReturnMember(ret, "__isAnalysisProxy") == null ?
                    readReturnMember(ret, "result")
                    : null;

            if (returnMember != null) {
                return returnMember;
            } else if (ret != Undefined.instance) {
                return ret;
            }
            return null;
        }

        public Object preCall(BaseEventHandlerNode handler, Object... args) {
            assertNoStringLeak(args);

            if (pre == null || !beforeCall()) return null;

            try {
                Object ret = preCall.call(args);
                checkDeactivate(ret, handler);

                return readReturnMember(ret, "result");
//                return readResult(ret);
            } catch (JSInterruptedExecutionException e) {
                Logger.error("execution cancelled probably due to timeout");
                return null;
            } finally {
                afterCall();
            }
        }

        public Object postCall(BaseEventHandlerNode handler, Object... args) {
            assertNoStringLeak(args);
            if (post == null || !beforeCall()) return null;

            try {
                Object ret = postCall.call(args);
                checkDeactivate(ret, handler);

                // Get result from js call and return it to change node result if needed
                // Maybe this would be nicer to do in the specific factories for more fine-grained control
                return readReturnMember(ret, "result");

            } catch (JSInterruptedExecutionException e) {
                Logger.error("execution cancelled probably due to timeout");
                return null;
            } finally {
                afterCall();
            }

        }

        public Object onInputCall(BaseEventHandlerNode handler, Object... args) {
            assertNoStringLeak(args);
            if (onInput == null || !beforeCall()) return null;

            try {
                Object ret = onInputCall.call(args);
                checkDeactivate(ret, handler);
                return readReturnMember(ret, "result");
            } catch (JSInterruptedExecutionException e) {
                Logger.error("execution cancelled probably due to timeout");
                return null;
            } finally {
                afterCall();
            }
        }

        public Object onExceptionCall(BaseEventHandlerNode handler, Object... args) {
            assertNoStringLeak(args);
            if (onException == null || !beforeCall()) return null;

            try {
                Object ret = onExceptionCall.call(args);
                checkDeactivate(ret, handler);
                return readReturnMember(ret, "result");
            } catch (JSInterruptedExecutionException e) {
                Logger.error("execution cancelled probably due to timeout");
                return null;
            } finally {
                afterCall();
            }
        }

        public void enterCall(BaseEventHandlerNode handler, Object... args) {
            if (enter == null || !beforeCall()) return;

            try {
                Object ret = enterCall.call(args);
                checkDeactivate(ret, handler);
            } catch (JSInterruptedExecutionException e) {
                Logger.error("execution cancelled probably due to timeout");
            } finally {
                afterCall();
            }
        }

        private void assertNoStringLeak(Object[] args) {
            for (Object arg : args) {
                assert !(arg instanceof String);
            }
        }
    }
}
