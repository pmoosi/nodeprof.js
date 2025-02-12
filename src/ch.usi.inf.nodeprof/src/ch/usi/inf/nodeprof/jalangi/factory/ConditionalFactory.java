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
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.BinaryEventHandler;
import ch.usi.inf.nodeprof.handlers.ConditionalEventHandler;
import com.oracle.truffle.js.nodes.cast.JSToBooleanUnaryNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTaggedExecutionNode;
import com.oracle.truffle.js.nodes.instrumentation.JSTags;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ConditionalFactory extends AbstractFactory {

    private boolean isValue = false;

    public ConditionalFactory(Object jalangiAnalysis, JSDynamicObject post, boolean isValue) {
        super("conditional", jalangiAnalysis, null, post);
        this.isValue = isValue;
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new ConditionalEventHandler(context, isValue) {
            @Child
            CallbackNode cbNode = new CallbackNode();

            @Override
            public Object executePost(VirtualFrame frame, Object result,
                                      Object[] inputs) throws InteropException {
                if (post != null) {
                    if (!isValue && isConditional()) {
                        Object input = getInputOrUndefined(0, inputs);
                        return cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), input, convertResult(result), false);
                    } else if (isValue) {
                        // ToDo - fix this (or remove it - it's not needed anymore)
                        // This is a workaround for non-insturmentable conditional nodes
                        // Specifically these are while loops with a boolean variable, function, ... as condition (e.g. while(someVar) {...})
//                        Node unaryWrapper = this.context.getInstrumentedNode().getParent().getParent();
//                        Node tagWrapper = unaryWrapper.getParent();
//                        if (unaryWrapper instanceof JSToBooleanUnaryNode
//                                && tagWrapper instanceof JSTaggedExecutionNode
//                                && ((JSTaggedExecutionNode) tagWrapper).hasTag(JSTags.ControlFlowBranchTag.class)) {
//                            return cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), convertResult(result), true);
//                        }
                    }
                }
                return null;
            }
        };
    }

}
