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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.Undefined;

import ch.usi.inf.nodeprof.handlers.BaseEventHandlerNode;
import ch.usi.inf.nodeprof.handlers.CFRootEventHandler;

public class AsyncRootFactory extends AbstractFactory {

    public AsyncRootFactory(Object jalangiAnalysis, JSDynamicObject pre, JSDynamicObject post) {
        super("async-root", jalangiAnalysis, pre, post);
    }

    @Override
    public BaseEventHandlerNode create(EventContext context) {
        return new CFRootEventHandler(context) {
            @Child CallbackNode cbNode = new CallbackNode();

            @Override
            public Object executePre(VirtualFrame frame, Object[] inputs) throws InteropException {
                if (pre != null && this.isAsyncRoot()) {
                    cbNode.preCall(this, jalangiAnalysis, pre, getSourceIID());
                }
                return null;
            }

            @Override
            public Object executePost(VirtualFrame frame, Object result,
                            Object[] inputs) throws InteropException {

                if (post != null && this.isAsyncRoot()) {
                    assert (result instanceof DynamicObject);
                    cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), result, createWrappedException(null));
                }

                return null;
            }

            @Override
            public Object executeExceptional(VirtualFrame frame, Throwable exception, Object[] inputs) throws InteropException {
                if (post != null) {
                    cbNode.postCall(this, jalangiAnalysis, post, getSourceIID(), Undefined.instance, createWrappedException(exception));
                }
                return null;
            }

            @Override
            public void executeExceptionalCtrlFlow(VirtualFrame frame, Throwable exception, Object[] inputs) {
                // TODO, to check if we need to deal with YieldException
            }
        };
    }
}
