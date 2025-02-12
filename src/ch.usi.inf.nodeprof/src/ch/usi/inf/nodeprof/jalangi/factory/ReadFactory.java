/* *****************************************************************************
 * Copyright 2018 Dynamic Analysis Group, Università della Svizzera Italiana (USI)
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.PropertyReadEventHandler;
import ch.usi.inf.nodeprof.handlers.VarReadEventHandler;
import com.oracle.truffle.js.nodes.cast.JSToBooleanUnaryNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.runtime.builtins.JSFunctionObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

public class ReadFactory extends AbstractFactory {

    private final boolean isProperty;

    public ReadFactory(Object jalangiAnalysis, JSDynamicObject post,
                       boolean isProperty) {
        super("read", jalangiAnalysis, null, post);
        this.isProperty = isProperty;
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        if (!isProperty) {
            return new VarReadEventHandler(context) {
                @Child
                CallbackNode cbNode = new CallbackNode();

                @Override
                public Object executePost(VirtualFrame frame, Object result,
                                          Object[] inputs) throws InteropException {
                    if (post == null) return null;

                    // TODO, isScriptLocal is set true here
                    return cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), getName(), convertResult(result), false, true);
                }
            };
        } else {
            return new PropertyReadEventHandler(context) {
                @Child
                CallbackNode cbNode = new CallbackNode();

                @Override
                public Object executePost(VirtualFrame frame, Object result,
                                          Object[] inputs) throws InteropException {
                    if (post != null && this.isGlobal(inputs)) {
                        return cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), getProperty(), convertResult(result), true, true);
                    }
                    return null;
                }

            };
        }
    }

}
